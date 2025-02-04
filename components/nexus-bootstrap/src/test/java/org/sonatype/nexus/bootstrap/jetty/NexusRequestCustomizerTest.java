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

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.eclipse.jetty.http.HttpVersion.HTTP_1_1;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class NexusRequestCustomizerTest
    extends TestSupport
{
  private NexusRequestCustomizer underTest;

  @Mock
  private ServerConnector connector;

  @Mock
  private HttpConfiguration httpConfig;

  @Mock
  private HttpURI httpURI;

  @Mock
  private Request request;

  @Before
  public void setUp() {
    underTest = new NexusRequestCustomizer("", 8081, 8443);
    when(request.getHeader("Host")).thenReturn("test");
    when(request.getHttpURI()).thenReturn(httpURI);
  }

  @Test
  public void testNullPath() {
    when(httpURI.getPath()).thenReturn(null);

    underTest.customize(connector, httpConfig, request);

    verify(request).getHttpURI();
    verifyNoMoreInteractions(request, connector, httpConfig);
  }

  @Test
  public void testCheckForLocalPortAndNotExternal() {
    when(httpURI.getPath()).thenReturn("/v2/");

    underTest.customize(connector, httpConfig, request);

    verify(connector).getLocalPort();
    verify(httpURI, never()).getPort();
  }

  @Test
  public void testProcessOnlyRequestOnJettyPort() {
    when(httpURI.getPath()).thenReturn("/v2/");
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

  @Test
  @Parameters({
      // docker subdomain requests
      "/v2/, , /repository/docker-repo/v2/",
      "/v2/token, , /repository/docker-repo/v2/token",
      "/v2/, /context, /context/repository/docker-repo/v2/",
      "/v2/token, /context, /context/repository/docker-repo/v2/token",
      // docker behind reverse proxy token requests
      "/repository/docker-repo/repository/docker-repo/v2/token, , /repository/docker-repo/v2/token",
      "/context/repository/docker-repo/context/repository/docker-repo/v2/token, /context, /context/repository/docker-repo/v2/token"
  })
  public void testRequestCustomization(
      final String incomingRequestPath,
      final String contextPath,
      final String expectedPath)
  {
    underTest = new NexusRequestCustomizer(contextPath, 8081, 8443);
    Request request1 = prepareRequest(incomingRequestPath);

    underTest.customize(connector, httpConfig, request1);

    String actualPath = request1.getHttpURI().getPath();

    assertThat(actualPath, is(expectedPath));
  }

  private Request prepareRequest(String currentPath) {
    HttpChannel httpChannel = mock(HttpChannel.class);
    when(httpChannel.getResponse()).thenReturn(mock(Response.class));
    when(httpChannel.getResponse().getHttpOutput()).thenReturn(mock(HttpOutput.class));

    when(connector.getLocalPort()).thenReturn(8081);

    Request request1 = new Request(httpChannel, mock(HttpInput.class));
    HttpURI currentUri = new HttpURI("http://localhost:8081" + currentPath);
    HttpFields headers = new HttpFields();
    headers.put("Host", "test.localhost");
    MetaData.Request currentMetadata = new MetaData.Request("GET", currentUri, HTTP_1_1, headers);
    request1.setMetaData(currentMetadata);

    DockerSubdomainRepositoryMapping.put("test", "docker-repo");

    return request1;
  }
}
