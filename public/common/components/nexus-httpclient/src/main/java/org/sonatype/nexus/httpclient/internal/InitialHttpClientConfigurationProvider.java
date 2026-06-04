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

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Initial {@link HttpClientConfiguration} provider.
 *
 * @since 3.0
 */
@Component
@Qualifier("initial")
public class InitialHttpClientConfigurationProvider
    implements FactoryBean<HttpClientConfiguration>
{
  private final HttpClientManager clientManager;

  @Autowired
  public InitialHttpClientConfigurationProvider(final HttpClientManager clientManager) {
    this.clientManager = checkNotNull(clientManager);
  }

  @Override
  public HttpClientConfiguration getObject() throws Exception {
    return clientManager.newConfiguration();
  }

  @Override
  public Class<?> getObjectType() {
    return HttpClientConfiguration.class;
  }
}
