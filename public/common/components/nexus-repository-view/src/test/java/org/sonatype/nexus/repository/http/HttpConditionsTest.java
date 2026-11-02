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

import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.net.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

class HttpConditionsTest
{
  private static Request.Builder request(final String action) {
    return new Request.Builder().action(action).path("/foo");
  }

  /**
   * Builds a {@link Context} from the given request builder, running it through
   * {@link HttpConditions#makeUnconditional(Request)} first so that any conditional headers are stashed the same way
   * {@code ConditionalRequestHandler} stashes them before calling
   * {@link HttpConditions#maybeCreateConditionalResponse}.
   */
  private static Context context(final Request.Builder requestBuilder) {
    Request request = HttpConditions.makeUnconditional(requestBuilder.build());
    return new Context(mock(), request);
  }

  private static Response responseWithEtag(final String etag) {
    return new Response.Builder()
        .status(Status.success(200))
        .header(HttpHeaders.ETAG, etag)
        .build();
  }

  @Nested
  @DisplayName("isConditional")
  class IsConditional
  {
    @Test
    @DisplayName("returns false when no conditional headers are present")
    void noConditionalHeaders() {
      Request request = request(GET).build();

      assertFalse(HttpConditions.isConditional(request));
    }

    @Test
    @DisplayName("ignores unsupported headers")
    void unsupportedHeaderIsIgnored() {
      Request request = request(GET).header(HttpHeaders.ACCEPT, "text/plain").build();

      assertFalse(HttpConditions.isConditional(request));
    }

    @ParameterizedTest(name = "returns true when {0} is present")
    @ValueSource(strings = {
        HttpHeaders.IF_MODIFIED_SINCE,
        HttpHeaders.IF_UNMODIFIED_SINCE,
        HttpHeaders.IF_MATCH,
        HttpHeaders.IF_NONE_MATCH
    })
    void conditionalHeaderIsDetected(final String headerName) {
      Request request = request(GET).header(headerName, "value").build();

      assertTrue(HttpConditions.isConditional(request));
    }
  }

  @Nested
  @DisplayName("makeUnconditional / makeConditional")
  class MakeUnconditionalAndConditional
  {
    @Test
    @DisplayName("makeUnconditional rejects a null request")
    void makeUnconditionalRejectsNull() {
      assertThrows(NullPointerException.class, () -> HttpConditions.makeUnconditional(null));
    }

    @Test
    @DisplayName("makeConditional rejects a null request")
    void makeConditionalRejectsNull() {
      assertThrows(NullPointerException.class, () -> HttpConditions.makeConditional(null));
    }

