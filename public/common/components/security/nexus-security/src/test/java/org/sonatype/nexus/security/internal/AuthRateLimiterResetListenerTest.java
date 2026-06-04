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

import java.util.Arrays;

import org.sonatype.nexus.security.authc.AuthRateLimiterService;
import org.sonatype.nexus.security.authc.UserPasswordChanged;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AuthRateLimiterResetListenerTest
{
  @Mock
  private AuthRateLimiterService rateLimiterService;

  private AuthRateLimiterResetListener listener;

  @Before
  public void setUp() {
    listener = new AuthRateLimiterResetListener(rateLimiterService);
  }

  @Test
  public void testClass_hasConditionalOnPropertyForEnabledFlag() {
    ConditionalOnProperty annotation = AuthRateLimiterResetListener.class.getAnnotation(ConditionalOnProperty.class);

    assertThat("@ConditionalOnProperty must be present", annotation, is(notNullValue()));
    assertThat(Arrays.asList(annotation.name()), hasItem("nexus.auth.ratelimit.enabled"));
    assertThat(annotation.havingValue(), is("true"));
    assertThat(annotation.matchIfMissing(), is(true));
  }

  @Test
  public void testOnUserUpdated_resetsRateLimitCounter() {
    User user = new User();
    user.setUserId("jsmith");

    listener.on(new UserUpdatedEvent(user));

    verify(rateLimiterService).reset("jsmith");
  }

  @Test
  public void testOnPasswordChanged_resetsRateLimitCounter() {
    listener.on(new UserPasswordChanged("jsmith"));

    verify(rateLimiterService).reset("jsmith");
  }
}
