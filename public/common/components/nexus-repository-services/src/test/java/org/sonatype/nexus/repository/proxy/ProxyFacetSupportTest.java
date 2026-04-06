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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.manager.RepositoryAttributeService;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.transaction.RetryDeniedException;
import org.sonatype.nexus.validation.ssrf.AntiSsrfHelper;
import org.sonatype.nexus.validation.ssrf.AntiSsrfHelper.SsrfValidationResult;

import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_NAME;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_VALUE;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.PROXY_THROTTLED_ANALYTICS_MARKED;

/**
 * Tests for the abstract class {@link ProxyFacetSupport}
 */
public class ProxyFacetSupportTest
    extends TestSupport
{
  @Mock
  ThrottlerInterceptor throttlerInterceptor;

  @Mock
  GracePeriodInterceptor gracePeriodInterceptor;

  @Spy
  @InjectMocks
  ProxyFacetSupport underTest = new ProxyFacetSupport()
  {
    @Nullable
    @Override
    protected Content getCachedContent(final Context context) throws IOException {
      return null;
    }

    @Override
    protected Content store(final Context context, final Content content) throws IOException {
      return null;
    }

    @Override
    protected void indicateVerified(
        final Context context,
        final Content content,
        final CacheInfo cacheInfo) throws IOException
    {
      // Method intentionally left empty as no specific behavior is required for this test.
    }

    @Override
    protected String getUrl(@Nonnull final Context context) {
      return null;
    }
  };

  @Mock
  Content content;

  @Mock
  StatusLine statusLine;

  @Mock
  Content reFetchedContent;

  @Mock
  Content storedContent;

  @Mock
  AttributesMap attributesMap;

  @Mock
  CacheInfo cacheInfo;

  @Mock
  Context cachedContext;

  @Mock
  AttributesMap cachedContextAttributesMap;

  @Mock
  Context missingContext;

  @Mock
  AttributesMap missingContextAttributesMap;

  @Mock
  CacheControllerHolder cacheControllerHolder;

  @Mock
  CacheController cacheController;

  @Mock
  Repository repository;

  @Mock
  EventManager eventManager;

  @Mock
  private Format format;

  @Mock
  private RepositoryAttributeService repositoryAttributeService;

  @Mock
  private AntiSsrfHelper antiSsrfHelper;

  private final ArgumentCaptor<ProxyThrottledRequestEvent> captor =
      ArgumentCaptor.forClass(ProxyThrottledRequestEvent.class);

  @Before
  public void setUp() throws Exception {
    when(content.getAttributes()).thenReturn(attributesMap);

    when(attributesMap.get(CacheInfo.class)).thenReturn(cacheInfo);

    when(cacheControllerHolder.getContentCacheController()).thenReturn(cacheController);

    when(cachedContext.getRepository()).thenReturn(repository);

    Request request = mock(Request.class);
    when(cachedContext.getRequest()).thenReturn(request);

    when(missingContext.getRepository()).thenReturn(repository);
    when(missingContext.getRequest()).thenReturn(request);

    underTest.cacheControllerHolder = cacheControllerHolder;
    when(format.getValue()).thenReturn("raw");
    when(repository.getFormat()).thenReturn(format);

    when(cachedContextAttributesMap.get("proxy.remote-fetch.skip")).thenReturn(false);
    when(missingContextAttributesMap.get("proxy.remote-fetch.skip")).thenReturn(false);
    when(cachedContext.getAttributes()).thenReturn(cachedContextAttributesMap);
    when(missingContext.getAttributes()).thenReturn(missingContextAttributesMap);

    when(antiSsrfHelper.validateHost(anyString()))
        .thenReturn(SsrfValidationResult.success());

    underTest.installDependencies(eventManager);
    underTest.attach(repository);
    DefaultCooperation2Factory cooperationFactory = new DefaultCooperation2Factory();
    underTest.configureCooperation(cooperationFactory, false, Duration.ofSeconds(0),
        Duration.ofSeconds(60), 10);
    underTest.buildCooperation();
  }

  @Test
  public void testGetRemoteFetchSkipNoContentHasFound() throws Exception {
    doReturn(null).when(underTest).getCachedContent(cachedContext);
    when(cachedContextAttributesMap.get("proxy.remote-fetch.skip"))
        .thenReturn(true);

    Content actual = underTest.get(cachedContext);
    assertNull(actual);
    verify(underTest, never()).fetch(any(), any(), any());
    verify(underTest, never()).store(any(), any());
  }

  @Test
  public void testGetRemoteFetchSkipContentHasFound() throws Exception {
    when(cachedContextAttributesMap.get("proxy.remote-fetch.skip"))
        .thenReturn(true);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    when(cacheController.isStale(cacheInfo)).thenReturn(false);

    Content actual = underTest.get(cachedContext);
    assertThat(actual, is(content));
    verify(underTest, never()).fetch(any(), any(), any());
    verify(underTest, never()).store(any(), any());
  }

  @Test
  public void testGetRemoteFetchSkipContentHasFoundWithInvalidCache() throws Exception {
    when(cachedContextAttributesMap.get("proxy.remote-fetch.skip"))
        .thenReturn(true);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    when(cacheController.isStale(cacheInfo)).thenReturn(true);

    Content actual = underTest.get(cachedContext);
    assertThat(actual, is(content));
    verify(underTest, never()).fetch(any(), any(), any());
    verify(underTest, never()).store(any(), any());
  }

  @Test
  public void testGet() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(false);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    Content foundContent = underTest.get(cachedContext);

    assertThat(foundContent, is(content));
  }

  @Test
  public void testGet_stale() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    doReturn(reFetchedContent).when(underTest).fetch(cachedContext, content);
    doReturn(storedContent).when(underTest).store(cachedContext, reFetchedContent);

    Content foundContent = underTest.get(cachedContext);

    assertThat(foundContent, is(storedContent));
  }

  @Test
  public void testGet_ProxyServiceException_contentReturnedIfCached() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    doThrow(new ProxyServiceException(new BasicHttpResponse(null, 503, "Offline")))
        .when(underTest)
        .fetch(cachedContext, content);

    Content foundContent = underTest.get(cachedContext);

    assertThat(foundContent, is(content));
  }

  @Test(expected = ProxyServiceException.class)
  public void testGet_ProxyServiceException_thrownIfNotCached() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(null).when(underTest).getCachedContent(cachedContext);

    doThrow(new ProxyServiceException(new BasicHttpResponse(null, 503, "Offline")))
        .when(underTest)
        .fetch(missingContext, null);

    underTest.get(missingContext);
  }

  @Test
  public void testGet_RemoteBlockedException_contentReturnedIfCached() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    doThrow(new RemoteBlockedIOException("blocked")).when(underTest).fetch(cachedContext, content);

    Content foundContent = underTest.get(cachedContext);

    assertThat(foundContent, is(content));
  }

  @Test(expected = RemoteBlockedIOException.class)
  public void testGet_RemoteBlockedException_thrownIfNotCached() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(null).when(underTest).getCachedContent(cachedContext);

    doThrow(new RemoteBlockedIOException("blocked")).when(underTest).fetch(missingContext, null);

    underTest.get(missingContext);
  }

  @Test
  public void testGet_IOException_contentReturnedIfCached() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    doThrow(new IOException()).when(underTest).fetch(cachedContext, content);

    Content foundContent = underTest.get(cachedContext);

    assertThat(foundContent, is(content));
  }

  @Test(expected = IOException.class)
  public void testGet_IOException_thrownIfNotCached() throws IOException {
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(null).when(underTest).getCachedContent(cachedContext);

    doThrow(new IOException()).when(underTest).fetch(missingContext, null);

    underTest.get(missingContext);
  }

  @Test
  public void testGet_MissingBlobException() throws IOException {
    RetryDeniedException e = new RetryDeniedException("Denied", new MissingBlobException(null));
    doThrow(e).when(underTest).getCachedContent(cachedContext);

    doReturn(reFetchedContent).when(underTest).fetch(cachedContext, null);
    doReturn(storedContent).when(underTest).store(cachedContext, reFetchedContent);

    Content foundContent = underTest.get(cachedContext);

    assertThat(foundContent, is(storedContent));
  }

  @Test(expected = RetryDeniedException.class)
  public void testGet_differentRetryReason() throws IOException {
    RetryDeniedException e = new RetryDeniedException("Denied", new IOException());
    doThrow(e).when(underTest).getCachedContent(cachedContext);

    underTest.get(cachedContext);
  }

  @Test
  public void testBuildLogMessage_ContentFound_WithStatusLine() {
    String message = underTest.buildLogContentMessage(content, statusLine);

    assertThat(message, containsString("Exception {} checking remote for update"));
    assertThat(message, containsString("proxy repo {} failed to fetch {} with status line {}"));
    assertThat(message, containsString("returning content from cache."));
  }

  @Test
  public void testBuildLogMessage_ContentFound_WithoutStatusLine() {
    String message = underTest.buildLogContentMessage(content, null);

    assertThat(message, containsString("Exception {} checking remote for update"));
    assertThat(message, containsString("proxy repo {} failed to fetch {}"));
    assertThat(message, containsString("returning content from cache."));
  }

  @Test
  public void testBuildLogMessage_ContentNotFound_WithStatusLine() {
    String message = underTest.buildLogContentMessage(null, statusLine);

    assertThat(message, containsString("Exception {} checking remote for update"));
    assertThat(message, containsString("proxy repo {} failed to fetch {} with status line {}"));
    assertThat(message, containsString("content not in cache."));
  }

  @Test
  public void testBuildLogMessage_ContentNotFound_WithoutStatusLine() {
    String message = underTest.buildLogContentMessage(null, null);

    assertThat(message, containsString("Exception {} checking remote for update"));
    assertThat(message, containsString("proxy repo {} failed to fetch {}"));
    assertThat(message, containsString("content not in cache."));
  }

  @Test
  public void whenCacheInfoIsNullThenIsStaleIsTrue() throws Exception {
    when(attributesMap.get(CacheInfo.class)).thenReturn(null);
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    doReturn(reFetchedContent).when(underTest).fetch(cachedContext, content);
    doReturn(storedContent).when(underTest).store(cachedContext, reFetchedContent);

    Content foundContent = underTest.get(cachedContext);

    assertThat(foundContent, is(storedContent));
  }

  @Test
  public void leak() throws Exception {
    HttpClientFacet httpClientFacet = mock(HttpClientFacet.class);
    HttpClient httpClient = mock(HttpClient.class);
    ConfigurationFacet configurationFacet = mock(ConfigurationFacet.class);
    ProxyFacetSupport.ProxyConfig config = new ProxyFacetSupport.ProxyConfig();
    config.remoteUrl = new URI("http://example.com");

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(configurationFacet.readSection(any(Configuration.class), anyString(), eq(ProxyFacetSupport.ProxyConfig.class)))
        .thenReturn(config);

    HttpResponse httpResponse = new BasicHttpResponse(
        new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 304, "NOT MODIFIED").getStatusLine());
    httpResponse.addHeader(BYPASS_HTTP_ERRORS_HEADER_NAME, BYPASS_HTTP_ERRORS_HEADER_VALUE);
    doReturn("http://example.com").when(underTest).getUrl(cachedContext);
    doReturn(httpResponse).when(underTest).execute(eq(cachedContext), eq(httpClient), any(HttpRequestBase.class));

    try (MockedStatic<HttpClientUtils> httpClientUtils = mockStatic(HttpClientUtils.class)) {
      Configuration configuration = mock(Configuration.class);
      when(configuration.attributes("proxy")).thenReturn(new NestedAttributesMap(
          "proxy",
          singletonMap("remoteUrl", "http://example.com")));
      underTest.setRepositoryAttributeService(repositoryAttributeService);
      underTest.doConfigure(configuration);
      underTest.doStart();

      try {
        underTest.get(cachedContext);
        fail("Expected BypassHttpErrorException to be thrown");
      }
      catch (BypassHttpErrorException expected) {
        // expected
      }

      httpClientUtils.verify(() -> HttpClientUtils.closeQuietly(httpResponse), times(1));
    }
  }

  @Test
  public void normalizeURLPath() throws Exception {
    assertEquals(
        URI.create("https://remoteserver/com/foo/this%20is%20a%20space/"),
        underTest.normalizeURLPath(URI.create("https://remoteserver/com/foo/this%20is%20a%20space/")));

    assertEquals(
        URI.create("https://remoteserver/com/foo/this%20is%20a%20space/"),
        underTest.normalizeURLPath(URI.create("https://remoteserver/com/foo/this%20is%20a%20space")));

    assertEquals(
        URI.create("https://remoteserver/com/foo/thisisaspace/"),
        underTest.normalizeURLPath(URI.create("https://remoteserver/com/foo/thisisaspace")));
  }

  @Test
  public void testGetPostsBlockedEvents() throws IOException {
    when(throttlerInterceptor.shouldBlock()).thenReturn(true);
    when(gracePeriodInterceptor.isInGracePeriod()).thenReturn(false);
    doReturn(null).when(underTest).getCachedContent(cachedContext);

    underTest.get(cachedContext);

    verify(eventManager).post(any(ProxyRequestEvent.class));
    verify(eventManager).post(captor.capture());
    assertThat(captor.getValue().isBlocked(), is(true));
  }

  @Test
  public void testGetPostsGracePeriodEvents() throws IOException {
    when(throttlerInterceptor.shouldBlock()).thenReturn(true);
    when(gracePeriodInterceptor.isInGracePeriod()).thenReturn(true);

    doReturn(content).when(underTest).getCachedContent(cachedContext);
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(content).when(underTest).get(cachedContext, content);

    underTest.get(cachedContext);

    verify(eventManager).post(any(ProxyRequestEvent.class));
    verify(eventManager).post(captor.capture());
    assertThat(captor.getValue().isBlocked(), is(false));
  }

  @Test
  public void extractUrls_singleLinkHeader() {
    HttpResponse response = mock(HttpResponse.class);
    Header header = new BasicHeader(HttpHeaders.LINK, "<http://example.com>");
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{header});

    List<String> urls = underTest.extractUrls(response);

    assertThat(urls.size(), is(1));
    assertThat(urls.get(0), is("http://example.com"));
  }

  @Test
  public void extractUrls_multipleLinkHeaders() {
    HttpResponse response = mock(HttpResponse.class);
    Header header1 = new BasicHeader(HttpHeaders.LINK, "<http://example.com>");
    Header header2 = new BasicHeader(HttpHeaders.LINK, "<http://example.org>");
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{header1, header2});

    List<String> urls = underTest.extractUrls(response);

    assertThat(urls.size(), is(2));
    assertThat(urls.get(0), is("http://example.com"));
    assertThat(urls.get(1), is("http://example.org"));
  }

  @Test
  public void extractUrls_noLinkHeaders() {
    HttpResponse response = mock(HttpResponse.class);
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{});

    List<String> urls = underTest.extractUrls(response);

    assertThat(urls.size(), is(0));
  }

  @Test
  public void extractUrls_malformedLinkHeader() {
    HttpResponse response = mock(HttpResponse.class);
    Header header = new BasicHeader(HttpHeaders.LINK, "malformed");
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{header});

    List<String> urls = underTest.extractUrls(response);

    assertThat(urls.size(), is(0));
  }

  @Test
  public void handle300MultipleChoicesError_returnsAlternativeContentWhenFound() throws IOException {
    URI uri = URI.create("http://example.com");
    HttpResponse response = mock(HttpResponse.class);
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{
        new BasicHeader(HttpHeaders.LINK, "<http://alternative.com>")
    });
    Content alternativeContent = mock(Content.class);
    doReturn(alternativeContent).when(underTest).fetch("http://alternative.com", cachedContext, content);

    Content result = underTest.handle300MultipleChoicesError(cachedContext, content, uri, response);

    assertThat(result, is(alternativeContent));
  }

  @Test
  public void handle300MultipleChoicesError_returnsNullWhenNoAlternativeContentFound() throws IOException {
    URI uri = URI.create("http://example.com");
    HttpResponse response = mock(HttpResponse.class);
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{
        new BasicHeader(HttpHeaders.LINK, "<http://alternative.com>")
    });
    doReturn(null).when(underTest).fetch("http://alternative.com", cachedContext, content);

    Content result = underTest.handle300MultipleChoicesError(cachedContext, content, uri, response);

    assertNull(result);
  }

  @Test
  public void handle300MultipleChoicesError_returnsNullWhenNoLinksInResponse() throws IOException {
    URI uri = URI.create("http://example.com");
    HttpResponse response = mock(HttpResponse.class);
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{});

    Content result = underTest.handle300MultipleChoicesError(cachedContext, content, uri, response);

    assertNull(result);
  }

  @Test
  public void handle300MultipleChoicesError_returnsNullWhenMalformedLinkHeader() throws IOException {
    URI uri = URI.create("http://example.com");
    HttpResponse response = mock(HttpResponse.class);
    when(response.getHeaders(HttpHeaders.LINK)).thenReturn(new Header[]{
        new BasicHeader(HttpHeaders.LINK, "malformed")
    });

    Content result = underTest.handle300MultipleChoicesError(cachedContext, content, uri, response);

    assertNull(result);
  }

  @Test
  public void testGetSendProxyThrottledRequestEvent() throws Exception {
    when(cachedContextAttributesMap.contains(PROXY_THROTTLED_ANALYTICS_MARKED))
        .thenReturn(false);
    doReturn(content).when(underTest).fetch(any(), any(), any());

    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    when(gracePeriodInterceptor.isInGracePeriod()).thenReturn(true);
    when(throttlerInterceptor.shouldBlock()).thenReturn(true);

    Content actual = underTest.get(cachedContext);
    verify(eventManager).post(any(ProxyThrottledRequestEvent.class));
  }

  @Test
  public void testGetDoNotSendProxyThrottledRequestEventWhenAlreadyMarked() throws Exception {
    when(cachedContextAttributesMap.contains(PROXY_THROTTLED_ANALYTICS_MARKED))
        .thenReturn(true);
    doReturn(content).when(underTest).fetch(any(), any(), any());

    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    when(gracePeriodInterceptor.isInGracePeriod()).thenReturn(true);
    when(throttlerInterceptor.shouldBlock()).thenReturn(true);

    Content actual = underTest.get(cachedContext);
    verify(eventManager, never()).post(any(ProxyThrottledRequestEvent.class));
  }

  @Test
  public void testBypassHttpErrorExceptionLoggedAtDebugLevel() throws IOException {
    com.google.common.collect.ListMultimap<String, String> headers =
        com.google.common.collect.ArrayListMultimap.create();
    BypassHttpErrorException bypassException = new BypassHttpErrorException(401, "Unauthorized", headers);

    try {
      underTest.logContentOrThrow(null, cachedContext, statusLine, bypassException);
      fail("Expected BypassHttpErrorException to be thrown");
    }
    catch (BypassHttpErrorException expected) {
      // Expected - BypassHttpErrorException should be logged at DEBUG level and then thrown
    }
  }

  @Test
  public void testBuildFetchHttpRequest_HeadRequest() throws Exception {
    URI uri = URI.create("http://example.com/test.jar");
    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    HttpRequestBase httpRequest = underTest.buildFetchHttpRequest(uri, cachedContext);

    assertThat(httpRequest.getMethod(), is("HEAD"));
    assertThat(httpRequest.getURI(), is(uri));
  }

  @Test
  public void testDoGet_HeadRequest_DoesNotCallStore() throws Exception {
    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    doReturn(null).when(underTest).getCachedContent(cachedContext);
    doReturn(reFetchedContent).when(underTest).fetch(cachedContext, null);

    Content result = underTest.doGet(cachedContext, null);

    assertThat(result, is(reFetchedContent));
    verify(underTest, never()).store(any(), any());
  }

  @Test
  public void testCreateContent_WithoutEntity_HeadResponse() {
    // Setup HEAD request context
    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    HttpResponse response = mock(HttpResponse.class);
    when(response.getEntity()).thenReturn(null);
    Header contentLengthHeader = new BasicHeader(HttpHeaders.CONTENT_LENGTH, "1557794");
    Header contentTypeHeader = new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/java-archive");
    when(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(contentLengthHeader);
    when(response.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(contentTypeHeader);

    Content content = underTest.createContent(cachedContext, response);

    assertThat(content, is(org.hamcrest.Matchers.notNullValue()));
    assertThat(content.getSize(), is(1557794L));
    assertThat(content.getContentType(), is("application/java-archive"));
  }

  @Test
  public void testHeadRequest_ReturnsCachedContent_WhenFresh() throws Exception {
    // Setup: Fresh cached content exists
    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    doReturn(content).when(underTest).getCachedContent(cachedContext);
    when(cacheController.isStale(cacheInfo)).thenReturn(false);

    // When: HEAD request made
    Content result = underTest.doGet(cachedContext, content);

    // Then: Should return cached content, not attempt remote fetch
    assertThat(result, is(content));
    verify(underTest, never()).fetch(any(), any());
    verify(underTest, never()).store(any(), any());
  }

  @Test
  public void testHeadRequest_RefetchesFromRemote_WhenStale() throws Exception {
    // Setup: Stale cached content exists
    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    doReturn(content).when(underTest).getCachedContent(cachedContext);
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doReturn(reFetchedContent).when(underTest).fetch(cachedContext, content);

    // When: HEAD request made
    Content result = underTest.doGet(cachedContext, content);

    // Then: Should fetch from remote and return without storing
    assertThat(result, is(reFetchedContent));
    verify(underTest, times(1)).fetch(cachedContext, content);
    verify(underTest, never()).store(any(), any());
  }

  @Test
  public void testHeadRequest_ReturnsCachedContent_WhenRemoteUnavailable() throws Exception {
    // Setup: Stale cached content exists, but remote is unavailable
    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    doReturn(content).when(underTest).getCachedContent(cachedContext);
    when(cacheController.isStale(cacheInfo)).thenReturn(true);
    doThrow(new IOException("Remote unavailable")).when(underTest).fetch(cachedContext, content);

    // When: HEAD request made
    Content result = underTest.doGet(cachedContext, content);

    // Then: Should return cached content despite fetch failure
    assertThat(result, is(content));
    verify(underTest, times(1)).fetch(cachedContext, content);
    verify(underTest, never()).store(any(), any());
  }

  @Test
  public void testHeadRequest_FetchesFromRemote_WhenNoCachedContent() throws Exception {
    // Setup: No cached content exists
    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    doReturn(null).when(underTest).getCachedContent(cachedContext);
    doReturn(reFetchedContent).when(underTest).fetch(cachedContext, null);

    // When: HEAD request made
    Content result = underTest.doGet(cachedContext, null);

    // Then: Should fetch from remote
    assertThat(result, is(reFetchedContent));
    verify(underTest, times(1)).fetch(cachedContext, null);
    verify(underTest, never()).store(any(), any());
  }

  /**
   * Test for NEXUS-36994: Verify that when getUrl() returns null (e.g., for Conan requests
   * without required metadata), fetch() returns null gracefully instead of throwing NPE.
   * This should result in a 404 response instead of a 500 error.
   */
  @Test
  public void testFetch_WithNullUrl_ReturnsNullWithoutNPE() throws Exception {
    // Setup: Request that would cause getUrl() to return null
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/conans/_/lz4/1.9.4/_/packages/xxx/conaninfo.txt");
    when(cachedContext.getRequest()).thenReturn(request);

    // The test implementation's getUrl() returns null by design (see line 123-125)
    // This simulates format-specific implementations returning null for invalid/incomplete requests

    // When: fetch is called with null URL
    Content result = underTest.fetch(null, cachedContext, null);

    // Then: Should return null (which triggers 404) instead of throwing NPE
    assertNull("fetch() should return null when URL is null", result);
  }

  /**
   * Test for NEXUS-36994: Verify fetch(Context, Content) variant also handles null URLs
   * gracefully when getUrl(context) returns null.
   */
  @Test
  public void testFetch_WithContextReturningNullUrl_ReturnsNullWithoutNPE() throws Exception {
    // Setup: Context that causes getUrl() to return null
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/some/invalid/path");
    when(cachedContext.getRequest()).thenReturn(request);

    // When: fetch is called via the Context variant (internally calls fetch(url, context, stale))
    Content result = underTest.fetch(cachedContext, null);

    // Then: Should return null gracefully
    assertNull("fetch() should return null when getUrl() returns null", result);
  }

  /**
   * Test for NEXUS-36994: Verify that the null URL check happens before any URI operations,
   * ensuring no NPE is thrown during URI resolution.
   */
  @Test
  public void testFetch_WithNullUrl_DoesNotAttemptURIResolution() throws Exception {
    // Setup: Request with a path that would be problematic for URI resolution
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/path/with/special/chars/[brackets]");
    when(cachedContext.getRequest()).thenReturn(request);

    // When: fetch is called with null URL (before any URI operations)
    Content result = underTest.fetch(null, cachedContext, content);

    // Then: Should return null immediately without attempting URI.resolve() which would NPE
    assertNull("fetch() should return null before attempting URI resolution", result);
  }

}