    @Test
    @DisplayName("stashes conditional headers and removes them from the request")
    void stashesAndRemovesConditionalHeaders() {
      Request request = request(GET)
          .header(HttpHeaders.IF_NONE_MATCH, "\"abc\"")
          .header(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 16 Jun 2015 11:30:00 GMT")
          .header(HttpHeaders.ACCEPT, "text/plain")
          .build();

      Request result = HttpConditions.makeUnconditional(request);

      assertSame(request, result);
      assertFalse(result.getHeaders().contains(HttpHeaders.IF_NONE_MATCH));
      assertFalse(result.getHeaders().contains(HttpHeaders.IF_MODIFIED_SINCE));
      assertTrue(result.getHeaders().contains(HttpHeaders.ACCEPT));

      Headers stashed = (Headers) result.getAttributes().require(HttpConditions.HTTP_CONDITIONS);
      assertEquals("\"abc\"", stashed.get(HttpHeaders.IF_NONE_MATCH));
      assertEquals("Tue, 16 Jun 2015 11:30:00 GMT", stashed.get(HttpHeaders.IF_MODIFIED_SINCE));
    }

    @Test
    @DisplayName("stashes all values of a multi-valued header")
    void stashesAllValuesOfMultiValuedHeader() {
      Request request = request(GET).header(HttpHeaders.IF_MATCH, "\"a\"", "\"b\"").build();

      Request result = HttpConditions.makeUnconditional(request);

      Headers stashed = (Headers) result.getAttributes().require(HttpConditions.HTTP_CONDITIONS);
      assertEquals(List.of("\"a\"", "\"b\""), stashed.getAll(HttpHeaders.IF_MATCH));
    }

    @Test
    @DisplayName("restoring a multi-valued header duplicates its values once per stashed value")
    void makeConditionalDuplicatesValuesForMultiValuedHeader() {
      // makeConditional iterates one loop cycle per stashed *value* (not per key), re-appending the
      // full value list on every cycle since header set(...) appends rather than replaces - so an
      // N-valued header ends up with N copies of its values after being restored.
      Request request = request(GET).header(HttpHeaders.IF_MATCH, "\"a\"", "\"b\"").build();

      HttpConditions.makeUnconditional(request);
      Request result = HttpConditions.makeConditional(request);

      assertEquals(List.of("\"a\"", "\"b\"", "\"a\"", "\"b\""), result.getHeaders().getAll(HttpHeaders.IF_MATCH));
    }

    @Test
    @DisplayName("restores previously stashed headers")
    void makeConditionalRestoresStashedHeaders() {
      Request request = request(GET).header(HttpHeaders.IF_MATCH, "\"abc\"").build();

      HttpConditions.makeUnconditional(request);
      Request result = HttpConditions.makeConditional(request);

      assertSame(request, result);
      assertEquals("\"abc\"", result.getHeaders().get(HttpHeaders.IF_MATCH));
    }

    @Test
    @DisplayName("throws if called without a prior makeUnconditional")
    void makeConditionalWithoutStashThrows() {
      Request request = request(GET).build();

      assertThrows(IllegalStateException.class, () -> HttpConditions.makeConditional(request));
    }
  }

  @Nested
  @DisplayName("maybeCreateConditionalResponse")
  class MaybeCreateConditionalResponse
  {
    @Test
    @DisplayName("returns empty when the request has no conditional headers")
    void noConditionalHeadersYieldsEmpty() {
      Context context = context(request(GET));
      Response response = responseWithEtag("\"abc\"");

      Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

      assertTrue(result.isEmpty());
    }

