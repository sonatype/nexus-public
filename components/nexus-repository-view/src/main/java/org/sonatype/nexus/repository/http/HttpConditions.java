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
package org.sonatype.nexus.repository.http;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import com.google.common.net.HttpHeaders;
import org.apache.http.client.utils.DateUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_MODIFIED;
import static org.sonatype.nexus.repository.http.HttpStatus.PRECONDITION_FAILED;

/**
 * Helper defining common HTTP conditions.
 *
 * @since 3.0
 */
public class HttpConditions
{
  private static final Logger log = LoggerFactory.getLogger(HttpConditions.class);

  /**
   * Request attribute containing the stashed conditions.
   */
  private static final String HTTP_CONDITIONS = HttpConditions.class.getName() + ".conditions";

  /**
   * List of supported headers. This list must be in sync with headers supported in {@link #requestPredicate(Request)}.
   */
  private static final List<String> SUPPORTED_HEADERS = Arrays.asList(
      HttpHeaders.IF_MODIFIED_SINCE,
      HttpHeaders.IF_UNMODIFIED_SINCE,
      HttpHeaders.IF_MATCH,
      HttpHeaders.IF_NONE_MATCH
  );

  private HttpConditions() {
    // empty
  }

  /**
   * Verifiers whether a {@link Request} contains conditional headers.
   *
   * @return {@code true} if the request contains conditional headers, {@code false} otherwise.
   */
  public static boolean isConditional(final Request request) {
    Set<String> headerNames = request.getHeaders().names();
    return headerNames.contains(HttpHeaders.IF_MODIFIED_SINCE) || headerNames.contains(HttpHeaders.IF_UNMODIFIED_SINCE)
        || headerNames.contains(HttpHeaders.IF_MATCH) || headerNames.contains(HttpHeaders.IF_NONE_MATCH);
  }

  /**
   * Stashes the conditions of the passed in request, making it non-conditional request. To reverse this change,
   * use {@link #makeConditional(Request)} method.
   */
  @Nonnull
  public static Request makeUnconditional(@Nonnull final Request request) {
    checkNotNull(request);
    final Headers stashedHeaders = new Headers();
    for (String httpHeader : SUPPORTED_HEADERS) {
      List<String> headerValues = request.getHeaders().getAll(httpHeader);
      if (headerValues != null) {
        stashedHeaders.set(httpHeader, headerValues);
      }
      request.getHeaders().remove(httpHeader);
    }
    request.getAttributes().set(HTTP_CONDITIONS, stashedHeaders);
    return request;
  }

  /**
   * Un-stash the conditions originally found in {@link Request}. This method accepts only requests returned by {@link
   * #makeUnconditional(Request)} method, otherwise will throw {@link IllegalStateException}. This method must be used
   * in pair with the method above.
   */
  @Nonnull
  public static Request makeConditional(@Nonnull final Request request) {
    checkNotNull(request);
    final Headers stashedHeaders = getConditionalHeaders(request);
    for (Entry<String, String> entry : stashedHeaders.entries()) {
      request.getHeaders().set(entry.getKey(), stashedHeaders.getAll(entry.getKey()));
    }
    return request;
  }

  /*
   * Retrieves cached conditional headers moved by {@link #makeUnconditional}
   */
  private static Headers getConditionalHeaders(final Request request) {
    return request.getAttributes().require(HTTP_CONDITIONS, Headers.class);
  }

