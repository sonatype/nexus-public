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
package org.sonatype.nexus.internal.web;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersionSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.internal.template.TemplateHelperImpl;
import org.sonatype.nexus.servlet.XFrameOptions;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.velocity.app.VelocityEngine;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static java.util.Arrays.asList;
import static org.eclipse.jetty.servlet.ErrorPageErrorHandler.GLOBAL_ERROR_PAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;

/**
 * Tests for {@link ErrorPageServlet}
 *
 */
public class ErrorPageServletTest
    extends TestSupport
{
  Server server;

  int port;

  @Before
  public void setUp() throws Exception {
    TemplateHelper templateHelper = new TemplateHelperImpl(new ApplicationVersionSupport()
    {
      @Override
      public String getEdition() {
        return "Test";
      }
    }, new VelocityEngine());

    XFrameOptions xFrameOptions = new XFrameOptions(true);

    ServletContextHandler context = new ServletContextHandler();
    context.addServlet(new ServletHolder(new ErrorPageServlet(templateHelper, xFrameOptions)), "/error.html");
    context.addServlet(new ServletHolder(new BadServlet()), "/bad/*");

    ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
    errorHandler.addErrorPage(GLOBAL_ERROR_PAGE, "/error.html");
    context.setErrorHandler(errorHandler);

    BaseUrlHolder.set("http://127.0.0.1");

    server = new Server(0);
    server.setHandler(context);
    server.start();

    port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  private static class BadServlet
      extends HttpServlet
  {
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
      String path = request.getPathInfo().substring(1);
      String[] codeAndMsg = path.split("/", 2);
      int code = Integer.valueOf(codeAndMsg[0]);
      String msg = codeAndMsg[1];
      response.sendError(code, msg);
    }
  }

  @Test
  public void customStatusMessageIsMaintained() throws Exception {
    String request = "http://127.0.0.1:" + port + "/bad/403/You%20can%27t%20see%20this";

    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      try (CloseableHttpResponse response = client.execute(new HttpGet(request))) {
        StatusLine statusLine = response.getStatusLine();

        assertThat(statusLine.getStatusCode(), is(403));
        assertThat(statusLine.getReasonPhrase(), is("You can't see this"));

        String body = EntityUtils.toString(response.getEntity());

        assertThat(body, stringContainsInOrder(asList("403", "Forbidden", "You can't see this")));

        assertThat(response.getFirstHeader(X_FRAME_OPTIONS), notNullValue());
        assertThat(response.getFirstHeader(X_FRAME_OPTIONS).getValue(), is("DENY"));
      }
    }
  }
}
