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
import javax.inject.Singleton;

import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Initial {@link HttpClientConfiguration} provider.
 *
 * @since 3.0
 */
@Named("initial")
@Singleton
public class InitialHttpClientConfigurationProvider
  implements Provider<HttpClientConfiguration>
{
  private final HttpClientManager clientManager;

  @Inject
  public InitialHttpClientConfigurationProvider(final HttpClientManager clientManager) {
    this.clientManager = checkNotNull(clientManager);
  }

  @Override
  public HttpClientConfiguration get() {
    HttpClientConfiguration configuration = clientManager.newConfiguration();
    // TODO:
    return configuration;
  }
}
