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
package org.sonatype.nexus.test.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Test helper to build plain Http Proxy server.
 *
 * @author cstamas
 * @since 2.13.1
 */
public class HttpProxyServer
    extends ComponentSupport
{
  private final List<String> accessedUris;

  private final int port;

  private final MonitorableProxyServlet monitorableProxyServlet;

  private final MonitorableConnectHandler monitorableConnectHandler;

  private Server server;

  public HttpProxyServer(final int port) throws Exception {
    this(port, null);
  }

  public HttpProxyServer(final int port,
                         @Nullable final Map<String, String> authentication) throws Exception
  {
    checkArgument(port > 1024);
    this.accessedUris = new ArrayList<>();
    this.port = port;
    this.monitorableProxyServlet = new MonitorableProxyServlet(this.accessedUris, null);
    this.monitorableConnectHandler = new MonitorableConnectHandler(this.accessedUris);
    startServer();
  }

  private void startServer() throws Exception {
    server = new Server(port);

    final HandlerCollection handlers = new HandlerCollection();
    server.setHandler(handlers);

    handlers.addHandler(monitorableConnectHandler);

    final ServletContextHandler context = new ServletContextHandler(
        handlers, "/", ServletContextHandler.SESSIONS
    );
    context.addServlet(new ServletHolder(monitorableProxyServlet), "/*");

    server.start();
  }

  private void stopServer() throws Exception {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  public void start() throws Exception {
  }

  public void stop() throws Exception {
    stopServer();
  }

  public int getPort() {
    return port;
  }

  public List<String> getAccessedUris() {
    return accessedUris;
  }
}