  /**
   * Creates a conditional {@link Response} if the provided {@code context} and provided {@code response} from a GET
   * request.
   *
   * <a href="See https://httpwg.org/specs/rfc7232.html#precedence">RFC 7232</a>
   *
   * @param context the context for the request
   * @param getResponse a response from a GET request
   * @return an Optional containing the conditional response, or an empty response
   */
  public static Optional<Response> maybeCreateConditionalResponse(
      final Context context,
      final Response getResponse)
  {
    // Request headers
    Headers headers = getConditionalHeaders(context.getRequest());
    Optional<String> ifMatchHeader = Optional.ofNullable(headers.get(HttpHeaders.IF_MATCH));
    Optional<String> ifUnmodifiedHeader = Optional.ofNullable(headers.get(HttpHeaders.IF_UNMODIFIED_SINCE));
    Optional<String> ifNoneMatchHeader = Optional.ofNullable(headers.get(HttpHeaders.IF_NONE_MATCH));
    Optional<String> ifModifiedHeader = Optional.ofNullable(headers.get(HttpHeaders.IF_MODIFIED_SINCE));

    Optional<DateTime> lastModified = parseDateHeader(getResponse.getHeaders().get(HttpHeaders.LAST_MODIFIED));

    if (ifMatchHeader.isPresent()) {
      // Step 1
      if (!HttpConditions.doesEtagMatch(ifMatchHeader.get(), getResponse)) {
        log.debug("ETag: {} does not If-Unmodified: {}", getResponse.getHeaders().get(HttpHeaders.ETAG),
            ifMatchHeader.get());
        return Optional.of(buildPreconditionFailed(getResponse));
      }
    }
    else if (ifUnmodifiedHeader.isPresent() && lastModified.isPresent()) {
      // While the header is present, the date may be invalid so we keep it as Optional
      boolean modifiedAfterSpecifiedDate = ifUnmodifiedHeader.flatMap(HttpConditions::parseDateHeader)
          .map(lastModified.get()::isAfter)
          .orElse(false);
      if (modifiedAfterSpecifiedDate) {
        log.debug("Precondition Failed - LastModified {} is after If-Unmodified-Since {}", lastModified.get(),
            ifUnmodifiedHeader.get());
        return Optional.of(buildPreconditionFailed(getResponse));
      }
    }

    if (ifNoneMatchHeader.isPresent()) {
      // Step 3
      if (HttpConditions.doesEtagMatch(ifNoneMatchHeader.get(), getResponse)) {
        log.debug("ETag: {} does not If-None-Match: {}", getResponse.getHeaders().get(HttpHeaders.ETAG),
            ifNoneMatchHeader.get());
        // 304 for get/head, 412 for post/put (not really applicable)
        return Optional.of(getOrHead(context.getRequest()) ? buildNotModified(getResponse)
            : buildPreconditionFailed(getResponse));
      }
    }
    else if (ifModifiedHeader.isPresent() && lastModified.isPresent() && getOrHead(context.getRequest())) {
      // Step 4
      // While the header is present, the date may be invalid so we keep it as Optional
      boolean modifiedAfterSpecifiedDate = ifModifiedHeader.flatMap(HttpConditions::parseDateHeader)
          .map(lastModified.get()::isAfter)
          .orElse(true);
      if (!modifiedAfterSpecifiedDate) {
        log.debug("Not Modified - LastModified {} is after If-Modified-Since {}", lastModified.get(),
            ifModifiedHeader.get());
        return Optional.of(buildNotModified(getResponse));
      }
    }

    return Optional.empty();
  }

  private static boolean getOrHead(final Request request) {
    String action = request.getAction();
    return GET.equals(action) || HEAD.equals(action);
  }

  private static Response buildPreconditionFailed(final Response response) {
    return new Response.Builder()
        .copy(response)
        .status(Status.failure(PRECONDITION_FAILED))
        .payload(null)
        .build();
  }

  private static Response buildNotModified(final Response response) {
    // copy only ETag header, leave out all other entity headers
    final Response.Builder responseBuilder = new Response.Builder().status(Status.success(NOT_MODIFIED));
    Optional.ofNullable(response.getHeaders().get(HttpHeaders.ETAG))
        .ifPresent(eTag -> responseBuilder.header(HttpHeaders.ETAG, eTag));
    return responseBuilder.build();
  }

  /**
   * Returns true if a match header matches the eTag of the response
   *
   * @param matchHeader a match header
   * @param response the response
   */
  private static boolean doesEtagMatch(final String matchHeader, final Response response) {
    if ("*".equals(matchHeader)) {
      return true;
    }
    return Optional.ofNullable(response.getHeaders().get(HttpHeaders.ETAG))
        .map(matchHeader::contains)
        .orElse(false);
  }

  private static Optional<DateTime> parseDateHeader(@Nullable final String httpDate) {
    return Optional.ofNullable(httpDate)
        .map(DateUtils::parseDate)
        .map(Date::getTime)
        .map(DateTime::new);
  }
}
