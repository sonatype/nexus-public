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
package org.sonatype.nexus.apachehttpclient;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Component for creating pre-configured Apache HttpClient4x instances in Nexus.
 *
 * @author cstamas
 * @since 2.2
 */
public interface Hc4Provider
{
  /**
   * HTTP context key of (usually proxy) repository on who's behalf request is made. To be used with
   * {@link HttpClient#execute(HttpUriRequest, HttpContext)} method of {@link HttpClient} instance got from this
   * provider. Example code snippet:
   *
   * <pre>
   * final HttpGet httpRequest = new HttpGet( proxyRepository.getRemoteUrl() );
   * final BasicHttpContext httpContext = new BasicHttpContext();
   * httpContext.setAttribute( HTTP_CTX_KEY_REPOSITORY, proxyRepository );
   * final HttpResponse httpResponse = httpClient.execute( httpRequest, httpContext );
   * </pre>
   *
   * @since 2.4
   */
  String HTTP_CTX_KEY_REPOSITORY = Hc4Provider.class.getName() + ".repository";

  /**
   * Returns a new pre-configured instance of Apache HttpClient4x. This call will assemble a new instance of client
   * per every invocation. Created instances should be kept only during request execution, and should not be kept for
   * reuse over longer time span. Rather keep reference to this component and re-create client when needed. On Nexus
   * configuration changes, the client needs reconfiguration too, hence if you keep a reference to the created
   * client, you might end up with stale and non-working client (for example, global HTTP Proxy got changed between
   * your invocation of this method and when you want to perform HTTP request. Your instance would still try to talk to
   * HTTP proxy set in time when you created the instance). For resource optimization's sake, HttpClient instance
   * returned by this method  <em>does not support Keep-Alive</em> (unless configuration needs it).
   * If you need explicit control over connection reuse, or must have one a client
   * that reuse connections at any cause, use method {@link #createHttpClient(boolean)} instead.
   *
   * @return HttpClient4x pre-configured instance, that uses global {@link RemoteStorageContext} to be configured
   * (see {@link ApplicationConfiguration#getGlobalRemoteStorageContext()}).
   */
  HttpClient createHttpClient();

  /**
   * Returns a new pre-configured instance of Apache HttpClient4x. This call will assemble a new instance of client
   * per every invocation. Created instances should be kept only during request execution, and should not be kept for
   * reuse over longer time span. Rather keep reference to this component and re-create client when needed. On Nexus
   * configuration changes, the client needs reconfiguration too, hence if you keep a reference to the created
   * client, you might end up with stale and non-working client (for example, global HTTP Proxy got changed between
   * your invocation of this method and when you want to perform HTTP request. Your instance would still try to talk to
   * HTTP proxy set in time when you created the instance). The instance returned by this method will support
   * connection reuse if asked to. Keep in mind, that stale connection detection is not reliable (without traffic)
   * and that Nexus uses shared connection pool, so even getting connection manager from the returned
   * instance and shutting it down will not help (nor should be attempted!).
   *
   * @param reuseConnections if {@code true} the returned HTTP client will reuse connections (and hence, support
   *                         HTTP features as Keep Alive, etc).
   * @return HttpClient4x pre-configured instance, that uses global {@link RemoteStorageContext} to be configured
   * (see {@link ApplicationConfiguration#getGlobalRemoteStorageContext()}).
   * @since 2.7.2
   */
  HttpClient createHttpClient(boolean reuseConnections);

  /**
   * Advanced. Primarily to be used by subsystem that wants full control over the HTTP Client, it only uses the
   * "factory"-like features of this provider. In short: it wants to have pre-configured instance adjusted to passed
   * in {@link RemoteStorageContext}, namely with authentication and HTTP proxy configuration set. So far, that
   * subsystem is Nexus Proxy repositories. The created {@link HttpClient} will use the shared
   * {@link HttpClientConnectionManager} managed by this component, so instances created with this method must not be
   * managed or shutdown!
   *
   * @param context to source connection parameters from.
   * @return HttpClient4x pre-configured instance, that uses passed {@link RemoteStorageContext} to be configured.
   */
  HttpClient createHttpClient(RemoteStorageContext context);

