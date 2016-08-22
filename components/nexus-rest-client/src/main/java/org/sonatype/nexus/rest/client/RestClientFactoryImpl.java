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
package org.sonatype.nexus.rest.client;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.rest.client.RestClientFactory;

import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * REST client factory.
 *
 * @since 3.0
 */
@Named
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
  public ResteasyClient create(@Nullable final Customizer customizer) {
    try (TcclBlock tccl = TcclBlock.begin(ResteasyClientBuilder.class)) {
      ResteasyClientBuilder builder = new ResteasyClientBuilder()
          .httpEngine(new ApacheHttpClient4Engine(httpClient.get()));

      if (customizer != null) {
        customizer.apply(builder);
      }

      return builder.build();
    }
  }

  @Override
  public ResteasyClient create() {
    return create(null);
  }
}
