/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.ops4j.pax.web.samples.helloworld.hs.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Hello World Servlet.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2008
 */
class HelloWorldServlet
    extends HttpServlet
{

  private final String m_registrationPath;

  HelloWorldServlet(final String registrationPath) {

    m_registrationPath = registrationPath;
  }

  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    response.setContentType("text/html");

    final PrintWriter writer = response.getWriter();
    writer.println("<html><body align='center'>");
    writer.println("<h1>Hello World</h1>");
    writer.println("<img src='/images/logo.png' border='0'/>");
    writer.println("<h1>" + getServletConfig().getInitParameter("from") + "</h1>");
    writer.println("<p>");
    writer.println("Served by servlet registered at: " + m_registrationPath);
    writer.println("<br/>");
    writer.println("Servlet Path: " + request.getServletPath());
    writer.println("<br/>");
    writer.println("Path Info: " + request.getPathInfo());
    writer.println("</p>");
    writer.println("</body></html>");
  }

}

