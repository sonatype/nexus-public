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
package org.sonatype.nexus.internal.httpclient;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.RedirectStrategy;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory {@link HttpClientConfigurationStore}.
 *
 * @since 3.0
 */
@Named("memory")
@Singleton
@Priority(Integer.MIN_VALUE)
@VisibleForTesting
public class MemoryHttpClientConfigurationStore
    extends ComponentSupport
    implements HttpClientConfigurationStore
{
  private HttpClientConfiguration model;

  @Nullable
  @Override
  public synchronized HttpClientConfiguration load() {
    return model;
  }

  @Override
  public synchronized void save(final HttpClientConfiguration configuration) {
    this.model = checkNotNull(configuration);
  }

  @Override
  public HttpClientConfiguration newConfiguration() {
    return new MemoryHttpClientConfiguration();
  }

  /**
   * @since 3.20
   */
  private static class MemoryHttpClientConfiguration
      implements HttpClientConfiguration, Cloneable
  {
    MemoryHttpClientConfiguration() {}

    @Valid
    @Nullable
    private ConnectionConfiguration connection;

    @Valid
    @Nullable
    private ProxyConfiguration proxy;

    @Valid
    @Nullable
    private RedirectStrategy redirectStrategy;

    /**
     * @see AuthenticationConfigurationDeserializer
     */
    @Valid
    @Nullable
    private AuthenticationConfiguration authentication;

    @Valid
    @Nullable
    private AuthenticationStrategy authenticationStrategy;

    @Valid
    @Nullable
    private Boolean shouldNormalizeUri;

    @Valid
    @Nullable
    private Boolean disableContentCompression;

    @Nullable
    public ConnectionConfiguration getConnection() {
      return connection;
    }

    public void setConnection(@Nullable final ConnectionConfiguration connection) {
      this.connection = connection;
    }

    @Nullable
    public ProxyConfiguration getProxy() {
      return proxy;
    }

    public void setProxy(@Nullable final ProxyConfiguration proxy) {
      this.proxy = proxy;
    }

    @Nullable
    public AuthenticationConfiguration getAuthentication() {
      return authentication;
    }

    public void setAuthentication(@Nullable final AuthenticationConfiguration authentication) {
      this.authentication = authentication;
    }

    @Nullable
    public RedirectStrategy getRedirectStrategy() {
      return redirectStrategy;
    }

    public void setRedirectStrategy(@Nullable final RedirectStrategy redirectStrategy) {
      this.redirectStrategy = redirectStrategy;
    }

    @Nullable
    @Override
    public AuthenticationStrategy getAuthenticationStrategy() {
      return authenticationStrategy;
    }

    @Override
    public void setAuthenticationStrategy(
        @Nullable final AuthenticationStrategy authenticationStrategy)
    {
      this.authenticationStrategy = authenticationStrategy;
    }

    @Override
    public Boolean getNormalizeUri() {
      return shouldNormalizeUri;
    }

    @Override
    public void setNormalizeUri(final Boolean normalizeUri) {
      this.shouldNormalizeUri = normalizeUri;
    }

    @Override
    public Boolean getDisableContentCompression() {
      return disableContentCompression;
    }

    @Override
    public void setDisableContentCompression(final Boolean disableContentCompression) {
      this.disableContentCompression = disableContentCompression;
    }

    public MemoryHttpClientConfiguration copy() {
      try {
        MemoryHttpClientConfiguration copy = (MemoryHttpClientConfiguration) clone();
        if (connection != null) {
          copy.connection = connection.copy();
        }
        if (proxy != null) {
          copy.proxy = proxy.copy();
        }
        if (authentication != null) {
          copy.authentication = authentication.copy();
        }
        if (redirectStrategy != null) {
          // no real cloning/copying needed, as we are allowed to use a singleton instance
          copy.redirectStrategy = redirectStrategy;
        }
        copy.shouldNormalizeUri = shouldNormalizeUri;
        copy.disableContentCompression = disableContentCompression;
        return copy;
      }
      catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "connection=" + connection +
          ", proxy=" + proxy +
          ", authentication=" + authentication +
          '}';
    }
  }
}
