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
package org.sonatype.nexus.repository.content.facet;

import java.io.IOException;
import java.net.URI;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ContentProxyFacetSupport}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentProxyFacetSupportTest
{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private Context context;

  @Mock
  private CacheInfo cacheInfo;

  @Mock
  private HttpClientFacet httpClientFacet;

  @Mock
  private HttpClient httpClient;

  @Mock
  private Format format;

  @Mock
  private Type type;

  private ContentProxyFacetSupport underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ContentProxyFacetSupport()
    {
      @Override
      protected Content getCachedContent(final Context context) {
        return null;
      }

      @Override
      protected Content store(final Context context, final Content content) {
        return content;
      }

      @Override
      protected String getUrl(final Context context) {
        return "/test/path";
      }
    };

    underTest.attach(repository);
  }

  // --- indicateVerified tests ---

  @Test
  public void indicateVerified_withAssetInAttributes_marksCached() throws IOException {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    Asset asset = mock(Asset.class);
    attributes.set(Asset.class, asset);
    when(content.getAttributes()).thenReturn(attributes);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);

    underTest.indicateVerified(context, content, cacheInfo);

    verify(fluentAsset).markAsCached(cacheInfo);
  }

  @Test
  public void indicateVerified_withoutAssetInAttributes_doesNotThrow() throws IOException {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    when(content.getAttributes()).thenReturn(attributes);

    // Should not throw and should not interact with content facet
    underTest.indicateVerified(context, content, cacheInfo);

    verify(repository, never()).facet(ContentFacet.class);
  }

  @Test
  public void indicateVerified_withNullAsset_logsDebugAndSkips() throws IOException {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    // Asset.class key exists but is null
    attributes.set(Asset.class, null);
    when(content.getAttributes()).thenReturn(attributes);

    underTest.indicateVerified(context, content, cacheInfo);

    verify(repository, never()).facet(ContentFacet.class);
  }

  // --- getPayload tests ---

  @Test
  public void getPayload_withOkResponse_returnsHttpEntityPayload() throws IOException {
    URI uri = URI.create("http://remote.example.com/artifact.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity entity = mock(HttpEntity.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(entity);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    Payload result = underTest.getPayload(repository, uri);

    assertThat(result, is(instanceOf(HttpEntityPayload.class)));
  }

  @Test
  public void getPayload_withNonOkResponse_returnsNull() throws IOException {
    URI uri = URI.create("http://remote.example.com/missing.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    try (MockedStatic<HttpClientUtils> utils = mockStatic(HttpClientUtils.class)) {
      Payload result = underTest.getPayload(repository, uri);

      assertThat(result, is(nullValue()));
      utils.verify(() -> HttpClientUtils.closeQuietly(response));
    }
  }

  @Test
  public void getPayload_withServerError_returnsNullAndClosesResponse() throws IOException {
    URI uri = URI.create("http://remote.example.com/error");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("npm");
    when(type.getValue()).thenReturn("proxy");

    try (MockedStatic<HttpClientUtils> utils = mockStatic(HttpClientUtils.class)) {
      Payload result = underTest.getPayload(repository, uri);

      assertThat(result, is(nullValue()));
      utils.verify(() -> HttpClientUtils.closeQuietly(response));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void getPayload_withOkResponseButNullEntity_throwsIllegalState() throws IOException {
    URI uri = URI.create("http://remote.example.com/no-entity");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(null);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    underTest.getPayload(repository, uri);
  }

  @Test
  public void getPayload_withNullFormat_doesNotPopulateContext() throws IOException {
    URI uri = URI.create("http://remote.example.com/artifact.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity entity = mock(HttpEntity.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(entity);
    when(repository.getFormat()).thenReturn(null);
    when(repository.getType()).thenReturn(type);

    Payload result = underTest.getPayload(repository, uri);

    assertThat(result, is(instanceOf(HttpEntityPayload.class)));
  }

  @Test
  public void getPayload_withNullType_doesNotPopulateContext() throws IOException {
    URI uri = URI.create("http://remote.example.com/artifact.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity entity = mock(HttpEntity.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(entity);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(null);

    Payload result = underTest.getPayload(repository, uri);

    assertThat(result, is(instanceOf(HttpEntityPayload.class)));
  }

  @Test
  public void getPayload_withUnauthorized_returnsNull() throws IOException {
    URI uri = URI.create("http://remote.example.com/private.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    try (MockedStatic<HttpClientUtils> utils = mockStatic(HttpClientUtils.class)) {
      Payload result = underTest.getPayload(repository, uri);

      assertThat(result, is(nullValue()));
      utils.verify(() -> HttpClientUtils.closeQuietly(response));
    }
  }

  @Test(expected = IOException.class)
  public void getPayload_withClientExecuteException_propagatesIOException() throws IOException {
    URI uri = URI.create("http://remote.example.com/fail");

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class)))
        .thenThrow(new IOException("Connection refused"));
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    underTest.getPayload(repository, uri);
  }

  // --- isNotModified tests (ETag-based blob reuse) ---

  /**
   * Verifies that HTTP 304 Not Modified returns true (parent behavior).
   */
  @Test
  public void isNotModified_with304_returnsTrue() {
    HttpResponse response = mockResponseWithEtag(HttpStatus.SC_NOT_MODIFIED, null);
    assertThat(underTest.isNotModified(response, null), is(true));
  }

  /**
   * Verifies that HTTP 200 OK with matching ETag returns true (blob reuse).
   * This is the main test for the blob reuse optimization.
   */
  @Test
  public void isNotModified_with200AndMatchingEtag_returnsTrue() {
    HttpResponse response = mockResponseWithEtag(HttpStatus.SC_OK, "abc123");
    Content stale = mockContentWithEtag("abc123");
    assertThat(underTest.isNotModified(response, stale), is(true));
  }

  /**
   * Verifies that HTTP 200 OK with mismatched ETag returns false (new blob needed).
   */
  @Test
  public void isNotModified_with200AndMismatchedEtag_returnsFalse() {
    HttpResponse response = mockResponseWithEtag(HttpStatus.SC_OK, "abc123");
    Content stale = mockContentWithEtag("xyz789");
    assertThat(underTest.isNotModified(response, stale), is(false));
  }

  /**
   * Verifies that HTTP 200 OK without ETag header returns false.
   */
  @Test
  public void isNotModified_with200AndNoEtag_returnsFalse() {
    HttpResponse response = mockResponseWithEtag(HttpStatus.SC_OK, null);
    Content stale = mockContentWithEtag("abc123");
    assertThat(underTest.isNotModified(response, stale), is(false));
  }

  /**
   * Verifies that HTTP 200 OK without stale content returns false.
   */
  @Test
  public void isNotModified_with200AndNoStale_returnsFalse() {
    HttpResponse response = mockResponseWithEtag(HttpStatus.SC_OK, "abc123");
    assertThat(underTest.isNotModified(response, null), is(false));
  }

  /**
   * Verifies that HTTP 200 OK with stale content having no cached ETag returns false.
   */
  @Test
  public void isNotModified_with200AndNoCachedEtag_returnsFalse() {
    HttpResponse response = mockResponseWithEtag(HttpStatus.SC_OK, "abc123");
    Content stale = mockContentWithEtag(null);
    assertThat(underTest.isNotModified(response, stale), is(false));
  }

  /**
   * Verifies that HTTP 404 Not Found returns false.
   */
  @Test
  public void isNotModified_with404_returnsFalse() {
    HttpResponse response = mockResponseWithEtag(HttpStatus.SC_NOT_FOUND, null);
    Content stale = mockContentWithEtag("abc123");
    assertThat(underTest.isNotModified(response, stale), is(false));
  }

  /**
   * Verifies that lowercase 'etag' header is handled (Azure Blob Storage compatibility).
   */
  @Test
  public void isNotModified_withLowercaseEtag_returnsTrue() {
    HttpResponse response = mockResponseWithLowercaseEtag(HttpStatus.SC_OK, "abc123");
    Content stale = mockContentWithEtag("abc123");
    assertThat(underTest.isNotModified(response, stale), is(true));
  }

  // --- isNotModified tests (Last-Modified based blob reuse) ---

  /**
   * Verifies that HTTP 200 OK with matching Last-Modified returns true (blob reuse).
   * This tests the fallback mechanism when ETag is not available (like Packagist).
   */
  @Test
  public void isNotModified_with200AndMatchingLastModified_returnsTrue() {
    DateTime lastModified = new DateTime(2026, 1, 2, 8, 56, 24, DateTimeZone.UTC);
    HttpResponse response = mockResponseWithLastModified(HttpStatus.SC_OK, "Fri, 02 Jan 2026 08:56:24 GMT");
    Content stale = mockContentWithLastModified(lastModified);
    assertThat(underTest.isNotModified(response, stale), is(true));
  }

  /**
   * Verifies that HTTP 200 OK with older Last-Modified than cached returns true (blob reuse).
   * If the server's Last-Modified is older, the cached version is still valid.
   */
  @Test
  public void isNotModified_with200AndOlderLastModified_returnsTrue() {
    DateTime cachedLastModified = new DateTime(2026, 1, 5, 10, 0, 0, DateTimeZone.UTC);
    HttpResponse response = mockResponseWithLastModified(HttpStatus.SC_OK, "Fri, 02 Jan 2026 08:56:24 GMT");
    Content stale = mockContentWithLastModified(cachedLastModified);
    assertThat(underTest.isNotModified(response, stale), is(true));
  }

  /**
   * Verifies that HTTP 200 OK with newer Last-Modified returns false (new blob needed).
   */
  @Test
  public void isNotModified_with200AndNewerLastModified_returnsFalse() {
    DateTime cachedLastModified = new DateTime(2026, 1, 1, 0, 0, 0, DateTimeZone.UTC);
    HttpResponse response = mockResponseWithLastModified(HttpStatus.SC_OK, "Fri, 02 Jan 2026 08:56:24 GMT");
    Content stale = mockContentWithLastModified(cachedLastModified);
    assertThat(underTest.isNotModified(response, stale), is(false));
  }

  /**
   * Verifies that HTTP 200 OK without Last-Modified header returns false.
   */
  @Test
  public void isNotModified_with200AndNoLastModified_returnsFalse() {
    HttpResponse response = mockResponseWithLastModified(HttpStatus.SC_OK, null);
    Content stale = mockContentWithLastModified(new DateTime(2026, 1, 2, 8, 56, 24, DateTimeZone.UTC));
    assertThat(underTest.isNotModified(response, stale), is(false));
  }

  /**
   * Verifies that HTTP 200 OK with stale content having no cached Last-Modified returns false.
   */
  @Test
  public void isNotModified_with200AndNoCachedLastModified_returnsFalse() {
    HttpResponse response = mockResponseWithLastModified(HttpStatus.SC_OK, "Fri, 02 Jan 2026 08:56:24 GMT");
    Content stale = mockContentWithLastModified(null);
    assertThat(underTest.isNotModified(response, stale), is(false));
  }

  /**
   * Verifies that lowercase 'last-modified' header is handled.
   */
  @Test
  public void isNotModified_withLowercaseLastModified_returnsTrue() {
    DateTime lastModified = new DateTime(2026, 1, 2, 8, 56, 24, DateTimeZone.UTC);
    HttpResponse response = mockResponseWithLowercaseLastModified(HttpStatus.SC_OK, "Fri, 02 Jan 2026 08:56:24 GMT");
    Content stale = mockContentWithLastModified(lastModified);
    assertThat(underTest.isNotModified(response, stale), is(true));
  }

  /**
   * Verifies that ETag takes precedence over Last-Modified when both are present and ETag matches.
   */
  @Test
  public void isNotModified_withBothEtagAndLastModified_etagTakesPrecedence() {
    HttpResponse response = mockResponseWithBothHeaders(HttpStatus.SC_OK, "abc123", "Fri, 02 Jan 2026 08:56:24 GMT");
    Content stale = mockContentWithBothHeaders("abc123", new DateTime(2026, 1, 1, 0, 0, 0, DateTimeZone.UTC));
    // ETag matches, so should return true even though Last-Modified would indicate newer content
    assertThat(underTest.isNotModified(response, stale), is(true));
  }

  /**
   * Verifies that when ETag doesn't match but Last-Modified does, returns true.
   */
  @Test
  public void isNotModified_withMismatchedEtagButMatchingLastModified_returnsTrue() {
    HttpResponse response =
        mockResponseWithBothHeaders(HttpStatus.SC_OK, "different-etag", "Fri, 02 Jan 2026 08:56:24 GMT");
    DateTime cachedLastModified = new DateTime(2026, 1, 2, 8, 56, 24, DateTimeZone.UTC);
    Content stale = mockContentWithBothHeaders("original-etag", cachedLastModified);
    // ETag doesn't match, but Last-Modified matches, so should still reuse blob
    assertThat(underTest.isNotModified(response, stale), is(true));
  }

  private static HttpResponse mockResponseWithEtag(final int statusCode, final String etag) {
    HttpResponse response = mock(HttpResponse.class);
    StatusLine status = mock(StatusLine.class);
    when(status.getStatusCode()).thenReturn(statusCode);
    when(response.getStatusLine()).thenReturn(status);

    if (etag != null) {
      when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(true);
      when(response.getFirstHeader(HttpHeaders.ETAG)).thenReturn(new BasicHeader(HttpHeaders.ETAG, etag));
    }
    else {
      when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(false);
      when(response.containsHeader("etag")).thenReturn(false);
    }

    return response;
  }

  private static HttpResponse mockResponseWithLowercaseEtag(final int statusCode, final String etag) {
    HttpResponse response = mock(HttpResponse.class);
    StatusLine status = mock(StatusLine.class);
    when(status.getStatusCode()).thenReturn(statusCode);
    when(response.getStatusLine()).thenReturn(status);

    when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(false);
    when(response.containsHeader("etag")).thenReturn(true);
    when(response.getFirstHeader(HttpHeaders.ETAG)).thenReturn(null);
    when(response.getFirstHeader("etag")).thenReturn(new BasicHeader("etag", etag));

    return response;
  }

  private static Content mockContentWithEtag(final String etag) {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    when(content.getAttributes()).thenReturn(attributes);
    if (etag != null) {
      attributes.set(Content.CONTENT_ETAG, etag);
    }
    return content;
  }

  private static HttpResponse mockResponseWithLastModified(final int statusCode, final String lastModified) {
    HttpResponse response = mock(HttpResponse.class);
    StatusLine status = mock(StatusLine.class);
    when(status.getStatusCode()).thenReturn(statusCode);
    when(response.getStatusLine()).thenReturn(status);

    // No ETag
    when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(false);
    when(response.containsHeader("etag")).thenReturn(false);
    when(response.getFirstHeader(HttpHeaders.ETAG)).thenReturn(null);
    when(response.getFirstHeader("etag")).thenReturn(null);

    if (lastModified != null) {
      when(response.getFirstHeader(HttpHeaders.LAST_MODIFIED)).thenReturn(
          new BasicHeader(HttpHeaders.LAST_MODIFIED, lastModified));
    }
    else {
      when(response.getFirstHeader(HttpHeaders.LAST_MODIFIED)).thenReturn(null);
      when(response.getFirstHeader("last-modified")).thenReturn(null);
    }

    return response;
  }

  private static HttpResponse mockResponseWithLowercaseLastModified(final int statusCode, final String lastModified) {
    HttpResponse response = mock(HttpResponse.class);
    StatusLine status = mock(StatusLine.class);
    when(status.getStatusCode()).thenReturn(statusCode);
    when(response.getStatusLine()).thenReturn(status);

    // No ETag
    when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(false);
    when(response.containsHeader("etag")).thenReturn(false);
    when(response.getFirstHeader(HttpHeaders.ETAG)).thenReturn(null);
    when(response.getFirstHeader("etag")).thenReturn(null);

    when(response.getFirstHeader(HttpHeaders.LAST_MODIFIED)).thenReturn(null);
    when(response.getFirstHeader("last-modified")).thenReturn(
        new BasicHeader("last-modified", lastModified));

    return response;
  }

  private static HttpResponse mockResponseWithBothHeaders(
      final int statusCode,
      final String etag,
      final String lastModified)
  {
    HttpResponse response = mock(HttpResponse.class);
    StatusLine status = mock(StatusLine.class);
    when(status.getStatusCode()).thenReturn(statusCode);
    when(response.getStatusLine()).thenReturn(status);

    if (etag != null) {
      when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(true);
      when(response.getFirstHeader(HttpHeaders.ETAG)).thenReturn(new BasicHeader(HttpHeaders.ETAG, etag));
    }
    else {
      when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(false);
      when(response.containsHeader("etag")).thenReturn(false);
    }

    if (lastModified != null) {
      when(response.getFirstHeader(HttpHeaders.LAST_MODIFIED)).thenReturn(
          new BasicHeader(HttpHeaders.LAST_MODIFIED, lastModified));
    }
    else {
      when(response.getFirstHeader(HttpHeaders.LAST_MODIFIED)).thenReturn(null);
      when(response.getFirstHeader("last-modified")).thenReturn(null);
    }

    return response;
  }

  private static Content mockContentWithLastModified(final DateTime lastModified) {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    when(content.getAttributes()).thenReturn(attributes);
    if (lastModified != null) {
      attributes.set(Content.CONTENT_LAST_MODIFIED, lastModified);
    }
    return content;
  }

  private static Content mockContentWithBothHeaders(final String etag, final DateTime lastModified) {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    when(content.getAttributes()).thenReturn(attributes);
    if (etag != null) {
      attributes.set(Content.CONTENT_ETAG, etag);
    }
    if (lastModified != null) {
      attributes.set(Content.CONTENT_LAST_MODIFIED, lastModified);
    }
    return content;
  }
}
