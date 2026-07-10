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
package org.sonatype.nexus.jetty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NEXUS-46395 smoke test for the custom HTTP/1.1 reason-phrase patch applied to
 * {@code org.eclipse.jetty.server.Response} and
 * {@code org.eclipse.jetty.server.internal.HttpChannelState}.
 *
 * <p>The Firewall feature depends on being able to set a custom reason phrase like
 * {@code "409 package-quarantined"} so HTTP/1.1 clients (npm, etc.) can surface
 * quarantine information. Jetty 12 removed reason-phrase support upstream; these
 * tests verify that our patched jetty-server artifact has re-added it correctly.
 *
 * <p>We open a raw TCP socket and parse the HTTP/1.1 status line directly because
 * most HTTP clients (including {@link java.net.http.HttpClient}) strip reason
 * phrases after parsing.
 */
class CustomReasonPhraseTest
{
    private Server server;

    private int port;

    private volatile ReasonPhraseHandler handler;

    @BeforeEach
    void startServer() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0); // random free port
        server.addConnector(connector);

        handler = new ReasonPhraseHandler();
        server.setHandler(handler);

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
        handler.responseStatus = HttpStatus.CONFLICT_409;
        handler.responseReason = "package-quarantined";

        String statusLine = fetchStatusLine();

        assertThat(statusLine)
            .as("HTTP/1.1 status line must include the custom reason phrase we set via Response#setReason")
            .isEqualTo("HTTP/1.1 409 package-quarantined");
    }

    @Test
    void nullReason_fallsBackToDefaultJettyBehavior() throws Exception {
        handler.responseStatus = HttpStatus.NOT_FOUND_404;
        handler.responseReason = null; // no setReason() call

        String statusLine = fetchStatusLine();

        // Jetty's default is to serialize the code without a reason phrase (or with
        // its own canonical phrase depending on version). Either is acceptable; we only
        // require that the code is present and our patch did not break the default path.
        assertThat(statusLine)
            .as("HTTP/1.1 status line must still include the status code when setReason was not called")
            .startsWith("HTTP/1.1 404");
    }

    @Test
    void getReason_returnsWhatSetReasonWasGiven() throws Exception {
        // This is an in-process round-trip verifying the interface contract without
        // going through the wire. Complements the on-the-wire tests above.
        ReasonCapturingHandler capture = new ReasonCapturingHandler();
        server.stop();
        server.setHandler(capture);
        server.start();
        port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();

        fetchStatusLine();

        assertThat(capture.observedReason)
            .as("Response#getReason must return what Response#setReason was given earlier in the same request")
            .isEqualTo("under-test");
    }

    private String fetchStatusLine() throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(("GET / HTTP/1.1\r\nHost: localhost:" + port + "\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            String statusLine = reader.readLine();
            return statusLine == null ? "" : statusLine;
        }
    }

    /**
     * Exercises Response#setStatus + Response#setReason (the custom patch) and writes an
     * empty body. Uses the core Jetty Server/Handler API, not the Servlet API.
     */
    private static final class ReasonPhraseHandler extends Handler.Abstract
    {
        volatile int responseStatus = HttpStatus.OK_200;

        volatile String responseReason;

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            response.setStatus(responseStatus);
            if (responseReason != null) {
                response.setReason(responseReason);
            }
            response.write(true, null, callback);
            return true;
        }
    }

    /**
     * Exercises the getReason() round-trip on the ChannelResponse without going to the wire.
     */
    private static final class ReasonCapturingHandler extends Handler.Abstract
    {
        volatile String observedReason;

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            response.setStatus(HttpStatus.OK_200);
            response.setReason("under-test");
            observedReason = response.getReason();
            Content.Sink.write(response, true, null, callback);
            return true;
        }
    }
}
