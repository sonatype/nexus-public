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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import org.apache.http.client.utils.DateUtils;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper defining common HTTP conditions.
 *
 * @since 3.0
 */
public class HttpConditions
{
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
    final Headers stashedHeaders = request.getAttributes().require(HTTP_CONDITIONS, Headers.class);
    for (Entry<String, String> entry : stashedHeaders.entries()) {
      request.getHeaders().set(entry.getKey(), stashedHeaders.getAll(entry.getKey()));
    }
    return request;
  }

  /**
   * Builds a {@link Predicate} that contains conditions in passed in {@link Request} or {@code null} if
   * request does not contains any condition. The predicate applies to {@link Response} if it meets all the conditions
   * found (they all are logically AND bound).
   */
  @Nullable
  public static Predicate<Response> requestPredicate(@Nonnull final Request request) {
    checkNotNull(request);
    final List<Predicate<Response>> predicates = new ArrayList<>();
    final Predicate<Response> ifModifiedSince = ifModifiedSince(request);
    if (ifModifiedSince != null) {
      predicates.add(ifModifiedSince);
    }
    final Predicate<Response> ifUnmodifiedSince = ifUnmodifiedSince(request);
    if (ifUnmodifiedSince != null) {
      predicates.add(ifUnmodifiedSince);
    }
    final Predicate<Response> ifMatch = ifMatch(request);
    if (ifMatch != null) {
      predicates.add(ifMatch);
    }
    final Predicate<Response> ifNoneMatch = ifNoneMatch(request);
    if (ifNoneMatch != null) {
      predicates.add(ifNoneMatch);
    }

    if (!predicates.isEmpty()) {
      return Predicates.and(predicates);
    }
    return null;
  }

  @Nullable
  private static Predicate<Response> ifModifiedSince(final Request request) {
    final DateTime date = parseDateHeader(request.getHeaders().get(HttpHeaders.IF_MODIFIED_SINCE));
    if (date != null) {
      return new Predicate<Response>()
      {
        @Override
        public boolean apply(final Response response) {
          final DateTime lastModified = parseDateHeader(response.getHeaders().get(HttpHeaders.LAST_MODIFIED));
          if (lastModified != null) {
            return lastModified.isAfter(date);
          }
          return true;
        }

        @Override
        public String toString() {
          return HttpConditions.class.getSimpleName() + ".ifModifiedSince(" + date + ")";
        }
      };
    }
    return null;
  }

  @Nullable
  private static Predicate<Response> ifUnmodifiedSince(final Request request) {
    final DateTime date = parseDateHeader(request.getHeaders().get(HttpHeaders.IF_UNMODIFIED_SINCE));
    if (date != null) {
      return new Predicate<Response>()
      {
        @Override
        public boolean apply(final Response response) {
          final DateTime lastModified = parseDateHeader(response.getHeaders().get(HttpHeaders.LAST_MODIFIED));
          if (lastModified != null) {
            return !lastModified.isAfter(date);
          }
          return true;
        }

        @Override
        public String toString() {
          return HttpConditions.class.getSimpleName() + ".ifUnmodifiedSince(" + date + ")";
        }
      };
    }
    return null;
  }

  @Nullable
  private static Predicate<Response> ifMatch(final Request request) {
    final String match = request.getHeaders().get(HttpHeaders.IF_MATCH);
    if (match != null && !"*".equals(match)) {
      return new Predicate<Response>()
      {
        @Override
        public boolean apply(final Response response) {
          final String etag = response.getHeaders().get(HttpHeaders.ETAG);
          if (etag != null) {
            return match.contains(etag);
          }
          return true;
        }

        @Override
        public String toString() {
          return HttpConditions.class.getSimpleName() + ".ifMatch(" + match + ")";
        }
      };
    }
    return null;
  }

  @Nullable
  private static Predicate<Response> ifNoneMatch(final Request request) {
    final String match = request.getHeaders().get(HttpHeaders.IF_NONE_MATCH);
    if (match != null && !"*".equals(match)) {
      return new Predicate<Response>()
      {
        @Override
        public boolean apply(final Response response) {
          final String etag = response.getHeaders().get(HttpHeaders.ETAG);
          if (etag != null) {
            return !match.contains(etag);
          }
          return true;
        }

        @Override
        public String toString() {
          return HttpConditions.class.getSimpleName() + ".ifNoneMatch(" + match + ")";
        }
      };
    }
    return null;
  }

  @Nullable
  private static DateTime parseDateHeader(@Nullable final String httpDate) {
    if (!Strings.isNullOrEmpty(httpDate)) {
      final Date date = DateUtils.parseDate(httpDate);
      if (date != null) {
        return new DateTime(date.getTime());
      }
    }
    return null;
  }
}
