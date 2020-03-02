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
package org.sonatype.nexus.repository.httpclient.internal;

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.httpclient.AutoBlockConfiguration;
import org.sonatype.nexus.repository.httpclient.FilteredHttpClientSupport.Filterable;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusObserver;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType;
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config;

import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.joda.time.DateTime.now;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AUTO_BLOCKED_UNAVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.BLOCKED;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.OFFLINE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.READY;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.UNAVAILABLE;

public class BlockingHttpClientTest
    extends TestSupport
{
  @Mock
  CloseableHttpClient httpClient;

  @Mock
  RemoteConnectionStatusObserver statusObserver;

  @Mock
  Filterable filterable;

  @Mock
  CloseableHttpResponse httpResponse;

  @Mock
  StatusLine statusLine;
  
  @Mock
  AutoBlockConfiguration autoBlockConfiguration;

  HttpHost httpHost;

  BlockingHttpClient underTest;

  @Before
  public void setup() throws Exception {
    when(filterable.call()).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_OK);
    
    when(autoBlockConfiguration.shouldBlock(SC_UNAUTHORIZED)).thenReturn(true);
    when(autoBlockConfiguration.shouldBlock(SC_BAD_GATEWAY)).thenReturn(true);
    when(autoBlockConfiguration.shouldBlock(SC_PROXY_AUTHENTICATION_REQUIRED)).thenReturn(true);
    
    httpHost = HttpHost.create("localhost");
    underTest = new BlockingHttpClient(httpClient, new Config(), statusObserver, true, autoBlockConfiguration);
  }

  @After
  public void teardown() throws IOException {
    if (underTest != null) {
      underTest.close();
    }
  }

  @Test
  public void updateStatusWhenBlocked() throws Exception {
    Config config = new Config();
    config.blocked = true;
    reset(statusObserver);
    BlockingHttpClient client = new BlockingHttpClient(httpClient, config, statusObserver, true,
        autoBlockConfiguration);
    client.close();
    ArgumentCaptor<RemoteConnectionStatus> newStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    verify(statusObserver).onStatusChanged(any(), newStatusCaptor.capture());
    assertThat(newStatusCaptor.getValue().getType(), is(equalTo(BLOCKED)));
  }

  @Test
  public void autoblockWontLeaveStatusThreadInterrupted() throws Exception {
    when(httpClient.execute(any(HttpHost.class), any(), any(HttpContext.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_BAD_GATEWAY);

    Config config = new Config();
    config.autoBlock = true;

    boolean[] statusThreadLeftInterrupted = new boolean[1];

    BlockingHttpClient client = new BlockingHttpClient(httpClient, config,
        (oldStatus, newStatus) -> statusThreadLeftInterrupted[0] = Thread.currentThread().isInterrupted(),
        true, autoBlockConfiguration);

    client.scheduleCheckStatus(httpHost.toURI(), now().plusMillis(100));

    Thread.sleep(500);

    assertThat(statusThreadLeftInterrupted[0], is(false));

    client.close();
  }

  @Test
  public void updateStatusWhenAvailable() throws Exception {
    filterAndHandleException();
    verifyUpdateStatus(AVAILABLE);
  }

  @Test
  public void updateStatusWhenUnavailableDueToExceptionAndAutoBlocked() throws Exception {
    setInternalState(underTest, "autoBlock", true);
    when(filterable.call()).thenThrow(new IOException());
    filterAndHandleException();
    verifyUpdateStatus(AUTO_BLOCKED_UNAVAILABLE);
  }

  @Test
  public void updateStatusWhenUnavailableDueToUnauthorizedResponseAndAutoBlocked() throws Exception {
    setInternalState(underTest, "autoBlock", true);
    when(statusLine.getStatusCode()).thenReturn(SC_UNAUTHORIZED);
    filterAndHandleException();
    verifyUpdateStatus(AUTO_BLOCKED_UNAVAILABLE, "Unauthorized");
  }

  @Test
  public void updateStatusWhenUnavailableDueToProxyAuthRequiredResponseAndAutoBlocked() throws Exception {
    setInternalState(underTest, "autoBlock", true);
    when(statusLine.getStatusCode()).thenReturn(SC_PROXY_AUTHENTICATION_REQUIRED);
    filterAndHandleException();
    verifyUpdateStatus(AUTO_BLOCKED_UNAVAILABLE, "Proxy Authentication Required");
  }

  @Test
  public void updateStatusWhenUnavailableDueTo5xxResponseAndAutoBlocked() throws Exception {
    setInternalState(underTest, "autoBlock", true);
    when(statusLine.getStatusCode()).thenReturn(SC_BAD_GATEWAY);
    filterAndHandleException();
    verifyUpdateStatus(AUTO_BLOCKED_UNAVAILABLE, "Bad Gateway");
  }

  @Test
  public void updateStatusWhenUnavailableDueToException() throws Exception {
    when(filterable.call()).thenThrow(new IOException());
    filterAndHandleException();
    verifyUpdateStatus(UNAVAILABLE);
  }

  @Test
  public void updateStatusWhenUnavailableDueToUnauthorizedResponse() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(SC_UNAUTHORIZED);
    filterAndHandleException();
    verifyUpdateStatus(UNAVAILABLE, "Unauthorized");
  }

  @Test
  public void updateStatusWhenUnavailableDueToProxyAuthRequiredResponse() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(SC_PROXY_AUTHENTICATION_REQUIRED);
    filterAndHandleException();
    verifyUpdateStatus(UNAVAILABLE, "Proxy Authentication Required");
  }

  @Test
  public void updateStatusWhenUnavailableDueTo5xxResponse() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(SC_BAD_GATEWAY);
    filterAndHandleException();
    verifyUpdateStatus(UNAVAILABLE, "Bad Gateway");
  }

  @Test
  public void doNotUpdateStatusWhenStatusHasNotChanged() throws Exception {
    filterAndHandleException();
    filterAndHandleException();
    verifyUpdateStatus(AVAILABLE);
  }

  @Test
  public void updateStatusWhenAutoBlockedTimeHasChanged() throws Exception {
    setInternalState(underTest, "autoBlock", true);
    when(filterable.call()).thenThrow(new IOException());
    filterAndHandleException();
    setInternalState(underTest, "blockedUntil", (Object) null);
    reset(statusObserver);
    Thread.sleep(10L); // guarantee a time increase.
    filterAndHandleException();
    verify(statusObserver).onStatusChanged(any(), any());
  }

  @Test
  public void setStatusToOfflineWhenPassed() throws Exception {
    underTest = new BlockingHttpClient(httpClient, new Config(), statusObserver, false, autoBlockConfiguration);
    ArgumentCaptor<RemoteConnectionStatus> newStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    verify(statusObserver, times(2)).onStatusChanged(any(), newStatusCaptor.capture());
    assertThat(newStatusCaptor.getAllValues().get(1).getType(), is(equalTo(OFFLINE)));
  }
  
  @Test
  public void shouldNotBlockWhenConfigurationNotSet() throws Exception {
    when(autoBlockConfiguration.shouldBlock(SC_UNAUTHORIZED)).thenReturn(false);
    
    when(statusLine.getStatusCode()).thenReturn(SC_UNAUTHORIZED);
    filterAndHandleException();
    verifyUpdateStatus(AVAILABLE);
  }

  private void verifyUpdateStatus(final RemoteConnectionStatusType newType) {
    verifyUpdateStatus(newType, null);
  }

  private void verifyUpdateStatus(final RemoteConnectionStatusType newType, final String newReason) {
    ArgumentCaptor<RemoteConnectionStatus> oldStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    ArgumentCaptor<RemoteConnectionStatus> newStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    verify(statusObserver, times(2)).onStatusChanged(oldStatusCaptor.capture(), newStatusCaptor.capture());
    assertThat(oldStatusCaptor.getAllValues().get(1).getType(), is(equalTo(READY)));
    assertThat(newStatusCaptor.getAllValues().get(1).getType(), is(equalTo(newType)));
    if (newReason != null) {
      assertThat(newStatusCaptor.getAllValues().get(1).getReason(), is(equalTo(newReason)));
    }
  }

  private void filterAndHandleException() throws IOException {
    try {
      underTest.filter(httpHost, filterable);
    }
    catch (IOException e) {
      //Intentionally not logged as this exception is expected in the test and doesn't give us any more information.
    }
  }
}
