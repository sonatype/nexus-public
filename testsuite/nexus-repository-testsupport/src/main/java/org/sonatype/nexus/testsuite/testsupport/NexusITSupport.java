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
package org.sonatype.nexus.testsuite.testsupport;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.pax.exam.distribution.NexusTestDistribution.Distribution;
import org.sonatype.nexus.pax.exam.distribution.NexusTestDistributionService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.tools.DeadBlobFinder;
import org.sonatype.nexus.repository.tools.DeadBlobResult;
import org.sonatype.nexus.rest.client.RestClientConfiguration;
import org.sonatype.nexus.rest.client.RestClientConfiguration.Customizer;
import org.sonatype.nexus.rest.client.RestClientFactory;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.testsuite.testsupport.fixtures.SecurityRule;
import org.sonatype.nexus.testsuite.testsupport.rest.TestSuiteObjectMapperResolver;

import com.google.common.base.Joiner;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.awaitility.Awaitility.await;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Support for Nexus integration tests.
 *
 * @deprecated Please write new tests using {@link NexusBaseITSupport}
 */
@Deprecated
public abstract class NexusITSupport
    extends NexusPaxExamSupport
{
  protected static final String DEFAULT_SESSION_COOKIE_NAME = "NXSESSIONID";

  protected static final String DEFAULT_JWT_COOKIE_NAME = "NXSESSIONID";

  protected static final String REST_SERVICE_PATH = "service/rest";

  @Rule
  public TestName testName = new TestName();

  @Inject
  private PoolingHttpClientConnectionManager connectionManager;

  @Inject
  private RepositoryManager repositoryManager;

  @Inject
  private DeadBlobFinder<?> deadBlobFinder;

  @Inject
  protected RestClientFactory restClientFactory;

  @Inject
  private TestSuiteObjectMapperResolver testSuiteObjectMapperResolver;

  @Inject
  private SecuritySystem securitySystem;

  @Inject
  private SelectorManager selectorManager;

  @Inject
  private AnonymousManager anonymousManager;

  @Inject
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @Rule
  public SecurityRule securityRule = new SecurityRule(() -> securitySystem, () -> selectorManager, () -> anonymousManager);

  @Configuration
  public static Option[] configureNexus() {
    return configureNexusBase();
  }

  /**
   * Configure Nexus base with out-of-the box settings (no HTTPS).
   */
  public static Option[] configureNexusBase() {
    return NexusTestDistributionService.getInstance().getDistribution(Distribution.BASE);
  }

  /**
   * Make sure Nexus is responding on the standard base URL before continuing
   */
  @Before
  public void waitForNexus() throws Exception {
    waitFor(responseFrom(nexusUrl));

    await().untilAsserted(() -> assertThat(upgradeTaskScheduler.getQueuedTaskCount(), is(0)));
  }

  /**
   * Verifies there are no unreleased HTTP connections in Nexus. This check runs automatically after each test but tests
   * may as well run this check manually at suitable points during their execution.
   */
  @After
  public void verifyNoConnectionLeak() throws Exception {
    // Some proxy repos directly serve upstream content, i.e. the connection to the upstream repo is actively used while
    // streaming out the response to the client. An HTTP client considers a response done when the content length has
    // been reached at which point the client/test can continue while NX still has to release the upstream connection
    // (cf. ResponseEntityProxy which releases a connection after the last byte has been handed out to the client).
    // So allow for some delay when checking the connection pool.
    waitFor(() -> connectionManager.getTotalStats().getLeased() == 0, 3 * 1000);
  }

  @After
  public void verifyNoDeadBlobs() throws Exception {
    //only need to verify no dead blobs for non-newdb dbs
    if (getValidTestDatabase().isUseContentStore()) {
      doVerifyNoDeadBlobs();
    }
  }

  /**
   * Left protected to allow specific subclasses to override where this behaviour is expected due to minimal test setup.
   */
  protected void doVerifyNoDeadBlobs() {
    Map<String, List<DeadBlobResult<?>>> badRepos = StreamSupport.stream(repositoryManager.browse().spliterator(), true)
        .map(repository -> deadBlobFinder.find(repository, shouldIgnoreMissingBlobRefs()))
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(DeadBlobResult::getRepositoryName));

    if (!badRepos.isEmpty()) {
      log.error("Detected dead blobs: {}", badRepos);
      throw new IllegalStateException("Dead blobs detected!");
    }
  }

  /**
   * Allow specific tests to override this behaviour where "missing" blobs are valid due to the test setup.
   */
  protected boolean shouldIgnoreMissingBlobRefs() {
    return false;
  }

  /**
   * @return Client that can use preemptive auth and self-signed certificates
   */
  protected HttpClientBuilder clientBuilder() throws Exception {
    return clientBuilder(nexusUrl);
  }

  protected HttpClientBuilder clientBuilder(final URL nexusUrl) throws Exception {
    return clientBuilder(nexusUrl, true);
  }

  protected HttpClientBuilder clientBuilder(final URL nexusUrl, final boolean useCredentials) throws Exception {
    HttpClientBuilder builder = HttpClients.custom();
    builder.setDefaultRequestConfig(requestConfig());
    if (useCredentials) {
      doUseCredentials(nexusUrl, builder);
    }
    builder.setSSLSocketFactory(sslSocketFactory());
    return builder;
  }

  protected void doUseCredentials(final URL nexusUrl, final HttpClientBuilder builder)
  {
    builder.setDefaultCredentialsProvider(credentialsProvider(nexusUrl));
  }

  /**
   * @return Policy for handling cookies on client side
   */
  protected RequestConfig requestConfig() {
    return RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
  }

  /**
   * @return Provider of credentials for preemptive auth
   */
  protected CredentialsProvider credentialsProvider() {
    return credentialsProvider(nexusUrl);
  }

  /**
   * @return Provider of credentials for preemptive auth
   */
  protected CredentialsProvider credentialsProvider(final URL nexusUrl) {
    AuthScope scope = new AuthScope(nexusUrl.getHost(), -1);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(scope, credentials());
    return credentialsProvider;
  }

  /**
   * @return Credentials to be used in preemptive auth
   */
  protected Credentials credentials() {
    return new UsernamePasswordCredentials("admin", "admin123");
  }

  /**
   * @return SSL socket factory that accepts self-signed certificates from any host
   */
  protected SSLConnectionSocketFactory sslSocketFactory() throws Exception {
    SSLContext context = SSLContexts.custom().loadTrustMaterial(trustStore(), new TrustSelfSignedStrategy()).build();
    return new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE);
  }

  /**
   * @return Client truststore containing exported Nexus certificate
   */
  protected KeyStore trustStore() throws Exception {
    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (FileInputStream instream = new FileInputStream(resolveBaseFile("target/it-resources/ssl/client.jks"))) {
      trustStore.load(instream, "password".toCharArray());
    }
    return trustStore;
  }

  /**
   * @return Context with preemptive auth enabled for Nexus
   */
  protected HttpClientContext clientContext() {
    HttpClientContext context = HttpClientContext.create();
    context.setAuthCache(basicAuthCache());
    return context;
  }

  /**
   * @return Cache with preemptive auth enabled for Nexus
   */
  protected AuthCache basicAuthCache() {
    String hostname = nexusUrl.getHost();
    AuthCache authCache = new BasicAuthCache();
    HttpHost hostHttp = new HttpHost(hostname, nexusUrl.getPort(), "http");
    HttpHost hostHttps = new HttpHost(hostname, nexusSecureUrl.getPort(), "https");
    authCache.put(hostHttp, new BasicScheme());
    authCache.put(hostHttps, new BasicScheme());
    return authCache;
  }

  /**
   * @return our session cookie; {@code null} if it doesn't exist
   */
  @Nullable
  protected Cookie getSessionCookie(final CookieStore cookieStore) {
    for (Cookie cookie : cookieStore.getCookies()) {
      if (DEFAULT_SESSION_COOKIE_NAME.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }


  /**
   * @return the last header containing a cookie; {@code null} if it doesn't exist
   */
  @Nullable
  protected Header getLastCookieHeader(@Nonnull final Header[] headers, final String cookieName) {
    for (int i = headers.length - 1; i >= 0; i--) {
      Header header = headers[i];
      if (header.getValue().startsWith(cookieName + "=")) {
        return header;
      }
    }
    return null;
  }

  /**
   * @return the header containing the anti-csrf token cookie; {@code null} if it doesn't exist
   */
  @Nullable
  protected Header getAntiCsrfTokenHeader(@Nonnull final Header[] headers) {
    for (Header header : headers) {
      if (header.getValue().startsWith("NX-ANTI-CSRF-TOKEN=")) {
        return header;
      }
    }
    return null;
  }

  /**
   * @return CookieOrigin suitable for validating session cookies from the given base URL
   */
  protected CookieOrigin cookieOrigin(final URL url) {
    return new CookieOrigin(url.getHost(), url.getPort(), cookiePath(url), "https".equals(url.getProtocol()));
  }

  /**
   * @return the expected cookie path value of our session cookie from the given base URL
   */
  protected String cookiePath(final URL url) {
    final String path = url.getPath();
    return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
  }

  /**
   * @return a proxy of the REST client described by {@code clazz} with authentication configured
   */
  protected <T> T restClient(final Class<T> clazz) throws Exception {
    URI restServiceUri = UriBuilder.fromUri(nexusUrl.toURI()).path(REST_SERVICE_PATH).build();
    return restClientFactory.proxy(clazz, restClient(), restServiceUri);
  }

  /**
   * @return a generic REST client with authentication configured
   */
  protected Client restClient() {
    try {
      final HttpClient httpClient = clientBuilder().build();
      final Credentials credentials = credentials();
      return restClientFactory
          .create(RestClientConfiguration.DEFAULTS
              .withHttpClient(() -> httpClient)
              .withCustomizer(getObjectMapperCustomizer(testSuiteObjectMapperResolver))
          )
          .register(new BasicAuthentication(credentials.getUserPrincipal().getName(), credentials.getPassword()));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The jax-rs clients require ObjectMapper customizations to work with ComponentXO.
   */
  private Customizer getObjectMapperCustomizer(final TestSuiteObjectMapperResolver testSuiteObjectMapperResolver) {
    return builder -> {
      ResteasyProviderFactory providerFactory = new LocalResteasyProviderFactory(
          ResteasyProviderFactory.newInstance());
      providerFactory.registerProviderInstance(testSuiteObjectMapperResolver, null, 1000, false);

      ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) builder;
      resteasyClientBuilder.providerFactory(providerFactory);
      RegisterBuiltin.register(providerFactory);
    };
  }

  /**
   * Preform a get request
   *
   * @param baseUrl (nexusUrl in most tests)
   * @param path    to the resource
   * @return the response object
   */
  protected Response get(final URL baseUrl, final String path) throws Exception {
    return get(baseUrl, path, null, true);
  }

  /**
   * Preform a get request
   *
   * @param baseUrl               (nexusUrl in most tests)
   * @param path                  to the resource
   * @param headers               {@link Header}s
   * @param useDefaultCredentials use {@link NexusITSupport#clientBuilder(URL, boolean)} for using credentials
   * @return the response object
   */
  protected Response get(final URL baseUrl,
                         final String path,
                         final Header[] headers,
                         final boolean useDefaultCredentials) throws Exception
  {
    HttpGet request = new HttpGet();
    request.setURI(UriBuilder.fromUri(baseUrl.toURI()).path(path).build());
    request.setHeaders(headers);

    try (CloseableHttpClient client = clientBuilder(nexusUrl, useDefaultCredentials).build()) {

      try (CloseableHttpResponse response = client.execute(request)) {
        ResponseBuilder responseBuilder = Response.status(response.getStatusLine().getStatusCode());
        Arrays.stream(response.getAllHeaders()).forEach(h -> responseBuilder.header(h.getName(), h.getValue()));

        HttpEntity entity = response.getEntity();
        if (entity != null) {
          responseBuilder.entity(new ByteArrayInputStream(IOUtils.toByteArray(entity.getContent())));
        }
        return responseBuilder.build();
      }
    }
  }

  protected String buildNexusUrl(final String... segments) {
    try {
      return nexusUrl.toURI().resolve(Joiner.on('/').join(asList(segments))).toString();
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
  }
}
