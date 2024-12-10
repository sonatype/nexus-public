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
package org.sonatype.nexus.content.raw.internal.recipe;

import javax.inject.Provider;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class RawProxyRecipeTest
    extends RawRecipeTestSupport
{
  @Mock
  private Repository rawProxyRepository;

  @Mock
  private NegativeCacheHandler negativeCacheHandler;

  @Mock
  private PartialFetchHandler partialFetchHandler;

  @Mock
  private ProxyHandler proxyHandler;

  @Mock
  private ConditionalRequestHandler conditionalRequestHandler;

  @Mock
  private HandlerContributor handlerContributor;

  @Mock
  private RoutingRuleHandler routingRuleHandler;

  @Mock
  private HttpClientFacet httpClientFacet;

  private final Provider<HttpClientFacet> httpClientFacetProvider = () -> httpClientFacet;

  @Mock
  private NegativeCacheFacet negativeCacheFacet;

  private final Provider<NegativeCacheFacet> negativeCacheFacetProvider = () -> negativeCacheFacet;

  @Mock
  private RawProxyFacet proxyFacet;

  private final Provider<RawProxyFacet> rawProxyFacetProvider = () -> proxyFacet;

  @Mock
  private PurgeUnusedFacet purgeUnusedFacet;

  private final Provider<PurgeUnusedFacet> purgeUnusedFacetProvider = () -> purgeUnusedFacet;

  private RawProxyRecipe underTest;

  @Before
  public void setup() {
    underTest =
        new RawProxyRecipe(new ProxyType(), new RawFormat(), httpClientFacetProvider, negativeCacheFacetProvider,
            rawProxyFacetProvider, purgeUnusedFacetProvider, negativeCacheHandler, proxyHandler, routingRuleHandler);
    mockDependencies(underTest);
  }

  @Test
  public void testExpectedFacetsAreAttached() throws Exception {
    underTest.apply(rawProxyRepository);
    verify(rawProxyRepository).attach(securityFacet);
    verify(rawProxyRepository).attach(viewFacet);
    verify(rawProxyRepository).attach(httpClientFacet);
    verify(rawProxyRepository).attach(negativeCacheFacet);
    verify(rawProxyRepository).attach(proxyFacet);
    verify(rawProxyRepository).attach(contentFacet);
    verify(rawProxyRepository).attach(maintenanceFacet);
    verify(rawProxyRepository).attach(searchFacet);
    verify(rawProxyRepository).attach(browseFacet);
    verify(rawProxyRepository).attach(purgeUnusedFacet);
  }
}