    @Nested
    @DisplayName("If-Match")
    class IfMatch
    {
      @Test
      @DisplayName("matching ETag yields empty (request proceeds)")
      void matchingEtagYieldsEmpty() {
        Context context = context(request(PUT).header(HttpHeaders.IF_MATCH, "\"abc\""));
        Response response = responseWithEtag("\"abc\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @Test
      @DisplayName("non-matching ETag yields 412 Precondition Failed")
      void nonMatchingEtagYields412() {
        Context context = context(request(PUT).header(HttpHeaders.IF_MATCH, "\"other\""));
        Response response = responseWithEtag("\"abc\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        Response failed = result.orElseThrow();
        assertEquals(412, failed.getStatus().getCode());
        assertFalse(failed.getStatus().isSuccessful());
      }

      @Test
      @DisplayName("wildcard always matches")
      void wildcardAlwaysMatches() {
        Context context = context(request(PUT).header(HttpHeaders.IF_MATCH, "*"));
        Response response = responseWithEtag("\"abc\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @ParameterizedTest(name = "strips {0} suffix before comparing")
      @ValueSource(strings = {"--gzip", "--zstd", "--br"})
      @DisplayName("strips known compression suffixes before comparing")
      void stripsCompressionSuffixes(final String suffix) {
        Context context = context(
            request(PUT).header(HttpHeaders.IF_MATCH, "W/\"88a4d081b7626ecf964ecdde53dae571%s\"".formatted(suffix)));
        Response response = responseWithEtag("W/\"88a4d081b7626ecf964ecdde53dae571\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @Test
      @DisplayName("412 response retains original headers but drops the payload")
      void preconditionFailedRetainsHeadersDropsPayload() {
        Context context = context(request(PUT).header(HttpHeaders.IF_MATCH, "\"other\""));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.ETAG, "\"abc\"")
            .payload(new StringPayload("body", ContentTypes.TEXT_PLAIN))
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        Response failed = result.orElseThrow();
        assertEquals("\"abc\"", failed.getHeaders().get(HttpHeaders.ETAG));
        assertNull(failed.getPayload());
      }

      @Test
      @DisplayName("takes precedence over If-Unmodified-Since per RFC 7232")
      void takesPrecedenceOverIfUnmodifiedSince() {
        Context context = context(request(PUT)
            .header(HttpHeaders.IF_MATCH, "\"abc\"")
            .header(HttpHeaders.IF_UNMODIFIED_SINCE, "Tue, 16 Jun 2014 11:30:00 GMT"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.ETAG, "\"abc\"")
            // would fail If-Unmodified-Since if it were evaluated
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2015 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }
    }

    @Nested
    @DisplayName("If-Unmodified-Since")
    class IfUnmodifiedSince
    {
      @Test
      @DisplayName("resource modified after the given date yields 412 Precondition Failed")
      void modifiedAfterYields412() {
        Context context = context(request(PUT).header(HttpHeaders.IF_UNMODIFIED_SINCE,
            "Tue, 16 Jun 2014 11:30:00 GMT"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2015 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertEquals(412, result.orElseThrow().getStatus().getCode());
      }

      @Test
      @DisplayName("resource not modified after the given date yields empty")
      void notModifiedAfterYieldsEmpty() {
        Context context = context(request(PUT).header(HttpHeaders.IF_UNMODIFIED_SINCE,
            "Tue, 16 Jun 2015 11:30:00 GMT"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2014 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @Test
      @DisplayName("an unparseable header value does not fail the precondition")
      void invalidDateIsIgnored() {
        Context context = context(request(PUT).header(HttpHeaders.IF_UNMODIFIED_SINCE, "not-a-date"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2015 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @Test
      @DisplayName("is ignored when the response has no Last-Modified header")
      void ignoredWithoutResponseLastModified() {
        Context context = context(request(PUT).header(HttpHeaders.IF_UNMODIFIED_SINCE,
            "Tue, 16 Jun 2014 11:30:00 GMT"));
        Response response = new Response.Builder().status(Status.success(200)).build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }
    }

    @Nested
    @DisplayName("If-None-Match")
    class IfNoneMatch
    {
      @Test
      @DisplayName("matching ETag on GET yields 304 Not Modified with only the ETag header")
      void matchingOnGetYieldsNotModified() {
        Context context = context(request(GET).header(HttpHeaders.IF_NONE_MATCH, "\"abc\""));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.ETAG, "\"abc\"")
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2015 11:30:00 GMT")
            .payload(new StringPayload("body", ContentTypes.TEXT_PLAIN))
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        Response notModified = result.orElseThrow();
        assertEquals(304, notModified.getStatus().getCode());
        assertTrue(notModified.getStatus().isSuccessful());
        assertEquals("\"abc\"", notModified.getHeaders().get(HttpHeaders.ETAG));
        assertNull(notModified.getHeaders().get(HttpHeaders.LAST_MODIFIED));
        assertNull(notModified.getPayload());
      }

      @Test
      @DisplayName("matching ETag on HEAD yields 304 Not Modified")
      void matchingOnHeadYieldsNotModified() {
        Context context = context(request(HEAD).header(HttpHeaders.IF_NONE_MATCH, "\"abc\""));
        Response response = responseWithEtag("\"abc\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertEquals(304, result.orElseThrow().getStatus().getCode());
      }

      @Test
      @DisplayName("matching ETag on a non-GET/HEAD action yields 412 Precondition Failed")
      void matchingOnNonGetHeadYields412() {
        Context context = context(request(PUT).header(HttpHeaders.IF_NONE_MATCH, "\"abc\""));
        Response response = responseWithEtag("\"abc\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertEquals(412, result.orElseThrow().getStatus().getCode());
      }

      @Test
      @DisplayName("non-matching ETag yields empty")
      void nonMatchingYieldsEmpty() {
        Context context = context(request(GET).header(HttpHeaders.IF_NONE_MATCH, "\"other\""));
        Response response = responseWithEtag("\"abc\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @ParameterizedTest(name = "strips {0} suffix before comparing")
      @ValueSource(strings = {"--gzip", "--zstd", "--br"})
      @DisplayName("strips known compression suffixes before comparing")
      void stripsCompressionSuffixes(final String suffix) {
        Context context = context(request(GET).header(HttpHeaders.IF_NONE_MATCH,
            "W/\"88a4d081b7626ecf964ecdde53dae571%s\"".formatted(suffix)));
        Response response = responseWithEtag("W/\"88a4d081b7626ecf964ecdde53dae571\"");

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertEquals(304, result.orElseThrow().getStatus().getCode());
      }

      @Test
      @DisplayName("takes precedence over If-Modified-Since per RFC 7232")
      void takesPrecedenceOverIfModifiedSince() {
        Context context = context(request(GET)
            .header(HttpHeaders.IF_NONE_MATCH, "\"other\"")
            .header(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 16 Jun 2015 11:30:00 GMT"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.ETAG, "\"abc\"")
            // would yield 304 if If-Modified-Since were evaluated
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2014 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }
    }

    @Nested
    @DisplayName("If-Modified-Since")
    class IfModifiedSince
    {
      @Test
      @DisplayName("resource modified after the given date yields empty")
      void modifiedAfterYieldsEmpty() {
        Context context = context(request(GET).header(HttpHeaders.IF_MODIFIED_SINCE,
            "Tue, 16 Jun 2014 11:30:00 GMT"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2015 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @Test
      @DisplayName("resource not modified since the given date yields 304 Not Modified for GET")
      void notModifiedYieldsNotModifiedForGet() {
        Context context = context(request(GET).header(HttpHeaders.IF_MODIFIED_SINCE,
            "Tue, 16 Jun 2015 11:30:00 GMT"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2014 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertEquals(304, result.orElseThrow().getStatus().getCode());
      }

      @Test
      @DisplayName("equal Last-Modified and If-Modified-Since dates yield 304 Not Modified")
      void equalDatesYieldNotModified() {
        String date = "Tue, 16 Jun 2015 11:30:00 GMT";
        Context context = context(request(GET).header(HttpHeaders.IF_MODIFIED_SINCE, date));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, date)
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertEquals(304, result.orElseThrow().getStatus().getCode());
      }

      @Test
      @DisplayName("is ignored for actions other than GET/HEAD")
      void ignoredForNonGetHeadActions() {
        Context context = context(request(PUT).header(HttpHeaders.IF_MODIFIED_SINCE,
            "Tue, 16 Jun 2015 11:30:00 GMT"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2014 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @Test
      @DisplayName("an unparseable header value is treated as modified")
      void invalidDateIsTreatedAsModified() {
        Context context = context(request(GET).header(HttpHeaders.IF_MODIFIED_SINCE, "not-a-date"));
        Response response = new Response.Builder()
            .status(Status.success(200))
            .header(HttpHeaders.LAST_MODIFIED, "Tue, 16 Jun 2015 11:30:00 GMT")
            .build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }

      @Test
      @DisplayName("is ignored when the response has no Last-Modified header")
      void ignoredWithoutResponseLastModified() {
        Context context = context(request(GET).header(HttpHeaders.IF_MODIFIED_SINCE,
            "Tue, 16 Jun 2015 11:30:00 GMT"));
        Response response = new Response.Builder().status(Status.success(200)).build();

        Optional<Response> result = HttpConditions.maybeCreateConditionalResponse(context, response);

        assertTrue(result.isEmpty());
      }
    }
  }
}