  // ==

  /**
   * Builder carries not-yet built HttpClient parts and configuration, enabling to have it passed around to apply
   * configuration changes on it before client is finally built. After having built {@link HttpClient}, the returned
   * instance is immutable and does not expose getters either for various members like pool etc. Still, this instance
   * of Builder might be reused to create multiple clients, but in that case care must be take to apply reusable
   * parts (ie. if connection manager is applied, it has to be reusable too).
   *
   * @since 2.8
   */
  public static class Builder
  {
    private final HttpClientBuilder httpClientBuilder;

    private final ConnectionConfig.Builder connectionConfigBuilder;

    private final SocketConfig.Builder socketConfigBuilder;

    private final RequestConfig.Builder requestConfigBuilder;

    private final CredentialsProvider credentialsProvider;

    private boolean credentialsProviderAltered;

    Builder() {
      this(HttpClientBuilder.create(), ConnectionConfig.copy(ConnectionConfig.DEFAULT),
          SocketConfig.copy(SocketConfig.DEFAULT), RequestConfig.copy(RequestConfig.DEFAULT));
    }

    Builder(final HttpClientBuilder httpClientBuilder, final ConnectionConfig.Builder connectionConfigBuilder,
            final SocketConfig.Builder socketConfigBuilder, final RequestConfig.Builder requestConfigBuilder)
    {
      this.httpClientBuilder = checkNotNull(httpClientBuilder);
      this.connectionConfigBuilder = checkNotNull(connectionConfigBuilder);
      this.socketConfigBuilder = checkNotNull(socketConfigBuilder);
      this.requestConfigBuilder = checkNotNull(requestConfigBuilder);
      this.credentialsProvider = new BasicCredentialsProvider();
      this.credentialsProviderAltered = false;
    }

    /**
     * Returns the {@link HttpClientBuilder}.
     * <p/>Word of warning about method {@link HttpClientBuilder#setDefaultCredentialsProvider(CredentialsProvider)}:
     * by design, this method replaces any previously set credentials provider, and, there is no getter for it to
     * inspect any existing previously set value. Hence, be careful when using this method! Recommended way to
     * set multiple credentials from multiple places is to use {@link #setCredentials(AuthScope, Credentials)}.
     */
    public HttpClientBuilder getHttpClientBuilder() {
      return httpClientBuilder;
    }

    /**
     * Sets the {@link Credentials credentials} for the given authentication scope. Any previous credentials for the
     * given scope will be overwritten. See {@link CredentialsProvider#setCredentials(AuthScope, Credentials)}. This
     * method, once invoked, will replace any credentials provider set on {@link HttpClientBuilder} when {@link
     * #build()} method is called to build the client.
     *
     * @since 2.8.0
     */
    public Builder setCredentials(AuthScope authscope, Credentials credentials) {
      checkNotNull(authscope);
      credentialsProvider.setCredentials(authscope, credentials);
      credentialsProviderAltered = true;
      return this;
    }

    @VisibleForTesting
    CredentialsProvider getCredentialsProvider() {
      return credentialsProvider;
    }

    public ConnectionConfig.Builder getConnectionConfigBuilder() {
      return connectionConfigBuilder;
    }

    public SocketConfig.Builder getSocketConfigBuilder() {
      return socketConfigBuilder;
    }

    public RequestConfig.Builder getRequestConfigBuilder() {
      return requestConfigBuilder;
    }

    /**
     * Builds the {@link HttpClient} from current state of this builder. Once client is built and returned, it is
     * immutable and thread safe (unless explicitly configured with non-thread safe client connection manager).
     * This instance might be re-used to create multiple clients, as the configuration state once client is built, is
     * detached from it.
     */
    public HttpClient build() {
      httpClientBuilder.setDefaultConnectionConfig(connectionConfigBuilder.build());
      httpClientBuilder.setDefaultSocketConfig(socketConfigBuilder.build());
      httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
      if (credentialsProviderAltered) {
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
      }
      return httpClientBuilder.build();
    }
  }

  /**
   * Prepares but does not build a Builder, allowing extra configuration to be applied to it by caller.
   */
  Builder prepareHttpClient(RemoteStorageContext context);
}
