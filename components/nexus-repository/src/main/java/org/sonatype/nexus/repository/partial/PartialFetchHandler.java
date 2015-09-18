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
package org.sonatype.nexus.repository.partial;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import com.google.common.collect.Range;
import com.google.common.net.HttpHeaders;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implements partial-fetch semantics (as per RFC 2616) for {@link Status#isSuccessful() successful}
 * responses with payloads.
 *
 * @since 3.0
 */
@Named
@Singleton
public class PartialFetchHandler
    implements Handler
{
  private final RangeParser rangeParser;

  @Inject
  public PartialFetchHandler(final RangeParser rangeParser) {
    this.rangeParser = checkNotNull(rangeParser);
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final Response response = context.proceed();

    // Range requests only apply to GET
    if (!HttpMethods.GET.equals(context.getRequest().getAction())) {
      return response;
    }

    if (response.getStatus().getCode() != HttpStatus.OK) {
      // We don't interfere with any non-200 responses
      return response;
    }

    final Payload payload = response.getPayload();
    if (payload == null) {
      return response;
    }

    if (payload.getSize() == Payload.UNKNOWN_SIZE) {
      // We can't do much if we don't know how big the payload is
      return response;
    }

    final String rangeHeader = getRangeHeader(context);
    if (rangeHeader == null) {
      return response;
    }

    final List<Range<Long>> ranges = rangeParser.parseRangeSpec(rangeHeader, payload.getSize());

    if (ranges == null) {
      // The ranges were not satisfiable
      return HttpResponses.rangeNotSatisfiable(payload.getSize());
    }

    if (ranges.isEmpty()) {
      // No ranges were specified, or they could not be parsed
      return response;
    }

    if (ranges.size() > 1) {
      return HttpResponses.notImplemented("Multiple ranges not supported.");
    }

    Range<Long> requestedRange = ranges.get(0);

    // Mutate the response
    return partialResponse(response, payload, requestedRange);
  }

  /**
   * Mutate the response into one that returns part of the payload.
   */
  private Response partialResponse(final Response response,
                                   final Payload payload,
                                   final Range<Long> requestedRange)
  {
    Response.Builder builder = new Response.Builder()
        .copy(response)
        .status(Status.success(HttpStatus.PARTIAL_CONTENT));

    Payload partialPayload = new PartialPayload(payload, requestedRange);
    builder.payload(partialPayload);

    // ResponseSender takes care of Content-Length header, via payload.size
    builder.header(HttpHeaders.CONTENT_RANGE,
        requestedRange.lowerEndpoint() + "-" + requestedRange.upperEndpoint() + "/" + payload.getSize());

    return builder.build();
  }

  private String getRangeHeader(final Context context) {
    final Request request = context.getRequest();
    return request.getHeaders().get(HttpHeaders.RANGE);
  }
}
