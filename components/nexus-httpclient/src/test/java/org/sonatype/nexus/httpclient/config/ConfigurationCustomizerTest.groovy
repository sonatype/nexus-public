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
package org.sonatype.nexus.httpclient.config

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.httpclient.internal.NexusHttpRoutePlanner

import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.protocol.HttpContext
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Mockito.mock
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTP
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTPS

/**
 * Tests for {@link ConfigurationCustomizer}.
 */
class ConfigurationCustomizerTest
    extends TestSupport
{
  ConfigurationCustomizer configurationCustomizer = new ConfigurationCustomizer(mock(HttpClientConfiguration))

  HttpHost httpProxyHost = new HttpHost('http-proxy', 8080)

  HttpHost httpsProxyHost = new HttpHost('https-proxy', 8443)

  private NexusHttpRoutePlanner create(String[] nonProxyHosts) {
    return configurationCustomizer.createRoutePlanner(new ProxyConfiguration(
        http: new ProxyServerConfiguration(host: httpProxyHost.hostName, port: httpProxyHost.port, enabled: true),
        https: new ProxyServerConfiguration(host: httpsProxyHost.hostName, port: httpsProxyHost.port, enabled: true),
        nonProxyHosts: nonProxyHosts
    ))
  }

  @Test
  void 'sanity test'() {
    NexusHttpRoutePlanner planner = create(null)
    HttpRoute route

    route = planner.determineRoute(new HttpHost('sonatype.org', 80, HTTP), mock(HttpRequest), mock(HttpContext))
    assertThat(route.getHopTarget(0), equalTo(httpProxyHost))

    route = planner.determineRoute(new HttpHost('sonatype.org', 443, HTTPS), mock(HttpRequest), mock(HttpContext))
    assertThat(route.getHopTarget(0), equalTo(httpsProxyHost))

    HttpHost localhost = new HttpHost('localhost', 80)
    route = planner.determineRoute(localhost, mock(HttpRequest), mock(HttpContext))
    assertThat(route.getHopTarget(0), equalTo(httpProxyHost))
  }

  @Test
  void 'custom nonProxyHosts'() {
    NexusHttpRoutePlanner planner = create(['*.sonatype.*', '*.example.com', 'localhost', '10.*', '[:*', '*:8]'] as String[])
    HttpRoute route

    // must not have proxy used
    for (String host : ['www.sonatype.org', 'www.sonatype.com', 'smtp.example.com', 'localhost', '10.0.0.1', '[::8]', '[::9]']) {
      HttpHost target = new HttpHost(host, 80)
      route = planner.determineRoute(target, mock(HttpRequest), mock(HttpContext))
      assertThat(route.getHopTarget(0), equalTo(target))
    }

    // must have HTTP proxy used
    for (String host : ['www.thesonatype.com', 'www.google.com', 'example.com', 'example.org']) {
      HttpHost target = new HttpHost(host, 80, HTTP)
      route = planner.determineRoute(target, mock(HttpRequest), mock(HttpContext))
      assertThat(route.getHopTarget(0), equalTo(httpProxyHost))
    }

    // must have HTTPS proxy used
    for (String host : ['www.google.com', 'example.com', 'example.org']) {
      HttpHost target = new HttpHost(host, 8443, HTTPS)
      route = planner.determineRoute(target, mock(HttpRequest), mock(HttpContext))
      assertThat(route.getHopTarget(0), equalTo(httpsProxyHost))
    }
  }

  @Test
  void 'ipv6 nonProxyHosts'() {
    NexusHttpRoutePlanner planner
    HttpRoute route

    planner = create(['*:8]'] as String[])
    // must not have proxy used
    for (String host : ['[::8]']) {
      HttpHost target = new HttpHost(host, 80)
      route = planner.determineRoute(target, mock(HttpRequest), mock(HttpContext))
      assertThat(route.getHopTarget(0), equalTo(target))
    }

    // must have HTTP proxy used
    for (String host : ['[::9]']) {
      HttpHost target = new HttpHost(host, 80, HTTP)
      route = planner.determineRoute(target, mock(HttpRequest), mock(HttpContext))
      assertThat(route.getHopTarget(0), equalTo(httpProxyHost))
    }

    planner = create(['[:*'] as String[])
    // must not have proxy used
    for (String host : ['[::8]', '[::9]']) {
      HttpHost target = new HttpHost(host, 80)
      route = planner.determineRoute(target, mock(HttpRequest), mock(HttpContext))
      assertThat(route.getHopTarget(0), equalTo(target))
    }

    // must have HTTP proxy used
    for (String host : ['sonatype.org', '1.2.3.4']) {
      HttpHost target = new HttpHost(host, 80, HTTP)
      route = planner.determineRoute(target, mock(HttpRequest), mock(HttpContext))
      assertThat(route.getHopTarget(0), equalTo(httpProxyHost))
    }
  }
}
