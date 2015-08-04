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
package org.sonatype.nexus.groovyremote

import com.sun.net.httpserver.HttpServer
import groovyx.remote.client.RemoteControl
import groovyx.remote.server.Receiver
import groovyx.remote.transport.http.HttpTransport
import groovyx.remote.transport.http.RemoteControlHttpHandler
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.sonatype.sisu.litmus.testsupport.TestSupport

/**
 * Trials of <a href="http://groovy.codehaus.org/modules/remote/">Groovy Remote Control</a>.
 *
 * This simply ensures that the basics of this library are functional from Groovy.
 */
class GroovyRemoteControlTest
    extends TestSupport
{
  private HttpServer server

  @Before
  void setUp() throws Exception {
    def receiver = new Receiver()
    def handler = new RemoteControlHttpHandler(receiver)
    def server = HttpServer.create(new InetSocketAddress(Inet4Address.localHost, 0), 0)
    // force use of ipv4
    server.createContext("/", handler)
    server.start()
    log("Address: $server.address")
    this.server = server
  }

  @After
  void tearDown() throws Exception {
    if (server != null) {
      server.stop(0)
      server = null
    }
  }

  @Test
  void trial() throws Exception {
    def addr = server.getAddress()
    String url = "http://${addr.hostName}:${addr.port}"
    log("URL: $url")

    def transport = new HttpTransport(url)
    def remote = new RemoteControl(transport)

    def value = remote.exec {
      println "${Thread.currentThread().name} -> sup"
      return 12345
    }

    log(value)
  }
}
