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
package org.sonatype.nexus.testsuite.testsupport.system;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.sonatype.goodies.testsupport.hamcrest.BeanMatchers;
import org.sonatype.nexus.httpclient.PreemptiveAuthHttpRequestInterceptor;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.client.RestClientConfiguration;
import org.sonatype.nexus.rest.client.RestClientConfiguration.Customizer;
import org.sonatype.nexus.rest.client.RestClientFactory;
import org.sonatype.nexus.testsuite.testsupport.rest.TestSuiteObjectMapperResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.net.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;

@Named
@Singleton
public class RestTestHelper
{
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20L);

  private static final Duration CONNECTION_REQUEST_TIMEOUT = Duration.ofSeconds(30L);

  private static final Logger log = LoggerFactory.getLogger(RestTestHelper.class);

  public static final String REST_SERVICE_PATH = "service/rest";

  @Inject
  private ObjectMapper mapper;

  @Inject
  private RestClientFactory restClientFactory;

  @Inject
  private TestSuiteObjectMapperResolver testSuiteObjectMapperResolver;

  @Inject
  @Named("http://localhost:${application-port}${nexus-context-path}")
  private URL nexusUrl;

  @Inject
  @Named("https://localhost:${application-port-ssl}${nexus-context-path}")
  private URL nexusSecureUrl;

  /**
   * @return a proxy of the REST client described by {@code clazz} without authentication configured
   */
  public <T> T restClient(final Class<T> clazz) throws UriBuilderException
  {
    URI restServiceUri = UriBuilder.fromUri(nexusUrl()).path(REST_SERVICE_PATH).build();
    return restClientFactory.proxy(clazz, restClient(), restServiceUri);
  }

  /**
   * @return a proxy of the REST client described by {@code clazz} with authentication configured
   */
  public <T> T restClient(
      final Class<T> clazz,
      final String username,
      final String password) throws UriBuilderException
  {
    URI restServiceUri = UriBuilder.fromUri(nexusUrl()).path(REST_SERVICE_PATH).build();
    return restClientFactory.proxy(clazz, restClient(username, password), restServiceUri);
  }

  /**
   * @return a generic REST client without authentication configured
   */
  public Client restClient() {
    try {
      final CloseableHttpClient httpClient = clientBuilder().build();
      return restClientFactory
          .create(RestClientConfiguration.DEFAULTS
              .withHttpClient(() -> httpClient)
              .withCustomizer(getObjectMapperCustomizer(testSuiteObjectMapperResolver)));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return a generic REST client with authentication configured
   */
  public Client restClient(final String username, final String password) {
    try {
      final CloseableHttpClient httpClient = clientBuilder(nexusUrl(), username, password).build();
      final Credentials credentials = credentials(username, password);
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
   * @return a generic REST client with authentication configured and custom request config
   */
  public Client restClientWithCustomRequestConfig(final String username, final String password, RequestConfig requestConfig) {
    try {
      final CloseableHttpClient httpClient = clientBuilder(nexusUrl(), username, password, requestConfig).build();
      final Credentials credentials = credentials(username, password);
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
   * @return a CloseableHttpClient configured for Nexus with the provided authentication
   */
  public CloseableHttpClient client(final String path, final String username, final String password) {
    return clientBuilder(resolveUri(nexusUrl, path), username, password).build();
  }

  /**
   * @return a CloseableHttpClient configured for Nexus with the provided authentication
   */
  public CloseableHttpClient client(final String username, final String password)
  {
    return clientBuilder(nexusUrl(), username, password).build();
  }

  /**
   * @return a CloseableHttpClient configured for Nexus with the provided authentication and preemptive auth
   */
  public CloseableHttpClient clientWithPreemptiveAuth(final String username, final String password)
  {
    return clientWithPreemptiveAuth(username, password, __ -> {});
  }

  /**
   * @return a CloseableHttpClient configured for Nexus with the provided authentication and preemptive auth
   */
  public CloseableHttpClient clientWithPreemptiveAuth(
      final String username,
      final String password,
      final Consumer<HttpClientBuilder> mutator)
  {
    HttpClientBuilder builder = clientBuilder(nexusUrl(), username, password)
        .useSystemProperties()
        .addInterceptorFirst(new PreemptiveAuthHttpRequestInterceptor());

    mutator.accept(builder);

    return builder.build();
  }

  /**
   * @return a CloseableHttpClient configured for Nexus
   */
  public CloseableHttpClient client() {
    return clientBuilder().build();
  }

  /**
   * Obtain the primary URI for Nexus, this may be secure if the IT suite has been set to force SSL.
   */
  public URI nexusUrl() {
    try {
      return nexusUrl.toURI();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Obtain the secured URI for Nexus.
   */
  public URI nexusSecureUrl() {
    try {
      return nexusSecureUrl.toURI();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Resolves a path against the Nexus URL
   */
  public URI resolveNexusPath(final String path) {
    return resolveUri(nexusUrl, path);
  }

  /**
   * @return Context with preemptive auth enabled for Nexus
   */
  public HttpClientContext clientContext() {
    HttpClientContext context = HttpClientContext.create();
    context.setAuthCache(basicAuthCache());
    return context;
  }

  public static <T> T readResponseBody(final Class<T> clazz, final Response response) {
    try (InputStream in = (InputStream) response.getEntity()) {
      return new ObjectMapper().readerFor(clazz).readValue(IOUtils.toString(in, Charset.defaultCharset()));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Response post(
      final String path,
      final Object entity,
      final Map<String, String> queryParams,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpPost(), path, entity, queryParams, username, password);
  }

  public Response post(
      final String path,
      final Object entity,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpPost(), path, entity, Collections.emptyMap(), username, password);
  }

  public Response post(
      final String path,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpPost(), path, Collections.emptyMap(), username, password);
  }

  public Response put(final String path, final Object entity)
  {
    return execute(new HttpPut(), path, entity, Collections.emptyMap(), null, null);
  }

  public Response put(
      final String path,
      final Object entity,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpPut(), path, entity, Collections.emptyMap(), username, password);
  }

  public Response delete(
      final String path,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpDelete(), path, Collections.emptyMap(), username, password);
  }

  public Response delete(final String path) {
    return delete(path, Collections.emptyMap());
  }

  public Response delete(final String path, final Map<String, String> queryParams) {
    return execute(new HttpDelete(), path, queryParams, null, null);
  }

  public Response delete(
      final String path,
      final Map<String, String> queryParams,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpDelete(), path, queryParams, username, password);
  }

  public Response get(
      final String path,
      @Nullable final String username,
      @Nullable final String password,
      final Map<String, String> headers)
  {
    return execute(new HttpGet(), path, headers, Collections.emptyMap(), username, password);
  }

  public Response get(
      final String path,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpGet(), path, Collections.emptyMap(), username, password);
  }

  public Response get(final String path) {
    return get(path, null, null);
  }

  public Response get(final String path, final Map<String, String> queryParams) {
    return get(path, queryParams, null, null);
  }

  public Response get(
      final String path,
      final Map<String, String> queryParams,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(new HttpGet(), path, queryParams, username, password);
  }

  private Response execute(
      final HttpEntityEnclosingRequestBase request,
      final String path,
      final Object body,
      final Map<String, String> queryParams,
      @Nullable final String username,
      @Nullable final String password)
  {
    try {
      if (body instanceof String) {
        request.setEntity(new StringEntity((String) body, ContentType.TEXT_PLAIN));
      }
      else if (body instanceof byte[]) {
        request.setEntity(new ByteArrayEntity((byte[]) body, ContentType.APPLICATION_OCTET_STREAM));
      }
      else if (body instanceof File) {
        request.setEntity(new FileEntity((File) body, ContentType.APPLICATION_OCTET_STREAM));
      }
      else if (body instanceof MultipartEntityBuilder) {
        request.setEntity(((MultipartEntityBuilder)body).build());
      }
      else if (body != null){
        request.setEntity(
            new StringEntity(mapper.writerFor(body.getClass()).writeValueAsString(body), ContentType.APPLICATION_JSON));
      }
      return execute(request, path, queryParams, username, password);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Response execute(
      final HttpRequestBase request,
      final String path,
      final Map<String, String> queryParams,
      @Nullable final String username,
      @Nullable final String password)
  {
    return execute(request, path, Collections.emptyMap(), queryParams, username, password);
  }

  private Response execute(
      final HttpRequestBase request,
      final String path,
      final Map<String, String> headers,
      final Map<String, String> queryParams,
      @Nullable final String username,
      @Nullable final String password)
  {
    UriBuilder uriBuilder = UriBuilder.fromUri(nexusUrl()).path(path);
    queryParams.forEach(uriBuilder::queryParam);
    request.setURI(uriBuilder.build());

    if (username != null) {
      String auth = username + ":" + password;
      request.setHeader(HttpHeaders.AUTHORIZATION,
          "Basic " + new String(Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8))));
    }

    headers.forEach((key, value) -> request.setHeader(key, value));

    try (CloseableHttpClient client = username == null ? client() : client(username, password)) {
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
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @return Cache with preemptive auth enabled for Nexus
   */
  private AuthCache basicAuthCache() {
    String hostname = nexusUrl.getHost();
    AuthCache authCache = new BasicAuthCache();
    HttpHost hostHttp = new HttpHost(hostname, nexusUrl.getPort(), "http");
    HttpHost hostHttps = new HttpHost(hostname, nexusSecureUrl.getPort(), "https");
    authCache.put(hostHttp, new BasicScheme());
    authCache.put(hostHttps, new BasicScheme());
    return authCache;
  }

  /**
   * @return Client that can use self-signed certificates
   */
  private static HttpClientBuilder clientBuilder() {
    HttpClientBuilder builder = HttpClients.custom();
    builder.setDefaultRequestConfig(requestConfig());
    builder.setSSLSocketFactory(sslSocketFactory());
    return builder;
  }

  /**
   * @return Client that can use preemptive auth and self-signed certificates
   */
  private static HttpClientBuilder clientBuilder(final URI url, final String username, final String password)
  {
    return clientBuilder(url, username, password, defaultRequestConfig());
  }

  /**
   * @return Client that can use custom request config, preemptive auth and self-signed certificates
   */
  private static HttpClientBuilder clientBuilder(final URI url, final String username, final String password, RequestConfig requestConfig)
  {
    HttpClientBuilder builder = clientBuilder();
    if (username != null) {
      doUseCredentials(url, builder, username, password);
    }
    builder.setDefaultRequestConfig(requestConfig);
    return builder;
  }

  /**
   * @return SSL socket factory that accepts self-signed certificates from any host
   */
  public static SSLConnectionSocketFactory sslSocketFactory() {
    try {
      SSLContext context = SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
      return new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE);
    }
    catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return Policy for handling cookies on client side
   */
  private static RequestConfig requestConfig() {
    return RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
  }

  /**
   * @return Default request config
   */
  private static RequestConfig defaultRequestConfig() {
    int defaultTimeoutMillis = (int) DEFAULT_TIMEOUT.toMillis();
    return RequestConfig.custom()
        .setConnectTimeout(defaultTimeoutMillis)
        .setConnectionRequestTimeout((int) CONNECTION_REQUEST_TIMEOUT.toMillis())
        .setSocketTimeout(defaultTimeoutMillis).build();
  }

  /**
   * Set the provider of credentials for preemptive auth
   */
  private static void doUseCredentials(
      final URI nexusUrl,
      final HttpClientBuilder builder,
      final String username,
      final String password)
  {
    AuthScope scope = new AuthScope(nexusUrl.getHost(), -1);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(scope, credentials(username, password));
    builder.setDefaultCredentialsProvider(credentialsProvider);
  }

  /**
   * @return Credentials to be used in preemptive auth
   */
  private static Credentials credentials(final String username, final String password) {
    return new UsernamePasswordCredentials(username, password);
  }

  /**
   * The jax-rs clients require ObjectMapper customizations to work with ComponentXO.
   */
  private static Customizer getObjectMapperCustomizer(
      final TestSuiteObjectMapperResolver testSuiteObjectMapperResolver)
  {
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
   * Resolves path against the given URL.
   */
  private static URI resolveUri(final URL url, final String path) {
    try {
      return URI.create(url + "/" + path).normalize();
    }
    catch (final Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a Matcher which matches a Response where the body is the same as the supplied message.
   */
  public static <T> Matcher<Response> hasMessage(final T message) {
    return new TypeSafeDiagnosingMatcher<Response>()
    {
      @Override
      public void describeTo(final Description description) {
        description.appendText("Expected message: ").appendText(message.toString());
      }

      @Override
      protected boolean matchesSafely(final Response response, final Description mismatchDescription) {
        T actualMessage = getMessage(response);
        if (!message.equals(actualMessage)) {
          mismatchDescription.appendText("Actual message: " + actualMessage);
          return false;
        }
        return true;
      }

      @SuppressWarnings("unchecked")
      private T getMessage(final Response response) {
        try (InputStream in = (InputStream) response.getEntity()) {
          in.reset();
          String actualMessage = IOUtils.toString(in, Charset.defaultCharset());
          if (message instanceof String) {
            return (T) actualMessage;
          }
          return new ObjectMapper().readerFor(message.getClass()).readValue(actualMessage);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  /**
   * Returns a Matcher which matches a Response where the status code is the same as the supplied status.
   */
  public static Matcher<Response> hasStatus(final Status status) {
    return hasStatus(status.getStatusCode());
  }

  /**
   * Returns a Matcher which matches a Response where the status code is the same as the supplied code.
   */
  public static Matcher<Response> hasStatus(final int statusCode) {
    return new TypeSafeDiagnosingMatcher<Response>()
    {
      @Override
      public void describeTo(final Description description) {
        description.appendText("Status code: " + statusCode + " " + Status.fromStatusCode(statusCode).getReasonPhrase());
      }

      @Override
      protected boolean matchesSafely(final Response response, final Description mismatchDescription) {
        int actualStatus = response.getStatus();
        if (actualStatus != statusCode) {
          String message = "";
          try {
            message = response.hasEntity() ? response.readEntity(String.class) : null;
          }
          catch (Exception e) {
            log.info("Failed to extract message body from response {}", response, e);
          }
          mismatchDescription.appendText("Status code: " + actualStatus + " "
              + Status.fromStatusCode(actualStatus).getReasonPhrase() + " Message: " + message);
          return false;
        }
        return true;
      }
    };
  }

  /**
   * Asserts that the response is from a validation error.
   *
   * @param response       the response from the server
   * @param expectedErrors the expected validation errors
   */
  public static void assertValidationResponse(
      final Response response,
      final ValidationErrorXO... expectedErrors)
  {
    assertThat(response.getStatus(), is(HttpStatus.SC_BAD_REQUEST));

    ValidationErrorXO[] errors = response.readEntity(ValidationErrorXO[].class);

    for (ValidationErrorXO expectedError : expectedErrors) {
      assertThat(errors, hasItemInArray(BeanMatchers.similarTo(expectedError)));
    }
    assertThat(errors.length, is(expectedErrors.length));
  }

}
