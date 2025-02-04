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
package org.sonatype.nexus.internal.security;

import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.security.authc.AuthenticationEvent;
import org.sonatype.nexus.security.authc.AuthenticationFailureReason;
import org.sonatype.nexus.security.authc.NexusAuthenticationEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthenticationEventSubscriberTest
    extends TestSupport
{
  @Mock
  private ClientInfoProvider clientInfoProvider;

  @Mock
  private EventManager eventManager;

  @Captor
  private ArgumentCaptor<Object> eventArgumentCaptor;

  private AuthenticationEventSubscriber authenticationEventSubscriber;

  @Before
  public void setup() {
    authenticationEventSubscriber = new AuthenticationEventSubscriber(() -> eventManager, () -> clientInfoProvider);
  }

  @Test
  public void testOn_successWithClientInfo() {
    ClientInfo clientInfo =
        ClientInfo.builder()
            .path("/path/to/artifact")
            .remoteIP("remote ip")
            .userAgent("user agent string")
            .userId("user id")
            .build();
    when(clientInfoProvider.getCurrentThreadClientInfo()).thenReturn(clientInfo);
    AuthenticationEvent authenticationEvent = new AuthenticationEvent("user id", true);

    authenticationEventSubscriber.on(authenticationEvent);

    verify(eventManager).post(eventArgumentCaptor.capture());

    NexusAuthenticationEvent nexusAuthenticationEvent = (NexusAuthenticationEvent) eventArgumentCaptor.getValue();
    assertThat(nexusAuthenticationEvent.getAuthenticationFailureReasons().isEmpty(), is(true));
    assertThat(nexusAuthenticationEvent.isSuccessful(), is(true));
    assertThat(nexusAuthenticationEvent.getClientInfo(), is(clientInfo));
  }

  @Test
  public void testOn_successWithoutClientInfo() {
    AuthenticationEvent authenticationEvent = new AuthenticationEvent("user id", true);

    authenticationEventSubscriber.on(authenticationEvent);

    verify(eventManager).post(eventArgumentCaptor.capture());

    NexusAuthenticationEvent nexusAuthenticationEvent = (NexusAuthenticationEvent) eventArgumentCaptor.getValue();
    assertThat(nexusAuthenticationEvent.getAuthenticationFailureReasons().isEmpty(), is(true));
    assertThat(nexusAuthenticationEvent.isSuccessful(), is(true));
    assertThat(nexusAuthenticationEvent.getClientInfo().getUserid(), is(authenticationEvent.getUserId()));
  }

  @Test
  public void testOn_failedEvent() {
    AuthenticationEvent authenticationEvent = new AuthenticationEvent("user id", false,
        Collections.singleton(AuthenticationFailureReason.INCORRECT_CREDENTIALS));

    authenticationEventSubscriber.on(authenticationEvent);

    verify(eventManager).post(eventArgumentCaptor.capture());

    NexusAuthenticationEvent nexusAuthenticationEvent = (NexusAuthenticationEvent) eventArgumentCaptor.getValue();
    assertThat(nexusAuthenticationEvent.getAuthenticationFailureReasons().size(), is(1));
    assertThat(nexusAuthenticationEvent.isSuccessful(), is(false));
    assertThat(nexusAuthenticationEvent.getAuthenticationFailureReasons(),
        contains(AuthenticationFailureReason.INCORRECT_CREDENTIALS));
  }
}
