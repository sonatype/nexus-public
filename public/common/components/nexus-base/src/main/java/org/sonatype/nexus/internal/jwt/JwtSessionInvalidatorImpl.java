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
package org.sonatype.nexus.internal.jwt;

import java.time.OffsetDateTime;
import java.util.Date;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.security.JwtHelper;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;
import org.sonatype.nexus.security.session.SessionInvalidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * JWT-mode session invalidator.
 *
 * <p>
 * Records a user-wide invalidation cutoff on password change. Because JWTs are stateless,
 * Nexus does not track active JWTs; instead {@code JwtSecurityFilter} consults the cutoff
 * recorded here against each presented JWT's {@code iat} claim and rejects pre-cutoff tokens.
 * This invalidates every JWT ever issued to the user before the password change, on every
 * device, independent of which node processed the change.
 */
@Component
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
public class JwtSessionInvalidatorImpl
    implements SessionInvalidator
{
  private static final Logger log = LoggerFactory.getLogger(JwtSessionInvalidatorImpl.class);

  private final JwtSessionRevocationService revocationService;

  private final JwtHelper jwtHelper;

  private final AuditRecorder auditRecorder;

  public JwtSessionInvalidatorImpl(
      final JwtSessionRevocationService revocationService,
      final JwtHelper jwtHelper,
      final AuditRecorder auditRecorder)
  {
    this.revocationService = checkNotNull(revocationService);
    this.jwtHelper = checkNotNull(jwtHelper);
    this.auditRecorder = checkNotNull(auditRecorder);
  }

  @Override
  public int invalidateSessionsForUser(final String username, final String userSource) {
    log.info("Invalidating JWT sessions for user '{}' (source '{}') due to password change",
        username, userSource);

    try {
      OffsetDateTime cutoff = OffsetDateTime.now();
      OffsetDateTime validUntil = cutoff.plusSeconds(jwtHelper.getExpirySeconds());

      revocationService.invalidateUser(username, userSource, cutoff, validUntil);

      recordAuditEvent(username);
      return 1;
    }
    catch (Exception e) {
      log.error("Failed to record JWT user invalidation for user '{}' (source '{}'): {}",
          username, userSource, e.getMessage(), e);
      return 0;
    }
  }

  private void recordAuditEvent(final String username) {
    if (auditRecorder.isEnabled()) {
      AuditData data = new AuditData();
      data.setDomain("security.session");
      data.setType("password-change-invalidation");
      data.setContext(username);
      data.setTimestamp(new Date());
      data.setInitiator(username);

      data.getAttributes().put("username", username);
      data.getAttributes().put("sessionCount", "1");
      data.getAttributes().put("sessionType", "jwt");

      auditRecorder.record(data);
    }
  }
}
