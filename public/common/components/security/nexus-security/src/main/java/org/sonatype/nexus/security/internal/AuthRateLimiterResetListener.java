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
package org.sonatype.nexus.security.internal;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.security.authc.AuthRateLimiterService;
import org.sonatype.nexus.security.authc.UserPasswordChanged;
import org.sonatype.nexus.security.user.UserUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.inject.Singleton;

import static org.sonatype.nexus.common.app.FeatureFlags.AUTH_RATE_LIMIT_ENABLED;

/**
 * Clears the rate-limit counter for a user when their account is updated by an administrator,
 * allowing a locked-out user to regain access without a server restart.
 */
@Component
@Singleton
@ConditionalOnProperty(name = AUTH_RATE_LIMIT_ENABLED, havingValue = "true", matchIfMissing = true)
public class AuthRateLimiterResetListener
    implements EventAware
{
  private final AuthRateLimiterService rateLimiterService;

  public AuthRateLimiterResetListener(final AuthRateLimiterService rateLimiterService) {
    this.rateLimiterService = rateLimiterService;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final UserUpdatedEvent event) {
    rateLimiterService.reset(event.getUser().getUserId());
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final UserPasswordChanged event) {
    rateLimiterService.reset(event.getUserId());
  }
}
