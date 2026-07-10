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
package org.sonatype.nexus.rest.client.internal;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.httpclient.SSLContextSelector;
import org.sonatype.nexus.rest.client.RestClientConfiguration;
import org.sonatype.nexus.rest.client.RestClientFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
// NEXUS-46395: ApacheHttpClient4Engine (Apache HttpClient 4.x) was removed in RESTEasy 7.
// We migrated to ApacheHttpClient43Engine (Apache HttpClient 4.3+) since the application-
// wide HttpClient is still HttpClient 4.x; a future bump to Apache HttpClient 5 +
// ApacheHttpClient5Engine is tracked separately.
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.cache.CacheLoader.from;

/**
 * REST client factory.
 *
 * @since 3.0
 */
@Primary
@Component
@Qualifier("default")
public class RestClientFactoryImpl
    implements RestClientFactory
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final LoadingCache<ClassLoader, ClassLoader> bridgeClassLoaderCache =
      CacheBuilder.newBuilder()
          .build(from(
              (loader) -> new BridgeClassLoader(loader, ProxyBuilder.class.getClassLoader())));

  private final Provider<HttpClient> httpClient;

  @Autowired
  public RestClientFactoryImpl(final Provider<HttpClient> httpClient) {
    this.httpClient = checkNotNull(httpClient);
  }

  @Override
  public Client create(final RestClientConfiguration configuration) {
    checkNotNull(configuration);

    try (TcclBlock tccl = TcclBlock.begin(ResteasyClientBuilder.class)) {
      final HttpClient client;
      if (configuration.getHttpClient() != null) {
        client = checkNotNull(configuration.getHttpClient().get());
      }
      else {
        client = httpClient.get();
      }

      // NEXUS-46395: build a fresh BasicHttpContext per request and route
      // SSLContextSelector.USE_TRUST_STORE through it when the caller has asked for the
      // Nexus-managed truststore. RESTEasy 7's ApacheHttpClient43Engine consumes the
      // context via HttpContextProvider#getContext (replacing v3's plain (HttpClient,
      // HttpContext) ctor); we hand it a fresh context per call so concurrent invocations
      // don't share mutable state.
      final boolean useTrustStore = configuration.getUseTrustStore();
      HttpContextProvider contextProvider = () -> {
        HttpContext ctx = new BasicHttpContext();
        if (useTrustStore) {
          ctx.setAttribute(SSLContextSelector.USE_TRUST_STORE, true);
        }
        return ctx;
      };

      // NEXUS-46395: the (HttpClient, HttpContextProvider) ctor sets
      // closeHttpClient=false internally. The (HttpClient, boolean) ctor defaults that
      // flag to true, which would dispose the application-wide pooled HttpClient on the
      // first Client#close() and break every subsequent caller (replication, IQ, S3
      // metadata, etc.). Always go through the provider form.
      ClientHttpEngine httpEngine = new ApacheHttpClient43Engine(client, contextProvider);

      // NEXUS-46395: ResteasyClientBuilder became abstract in RESTEasy 7; obtain an
      // implementation through the JAX-RS ClientBuilder SPI and cast.
      ResteasyClientBuilder builder =
          (ResteasyClientBuilder) jakarta.ws.rs.client.ClientBuilder.newBuilder();
      builder.httpEngine(httpEngine);

      if (configuration.getCustomizer() != null) {
        configuration.getCustomizer().apply(builder);
      }

      return builder.build();
    }
  }

  @Override
  public <T> T proxy(final Class<T> api, final Client client, final URI baseUri) {
    WebTarget target = client.target(baseUri);

    return ProxyBuilder.builder(api, target)
        .classloader(bridgeClassLoaderCache.getUnchecked(api.getClassLoader()))
        .build();
  }
}
