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
package org.sonatype.nexus.testsuite.repository;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.jetty.impl.ProxyServlet;

import com.google.common.collect.Sets;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * An test proxy sever which works also for HTTPS.
 *
 * @since 2.6
 */
public class ProxyServerWithHttpsTunneling
{

  private int port;

  private Server server;

  private final Set<String> proxiedHosts = Sets.newHashSet();

  public int getPort() {
    return port;
  }

  public Server getServer() {
    return server;
  }

  public void initialize() {
    Server proxy = new Server(getPort());

    final HandlerCollection handlers = new HandlerCollection();
    proxy.setHandler(handlers);

    final ServletContextHandler context = new ServletContextHandler(
        handlers, "/", ServletContextHandler.SESSIONS
    );
    final ProxyServlet proxyServlet = new RecordingProxyServlet();
    context.addServlet(new ServletHolder(proxyServlet), "/*");

    handlers.addHandler(new RecordingConnectHandler());

    setServer(proxy);
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public void start()
      throws Exception
  {
    getServer().start();
  }

  public void stop()
      throws Exception
  {
    getServer().stop();
  }

  public Set<String> getProxiedHosts() {
    return proxiedHosts;
  }

  private class RecordingConnectHandler
      extends ConnectHandler
  {

    @Override
    public void handle(final String target,
                       final Request baseRequest,
                       final HttpServletRequest request,
                       final HttpServletResponse response)
        throws ServletException, IOException
    {
      proxiedHosts.add(target);
      super.handle(target, baseRequest, request, response);
    }

  }

  private class RecordingProxyServlet
      extends ProxyServlet
  {

    @Override
    public void service(final ServletRequest req, final ServletResponse res)
        throws ServletException, IOException
    {
      final HttpURI uri = ((Request) req).getHttpURI();
      proxiedHosts.add(uri.getHost() + ":" + uri.getPort());
      super.service(req, res);
    }
  }

}
