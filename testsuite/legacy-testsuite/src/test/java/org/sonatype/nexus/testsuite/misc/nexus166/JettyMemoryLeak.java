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
package org.sonatype.nexus.testsuite.misc.nexus166;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JettyMemoryLeak
{

  private Server server;

  @Before
  public void start()
      throws Exception
  {
    JOptionPane.showConfirmDialog(null, "Start");
    Handler handler = new AbstractHandler()
    {
      private int[] ints = new int[1024 * 1024];

      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        int[] cache = ints;
        ints = null;
        ints = cache;
        cache = null;

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>Hello</h1>");
        ((Request) request).setHandled(true);
      }
    };

    server = new Server(8080);
    server.setHandler(handler);
    server.setStopAtShutdown(true);
    server.start();
  }

  @After
  public void stop()
      throws Exception
  {
    JOptionPane.showConfirmDialog(null, "Stop");
    server.stop();
    server = null;
    JOptionPane.showConfirmDialog(null, "Stoped");
  }

  @Test
  public void runsomething() {
    System.out.println(server);
    System.out.println("In use: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
        / 1024 / 1024);
  }
}
