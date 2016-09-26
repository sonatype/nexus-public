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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.httpclient.SSLContextSelector;
import org.sonatype.nexus.rest.client.RestClientConfiguration;
import org.sonatype.nexus.rest.client.RestClientFactory;

import org.apache.http.client.HttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * REST client factory.
 *
 * @since 3.0
 */
@Named("default")
@Singleton
public class RestClientFactoryImpl
    extends ComponentSupport
    implements RestClientFactory
{
  private final Provider<HttpClient> httpClient;

  @Inject
  public RestClientFactoryImpl(final Provider<HttpClient> httpClient) {
    this.httpClient = checkNotNull(httpClient);
  }

  @Override
  public Client create(final RestClientConfiguration configuration) {
    checkNotNull(configuration);

    try (TcclBlock tccl = TcclBlock.begin(ResteasyClientBuilder.class)) {
      HttpContext httpContext = new BasicHttpContext();
      if (configuration.getUseTrustStore()) {
          httpContext.setAttribute(SSLContextSelector.USE_TRUST_STORE, true);
      }
      HttpClient client;
      if (configuration.getHttpClient() != null) {
        client = checkNotNull(configuration.getHttpClient().get());
      }
      else {
        client = httpClient.get();
      }
      ClientHttpEngine httpEngine = new ApacheHttpClient4Engine(client, httpContext);

      ResteasyClientBuilder builder = new ResteasyClientBuilder().httpEngine(httpEngine);

      if (configuration.getCustomizer() != null) {
        configuration.getCustomizer().apply(builder);
      }

      return builder.build();
    }
  }
}
