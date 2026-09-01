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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.startsWith;

/**
 * STL-476: verifies {@link NexusReasonPhraseCustomizer} serializes a custom HTTP/1.1 reason phrase set via
 * the {@link NexusReasonPhraseCustomizer#REASON_PHRASE_HEADER} response header, using only public Jetty API,
 * and strips that sentinel header from the wire. This replaces the jetty-modifications fork's
 * {@code CustomReasonPhraseTest}.
 *
 * <p>
 * Nexus depends on descriptive reason phrases like {@code "409 ... redeploy is not allowed"} /
 * {@code "package-quarantined"} so HTTP/1.1 clients (Maven, npm) surface them (NEXUS-53528). We open a raw
 * TCP socket and parse the status line directly because most HTTP clients strip reason phrases after parsing.
 */
class NexusReasonPhraseCustomizerTest
{
  private Server server;

  private int port;

  private volatile String reason;

  private volatile int status = HttpStatus.OK_200;

  @BeforeEach
  void startServer() throws Exception {
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.addCustomizer(new NexusReasonPhraseCustomizer());

    server = new Server();
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    connector.setPort(0); // random free port
    server.addConnector(connector);
    server.setHandler(new Handler.Abstract()
    {
      @Override
      public boolean handle(final Request request, final Response response, final Callback callback) {
        if (reason != null) {
          response.getHeaders().put(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER, reason);
        }
        response.setStatus(status);
        response.write(true, null, callback);
        return true;
      }
    });
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
  void customReasonPhrase_isSentOnTheWireOnHttp11() throws Exception {
    status = HttpStatus.CONFLICT_409;
    reason = "package-quarantined";

    List<String> lines = fetch();

    assertThat(lines.get(0), is("HTTP/1.1 409 package-quarantined"));
    // the sentinel header must be consumed, never sent to the client
    assertThat(lines, everyItem(not(containsStringIgnoringCase(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER))));
  }

  @Test
  void longMessageReasonPhrase_isSentOnTheWire() throws Exception {
    status = HttpStatus.CONFLICT_409;
    reason = "artifact-1.0.jar - cannot be updated as asset already exists and redeploy is not allowed";

    assertThat(fetch().get(0),
        is("HTTP/1.1 409 artifact-1.0.jar - cannot be updated as asset already exists and redeploy is not allowed"));
  }

  @Test
  void noHeader_fallsBackToDefaultBehavior() throws Exception {
    status = HttpStatus.NOT_FOUND_404;
    reason = null; // no sentinel header set

    assertThat(fetch().get(0), startsWith("HTTP/1.1 404"));
  }

  @Test
  void reasonPhraseWithControlChars_cannotInjectHeadersOrSplitStatusLine() throws Exception {
    status = HttpStatus.CONFLICT_409;
    reason = "quarantined\r\nX-Injected: pwned";

    List<String> lines = fetch();

    // CR/LF are stripped, so the status line stays intact and the text cannot appear as its own header
    assertThat(lines.get(0), startsWith("HTTP/1.1 409 "));
    assertThat(lines.subList(1, lines.size()), everyItem(not(startsWith("X-Injected"))));
    assertThat(lines, everyItem(not(containsStringIgnoringCase(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER))));
  }

  @Test
  void emptyReasonPhrase_fallsBackToDefaultAndStripsSentinel() throws Exception {
    status = HttpStatus.CONFLICT_409;
    reason = ""; // empty sentinel must not override with an empty reason phrase

    List<String> lines = fetch();

    assertThat(lines.get(0), startsWith("HTTP/1.1 409"));
    assertThat(lines.get(0), not(is("HTTP/1.1 409 ")));
    // the empty sentinel header must still be stripped, never leaked to the client
    assertThat(lines, everyItem(not(containsStringIgnoringCase(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER))));
  }

  private List<String> fetch() throws Exception {
    try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
      socket.setSoTimeout(5000);
      OutputStream out = socket.getOutputStream();
      out.write(("GET / HTTP/1.1\r\nHost: localhost:" + port + "\r\nConnection: close\r\n\r\n")
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
