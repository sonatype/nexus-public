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
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.handler.HandlerCollection;

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
  public interface RequestResponseListener
  {
    void servicing(final ServletRequest req, final ServletResponse res);
  }

  private final int port;

  private final RequestResponseListener listener;

  private final Map<String, Object> authentication;

  private Server server;

  public HttpProxyServer(final int port) throws Exception {
    this(port, null);
  }

  public HttpProxyServer(final int port, @Nullable final RequestResponseListener listener) throws Exception {
    this(port, listener, null);
  }

  public HttpProxyServer(final int port,
                         @Nullable final RequestResponseListener listener,
                         @Nullable final Map<String, ? extends Object> authentication) throws Exception {
    checkArgument(port > 1024);
    this.port = port;
    this.listener = listener;
    if (authentication != null) {
      this.authentication = ImmutableMap.copyOf(authentication);
    }
    else {
      this.authentication = null;
    }
    startServer();
  }

  private void startServer() throws Exception {
    Server server = Server.withPort(port);
    if (authentication != null) {
      server.getServerProvider().addAuthentication("/*", "BASIC");
      for (Map.Entry<String, Object> user : authentication.entrySet()) {
        server.getServerProvider().addUser(user.getKey(), user.getValue());
      }
    }
    server.serve("/*").withServlet(new ProxyServlet());

    this.server = server.start();
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


  private class RecordingProxyServlet
      extends ProxyServlet
  {
    @Override
    public void service(final ServletRequest req, final ServletResponse res)
        throws ServletException, IOException
    {
      try {
        super.service(req, res);
      }
      finally {
        if (listener != null) {
          listener.servicing(req, res);
        }
      }
    }
  }
}
