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
package org.sonatype.nexus.bootstrap.jetty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextResponse;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * STL-476: verifies that a custom reason phrase supplied through the servlet {@code sendError(code, message)} API
 * still reaches the HTTP/1.1 status line after the jetty-modifications fork was removed.
 *
 * <p>
 * The deleted fork propagated {@code RequestDispatcher.ERROR_MESSAGE} onto the core response reason inside a patched
 * {@code ServletChannel}, so any two-arg {@code sendError} surfaced its message on the wire. The replacement relies on
 * Nexus's global {@code <error-page>/error.html</error-page>} (see {@code nexus-web.xml}): every servlet error is
 * dispatched to {@code ErrorPageServlet}, which hands the message to {@code ErrorPageService.setJettyReason}, setting
 * the {@link NexusReasonPhraseCustomizer#REASON_PHRASE_HEADER} sentinel that this customizer promotes to the status
 * line.
 *
 * <p>
 * This test reproduces that wiring end-to-end: a servlet calls {@code sendError(429, message)} (as
 * {@code NexusBasicHttpAuthenticationFilter}, {@code SessionAuthenticationFilter} and {@code LicensingRedirectFilter}
 * do), a global error page mirrors {@code ErrorPageService.setJettyReason} (the same
 * {@link ServletContextResponse#getServletContextResponse} unwrap), and we assert on the raw status line via a TCP
 * socket. It confirms the generic {@code sendError} reason-phrase behavior is preserved without the fork.
 */
class NexusReasonPhraseServletErrorDispatchTest
{
  private static final String ERROR_MESSAGE_ATTRIBUTE = "jakarta.servlet.error.message";

  private Server server;

  private int port;

  @BeforeEach
  void startServer() throws Exception {
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.addCustomizer(new NexusReasonPhraseCustomizer());

    server = new Server();
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    connector.setPort(0); // random free port
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");

    // Mirrors the production filters that call the two-arg servlet sendError(code, message).
    context.addServlet(new HttpServlet()
    {
      @Override
      protected void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.sendError(429, "Too many authentication attempts");
      }
    }, "/trigger");

    // Mirrors ErrorPageServlet + ErrorPageService.setJettyReason: read the servlet error message and set the
    // sentinel header on the unwrapped core response so NexusReasonPhraseCustomizer serializes it on the wire.
    context.addServlet(new HttpServlet()
    {
      @Override
      protected void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        String message = (String) request.getAttribute(ERROR_MESSAGE_ATTRIBUTE);
        if (message != null) {
          ServletContextResponse.getServletContextResponse(response)
              .getResponse()
              .getHeaders()
              .put(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER, message);
        }
        response.getWriter().write("error");
      }
    }, "/error.html");

    ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
    // Nexus registers a global (code-less) error page; map both the global slot and the specific code so the test
    // exercises the same dispatch regardless of Jetty's global-page resolution.
    errorHandler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, "/error.html");
    errorHandler.addErrorPage(429, "/error.html");
    context.setErrorHandler(errorHandler);

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
  void servletSendErrorMessage_isSentOnTheWireViaErrorPageDispatch() throws Exception {
    List<String> lines = fetch("/trigger");

    assertThat(lines.get(0), is("HTTP/1.1 429 Too many authentication attempts"));
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
