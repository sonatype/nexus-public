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
package org.sonatype.nexus.onboarding.internal;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.security.JwtHelper;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * JWT-mode {@link OnboardingSessionRefresher}: mints a fresh JWT cookie so the caller's next
 * request presents a token with {@code iat >= cutoff} recorded by the preceding
 * {@link org.sonatype.nexus.security.session.SessionInvalidator} call. {@code
 * JwtSecurityFilter} treats {@code iat == cutoff} as valid (strict {@code revoked_at > iat} in
 * {@code JwtSessionDAO.xml}), so no ordering workaround is required.
 */
@Component
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
class JwtOnboardingSessionRefresher
    implements OnboardingSessionRefresher
{
  private static final Logger log = LoggerFactory.getLogger(JwtOnboardingSessionRefresher.class);

  private final JwtHelper jwtHelper;

  JwtOnboardingSessionRefresher(final JwtHelper jwtHelper) {
    this.jwtHelper = checkNotNull(jwtHelper);
  }

  @Override
  public void refreshIfSelfChange(
      final String userId,
      final String newPassword,
      final HttpServletRequest request,
      final HttpServletResponse response)
  {
    Subject subject = SecurityUtils.getSubject();
    if (subject == null || subject.getPrincipal() == null
        || !userId.equals(subject.getPrincipal().toString())) {
      return;
    }
    try {
      Cookie freshCookie = jwtHelper.createJwtCookie(subject, request.isSecure());
      response.addCookie(freshCookie);
      log.info("Issued fresh JWT for onboarding self-password-change by user '{}'", userId);
    }
    catch (Exception e) {
      log.warn("Failed to issue fresh JWT for onboarding self-password-change by user '{}': {}",
          userId, e.getMessage());
    }
  }
}
