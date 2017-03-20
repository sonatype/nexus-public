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
import org.sonatype.nexus.repository.httpclient.FilteredHttpClientSupport.Filterable;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusObserver;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType;
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;
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
  HttpClient httpClient;

  @Mock
  RemoteConnectionStatusObserver statusObserver;

  @Mock
  Filterable filterable;

  HttpHost httpHost;

  BlockingHttpClient underTest;

  @Before
  public void setup() throws Exception {
    httpHost = HttpHost.create("localhost");
    underTest = new BlockingHttpClient(httpClient, new Config(), statusObserver, true);
  }

  @Test
  public void updateStatusWhenBlocked() throws Exception {
    Config config = new Config();
    config.blocked = true;
    reset(statusObserver);
    new BlockingHttpClient(httpClient, config, statusObserver, true);
    ArgumentCaptor<RemoteConnectionStatus> newStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    verify(statusObserver).onStatusChanged(any(), newStatusCaptor.capture());
    assertThat(newStatusCaptor.getValue().getType(), is(equalTo(BLOCKED)));
  }

  @Test
  public void updateStatusWhenAvailable() throws Exception {
    filterAndHandleException();
    verifyUpdateStatus(AVAILABLE);
  }

  @Test
  public void updateStatusWhenUnavailableAndAutoBlocked() throws Exception {
    setInternalState(underTest, "autoBlock", true);
    when(filterable.call()).thenThrow(new IOException());
    filterAndHandleException();
    verifyUpdateStatus(AUTO_BLOCKED_UNAVAILABLE);
  }

  @Test
  public void updateStatusWhenUnavailable() throws Exception {
    when(filterable.call()).thenThrow(new IOException());
    filterAndHandleException();
    verifyUpdateStatus(UNAVAILABLE);
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
    underTest = new BlockingHttpClient(httpClient, new Config(), statusObserver, false);
    ArgumentCaptor<RemoteConnectionStatus> newStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    verify(statusObserver, times(2)).onStatusChanged(any(), newStatusCaptor.capture());
    assertThat(newStatusCaptor.getAllValues().get(1).getType(), is(equalTo(OFFLINE)));
  }

  private void verifyUpdateStatus(final RemoteConnectionStatusType newType) {
    ArgumentCaptor<RemoteConnectionStatus> oldStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    ArgumentCaptor<RemoteConnectionStatus> newStatusCaptor = ArgumentCaptor.forClass(RemoteConnectionStatus.class);
    verify(statusObserver, times(2)).onStatusChanged(oldStatusCaptor.capture(), newStatusCaptor.capture());
    assertThat(oldStatusCaptor.getAllValues().get(1).getType(), is(equalTo(READY)));
    assertThat(newStatusCaptor.getAllValues().get(1).getType(), is(equalTo(newType)));
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
