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
package org.sonatype.nexus.repository.httpclient.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.httpclient.HttpClientManager
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration
import org.sonatype.nexus.repository.httpclient.RequestHeaderAuthenticationStrategy
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config

import org.junit.Assert
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Matchers.any
import static org.mockito.Mockito.when

class HttpClientCustomConfigFacetImplTest
    extends TestSupport
{
  @Mock
  HttpClientManager httpClientManager;

  @Mock
  Config config;

  @Mock
  RequestHeaderAuthenticationStrategy requestHeaderAuthenticationStrategy;

  @Mock
  HttpClientConfiguration httpClientConfiguration;

  @Mock
  HttpClientCustomConfigFacetImpl httpClientCustomConfigFacet;

  @Test
  void testGetHttpClientConfiguration() {
    when(httpClientCustomConfigFacet.getHttpClientConfiguration(any(), any())).thenCallRealMethod();
    when(httpClientManager.newConfiguration()).thenReturn(httpClientConfiguration);
    when(httpClientConfiguration.getAuthenticationStrategy()).thenReturn(requestHeaderAuthenticationStrategy);
    HttpClientConfiguration configuration =
        httpClientCustomConfigFacet.getHttpClientConfiguration(httpClientManager, config);
    Assert.assertTrue(configuration.getAuthenticationStrategy().equals(requestHeaderAuthenticationStrategy));
  }
}
