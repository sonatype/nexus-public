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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;

/**
 * Shiro-mode {@link OnboardingSessionRefresher}: re-authenticates the current Shiro subject with
 * the new password so Shiro creates a fresh session and sets the JSESSIONID cookie on the
 * response.
 */
@Component
@ConditionalOnProperty(name = SESSION_ENABLED, havingValue = "true", matchIfMissing = true)
class ShiroOnboardingSessionRefresher
    implements OnboardingSessionRefresher
{
  private static final Logger log = LoggerFactory.getLogger(ShiroOnboardingSessionRefresher.class);

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
      subject.login(new UsernamePasswordToken(userId, newPassword));
      log.info("Refreshed Shiro session for onboarding self-password-change by user '{}'", userId);
    }
    catch (Exception e) {
      log.warn("Failed to refresh Shiro session for onboarding self-password-change by user '{}': {}",
          userId, e.getMessage());
    }
  }
}
