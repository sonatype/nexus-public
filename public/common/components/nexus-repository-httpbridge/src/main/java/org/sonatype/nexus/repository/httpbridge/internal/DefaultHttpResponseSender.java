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
import javax.annotation.Nullable;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

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

    // add response headers
    response.getHeaders().forEach(header -> httpResponse.addHeader(header.getKey(), header.getValue()));

    // add status followed by payload if we have one
    Status status = response.getStatus();
    String statusMessage = status.getMessage();
    try (Payload payload = response.getPayload()) {
      // NEXUS-46395: HttpServletResponse.setStatus(int, String) was removed in Jakarta
      // Servlet 6 — only setStatus(int) remains. The custom HTTP/1.1 reason phrase that
      // Firewall depends on is propagated through the Jetty handler chain via our
      // jetty-modifications/jetty-server patch on org.eclipse.jetty.server.Response,
      // which adds setReason(String)/getReason() back to the core Response API.
      //
      // The previous EE8 path (jetty.ee8.nested.Response#setStatusWithReason) is gone
      // in EE10. To reach the patched core Response from inside the servlet wrapper we
      // unwrap through Shiro / HttpServletResponseWrapper layers down to Jetty's
      // ServletApiResponse, then call getResponse().setReason(...). On the sendError()
      // path our patched ServletChannelState already calls Response#setReason; this
      // covers the response-with-payload path (e.g. Firewall quarantine bodies that
      // return 403/409 with an RFC 9457 / format-specific report payload).
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
          ((ServletApiResponse) resp).getResponse().setReason(statusMessage);
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
        httpResponse.sendError(status.getCode(), statusMessage);
      }
    }
  }
}
