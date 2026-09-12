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
package org.sonatype.nexus.bootstrap.entrypoint.jetty;

import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nexus {@link ErrorHandler} that adds no-cache headers to error responses and derives the status code from a
 * propagated {@link HttpException}.
 *
 * <p>
 * Formerly shipped inside the {@code jetty-modifications} fork (in package {@code org.eclipse.jetty.server});
 * relocated here as a plain subclass of Jetty's public {@link ErrorHandler} when the fork was removed (STL-476).
 * It uses only public/protected Jetty API, so it no longer needs to live in a Jetty package.
 */
public class CustomErrorHandler
    extends ErrorHandler
{
  private static final Logger log = LoggerFactory.getLogger(CustomErrorHandler.class);

  private final HttpField cacheControl =
      new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, "must-revalidate,no-cache,no-store");

  @Override
  public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("handle({}, {}, {})", request, response, callback);
    }
    response.getHeaders().put(cacheControl);

    int code = response.getStatus();
    Throwable cause = (Throwable) request.getAttribute(ERROR_EXCEPTION);
    if (cause instanceof HttpException httpException) {
      code = httpException.getCode();
      response.setStatus(code);
    }

    if (!errorPageForMethod(request.getMethod()) || HttpStatus.hasNoBody(code)) {
      callback.succeeded();
    }
    else {
      String message = HttpStatus.getMessage(code);
      generateResponse(request, response, code, message, cause, callback);
    }
    return true;
  }
}
