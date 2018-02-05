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
package org.sonatype.nexus.internal.httpclient;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.httpclient.HttpClientPlan;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfigurationChangedEvent;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HttpClientManagerImpl}.
 */
public class HttpClientManagerImplTest
    extends TestSupport
{

  @Mock
  private EventManager eventManager;

  @Mock
  private HttpClientConfigurationStore configStore;

  @Mock
  private SharedHttpClientConnectionManager connectionManager;

  @Mock
  private DefaultsCustomizer defaultsCustomizer;

  @Mock
  private HttpClientConfigurationEvent configEvent;

  private HttpClientManagerImpl underTest;

  @Before
  public void setUp() {
    underTest = new HttpClientManagerImpl(eventManager, configStore, HttpClientConfiguration::new, connectionManager,
        defaultsCustomizer);
  }

  @Test
  public void testPrepareUserAgentHeaderSetOnBuilder() {
    // Setup
    String expectedUserAgentHeader = "Nexus/Agent my user agent";
    HttpClientPlan plan = mock(HttpClientPlan.class);
    doReturn(expectedUserAgentHeader).when(plan).getUserAgent();
    HttpClientBuilder builder = mock(HttpClientBuilder.class);
    doReturn(builder).when(plan).getClient();

    ConnectionConfig.Builder conn = mock(ConnectionConfig.Builder.class);
    SocketConfig.Builder sock = mock(SocketConfig.Builder.class);
    RequestConfig.Builder req = mock(RequestConfig.Builder.class);
    doReturn(null).when(conn).build();
    doReturn(null).when(sock).build();
    doReturn(null).when(req).build();
    doReturn(conn).when(plan).getConnection();
    doReturn(sock).when(plan).getSocket();
    doReturn(req).when(plan).getRequest();

    HttpClientManagerImpl spy = spy(underTest);
    doReturn(plan).when(spy).httpClientPlan();

    // Execute
    HttpClientBuilder returned = spy.prepare(null);

    // Verify
    assertNotNull("Returned builder must not be null.", returned);
    assertEquals("Returned builder must be expected builder.", builder, returned);
    verify(spy).setUserAgent(builder, expectedUserAgentHeader);
  }

  @Test
  public void testOnStoreChanged_LocalEvent() {
    when(configEvent.isLocal()).thenReturn(true);
    underTest.onStoreChanged(configEvent);
    verifyZeroInteractions(eventManager, configStore);
  }

  @Test
  public void testOnStoreChanged_RemoteEvent() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    when(configStore.load()).thenReturn(config);
    when(configEvent.isLocal()).thenReturn(false);
    when(configEvent.getRemoteNodeId()).thenReturn("remote-node-id");
    underTest.onStoreChanged(configEvent);
    ArgumentCaptor<HttpClientConfigurationChangedEvent> eventCaptor = ArgumentCaptor
        .forClass(HttpClientConfigurationChangedEvent.class);
    verify(eventManager).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getConfiguration(), is(config));
  }
}
