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

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * STL-476: HTTP/2 has no status-line reason phrase, so {@link NexusReasonPhraseCustomizer} cannot (and does not)
 * surface a custom reason phrase on h2. This test pins the other half of the contract that still matters on h2:
 * the internal {@link NexusReasonPhraseCustomizer#REASON_PHRASE_HEADER} sentinel must still be stripped so it
 * never leaks to the client.
 *
 * <p>
 * The wrapper is installed per request regardless of protocol and the strip in {@code send(...)} is shared with
 * HTTP/1.1, but {@link NexusReasonPhraseCustomizerTest} only exercises HTTP/1.1. This drives a real h2c request so
 * a future change that made the strip conditional on protocol (leaking the sentinel over HTTP/2) would fail here.
 */
class NexusReasonPhraseCustomizerHttp2Test
{
  private Server server;

  private int port;

  @BeforeEach
  void startServer() throws Exception {
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.addCustomizer(new NexusReasonPhraseCustomizer());

    server = new Server();
    // h2c (cleartext HTTP/2) connector, so no TLS setup is needed for the test.
    ServerConnector connector = new ServerConnector(server, new HTTP2CServerConnectionFactory(httpConfig));
    connector.setPort(0); // random free port
    server.addConnector(connector);
    server.setHandler(new Handler.Abstract()
    {
      @Override
      public boolean handle(final Request request, final Response response, final Callback callback) {
        response.getHeaders().put(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER, "package-quarantined");
        response.setStatus(HttpStatus.CONFLICT_409);
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
  void sentinelHeaderIsStrippedOnHttp2() throws Exception {
    HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP2(new HTTP2Client()));
    httpClient.start();
    try {
      ContentResponse response = httpClient.newRequest("http://localhost:" + port + "/").send();

      assertThat(response.getStatus(), is(HttpStatus.CONFLICT_409));
      // HTTP/2 carries no reason phrase, but the internal sentinel header must never reach the client.
      assertThat(response.getHeaders().contains(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER), is(false));
    }
    finally {
      httpClient.stop();
    }
  }
}
