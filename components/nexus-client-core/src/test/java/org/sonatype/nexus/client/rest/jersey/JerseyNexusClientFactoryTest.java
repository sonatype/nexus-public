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
package org.sonatype.nexus.client.rest.jersey;

import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.rest.BaseUrl;
import org.sonatype.nexus.client.rest.ConnectionInfo;
import org.sonatype.nexus.client.rest.ConnectionInfo.ValidationLevel;
import org.sonatype.nexus.client.rest.ProxyInfo;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.thoughtworks.xstream.XStream;
import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.client.rest.Protocol.HTTP;

/**
 * Tests for JerseyNexusClientFactory.
 */
public class JerseyNexusClientFactoryTest
    extends TestSupport
{

  private JerseyNexusClientFactory underTest;

  @Mock
  private SubsystemFactory<?, JerseyNexusClient> factory;

  @Mock
  private XStream xstream;

  @Mock
  private ConnectionInfo connection;

  @Mock
  private ProxyInfo proxyInfo;

  @Before
  public void setup() {
    underTest = new JerseyNexusClientFactory(factory);
  }

  @Test
  public void testProxySettings() {
    when(connection.getProxyInfos()).thenReturn(ImmutableMap.of(HTTP, proxyInfo));
    when(connection.getBaseUrl()).thenReturn(new BaseUrl(HTTP, "otherhost", 8080, "path"));
    when(connection.getSslCertificateValidation()).thenReturn(ValidationLevel.STRICT);
    when(connection.getSslCertificateHostnameValidation()).thenReturn(ValidationLevel.LAX);

    when(proxyInfo.getProxyHost()).thenReturn("somehost");
    when(proxyInfo.getProxyPort()).thenReturn(8888);
    when(proxyInfo.getProxyProtocol()).thenReturn(HTTP);

    final ApacheHttpClient4 client = underTest.doCreateHttpClientFor(connection, xstream);

    assertThat(client.getClientHandler().getHttpClient().getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY),
        (Matcher) allOf(
            Matchers.isA(HttpHost.class),
            Matchers.hasProperty("hostName", is("somehost")),
            Matchers.hasProperty("port", is(8888))
        ));
  }

}
