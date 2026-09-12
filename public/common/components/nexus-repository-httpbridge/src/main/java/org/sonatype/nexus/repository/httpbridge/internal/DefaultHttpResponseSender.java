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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.sonatype.nexus.bootstrap.jetty.NexusReasonPhraseCustomizer;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

import org.apache.shiro.web.servlet.ShiroHttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link HttpResponseSender}.
 *
 * @since 3.0
 */
@Component
@Qualifier(DefaultHttpResponseSender.NEXUS_HTTP_RESPONSE_SENDER)
public class DefaultHttpResponseSender
    implements HttpResponseSender
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String NEXUS_HTTP_RESPONSE_SENDER = "NexusHttpResponseSender";

  @Override
  public void send(
      @Nullable final Request request,
      final Response response,
      final HttpServletResponse httpResponse) throws ServletException, IOException
  {
    log.debug("Sending response: {}", response);

    // add response headers, but never copy the internal reason-phrase sentinel. On proxied error
    // responses these view headers can originate from an upstream remote (STL-476), which must not be
    // able to choose the HTTP/1.1 reason phrase; the legitimate reason phrase is set below via the core
    // response, not through these view headers.
    response.getHeaders().forEach(header -> {
      if (!NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER.equalsIgnoreCase(header.getKey())) {
        httpResponse.addHeader(header.getKey(), header.getValue());
      }
    });

    // add status followed by payload if we have one
    Status status = response.getStatus();
    String statusMessage = status.getMessage();
    try (Payload payload = response.getPayload()) {
      // NEXUS-46395 / STL-476: HttpServletResponse.setStatus(int, String) was removed in Jakarta
      // Servlet 6 — only setStatus(int) remains. The custom HTTP/1.1 reason phrase that Firewall
      // depends on is recorded as a sentinel response header and serialized on the wire by
      // NexusReasonPhraseCustomizer (public Jetty API), replacing the former jetty-modifications fork.
      //
      // To reach the core Jetty Response (which carries the sentinel header) from inside the servlet
      // wrapper we unwrap through Shiro / HttpServletResponseWrapper layers down to Jetty's
      // ServletApiResponse, then getResponse().getHeaders().put(...). This covers the
      // response-with-payload path (e.g. Firewall quarantine bodies that return 403/409 with a
      // format-specific report payload). Servlet sendError(code, message) responses reach the same
      // customizer by a different route: Nexus's global <error-page>/error.html</error-page>
      // (nexus-web.xml) dispatches to ErrorPageServlet, which calls ErrorPageService.setJettyReason to
      // set this same sentinel header.
      httpResponse.setStatus(status.getCode());
      if (statusMessage != null) {
        ServletResponse resp = httpResponse;
        if (resp instanceof ShiroHttpServletResponse) {
          resp = ((ShiroHttpServletResponse) resp).getResponse();
        }
        while (resp instanceof HttpServletResponseWrapper) {
          resp = ((HttpServletResponseWrapper) resp).getResponse();
        }
        if (resp instanceof ServletApiResponse) {
          // Sentinel header consumed (and stripped) by NexusReasonPhraseCustomizer (nexus-spring).
          ((ServletApiResponse) resp).getResponse()
              .getHeaders()
              .put(NexusReasonPhraseCustomizer.REASON_PHRASE_HEADER, statusMessage);
        }
        else {
          // Make this observable in verbose logs so future Jetty / Shiro / wrapper
          // changes that break the unwrap path don't silently drop the reason phrase
          // (the way NEXUS-46395 did before NEXUS-53114).
          log.debug(
              "Unable to set HTTP/1.1 reason phrase '{}': unwrapped response type {} is not a ServletApiResponse",
              statusMessage,
              resp.getClass().getName());
        }
      }
      if (payload != null) {
        log.trace("Attaching payload: {}", payload);

        if (payload.getContentType() != null) {
          httpResponse.setContentType(payload.getContentType());
        }
        if (payload.getSize() != Payload.UNKNOWN_SIZE) {
          httpResponse.setContentLengthLong(payload.getSize());
        }

        if (request != null && !HttpMethods.HEAD.equals(request.getAction())) {
          try (InputStream input = payload.openInputStream(); OutputStream output = httpResponse.getOutputStream()) {
            payload.copy(input, output);
          }
        }
      }
      else if (!status.isSuccessful()) {
        // NEXUS-53528: HTTP/2 proxies (GCLB, Cloudflare, AWS ALB) strip the HTTP/1.1 reason phrase.
        // Write error message to body so it survives any protocol translation.
        // A HEAD response must not transfer a body (RFC 9110 §9.3.2), so skip the body write for
        // HEAD - mirroring the guard on the payload branch above - and emit status only.
        boolean isHead = request != null && HttpMethods.HEAD.equals(request.getAction());
        if (statusMessage != null && !statusMessage.isEmpty() && !isHead) {
          byte[] bytes = statusMessage.getBytes(StandardCharsets.UTF_8);
          httpResponse.setContentType("text/plain;charset=utf-8");
          httpResponse.setContentLengthLong(bytes.length);
          try (OutputStream output = httpResponse.getOutputStream()) {
            output.write(bytes);
            output.flush();
          }
        }
        else {
          httpResponse.sendError(status.getCode());
        }
      }
    }
  }
}
