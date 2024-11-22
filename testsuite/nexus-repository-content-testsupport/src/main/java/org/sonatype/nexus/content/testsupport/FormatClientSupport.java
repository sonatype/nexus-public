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
package org.sonatype.nexus.content.testsupport;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.http.HttpHeaders.IF_MODIFIED_SINCE;

/**
 * Support class for HTTP-based repository test clients.
 */
public class FormatClientSupport
    extends ComponentSupport
{
  protected final CloseableHttpClient httpClient;

  protected final HttpClientContext httpClientContext;

  protected final URI repositoryBaseUri;

  public FormatClientSupport(
      final CloseableHttpClient httpClient,
      final HttpClientContext httpClientContext,
      final URI repositoryBaseUri)
  {
    this.httpClient = checkNotNull(httpClient);
    this.httpClientContext = checkNotNull(httpClientContext);
    this.repositoryBaseUri = checkNotNull(repositoryBaseUri);
  }

  public static String asString(final HttpResponse response) throws IOException {
    return EntityUtils.toString(response.getEntity());
  }

  /**
   * GET a response from the repository.
   */
  public CloseableHttpResponse get(final String path) throws IOException {
    return get(path, Collections.emptyMap());
  }

  /**
   * GET a conditional response from the repository using the If-Modified-Since header.
   */
  public CloseableHttpResponse getIfModifiedSince(final String path, final String modified) throws IOException {
    HttpGet get = new HttpGet(resolve(path));
    get.setHeader(IF_MODIFIED_SINCE, modified);
    return execute(get);
  }

  /**
   * GET a response from the repository, adding headers to the request.
   */
  public CloseableHttpResponse get(final String path, Map<String, String> headers) throws IOException {
    final URI uri = resolve(path);
    final HttpGet get = new HttpGet(uri);
    for (Entry<String, String> header : headers.entrySet()) {
      get.setHeader(header.getKey(), header.getValue());
    }
    return execute(get);
  }

  public CloseableHttpResponse head(final String path) throws IOException {
    return execute(new HttpHead(resolve(path)));
  }

  protected CloseableHttpResponse execute(final HttpUriRequest request) throws IOException {
    return execute(request, new BasicHttpContext(httpClientContext));
  }

  protected CloseableHttpResponse execute(final HttpUriRequest request, HttpContext context) throws IOException {
    log.info("Requesting {}", request);
    final CloseableHttpResponse response = httpClient.execute(request, context);
    log.info("Received {}", response);
    return response;
  }

  protected CloseableHttpResponse execute(
      final HttpUriRequest request,
      String username,
      String password) throws IOException
  {
    log.debug("Authorizing request for {} using credentials provided for username: {}",
        request.getURI(), username);
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

    HttpHost host = URIUtils.extractHost(request.getURI());

    AuthCache authCache = new BasicAuthCache();
    authCache.put(host, new BasicScheme());

    HttpClientContext clientContext = new HttpClientContext(httpClientContext);
    clientContext.setAuthCache(authCache);
    clientContext.setCredentialsProvider(credsProvider);

    return execute(request, clientContext);
  }

  @Nonnull
  protected URI resolve(final String path) {
    return repositoryBaseUri.resolve(path);
  }

  public static int status(HttpResponse response) {
    checkNotNull(response);
    return response.getStatusLine().getStatusCode();
  }

  public static byte[] bytes(HttpResponse response) throws IOException {
    checkState(response.getEntity() != null);
    return EntityUtils.toByteArray(response.getEntity());
  }

  public static HttpResponse consume(final CloseableHttpResponse response) throws IOException {
    EntityUtils.consume(response.getEntity());
    return response;
  }
}
