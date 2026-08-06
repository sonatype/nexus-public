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
package org.sonatype.nexus.content.raw.internal.recipe;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.template.EscapeHelper;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RawProxyFacetTest
{
  private RawProxyFacet rawProxyFacet;

  private QueryParameterForwardingHelper queryParamHelper;

  @Before
  public void setUp() throws Exception {
    rawProxyFacet = new RawProxyFacet();
    // Initialize EscapeHelper which is needed for URL encoding
    ReflectionTestUtils.setField(rawProxyFacet, "escapeHelper", new EscapeHelper((String) null));
    queryParamHelper = rawProxyFacet.queryParamHelper;
  }

  /**
   * Creates a mocked Context that returns the given asset path via TokenMatcher.State and returns the given Parameters
   * from the request.
   */
  private Context mockContext(final String assetPath, final Parameters parameters) {
    Context context = mock(Context.class);
    Request request = mock(Request.class);
    AttributesMap attributes = new AttributesMap();

    TokenMatcher.State tokenState = mock(TokenMatcher.State.class);
    Map<String, String> tokens = Collections.singletonMap(RawRecipeSupport.PATH_NAME, assetPath);
    when(tokenState.getTokens()).thenReturn(tokens);
    attributes.set(TokenMatcher.State.class, tokenState);

    when(context.getAttributes()).thenReturn(attributes);
    when(context.getRequest()).thenReturn(request);
    when(request.getParameters()).thenReturn(parameters);
    return context;
  }

  @Test
  public void testEncodeUrlWithCaret() throws UnsupportedEncodingException {
    String url = "http://example.com/path^test";
    String expectedEncodedUrl = "http://example.com/path%5Etest";
    assertEncodedUrl(url, expectedEncodedUrl, "^");
  }

  @Test
  public void testEncodeUrlWithHash() throws UnsupportedEncodingException {
    String url = "http://example.com/path#test";
    String expectedEncodedUrl = "http://example.com/path%23test";
    assertEncodedUrl(url, expectedEncodedUrl, "#");
  }

  @Test
  public void testEncodeUrlWithQuestionMark() throws UnsupportedEncodingException {
    String url = "http://example.com/path?test";
    String expectedEncodedUrl = "http://example.com/path%3Ftest";
    assertEncodedUrl(url, expectedEncodedUrl, "?");
  }

  @Test
  public void testEncodedUrlWithNarrowNoBreakSpace() throws UnsupportedEncodingException {
    String url = "http://example.com/path\u202Ftest";
    String expectedEncodedUrl = "http://example.com/path%E2%80%AFtest";
    assertEncodedUrl(url, expectedEncodedUrl, "\u202F");
  }

  @Test
  public void testEncodedUrlWithLeftSquareBracket() throws UnsupportedEncodingException {
    String url = "http://example.com/path[test";
    String expectedEncodedUrl = "http://example.com/path%5Btest";
    assertEncodedUrl(url, expectedEncodedUrl, "[");
  }

  @Test
  public void testEncodedUrlWithRightSquareBracket() throws UnsupportedEncodingException {
    String url = "http://example.com/path]test";
    String expectedEncodedUrl = "http://example.com/path%5Dtest";
    assertEncodedUrl(url, expectedEncodedUrl, "]");
  }

  private void assertEncodedUrl(
      String url,
      String expectedEncodedUrl,
      String character) throws UnsupportedEncodingException
  {
    String actualEncodedUrl = rawProxyFacet.encodeUrl(url);
    assertEquals("Failed to encode character: " + character, expectedEncodedUrl, actualEncodedUrl);
  }

  // --- getUrl tests (NEXUS-49835) ---
  // After architecture fix, getUrl() returns only the path - query parameters
  // are now appended in buildFetchHttpRequest() to avoid encoding pipeline issues.

  /**
   * Integration test 1: getUrl() with no query parameters returns path only.
   */
  @Test
  public void getUrl_noQueryParameters_returnsPathOnly() {
    Parameters parameters = new Parameters();
    Context context = mockContext("/some/file.txt", parameters);

    String url = rawProxyFacet.getUrl(context);

    assertThat(url, is("some/file.txt"));
    assertThat(url, not(containsString("?")));
  }

  /**
   * Integration test 2: getUrl() with query parameters returns ONLY path (no query string). Query string is appended
   * later in buildFetchHttpRequest().
   */
  @Test
  public void getUrl_withQueryParameters_returnsPathOnly() {
    Parameters parameters = new Parameters();
    parameters.set("version", "1.2.3");
    Context context = mockContext("/file.txt", parameters);

    String url = rawProxyFacet.getUrl(context);

    assertThat(url, is("file.txt"));
    assertThat(url, not(containsString("?")));
    assertThat(url, not(containsString("version")));
  }

  // --- buildFetchHttpRequest tests (NEXUS-49835) ---
  // These tests verify that query parameters are appended to the URI AFTER
  // the encoding pipeline has run, preventing ? from being encoded to %3F.

  /**
   * Test buildFetchHttpRequest with no query parameters - URI unchanged.
   */
  @Test
  public void buildFetchHttpRequest_noQueryParameters_uriUnchanged() {
    Parameters parameters = new Parameters();
    Context context = mockContext("/file.txt", parameters);
    URI uri = URI.create("http://example.com/file.txt");

    HttpRequestBase request = rawProxyFacet.buildFetchHttpRequest(uri, context, null);

    assertThat(request, instanceOf(HttpGet.class));
    assertThat(request.getURI().toString(), is("http://example.com/file.txt"));
    assertThat(request.getURI().toString(), not(containsString("?")));
  }

  /**
   * Test buildFetchHttpRequest with query parameters - appends to URI.
   */
  @Test
  public void buildFetchHttpRequest_withQueryParameters_appendsToUri() throws Exception {
    configureQueryParamForwarding(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("page", "1");
    parameters.set("limit", "10");
    Context context = mockContext("/api/data", parameters);
    URI uri = URI.create("http://example.com/api/data");

    HttpRequestBase request = rawProxyFacet.buildFetchHttpRequest(uri, context, null);

    String finalUri = request.getURI().toString();
    assertThat(finalUri, containsString("?"));
    assertThat(finalUri, containsString("page=1"));
    assertThat(finalUri, containsString("limit=10"));
    assertThat(finalUri, containsString("&"));
  }

  /**
   * Test buildFetchHttpRequest with query parameters containing spaces - they are URL-encoded.
   */
  @Test
  public void buildFetchHttpRequest_queryParameterWithSpaces_encodedProperly() throws Exception {
    configureQueryParamForwarding(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("q", "hello world");
    Context context = mockContext("/search", parameters);
    URI uri = URI.create("http://example.com/search");

    HttpRequestBase request = rawProxyFacet.buildFetchHttpRequest(uri, context, null);

    // URLEncoder uses + for spaces (equivalent to %20)
    assertThat(request.getURI().toString(), is("http://example.com/search?q=hello+world"));
  }

  /**
   * Test buildFetchHttpRequest with stale content - calls super with modified URI.
   */
  @Test
  public void buildFetchHttpRequest_withStaleContent_callsSuperWithModifiedUri() throws Exception {
    configureQueryParamForwarding(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("key", "value");
    Context context = mockContext("/file.txt", parameters);
    URI uri = URI.create("http://example.com/file.txt");
    Content stale = mock(Content.class);
    // Mock getAttributes to return empty AttributesMap (parent method needs this)
    when(stale.getAttributes()).thenReturn(new AttributesMap());

    HttpRequestBase request = rawProxyFacet.buildFetchHttpRequest(uri, context, stale);

    // Verify query parameters are appended even when stale content is provided
    assertThat(request.getURI().toString(), is("http://example.com/file.txt?key=value"));
  }

  /**
   * Test: Integration - buildFetchHttpRequest with exclusions.
   */
  @Test
  public void buildFetchHttpRequest_withExclusions_filtersExcludedParams() throws Exception {
    configureQueryParamForwarding(true, Collections.singletonList("token"));

    Parameters parameters = new Parameters();
    parameters.set("token", "secret");
    parameters.set("version", "1.0");
    Context context = mockContext("/file.txt", parameters);
    URI uri = URI.create("http://example.com/file.txt");

    HttpRequestBase request = rawProxyFacet.buildFetchHttpRequest(uri, context, null);

    String finalUri = request.getURI().toString();
    assertThat(finalUri, not(containsString("token")));
    assertThat(finalUri, containsString("version=1.0"));
  }

  // --- Caching behavior with query parameters tests ---

  /**
   * Test: cacheKey includes query parameters when forwarding is enabled.
   */
  @Test
  public void cacheKey_withQueryParamsForwardingEnabled_includesQueryParams() throws Exception {
    configureQueryParamForwarding(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("platform", "win32");
    Context context = mockContext("/desktop/3.8.3", parameters);

    assertThat("Cache key should include query parameters", cacheKey(rawProxyFacet, context),
        is("/desktop/3.8.3?platform=win32"));
  }

  /**
   * Test: cacheKey with forwarding disabled uses path only (ignores query params).
   */
  @Test
  public void cacheKey_withQueryParamsForwardingDisabled_usesPathOnly() throws Exception {
    configureQueryParamForwarding(false, Collections.emptyList());

    Parameters parameters = new Parameters();
    parameters.set("platform", "win32");
    Context context = mockContext("/desktop/3.8.3", parameters);

    assertThat("Cache key should be path only when forwarding is disabled", cacheKey(rawProxyFacet, context),
        is("/desktop/3.8.3"));
  }

  /**
   * Test: cacheKey without query params uses path only.
   */
  @Test
  public void cacheKey_withoutQueryParams_usesPathOnly() throws Exception {
    configureQueryParamForwarding(true, Collections.emptyList());

    Parameters parameters = new Parameters();
    Context context = mockContext("/desktop/3.8.3", parameters);

    assertThat("Cache key should be path only when no query params", cacheKey(rawProxyFacet, context),
        is("/desktop/3.8.3"));
  }

  /**
   * Test: cacheKey with only excluded params uses path only (excluded params not in cache key).
   */
  @Test
  public void cacheKey_withOnlyExcludedParams_usesPathOnly() throws Exception {
    configureQueryParamForwarding(true, Arrays.asList("api_key", "session_id"));

    Parameters parameters = new Parameters();
    parameters.set("api_key", "secret123");
    parameters.set("session_id", "abc");
    Context context = mockContext("/desktop/3.8.3", parameters);

    assertThat("Cache key should be path only when all params are excluded", cacheKey(rawProxyFacet, context),
        is("/desktop/3.8.3"));
  }

  /**
   * Test: cacheKey with mixed (excluded + forwarded) params includes only forwarded params.
   */
  @Test
  public void cacheKey_withMixedParams_includesOnlyForwardedParams() throws Exception {
    configureQueryParamForwarding(true, Collections.singletonList("api_key"));

    Parameters parameters = new Parameters();
    parameters.set("api_key", "secret123");
    parameters.set("platform", "darwin");
    Context context = mockContext("/desktop/3.8.3", parameters);

    assertThat("Cache key should include only forwarded params", cacheKey(rawProxyFacet, context),
        is("/desktop/3.8.3?platform=darwin"));
  }

  /**
   * Test: Different query params produce different cache keys (no collision).
   */
  @Test
  public void cacheKey_differentQueryParams_produceDifferentKeys() throws Exception {
    configureQueryParamForwarding(true, Collections.emptyList());

    Parameters params1 = new Parameters();
    params1.set("platform", "win32");
    Context context1 = mockContext("/desktop/3.8.3", params1);

    Parameters params2 = new Parameters();
    params2.set("platform", "darwin");
    Context context2 = mockContext("/desktop/3.8.3", params2);

    String key1 = cacheKey(rawProxyFacet, context1);
    String key2 = cacheKey(rawProxyFacet, context2);

    assertThat("Different query params should produce different cache keys", key1, is(not(key2)));
    assertThat(key1, is("/desktop/3.8.3?platform=win32"));
    assertThat(key2, is("/desktop/3.8.3?platform=darwin"));
  }

  /**
   * Test: Same query params in different order produce the same cache key (sorted).
   */
  @Test
  public void cacheKey_sameParamsReordered_produceSameKey() throws Exception {
    configureQueryParamForwarding(true, Collections.emptyList());

    Parameters params1 = new Parameters();
    params1.set("a", "1");
    params1.set("b", "2");
    Context context1 = mockContext("/path", params1);

    Parameters params2 = new Parameters();
    params2.set("b", "2");
    params2.set("a", "1");
    Context context2 = mockContext("/path", params2);

    String key1 = cacheKey(rawProxyFacet, context1);
    String key2 = cacheKey(rawProxyFacet, context2);

    assertThat("Same params in different order should produce same cache key", key1, is(key2));
  }

  // --- Feature flag gate tests (NEXUS-49835) ---

  /**
   * Test: When the system feature flag is disabled, query parameter forwarding is always disabled regardless of
   * per-repo configuration.
   */
  @Test
  public void configureQueryParamForwarding_featureFlagDisabled_alwaysDisablesForwarding() throws Exception {
    Configuration configuration = mock(Configuration.class);
    NestedAttributesMap rawAttributes = new NestedAttributesMap("raw", new java.util.HashMap<>());
    rawAttributes.set("forwardQueryParameters", Boolean.TRUE);
    when(configuration.attributes("raw")).thenReturn(rawAttributes);

    configureQueryParamForwarding(rawProxyFacet, configuration);

    Parameters parameters = new Parameters();
    parameters.set("platform", "win32");
    Context context = mockContext("/desktop/3.8.3", parameters);

    assertThat("Feature flag disabled should prevent query param forwarding",
        cacheKey(rawProxyFacet, context), is("/desktop/3.8.3"));
  }

  /**
   * Configures the facet's query parameter forwarding helper for testing.
   */
  private void configureQueryParamForwarding(
      final boolean forwardQueryParameters,
      final List<String> excludedQueryParameters)
  {
    queryParamHelper.updateConfig(forwardQueryParameters,
        excludedQueryParameters != null ? excludedQueryParameters : Collections.emptyList());
  }

  private static void configureQueryParamForwarding(final RawProxyFacet facet, final Configuration configuration) {
    ReflectionTestUtils.invokeMethod(facet, "configureQueryParamForwarding", configuration);
  }

  private static String cacheKey(final RawProxyFacet facet, final Context context) {
    return ReflectionTestUtils.invokeMethod(facet, "cacheKey", context);
  }
}
