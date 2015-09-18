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
package org.sonatype.nexus.httpclient.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.httpclient.HttpClientManager;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.sisu.Typed;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link HttpClient} (and {@link CloseableHttpClient}) provider via {@link HttpClientManager}.
 *
 * @since 3.0
 */
@Named
@Typed({HttpClient.class, CloseableHttpClient.class})
public class HttpClientProvider
    implements Provider<CloseableHttpClient>
{
  private final HttpClientManager httpClientManager;

  @Inject
  public HttpClientProvider(final HttpClientManager httpClientManager) {
    this.httpClientManager = checkNotNull(httpClientManager);
  }

  @Override
  public CloseableHttpClient get() {
    return httpClientManager.create();
  }
}
