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
package org.sonatype.nexus.bootstrap.jetty;

import org.sonatype.goodies.testsupport.TestSupport;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerSubdomainRequestCustomizerTest
    extends TestSupport
{
  private DockerSubdomainRequestCustomizer underTest;

  @Mock
  private ServerConnector connector;

  @Mock
  private HttpConfiguration httpConfig;

  @Mock
  private Request request;

  @Before
  public void setUp() throws Exception {
    underTest = new DockerSubdomainRequestCustomizer("nexus", 8081, 8443);
    when(request.getHeader("Host")).thenReturn("test");
  }

  @Test
  public void testCheckForLocalPortAndNotExternal() {
    HttpURI httpURI = mock(HttpURI.class);
    when(httpURI.getPath()).thenReturn("/v2/");
    when(request.getHttpURI()).thenReturn(httpURI);

    underTest.customize(connector, httpConfig, request);

    verify(connector).getLocalPort();
    verify(httpURI, never()).getPort();
  }

  @Test
  public void testProcessOnlyRequestOnJettyPort() {
    HttpURI httpURI = mock(HttpURI.class);
    when(httpURI.getPath()).thenReturn("/v2/");
    when(request.getHttpURI()).thenReturn(httpURI);
    when(connector.getLocalPort()).thenReturn(-1);

    underTest.customize(connector, httpConfig, request);

    verify(request, never()).getHeader("Host");

    when(connector.getLocalPort()).thenReturn(8081);

    underTest.customize(connector, httpConfig, request);

    verify(request).getHeader("Host");

    when(connector.getLocalPort()).thenReturn(8443);

    underTest.customize(connector, httpConfig, request);

    verify(request, times(2)).getHeader("Host");
  }
}
