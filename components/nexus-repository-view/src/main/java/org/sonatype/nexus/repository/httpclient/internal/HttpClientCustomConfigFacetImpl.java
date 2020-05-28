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
package org.sonatype.nexus.repository.httpclient.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.repository.httpclient.AutoBlockConfiguration;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RequestHeaderAuthenticationStrategy;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.RedirectStrategy;

/**
 * Custom {@link HttpClientFacet} implementation which allow override HttpClientConfiguration.
 *
 * @since 3.24
 */
@Named(HttpClientCustomConfigFacetImpl.NAME)
public class HttpClientCustomConfigFacetImpl
    extends HttpClientFacetImpl
{
  public static final String NAME = "HttpClientCustomConfigFacet";

  private final RequestHeaderAuthenticationStrategy requestHeaderAuthenticationStrategy;

  @Inject
  public HttpClientCustomConfigFacetImpl(
      final HttpClientManager httpClientManager,
      final Map<String, AutoBlockConfiguration> autoBlockConfiguration,
      final Map<String, RedirectStrategy> redirectStrategy,
      final RequestHeaderAuthenticationStrategy requestHeaderAuthenticationStrategy)
  {
    super(httpClientManager, autoBlockConfiguration, redirectStrategy);
    this.requestHeaderAuthenticationStrategy = requestHeaderAuthenticationStrategy;
  }

  @Override
  @VisibleForTesting
  protected HttpClientConfiguration getHttpClientConfiguration(
      final HttpClientManager httpClientManager,
      final Config config) {
    // construct http client delegate
    HttpClientConfiguration delegateConfig = httpClientManager.newConfiguration();
    delegateConfig.setConnection(config.connection);
    delegateConfig.setAuthentication(config.authentication);
    delegateConfig.setRedirectStrategy(getRedirectStrategy());
    delegateConfig.setAuthenticationStrategy(requestHeaderAuthenticationStrategy);
    return delegateConfig;
  }
}
