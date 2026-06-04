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
package org.sonatype.nexus.internal.jwt.datastore;

import java.time.OffsetDateTime;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;
import org.sonatype.nexus.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Named;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * Store for managing revoked JWT sessions.
 */
@Named
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
public class JwtSessionStore
    extends ConfigStoreSupport<JwtSessionDAO>
    implements JwtSessionRevocationService
{
  @Autowired
  public JwtSessionStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  /**
   * Revoke a JWT session by recording its userSessionId.
   *
   * @param userSessionId the unique session identifier from JWT claim
   * @param username the username associated with the session
   * @param userSource the user source/realm (e.g., "default", "LDAP", "SAML")
   * @param expiresAt when the JWT expires
   */
  @Transactional
  public void revokeSession(
      final String userSessionId,
      final String username,
      final String userSource,
      final OffsetDateTime expiresAt)
  {
    log.debug("Revoking JWT session in database: sessionId={}, username={}, userSource={}, expiresAt={}",
        userSessionId, username, userSource, expiresAt);

    JwtSessionData session = new JwtSessionData();
    session.setUserSessionId(userSessionId);
    session.setUsername(username);
    session.setUserSource(userSource);
    session.setRevokedAt(OffsetDateTime.now());
    session.setExpiresAt(expiresAt);

    dao().revokeSession(session);

    log.trace("Successfully recorded JWT session revocation in database for sessionId={}", userSessionId);
  }

  /**
   * Check if a JWT session is revoked.
   *
   * @param userSessionId the unique session identifier from JWT claim
   * @return true if the session is revoked, false otherwise
   */
  @Transactional
  public boolean isRevoked(final String userSessionId) {
    if (userSessionId == null || userSessionId.isEmpty()) {
      log.trace("isRevoked check skipped - userSessionId is null or empty");
      return false;
    }

    boolean revoked = dao().isRevoked(userSessionId);
    log.trace("JWT session revocation check: sessionId={}, isRevoked={}", userSessionId, revoked);

    return revoked;
  }

  /**
   * Delete expired session revocations that are past their JWT expiration time.
   * This should be called periodically to prevent table bloat.
   *
   * @return the number of expired revocations deleted
   */
  @Transactional
  public int deleteExpiredSessions() {
    log.debug("Starting cleanup of expired JWT session revocations");
    int deleted = dao().deleteExpiredSessions();

    if (deleted > 0) {
      log.debug("Deleted {} expired JWT session revocation records from database", deleted);
    }
    else {
      log.trace("No expired JWT session revocations found to delete");
    }

    return deleted;
  }
}
