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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.outbound.context.OutboundRequestContext;
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
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.HeaderOnlyPayload;
import org.sonatype.nexus.transaction.RetryDeniedException;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import com.google.common.base.Stopwatch;
import com.google.common.net.HttpHeaders;
import org.sonatype.nexus.common.template.EscapeHelper;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.slf4j.Logger;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.OUTBOUND_REQUESTS_LOG_ONLY;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_NAME;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.MISSING_BLOB_SKIP_NEGATIVE_CACHE;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_VALUE;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.PROXY_THROTTLED_ANALYTICS_MARKED;
import static org.sonatype.nexus.repository.replication.PullReplicationSupport.IS_REPLICATION_REQUEST;

import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for the abstract class {@link ProxyFacetSupport}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ProxyFacetSupportTest
{
  private static final String SHARED_PATH = "/com/example/artifact/1.0/artifact-1.0.pom";

  @Mock
  ThrottlerInterceptor contentUsageThrottlerInterceptor;

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
  private AntiSsrfService antiSsrfService;

  private final ArgumentCaptor<ProxyThrottledRequestEvent> captor =
      ArgumentCaptor.forClass(ProxyThrottledRequestEvent.class);

  @Before
  public void setUp() throws Exception {
    OutboundRequestContext.remove();
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

    underTest.installDependencies(eventManager);
    underTest.attach(repository);
    DefaultCooperation2Factory cooperationFactory = new DefaultCooperation2Factory();
    underTest.configureCooperation(cooperationFactory, false, Duration.ofSeconds(0),
        Duration.ofSeconds(60), 10);
    underTest.buildCooperation();
  }

  @Test
  public void printOutboundLogging_nullStopwatch_cleansUpThreadLocal() throws Exception {
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/path/to/file.txt");

    Logger outboundReqLog = mock(Logger.class);
    Logger outboundLog = mock(Logger.class);

    when(cachedContext.getRequest()).thenReturn(request);
    when(cachedContext.getAttribute("request.stopwatch", Stopwatch.class)).thenReturn(null);

    OutboundRequestContext.setFormattedString("should be cleaned up");

    setField(underTest, "outboundReqLog", outboundReqLog);
    setField(underTest, "outboundLog", outboundLog);

    Method method = ProxyFacetSupport.class.getDeclaredMethod("printOutboundLogging", Context.class);
    method.setAccessible(true);
    method.invoke(underTest, cachedContext);

    verifyNoInteractions(outboundReqLog);
    verifyNoInteractions(outboundLog);
    assertNull(OutboundRequestContext.getFormattedString());
    assertFalse(OutboundRequestContext.getContextMapSize() > 0);
  }

  @Test
  public void printOutboundLogging_nullFormattedString_onlyLogsDebug() throws Exception {
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/path/to/file.txt");

    HttpContext httpContext = mock(HttpContext.class);
    Stopwatch stopwatch = Stopwatch.createStarted();
    Logger outboundReqLog = mock(Logger.class);
    Logger outboundLog = mock(Logger.class);

    when(cachedContext.getRequest()).thenReturn(request);
    when(cachedContext.getAttribute("request.stopwatch", Stopwatch.class)).thenReturn(stopwatch);
    when(cachedContext.getAttribute("request.http_context", HttpContext.class)).thenReturn(httpContext);
    when(httpContext.getAttribute("request.uri")).thenReturn(URI.create("https://example.com/path/to/file.txt"));

    setField(underTest, "outboundReqLog", outboundReqLog);
    setField(underTest, "outboundLog", outboundLog);

    Method method = ProxyFacetSupport.class.getDeclaredMethod("printOutboundLogging", Context.class);
    method.setAccessible(true);
    method.invoke(underTest, cachedContext);

    verify(outboundLog).debug(eq("Request for {} took {} milliseconds"),
        eq("https://example.com/path/to/file.txt"), any(Long.class));
    verifyNoInteractions(outboundReqLog);
  }

  @Test
  public void printOutboundLogging_replacesNamedPlaceholders() throws Exception {
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/path/to/file.txt");

    HttpContext httpContext = mock(HttpContext.class);
    Stopwatch stopwatch = Stopwatch.createStarted();
    Logger outboundReqLog = mock(Logger.class);
    ArgumentCaptor<String> loggedMessage = ArgumentCaptor.forClass(String.class);

    when(cachedContext.getRequest()).thenReturn(request);
    when(cachedContext.getAttribute("request.stopwatch", Stopwatch.class)).thenReturn(stopwatch);
    when(cachedContext.getAttribute("request.http_context", HttpContext.class)).thenReturn(httpContext);
    when(httpContext.getAttribute("request.uri")).thenReturn(URI.create("https://example.com/path/to/file.txt"));

    OutboundRequestContext.setFormattedString(String.format("[%s] 200 %s",
        OutboundRequestContext.TIMESTAMP_PLACEHOLDER,
        OutboundRequestContext.ELAPSED_TIME_PLACEHOLDER));

    setField(underTest, "outboundReqLog", outboundReqLog);

    Method method = ProxyFacetSupport.class.getDeclaredMethod("printOutboundLogging", Context.class);
    method.setAccessible(true);
    method.invoke(underTest, cachedContext);

    verify(outboundReqLog).info(eq(OUTBOUND_REQUESTS_LOG_ONLY), eq("{}"), loggedMessage.capture());
    assertThat(loggedMessage.getValue(), not(containsString(OutboundRequestContext.TIMESTAMP_PLACEHOLDER)));
    assertThat(loggedMessage.getValue(), not(containsString(OutboundRequestContext.ELAPSED_TIME_PLACEHOLDER)));

    assertNull(OutboundRequestContext.getFormattedString());
    assertFalse(OutboundRequestContext.getContextMapSize() > 0);
  }

  private static void setField(final Object target, final String fieldName, final Object value) throws Exception {
    Field field = ProxyFacetSupport.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
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
  public void doConfigure_preserveEncodedCharactersTrue_encodingHelperIsNotNull() throws Exception {
    ConfigurationFacet configurationFacet = mock(ConfigurationFacet.class);
    ProxyFacetSupport.ProxyConfig config = new ProxyFacetSupport.ProxyConfig();
    config.remoteUrl = new URI("http://example.com");
    config.preserveEncodedCharacters = true;

    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(configurationFacet.readSection(any(Configuration.class), anyString(), eq(ProxyFacetSupport.ProxyConfig.class)))
        .thenReturn(config);

    underTest.configureUrlEscapeRules(null);
    underTest.setRepositoryAttributeService(repositoryAttributeService);
    underTest.doConfigure(mock(Configuration.class));

    assertNotNull(underTest.getEncodingHelper());
  }

  @Test
  public void doConfigure_preserveEncodedCharactersFalse_encodingHelperIsNull() throws Exception {
    ConfigurationFacet configurationFacet = mock(ConfigurationFacet.class);
    ProxyFacetSupport.ProxyConfig config = new ProxyFacetSupport.ProxyConfig();
    config.remoteUrl = new URI("http://example.com");
    config.preserveEncodedCharacters = false;

    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(configurationFacet.readSection(any(Configuration.class), anyString(), eq(ProxyFacetSupport.ProxyConfig.class)))
        .thenReturn(config);

    underTest.setRepositoryAttributeService(repositoryAttributeService);
    underTest.doConfigure(mock(Configuration.class));

    assertNull(underTest.getEncodingHelper());
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
    when(contentUsageThrottlerInterceptor.shouldBlock()).thenReturn(true);
    when(gracePeriodInterceptor.isInGracePeriod()).thenReturn(false);
    doReturn(null).when(underTest).getCachedContent(cachedContext);

    underTest.get(cachedContext);

    verify(eventManager).post(any(ProxyRequestEvent.class));
    verify(eventManager).post(captor.capture());
    assertThat(captor.getValue().isBlocked(), is(true));
  }

  @Test
  public void testGetPostsGracePeriodEvents() throws IOException {
    when(contentUsageThrottlerInterceptor.shouldBlock()).thenReturn(true);
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
    when(contentUsageThrottlerInterceptor.shouldBlock()).thenReturn(true);

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
    when(contentUsageThrottlerInterceptor.shouldBlock()).thenReturn(true);

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

  @Test
  public void testDoGet_HeadRequest_CallsStore_WhenShouldSkipStoreForHeadReturnsFalse() throws Exception {
    ProxyFacetSupport noSkipStore = spy(new ProxyFacetSupport()
    {
      @Nullable
      @Override
      protected Content getCachedContent(final Context context) {
        return null;
      }

      @Override
      protected Content store(final Context context, final Content content) {
        return content;
      }

      @Override
      protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo) {
      }

      @Override
      protected String getUrl(@Nonnull final Context context) {
        return null;
      }

      @Override
      protected boolean shouldSkipStoreForHead() {
        return false;
      }
    });

    Request request = mock(Request.class);
    when(request.getAction()).thenReturn(HttpMethods.HEAD);
    when(cachedContext.getRequest()).thenReturn(request);

    doReturn(null).when(noSkipStore).getCachedContent(cachedContext);
    doReturn(reFetchedContent).when(noSkipStore).fetch(cachedContext, null);
    doReturn(storedContent).when(noSkipStore).store(cachedContext, reFetchedContent);

    Content result = noSkipStore.doGet(cachedContext, null);

    assertThat(result, is(storedContent));
    verify(noSkipStore, times(1)).store(cachedContext, reFetchedContent);
  }

  @Test
  public void execute_setsNormalizeUriFalse_whenEncodingHelperIsSet() throws Exception {
    setField(underTest, "encodingHelper", new EncodingHelper(new EscapeHelper()));

    HttpGet request = new HttpGet("http://example.com/path");
    // Plain mock does not implement Configurable — exercises the RequestConfig.DEFAULT fallback
    HttpClient httpClient = mock(HttpClient.class);
    BasicHttpResponse stubResponse = new BasicHttpResponse(
        new ProtocolVersion("HTTP", 1, 1), 200, "OK");
    when(httpClient.execute(any(HttpRequestBase.class), any(HttpContext.class))).thenReturn(stubResponse);

    underTest.execute(cachedContext, httpClient, request);

    assertThat(request.getConfig().isNormalizeUri(), is(false));
    // RequestConfig.DEFAULT has -1 (unset) for all timeouts — nothing was shadowed
    assertThat(request.getConfig().getSocketTimeout(), is(-1));
  }

  @Test
  public void execute_preservesExistingRequestConfig_whenMergingNormalizeUri() throws Exception {
    setField(underTest, "encodingHelper", new EncodingHelper(new EscapeHelper()));

    HttpGet request = new HttpGet("http://example.com/path");
    int expectedTimeout = 42_000;
    request.setConfig(RequestConfig.custom()
        .setSocketTimeout(expectedTimeout)
        .build());

    HttpClient httpClient = mock(HttpClient.class);
    BasicHttpResponse stubResponse = new BasicHttpResponse(
        new ProtocolVersion("HTTP", 1, 1), 200, "OK");
    when(httpClient.execute(any(HttpRequestBase.class), any(HttpContext.class))).thenReturn(stubResponse);

    underTest.execute(cachedContext, httpClient, request);

    assertThat(request.getConfig().isNormalizeUri(), is(false));
    assertThat(request.getConfig().getSocketTimeout(), is(expectedTimeout));
  }

  @Test
  public void execute_usesClientDefaultConfig_whenRequestHasNoConfig() throws Exception {
    setField(underTest, "encodingHelper", new EncodingHelper(new EscapeHelper()));

    HttpGet request = new HttpGet("http://example.com/path");
    int clientSocketTimeout = 99_000;
    RequestConfig clientDefault = RequestConfig.custom()
        .setSocketTimeout(clientSocketTimeout)
        .build();

    // A Configurable HttpClient that exposes its default RequestConfig (e.g. InternalHttpClient)
    HttpClient configurableClient = mock(HttpClient.class, withSettings().extraInterfaces(Configurable.class));
    when(((Configurable) configurableClient).getConfig()).thenReturn(clientDefault);
    BasicHttpResponse stubResponse = new BasicHttpResponse(
        new ProtocolVersion("HTTP", 1, 1), 200, "OK");
    when(configurableClient.execute(any(HttpRequestBase.class), any(HttpContext.class))).thenReturn(stubResponse);

    underTest.execute(cachedContext, configurableClient, request);

    // normalizeUri must be disabled and the client's timeout preserved
    assertThat(request.getConfig().isNormalizeUri(), is(false));
    assertThat(request.getConfig().getSocketTimeout(), is(clientSocketTimeout));
  }

  // NEXUS-54133: FilteredHttpClientSupport (base of BlockingHttpClient / MonitoredHttpClient) now
  // implements Configurable. This verifies that when a REAL decorator (rather than a mock with
  // extraInterfaces) wraps a Configurable delegate, ProxyFacetSupport.execute() reaches through the
  // decorator via getConfig() and preserves the delegate's timeouts on the outbound request instead
  // of silently overwriting them with RequestConfig.DEFAULT (all -1) on the NEXUS-52769 code path.
  @Test
  public void execute_preservesTimeouts_whenClientIsFilteredHttpClientSupportDecorator() throws Exception {
    setField(underTest, "encodingHelper", new EncodingHelper(new EscapeHelper()));

    HttpGet request = new HttpGet("http://example.com/path");
    int clientSocketTimeout = 77_000;
    RequestConfig delegateConfig = RequestConfig.custom()
        .setSocketTimeout(clientSocketTimeout)
        .build();

    // Delegate implements Configurable, mirroring org.apache.http.impl.client.InternalHttpClient.
    org.apache.http.impl.client.CloseableHttpClient delegate = mock(
        org.apache.http.impl.client.CloseableHttpClient.class,
        withSettings().extraInterfaces(Configurable.class));
    when(((Configurable) delegate).getConfig()).thenReturn(delegateConfig);
    org.apache.http.client.methods.CloseableHttpResponse stubResponse =
        mock(org.apache.http.client.methods.CloseableHttpResponse.class);
    when(delegate.execute(any(org.apache.http.HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class)))
        .thenReturn(stubResponse);

    // Real FilteredHttpClientSupport subclass — the actual decorator shape used in production
    // (BlockingHttpClient / MonitoredHttpClient extend this class). No Mockito extraInterfaces trick.
    HttpClient decorator =
        new org.sonatype.nexus.repository.httpclient.FilteredHttpClientSupport(delegate)
        {
          @Override
          protected org.apache.http.client.methods.CloseableHttpResponse filter(
              final org.apache.http.HttpHost target,
              final org.sonatype.nexus.repository.httpclient.FilteredHttpClientSupport.Filterable filterable) throws IOException
        {
            return filterable.call();
          }
        };

    underTest.execute(cachedContext, decorator, request);

    // The decorator itself must be recognised as Configurable (NEXUS-54133 fix)…
    assertThat(decorator instanceof Configurable, is(true));
    // …ProxyFacetSupport must set normalizeUri=false for the SigV4/NEXUS-52769 path…
    assertThat(request.getConfig().isNormalizeUri(), is(false));
    // …and must propagate the delegate's timeouts instead of falling back to RequestConfig.DEFAULT (-1).
    assertThat(request.getConfig().getSocketTimeout(), is(clientSocketTimeout));
  }

  // NEXUS-53338: replication requests must bypass the staleness short-circuit so
  // modified assets are re-fetched regardless of the proxy content max-age setting.
  @Test
  public void testGet_replicationRequest_bypassesStalenessCheckAndFetchesFromRemote() throws IOException {
    // Content is in cache and NOT stale (max-age not yet exceeded)
    doReturn(content).when(underTest).getCachedContent(cachedContext);
    when(cacheController.isStale(cacheInfo)).thenReturn(false);

    // Flag this context as a replication pull request
    when(cachedContextAttributesMap.get(eq(IS_REPLICATION_REQUEST), eq(Boolean.class), eq(false)))
        .thenReturn(Boolean.TRUE);

    // When the staleness check is properly bypassed, fetch + store are invoked
    doReturn(reFetchedContent).when(underTest).fetch(cachedContext, content);
    doReturn(storedContent).when(underTest).store(cachedContext, reFetchedContent);

    Content result = underTest.get(cachedContext);

    // Replication must not return the stale cached copy; it must re-fetch from the source
    verify(underTest).fetch(cachedContext, content);
    assertThat(result, is(storedContent));
  }

  @Test
  public void testGet_nonReplicationRequest_returnsCachedContentWhenNotStale() throws IOException {
    // Confirm that regular (non-replication) requests continue to honour the cache
    doReturn(content).when(underTest).getCachedContent(cachedContext);
    when(cacheController.isStale(cacheInfo)).thenReturn(false);

    // IS_REPLICATION_REQUEST is not set → isReplicationRequest() returns false by default

    Content result = underTest.get(cachedContext);

    verify(underTest, never()).fetch(any(), any(), any());
    assertThat(result, is(content));
  }

  // ---------- hasContentFor (NEXUS-54130 read-through HA divergence check) ----------

  /**
   * Happy path: getCachedContent returns non-null content → hasContentFor returns true and the context is
   * not mutated. This is the branch used by NegativeCacheHandler in an HA cluster to detect that a peer has
   * populated the shared local store since this node cached its 404.
   */
  @Test
  public void hasContentFor_returnsTrueWhenCachedContentPresent() throws IOException {
    doReturn(content).when(underTest).getCachedContent(cachedContext);

    boolean result = underTest.hasContentFor(cachedContext);

    assertThat(result, is(true));
    verify(cachedContextAttributesMap, never()).set(eq(MISSING_BLOB_SKIP_NEGATIVE_CACHE), any());
  }

  /**
   * MissingBlobException path: metadata says the asset exists but the underlying blob is missing.
   * hasContentFor must return false AND must NOT set {@link ProxyFacetSupport#MISSING_BLOB_SKIP_NEGATIVE_CACHE}
   * on the context — the private maybeGetCachedContent sets that flag on the fetch path, and it must never
   * fire from a read-only probe (setting it would permanently defeat NFC upstream-shielding for that path).
   */
  @Test
  public void hasContentFor_missingBlobException_returnsFalseAndDoesNotMutateContext() throws IOException {
    doThrow(new MissingBlobException(null)).when(underTest).getCachedContent(cachedContext);

    boolean result = underTest.hasContentFor(cachedContext);

    assertThat(result, is(false));
    verify(cachedContextAttributesMap, never()).set(eq(MISSING_BLOB_SKIP_NEGATIVE_CACHE), any());
  }

  /**
   * Generic exception path: any other IOException / RuntimeException from getCachedContent is swallowed and
   * the caller sees "no local content". The failure is logged at WARN so operators can detect a real storage
   * regression, but must never propagate to the request path.
   */
  @Test
  public void hasContentFor_unexpectedException_returnsFalseAndSwallowsSilently() throws IOException {
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/some/path.txt");
    when(cachedContext.getRequest()).thenReturn(request);
    doThrow(new IOException("simulated storage failure")).when(underTest).getCachedContent(cachedContext);

    boolean result = underTest.hasContentFor(cachedContext);

    assertThat(result, is(false));
    verify(cachedContextAttributesMap, never()).set(eq(MISSING_BLOB_SKIP_NEGATIVE_CACHE), any());
  }

  // ---------- NEXUS-54507 concurrent HEAD/GET cooperation ----------

  /**
   * <p>
   * The cooperation key is method-insensitive, so a HEAD and a GET for the same uncached path share
   * one fetch. A HEAD leader returns a {@link HeaderOnlyPayload}, which reports the upstream
   * Content-Length but opens an empty stream. Handing that to a GET makes the view declare
   * {@code Content-Length: N} and then write 0 bytes, which Jetty rejects with
   * {@code IOException: written 0 < N content-length} - surfacing to the client as a 500.
   */
  @Test
  public void testGet_concurrentHeadAndGet_getDoesNotInheritHeaderOnlyPayload() throws Exception {
    // cooperation must be ON for the GET to join the HEAD's in-flight fetch
    underTest.configureCooperation(new DefaultCooperation2Factory(), true, Duration.ofSeconds(0),
        Duration.ofSeconds(60), 10);
    underTest.buildCooperation();

    Request headRequest = mock(Request.class);
    when(headRequest.getAction()).thenReturn(HttpMethods.HEAD);
    when(headRequest.getPath()).thenReturn(SHARED_PATH);
    Context headContext = proxyContext(headRequest);

    Request getRequest = mock(Request.class);
    when(getRequest.getAction()).thenReturn(HttpMethods.GET);
    when(getRequest.getPath()).thenReturn(SHARED_PATH);
    Context getContext = proxyContext(getRequest);

    // nothing cached: both requests must go to the remote
    doReturn(null).when(underTest).getCachedContent(any());

    // the HEAD leader's remote response - headers only, no body (as built by createContent())
    HttpResponse headResponse = mock(HttpResponse.class);
    when(headResponse.getEntity()).thenReturn(null);
    when(headResponse.getFirstHeader(HttpHeaders.CONTENT_LENGTH))
        .thenReturn(new BasicHeader(HttpHeaders.CONTENT_LENGTH, "4148"));
    Content headerOnlyContent = new Content(new HeaderOnlyPayload(headResponse));

    // a real GET fetch would return a body-bearing payload
    Content bodyContent = new Content(new BytesPayload(new byte[4148], "application/octet-stream"));

    CountDownLatch headIsFetching = new CountDownLatch(1);
    CountDownLatch getHasJoined = new CountDownLatch(1);

    doAnswer(invocation -> {
      Context ctx = invocation.getArgument(0);
      if (HttpMethods.HEAD.equals(ctx.getRequest().getAction())) {
        // hold the cooperation key open so the GET arrives while this fetch is in flight
        headIsFetching.countDown();
        getHasJoined.await(5, TimeUnit.SECONDS);
        return headerOnlyContent;
      }
      return bodyContent;
    }).when(underTest).fetch(any(Context.class), any());

    doAnswer(invocation -> invocation.getArgument(1)).when(underTest).store(any(), any());

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<Content> headResult = executor.submit(() -> underTest.get(headContext));
      assertTrue("HEAD did not start fetching", headIsFetching.await(5, TimeUnit.SECONDS));

      Future<Content> getResult = executor.submit(() -> underTest.get(getContext));
      // wait for the GET to actually block on the HEAD's cooperation future before releasing the
      // leader, rather than sleeping a guessed interval that a loaded CI host could outrun
      awaitCooperatingThreads(2);
      getHasJoined.countDown();

      Content headContent = headResult.get(10, TimeUnit.SECONDS);
      Content getContent = getResult.get(10, TimeUnit.SECONDS);

      // the HEAD is entitled to the header-only payload
      assertThat(headContent.getPayload(), is(org.hamcrest.Matchers.instanceOf(HeaderOnlyPayload.class)));

      // the GET must not be: a declared Content-Length with an empty stream is unwritable
      assertThat("GET inherited the HEAD's body-less payload (NEXUS-54507)",
          getContent.getPayload(), is(not(org.hamcrest.Matchers.instanceOf(HeaderOnlyPayload.class))));
      assertThat(getContent.getSize(), is(4148L));
    }
    finally {
      executor.shutdownNow();
    }
  }

  /**
   * The fix must not
   * disable cooperation for GETs - that de-duplication is what shields the remote from a
   * thundering herd.
   */
  @Test
  public void testGet_concurrentGets_stillCooperateOnASingleFetch() throws Exception {
    underTest.configureCooperation(new DefaultCooperation2Factory(), true, Duration.ofSeconds(0),
        Duration.ofSeconds(60), 10);
    underTest.buildCooperation();

    Request firstRequest = mock(Request.class);
    when(firstRequest.getAction()).thenReturn(HttpMethods.GET);
    when(firstRequest.getPath()).thenReturn(SHARED_PATH);
    Context firstContext = proxyContext(firstRequest);

    Request secondRequest = mock(Request.class);
    when(secondRequest.getAction()).thenReturn(HttpMethods.GET);
    when(secondRequest.getPath()).thenReturn(SHARED_PATH);
    Context secondContext = proxyContext(secondRequest);

    doReturn(null).when(underTest).getCachedContent(any());

    Content bodyContent = new Content(new BytesPayload(new byte[4148], "application/octet-stream"));

    AtomicInteger fetchCount = new AtomicInteger();
    CountDownLatch leaderIsFetching = new CountDownLatch(1);
    CountDownLatch followerHasJoined = new CountDownLatch(1);

    doAnswer(invocation -> {
      fetchCount.incrementAndGet();
      leaderIsFetching.countDown();
      followerHasJoined.await(5, TimeUnit.SECONDS);
      return bodyContent;
    }).when(underTest).fetch(any(Context.class), any());

    doAnswer(invocation -> invocation.getArgument(1)).when(underTest).store(any(), any());

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<Content> leader = executor.submit(() -> underTest.get(firstContext));
      assertTrue("leader did not start fetching", leaderIsFetching.await(5, TimeUnit.SECONDS));

      Future<Content> follower = executor.submit(() -> underTest.get(secondContext));
      awaitCooperatingThreads(2);
      followerHasJoined.countDown();

      leader.get(10, TimeUnit.SECONDS);
      follower.get(10, TimeUnit.SECONDS);

      assertThat("concurrent GETs must share one upstream fetch", fetchCount.get(), is(1));
    }
    finally {
      executor.shutdownNow();
    }
  }

  /**
   * Blocks until {@code expected} threads are cooperating under a single request key, so the
   * concurrent tests can release the lead thread at a known state instead of sleeping a fixed
   * interval. Fails the test rather than hanging if the follower never joins.
   */
  private void awaitCooperatingThreads(final int expected) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      if (underTest.getThreadCooperationPerRequest()
          .values()
          .stream()
          .anyMatch(count -> count >= expected)) {
        return;
      }
      Thread.sleep(10);
    }
    fail("timed out waiting for " + expected + " threads to cooperate; saw "
        + underTest.getThreadCooperationPerRequest());
  }

  /**
   * Builds a context that shares the mocked repository/cache wiring but carries its own request,
   * so HEAD and GET can be driven concurrently through {@link ProxyFacetSupport#get(Context)}.
   */
  private Context proxyContext(final Request request) {
    Context context = mock(Context.class);
    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    AttributesMap attributes = new AttributesMap();
    when(context.getAttributes()).thenReturn(attributes);
    return context;
  }
}
