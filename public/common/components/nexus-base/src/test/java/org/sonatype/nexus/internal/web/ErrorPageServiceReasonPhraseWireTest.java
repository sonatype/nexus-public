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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.bootstrap.jetty.NexusReasonPhraseCustomizer;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.internal.web.ErrorPageService.ErrorInfo;
import org.sonatype.nexus.servlet.XFrameOptions;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * STL-476: end-to-end guard for the real {@link ErrorPageService} reason-phrase path.
 *
 * <p>
 * {@code ErrorPageServiceTest} mocks the Jetty unwrap ({@code ServletContextResponse.getServletContextResponse})
 * and {@code NexusReasonPhraseServletErrorDispatchTest} (nexus-spring) mirrors {@code setJettyReason} with a
 * hand-written copy of its three lines - so neither exercises the real {@code setJettyReason} against a live Jetty
 * response. This test closes that gap: it starts Jetty, dispatches to a servlet that calls the real
 * {@code ErrorPageService.writeErrorResponse(...)}, and asserts the custom reason phrase reaches the HTTP/1.1
 * status line while the {@link NexusReasonPhraseCustomizer#REASON_PHRASE_HEADER} sentinel is stripped from the
 * wire. It fails if the real unwrap chain in {@code setJettyReason} regresses (e.g. a changed wrapper chain or a
 * header set after commit).
 */
class ErrorPageServiceReasonPhraseWireTest
{
  private Server server;

  private int port;

  @BeforeEach
  void startServer() throws Exception {
    // RETURNS_MOCKS so templateHelper.parameters() and render() work without a real Velocity engine; the
    // reason-phrase path under test does not depend on the rendered body.
    TemplateHelper templateHelper = mock(TemplateHelper.class, Answers.RETURNS_MOCKS);
    XFrameOptions xFrameOptions = mock(XFrameOptions.class);
    when(xFrameOptions.getValueForPath(any())).thenReturn("DENY");
    ErrorPageService errorPageService = new ErrorPageService(templateHelper, xFrameOptions);

    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.addCustomizer(new NexusReasonPhraseCustomizer());

    server = new Server();
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    connector.setPort(0); // random free port
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new HttpServlet()
    {
      @Override
      protected void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        errorPageService.writeErrorResponse(new ErrorInfo(409, "package-quarantined"), request, response);
      }
    }, "/error");
    server.setHandler(context);
    server.start();
    port = connector.getLocalPort();
  }

  @AfterEach
  void stopServer() throws Exception {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  @Test
  void realErrorPageServiceReasonPhrase_reachesTheWire() throws Exception {
    List<String> lines = fetch("/error");

    assertThat(lines.get(0), is("HTTP/1.1 409 package-quarantined"));
    // the sentinel header must be consumed, never sent to the client
    assertThat(lines, everyItem(not(containsStringIgnoringCase(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER))));
  }

  private List<String> fetch(final String path) throws Exception {
    try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
      socket.setSoTimeout(5000);
      OutputStream out = socket.getOutputStream();
      out.write(("GET " + path + " HTTP/1.1\r\nHost: localhost:" + port + "\r\nConnection: close\r\n\r\n")
          .getBytes(StandardCharsets.ISO_8859_1));
      out.flush();

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
      List<String> head = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null && !line.isEmpty()) {
        head.add(line);
      }
      return head;
    }
  }
}
