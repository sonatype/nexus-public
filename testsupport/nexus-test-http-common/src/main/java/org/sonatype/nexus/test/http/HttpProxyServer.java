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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Helper to build fully functional Http Proxy server.
 *
 * @since 2.14.0
 */
public class HttpProxyServer
    extends ComponentSupport
{
  private final List<String> accessedUris; // URIs

  private final List<String> accessedHosts; // host:port

  private final int port;

  private final Map<String, String> authentication;

  private final Server server;

  public HttpProxyServer(final int port) throws Exception {
    this(port, null);
  }

  public HttpProxyServer(final int port,
                         @Nullable final Map<String, String> authentication) throws Exception
  {
    checkArgument(port > 1024 && port < 65536, "Invalid port %s", port);
    this.accessedUris = new ArrayList<>();
    this.accessedHosts = new ArrayList<>();
    this.port = port;
    this.authentication = authentication;
    this.server = createServer();
  }

  private Server createServer() throws Exception {
    Server server = new Server(port);
    final HandlerCollection handlers = new HandlerCollection();
    server.setHandler(handlers);

    // handler to log requests
    handlers.addHandler(new HandlerWrapper()
    {
      @Override
      public void handle(final String target,
                         final Request baseRequest,
                         final HttpServletRequest request,
                         final HttpServletResponse response)
          throws IOException, ServletException
      {
        final HttpURI uri = ((Request) request).getHttpURI();
        accessedUris.add(uri.toString());
        accessedHosts.add(uri.getHost() + ":" + uri.getPort());
        super.handle(target, baseRequest, request, response);
      }
    });
    // handler for HTTP CONNECT
    handlers.addHandler(new ConnectHandler());
    // proxy servlet
    final ServletContextHandler context = new ServletContextHandler(
        handlers, "/", ServletContextHandler.SESSIONS
    );
    context.addServlet(new ServletHolder(new ProxyServlet(authentication)), "/*");

    return server;
  }

  public boolean isStarted() {
    return server.isStarted();
  }

  public HttpProxyServer start() throws Exception {
    if (!server.isStarted()) {
      accessedUris.clear();
      accessedHosts.clear();
      server.start();
      log.info("Started HttpProxyServer on port {}", port);
    }
    return this;
  }

  public HttpProxyServer stop() throws Exception {
    if (server.isStarted()) {
      server.stop();
      log.info("Stopped HttpProxyServer on port {}", port);
    }
    return this;
  }

  public int getPort() {
    return port;
  }

  public List<String> getAccessedUris() {
    return accessedUris;
  }

  public List<String> getAccessedHosts() {
    return accessedHosts;
  }
}
