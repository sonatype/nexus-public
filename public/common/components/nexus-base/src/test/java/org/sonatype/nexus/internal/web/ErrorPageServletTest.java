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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.common.app.ApplicationVersionSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.internal.template.TemplateHelperImpl;
import org.sonatype.nexus.internal.webresources.DevModeResources;
import org.sonatype.nexus.internal.webresources.WebResourceServiceImpl;
import org.sonatype.nexus.internal.webresources.WebResourceServlet;
import org.sonatype.nexus.mime.internal.DefaultMimeSupport;
import org.sonatype.nexus.servlet.XFrameOptions;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.velocity.app.VelocityEngine;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.event.Level;

// NEXUS-46395: jetty ee8 -> ee10 (Phase 1 brought in jetty 12 + ee10-servlet)
import static org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler.GLOBAL_ERROR_PAGE;
import static org.eclipse.jetty.http.HttpStatus.Code.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.logLevel;

/**
 * Tests for {@link ErrorPageServlet}
 *
 */
@ExtendWith(LoggingExtension.class)
class ErrorPageServletTest
{
  @CaptureLogsFor(value = ErrorPageServlet.class, level = Level.TRACE)
  TestLogAccessor log;

  static Server server;

  static int port;

  @BeforeAll
  static void setUp() throws Exception {
    TemplateHelper templateHelper = new TemplateHelperImpl(new ApplicationVersionSupport()
    {
      @Override
      public String getEdition() {
        return "Test";
      }
    }, new VelocityEngine());

    XFrameOptions xFrameOptions = new XFrameOptions(true);

    ServletContextHandler context = new ServletContextHandler();
    ErrorPageService errorPageService = new ErrorPageService(templateHelper, xFrameOptions);
    context.addServlet(new ServletHolder(new ErrorPageServlet(errorPageService)), "/error.html");
    WebResourceServiceImpl webResources =
        new WebResourceServiceImpl(new DevModeResources(), new DefaultMimeSupport(), List.of(), List.of());
    context.addServlet(
        new ServletHolder(new WebResourceServlet(webResources, xFrameOptions, new Time(10, TimeUnit.DAYS))), "/bad/*");

    ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
    errorHandler.addErrorPage(GLOBAL_ERROR_PAGE, "/error.html");
    context.setErrorHandler(errorHandler);

    BaseUrlHolder.set("http://127.0.0.1", "");

    server = new Server(0);
    server.setHandler(context);
    server.start();

    port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  @Test
  void errorCodeIsMaintained() throws Exception {
    String request = resolve("/bad/403/You%20can%27t%20see%20this");

    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      try (CloseableHttpResponse response = client.execute(new HttpGet(request))) {
        StatusLine statusLine = response.getStatusLine();

        assertThat(statusLine.getStatusCode(), is(NOT_FOUND.getCode()));
      }
    }
  }

  @Test
  void testAttachCause() throws Exception {
    ErrorPageServlet.attachCause(mock(), new EofException());
    ErrorPageServlet.attachCause(mock(), new IllegalStateException(new EofException()));

    Exception exceptionToThrow = new IOException();
    exceptionToThrow.addSuppressed(new EofException());
    ErrorPageServlet.attachCause(mock(), exceptionToThrow);

    List<String> logs = log.logs()
        .stream()
        .filter(log -> log.getLevel() == ch.qos.logback.classic.Level.TRACE)
        .map(ILoggingEvent::getMessage)
        .toList();

    assertThat(logs,
        contains("Client terminated connection", "Client terminated connection", "Client terminated connection"));
  }

  @Test
  void attachCauseLogsBadMessageExceptionAtTraceNotWarn() {
    ErrorPageServlet.attachCause(mock(), new BadMessageException(400, "Unable to parse form content"));
    ErrorPageServlet.attachCause(mock(), new RuntimeException(new BadMessageException(400, "wrapped")));

    // BadMessageException is a client error — must not produce WARN or ERROR log noise
    assertThat(log.logs(), not(hasItem(logLevel(Level.WARN))));
    assertThat(log.logs(), not(hasItem(logLevel(Level.ERROR))));
    // Should log at trace level to avoid stack traces in cloud environments where debug is enabled
    assertThat(log.logs(), hasItem(logLevel(Level.TRACE)));
  }

  private String resolve(final String path) {
    return "http://127.0.0.1:" + port + path;
  }
}
