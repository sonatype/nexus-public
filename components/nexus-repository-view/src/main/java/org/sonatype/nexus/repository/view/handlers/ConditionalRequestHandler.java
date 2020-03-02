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
package org.sonatype.nexus.repository.view.handlers;

import javax.annotation.Nonnull;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.base.Predicate;
import com.google.common.net.HttpHeaders;

import static org.sonatype.nexus.repository.http.HttpConditions.makeConditional;
import static org.sonatype.nexus.repository.http.HttpConditions.makeUnconditional;
import static org.sonatype.nexus.repository.http.HttpConditions.requestPredicate;
import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_MODIFIED;
import static org.sonatype.nexus.repository.http.HttpStatus.PRECONDITION_FAILED;

/**
 * A format-neutral handler for conditional requests. It relies on existence of following HTTP entity headers:
 * <ul>
 * <li>Last-Modified</li>
 * <li>ETag</li>
 * </ul>
 * There is a handy {@link ContentHeadersHandler} handler that adds these for you, if you are serving up {@link
 * Content} payloads properly decorated. If not, you should have them somehow (format specific way) set on {@link
 * Response} instances coming from your {@link Handler}s.
 *
 * @see ContentHeadersHandler
 * @see Content
 * @since 3.0
 */
public class ConditionalRequestHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final Predicate<Response> requestPredicate = requestPredicate(context.getRequest());
    if (requestPredicate != null) {
      makeUnconditional(context.getRequest());
      try {
        return handleConditional(context, requestPredicate);
      }
      finally {
        makeConditional(context.getRequest());
      }
    }

    return context.proceed();
  }

  @Nonnull
  private Response handleConditional(@Nonnull final Context context,
                                     @Nonnull final Predicate<Response> requestPredicate) throws Exception
  {
    final String action = context.getRequest().getAction();
    log.debug("Conditional request: {} {}: {}",
        action,
        context.getRequest().getPath(),
        requestPredicate);
    switch (action) {
      case GET:
      case HEAD: {
        final Response response = context.proceed();
        if (response.getStatus().isSuccessful() && !requestPredicate.apply(response)) {
          // copy only ETag header, leave out all other entity headers
          final Response.Builder responseBuilder = new Response.Builder().status(Status.success(NOT_MODIFIED));
          if (response.getHeaders().contains(HttpHeaders.ETAG)) {
            responseBuilder.header(HttpHeaders.ETAG, response.getHeaders().get(HttpHeaders.ETAG));
          }
          return responseBuilder.build();
        }
        else {
          return response;
        }
      }

      case POST:
      case PUT:
      case DELETE: {
        final Request getRequest = new Request.Builder().copy(context.getRequest()).action(GET).build();
        final Response response = context.getRepository().facet(ViewFacet.class).dispatch(getRequest);
        if (response.getStatus().isSuccessful() && !requestPredicate.apply(response)) {
          // keep all response headers like Last-Modified and ETag, etc
          return new Response.Builder()
              .copy(response)
              .status(Status.failure(PRECONDITION_FAILED))
              .payload(null)
              .build();
        }
        else {
          return context.proceed();
        }
      }

      default:
        return context.proceed();
    }
  }
}
