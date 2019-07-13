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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory for creating clients of the type {@link FormatClientITSupport}
 *
 * @since 3.13
 */
public abstract class NexusClientFactory<T extends FormatClientSupport>
    extends ComponentSupport
{
  public abstract T createClient(final CloseableHttpClient httpClient,
                                 final HttpClientContext httpClientContext,
                                 final URI repositoryBaseUri);

  @Nullable
  public T createClient(final URL repositoryUrl,
                        final String username,
                        final String password)
  {
    checkNotNull(repositoryUrl);
    checkNotNull(username);
    checkNotNull(password);

    AuthScope scope = new AuthScope(repositoryUrl.getHost(), -1);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(scope, new UsernamePasswordCredentials(username, password));

    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
    requestConfigBuilder.setExpectContinueEnabled(true);

    AuthCache authCache = new BasicAuthCache();
    authCache.put(new HttpHost(repositoryUrl.getHost(), repositoryUrl.getPort()), new BasicScheme());

    HttpClientContext httpClientContext = HttpClientContext.create();
    httpClientContext.setAuthCache(authCache);
    httpClientContext.setRequestConfig(requestConfigBuilder.build());

    try {
      return createClient(
          HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build(),
          httpClientContext,
          repositoryUrl.toURI()
      );
    }
    catch (URISyntaxException e) {
      log.warn("Uri exception creating Client", e);
    }

    return null;
  }
}
