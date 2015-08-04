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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import java.lang.reflect.Field;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.apachehttpclient.Hc4Provider.Builder;
import org.sonatype.nexus.apachehttpclient.Hc4ProviderImpl;
import org.sonatype.nexus.apachehttpclient.PoolingClientConnectionManagerMBeanInstaller;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.base.Throwables;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

/**
 *
 */
public class HttpClientManagerTest
    extends TestSupport
{
  @Mock
  private ProxyRepository proxyRepository;

  // ==

  @Mock
  private ApplicationConfiguration applicationConfiguration;

  @Mock
  private UserAgentBuilder userAgentBuilder;

  @Mock
  private EventBus eventBus;

  @Mock
  private RemoteStorageContext globalRemoteStorageContext;

  @Mock
  private RemoteProxySettings remoteProxySettings;

  @Mock
  private PoolingClientConnectionManagerMBeanInstaller jmxInstaller;

  @Mock
  private HttpResponse response;

  @Mock
  private StatusLine statusLine;

  private HttpGet request;

  private Hc4Provider hc4Provider;

  @Before
  public void before() {
    final DefaultRemoteConnectionSettings rcs = new DefaultRemoteConnectionSettings();
    rcs.setConnectionTimeout(10000);
    when(globalRemoteStorageContext.getRemoteConnectionSettings()).thenReturn(rcs);
    when(globalRemoteStorageContext.getRemoteProxySettings()).thenReturn(remoteProxySettings);
    when(applicationConfiguration.getGlobalRemoteStorageContext()).thenReturn(globalRemoteStorageContext);

    hc4Provider = new Hc4ProviderImpl(applicationConfiguration, userAgentBuilder, eventBus, jmxInstaller, null);

    when(proxyRepository.getId()).thenReturn("central");
    when(response.getStatusLine()).thenReturn(statusLine);
  }

  @Test
  public void doNotFollowRedirectsToDirIndex()
      throws ProtocolException
  {
    final HttpClientManagerImpl httpClientManager = new HttpClientManagerImpl(hc4Provider, userAgentBuilder);
    final RedirectStrategy underTest =
        httpClientManager.getProxyRepositoryRedirectStrategy(proxyRepository, globalRemoteStorageContext);
    HttpContext httpContext;

    // no location header
    request = new HttpGet("http://localhost/dir/fileA");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(HttpClientRemoteStorage.CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    assertThat(underTest.isRedirected(request, response, httpContext), is(false));

    // redirect to file
    request = new HttpGet("http://localhost/dir/fileA");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(HttpClientRemoteStorage.CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader("location")).thenReturn(
        new BasicHeader("location", "http://localhost/dir/fileB"));
    assertThat(underTest.isRedirected(request, response, httpContext), is(true));

    // redirect to dir
    request = new HttpGet("http://localhost/dir");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(HttpClientRemoteStorage.CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader("location")).thenReturn(new BasicHeader("location", "http://localhost/dir/"));
    assertThat(underTest.isRedirected(request, response, httpContext), is(false));
  }

  @Test
  public void doFollowCrossSiteRedirects()
      throws ProtocolException
  {
    final HttpClientManagerImpl httpClientManager = new HttpClientManagerImpl(hc4Provider, userAgentBuilder);
    final RedirectStrategy underTest =
        httpClientManager.getProxyRepositoryRedirectStrategy(proxyRepository, globalRemoteStorageContext);

    // simple cross redirect
    request = new HttpGet("http://hostA/dir");
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader("location")).thenReturn(
        new BasicHeader("location", "http://hostB/dir"));
    assertThat(underTest.isRedirected(request, response, new BasicHttpContext()), is(true));

    // cross redirect to dir (failed coz NEXUS-5744)
    request = new HttpGet("http://hostA/dir/");
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader("location")).thenReturn(new BasicHeader("location", "http://hostB/dir/"));
    assertThat(underTest.isRedirected(request, response, new BasicHttpContext()), is(true));
  }

  @Test
  public void enableCircularRedirectsForHosts()
  {
    System.setProperty("nexus.remoteStorage.enableCircularRedirectsForHosts", "Other.org,someDomain.com");
    try {
      final HttpClientManagerImpl httpClientManager = new HttpClientManagerImpl(hc4Provider, userAgentBuilder);
      Builder builder;

      // not listed hostname, all as before
      when(proxyRepository.getRemoteUrl()).thenReturn("http://repo1.central.org/maven2/");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "circularRedirectsAllowed", false);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "maxRedirects", 50);

      // not listed hostname, all as before (non-standard url, like procurement)
      when(proxyRepository.getRemoteUrl()).thenReturn("foo:bar");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "circularRedirectsAllowed", false);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "maxRedirects", 50);

      // listed, changes should be applied
      when(proxyRepository.getRemoteUrl()).thenReturn("http://other.org/maven2/");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "circularRedirectsAllowed", true);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "maxRedirects", 10);

      // listed, changes should be applied
      when(proxyRepository.getRemoteUrl()).thenReturn("https://SOMEDOMAIN.COM/mavenrepo/");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "circularRedirectsAllowed", true);
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "maxRedirects", 10);
    } finally {
      System.clearProperty("nexus.remoteStorage.enableCircularRedirectsForHosts");
    }
  }

  @Test
  public void useCookiesForHosts()
  {
    System.setProperty("nexus.remoteStorage.useCookiesForHosts", "Other.org,someDomain.com");
    try {
      final HttpClientManagerImpl httpClientManager = new HttpClientManagerImpl(hc4Provider, userAgentBuilder);
      Builder builder;

      // not listed hostname, all as before
      when(proxyRepository.getRemoteUrl()).thenReturn("http://repo1.central.org/maven2/");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getHttpClientBuilder(), "cookieStore", nullValue());
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "cookieSpec", CookieSpecs.IGNORE_COOKIES);

      // not listed hostname, all as before (non-standard url, like procurement)
      when(proxyRepository.getRemoteUrl()).thenReturn("foo:bar");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getHttpClientBuilder(), "cookieStore", nullValue());
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "cookieSpec", CookieSpecs.IGNORE_COOKIES);

      // listed, changes should be applied
      when(proxyRepository.getRemoteUrl()).thenReturn("http://other.org/maven2/");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getHttpClientBuilder(), "cookieStore", notNullValue());
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "cookieSpec", CookieSpecs.BROWSER_COMPATIBILITY);

      // listed, changes should be applied
      when(proxyRepository.getRemoteUrl()).thenReturn("https://SOMEDOMAIN.COM/mavenrepo/");
      builder = hc4Provider.prepareHttpClient(globalRemoteStorageContext);
      httpClientManager.configure(proxyRepository, globalRemoteStorageContext, builder);
      fieldEqualsReflection(builder.getHttpClientBuilder(), "cookieStore", notNullValue());
      fieldEqualsReflection(builder.getRequestConfigBuilder(), "cookieSpec", CookieSpecs.BROWSER_COMPATIBILITY);
    } finally {
      System.clearProperty("nexus.remoteStorage.useCookiesForHosts");
    }
  }

  private void fieldEqualsReflection(final Object instance, final String fieldName, final Object expected) {
    if (expected == null) {
      fieldEqualsReflection(instance, fieldName, nullValue());
    } else {
      fieldEqualsReflection(instance, fieldName, equalTo(expected));
    }
  }

  private void fieldEqualsReflection(final Object instance, final String fieldName, final Matcher<? super Object> matcher) {
    try {
      final Field field = instance.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      final Object value = field.get(instance);
      assertThat("Class " + instance.getClass().getSimpleName() + " field " + fieldName, value, matcher);
    }
    catch (Exception e) {
      throw new RuntimeException("Class " + instance.getClass().getSimpleName() + " field " + fieldName, e);
    }
  }

  @Test
  @Ignore("Hc4Provider does not deliver DefaultHttpClient deprecated instances anymore")
  public void doNotHandleRetries() {
    final DefaultHttpClient client =
        (DefaultHttpClient) new HttpClientManagerImpl(hc4Provider, userAgentBuilder).create(proxyRepository,
            globalRemoteStorageContext);
    // check is all set as needed: retries should be not attempted, as it is manually handled in proxy repo
    Assert.assertTrue(
        ((DefaultHttpClient) client).getHttpRequestRetryHandler() instanceof StandardHttpRequestRetryHandler);
    Assert.assertTrue(globalRemoteStorageContext.getRemoteConnectionSettings().getRetrievalRetryCount() != 0);
    // TODO: NEXUS-5368 This is disabled on purpose for now (same in HttpClientManagerImpl!)
    //Assert.assertEquals(
    //    0,
    //    ( (StandardHttpRequestRetryHandler) ( (DefaultHttpClient) client ).getHttpRequestRetryHandler() ).getRetryCount() );
    //Assert.assertEquals(
    //    false,
    //    ( (StandardHttpRequestRetryHandler) ( (DefaultHttpClient) client ).getHttpRequestRetryHandler() ).isRequestSentRetryEnabled() );
  }
}
