/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.security.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.nexus.NexusWebSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;

/**
 * Session-based invalidator for Shiro sessions.
 * Invalidates cached sessions when a user's password is changed.
 */
@Component
@ConditionalOnProperty(name = SESSION_ENABLED, havingValue = "true", matchIfMissing = true)
public class NexusSessionInvalidatorImpl
    implements SessionInvalidator
{
  private static final Logger log = LoggerFactory.getLogger(NexusSessionInvalidatorImpl.class);

  private static final String PRINCIPALS_SESSION_KEY =
      "org.apache.shiro.subject.support.DefaultSubjectContext_PRINCIPALS_SESSION_KEY";

  private final NexusWebSessionManager sessionManager;

  private final AuditRecorder auditRecorder;

  public NexusSessionInvalidatorImpl(
      final NexusWebSessionManager sessionManager,
      final AuditRecorder auditRecorder)
  {
    this.sessionManager = checkNotNull(sessionManager);
    this.auditRecorder = checkNotNull(auditRecorder);
  }

  @Override
  public int invalidateSessionsForUser(final String username, final String userSource, final String reason) {
    log.info("Invalidating sessions for user '{}' (source '{}') due to {}", username, userSource, reason);

    try {
      SessionDAO sessionDAO = sessionManager.getSessionDAO();
      if (!(sessionDAO instanceof CachingSessionDAO)) {
        log.debug("SessionDAO is not a CachingSessionDAO, skipping session invalidation");
        return 0;
      }

      CachingSessionDAO cachingDAO = (CachingSessionDAO) sessionDAO;
      Cache<Serializable, Session> cache = cachingDAO.getActiveSessionsCache();

      if (cache == null) {
        log.debug("Active sessions cache is null, skipping session invalidation");
        return 0;
      }

      List<Session> toDelete = new ArrayList<>();
      for (Session session : cache.values()) {
        PrincipalCollection principals = getPrincipalsFromSession(session);
        if (principals != null && username.equals(principals.getPrimaryPrincipal())) {
          toDelete.add(session);
        }
      }
      toDelete.forEach(sessionDAO::delete);

      int count = toDelete.size();
      log.info("Invalidated {} session(s) for user '{}'", count, username);

      if (count > 0) {
        recordAuditEvent(username, count, reason);
      }

      return count;
    }
    catch (Exception e) {
      log.error("Failed to invalidate sessions for user '{}': {}", username, e.getMessage(), e);
      return 0;
    }
  }

  /**
   * Extract principals from a Shiro session.
   */
  private PrincipalCollection getPrincipalsFromSession(final Session session) {
    try {
      Object principals = session.getAttribute(PRINCIPALS_SESSION_KEY);
      if (principals instanceof PrincipalCollection) {
        return (PrincipalCollection) principals;
      }
    }
    catch (Exception e) {
      log.trace("Could not get principals from session: {}", e.getMessage());
    }
    return null;
  }

  private void recordAuditEvent(final String username, final int sessionCount, final String reason) {
    if (auditRecorder.isEnabled()) {
      AuditData data = new AuditData();
      data.setDomain("security.session");
      data.setType("user-session-invalidation");
      data.setContext(username);
      data.setTimestamp(new Date());
      data.setInitiator(username);

      data.getAttributes().put("username", username);
      data.getAttributes().put("sessionCount", String.valueOf(sessionCount));
      data.getAttributes().put("sessionType", "session");
      data.getAttributes().put("reason", reason);

      auditRecorder.record(data);
    }
  }
}
