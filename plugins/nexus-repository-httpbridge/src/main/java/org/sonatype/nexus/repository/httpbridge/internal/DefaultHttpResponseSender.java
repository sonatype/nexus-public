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
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

/**
 * Default {@link HttpResponseSender}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DefaultHttpResponseSender
    extends ComponentSupport
    implements HttpResponseSender
{
  @Override
  public void send(@Nullable final Request request, final Response response, final HttpServletResponse httpResponse)
      throws ServletException, IOException
  {
    log.debug("Sending response: {}", response);

    // add response headers
    for (Map.Entry<String, String> header : response.getHeaders()) {
      httpResponse.addHeader(header.getKey(), header.getValue());
    }

    // add status followed by payload if we have one
    Status status = response.getStatus();
    String statusMessage = status.getMessage();
    try (Payload payload = response.getPayload()) {
      if (status.isSuccessful() || payload != null) {

        if (statusMessage == null) {
          httpResponse.setStatus(status.getCode());
        }
        else {
          httpResponse.setStatus(status.getCode(), statusMessage);
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
      }
      else {
        httpResponse.sendError(status.getCode(), statusMessage);
      }
    }
  }
}
