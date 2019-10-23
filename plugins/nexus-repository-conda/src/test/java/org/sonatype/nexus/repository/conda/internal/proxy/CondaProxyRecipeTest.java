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
package org.sonatype.nexus.repository.conda.internal.proxy;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.conda.internal.CondaComponentMaintenanceFacet;
import org.sonatype.nexus.repository.conda.internal.CondaFacetImpl;
import org.sonatype.nexus.repository.conda.internal.CondaFormat;
import org.sonatype.nexus.repository.conda.internal.security.CondaSecurityFacet;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.handlers.BrowseUnsupportedHandler;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.FormatHighAvailabilitySupportHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import static org.sonatype.nexus.repository.view.Router.LOCAL_ATTRIBUTE_PREFIX;

/**
 * @since 3.19
 */
public class CondaProxyRecipeTest
    extends TestSupport
{
  private final static String CONDA_NAME = "conda";

  private CondaProxyRecipe proxyRecipe;

  @Mock
  private CondaFormat format;

  @Mock
  private Context context;

  @Mock
  private Request request;

  @Mock
  private Repository repository;

  @Mock
  private TimingHandler timingHandler;

  @Mock
  private SecurityHandler securityHandler;

  @Mock
  private HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  @Mock
  private FormatHighAvailabilitySupportHandler highAvailabilitySupportHandler;

  @Mock
  private Provider<CondaSecurityFacet> securityFacet;

  @Mock
  private ExceptionHandler exceptionHandler;

  @Mock
  private HandlerContributor handlerContributor;

  @Mock
  private NegativeCacheHandler negativeCacheHandler;

  @Mock
  private ConditionalRequestHandler conditionalRequestHandler;

  @Mock
  private PartialFetchHandler partialFetchHandler;

  @Mock
  private ContentHeadersHandler contentHeadersHandler;

  @Mock
  private UnitOfWorkHandler unitOfWorkHandler;

  @Mock
  private LastDownloadedHandler lastDownloadedHandler;

  @Mock
  private ProxyHandler proxyHandler;

  @Mock
  private Provider<ConfigurableViewFacet> viewFacet;

  private ConfigurableViewFacet condaViewFacet;

  @Mock
  private Provider<HttpClientFacet> httpClientFacet;

  @Mock
  private Provider<NegativeCacheFacet> negativeCacheFacet;

  @Mock
  private Provider<CondaProxyFacetImpl> proxyFacet;

  @Mock
  private Provider<CondaFacetImpl> condaFacet;

  @Mock
  private Provider<StorageFacet> storageFacet;

  @Mock
  private Provider<AttributesFacet> attributesFacet;

  @Mock
  private Provider<CondaComponentMaintenanceFacet> componentMaintenanceFacet;

  @Mock
  private Provider<SearchFacet> searchFacet;

  @Mock
  private Provider<PurgeUnusedFacet> purgeUnusedFacet;

  @Mock
  private BrowseUnsupportedHandler browseUnsupportedHandler;

  @Mock
  RoutingRuleHandler routingRuleHandler;

  @Before
  public void setup() throws Exception
  {
    condaViewFacet = new ConfigurableViewFacet();
    AttributesMap attributesMap = new AttributesMap();
    attributesMap.set(LOCAL_ATTRIBUTE_PREFIX + "some_key", "some_value");
    when(format.getValue()).thenReturn(CONDA_NAME);
    when(viewFacet.get()).thenReturn(condaViewFacet);
    when(context.getRepository()).thenReturn(repository);
    when(context.getAttributes()).thenReturn(attributesMap);
    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(securityHandler.handle(any())).thenAnswer(createPropagationAnswer());
    when(timingHandler.handle(any())).thenAnswer(createPropagationAnswer());
    when(browseUnsupportedHandler.getRoute()).thenReturn(
        new Route(mock(Matcher.class), ImmutableList.of(securityHandler, browseUnsupportedHandler)));

    proxyRecipe = new CondaProxyRecipe(new ProxyType(), format);

    proxyRecipe.setTimingHandler(timingHandler);
    proxyRecipe.setHighAvailabilitySupportHandler(highAvailabilitySupportHandler);
    proxyRecipe.setSecurityHandler(securityHandler);
    proxyRecipe.setExceptionHandler(exceptionHandler);
    proxyRecipe.setHandlerContributor(handlerContributor);
    proxyRecipe.setNegativeCacheHandler(negativeCacheHandler);
    proxyRecipe.setConditionalRequestHandler(conditionalRequestHandler);
    proxyRecipe.setPartialFetchHandler(partialFetchHandler);
    proxyRecipe.setContentHeadersHandler(contentHeadersHandler);
    proxyRecipe.setUnitOfWorkHandler(unitOfWorkHandler);
    proxyRecipe.setLastDownloadedHandler(lastDownloadedHandler);
    proxyRecipe.setProxyHandler(proxyHandler);
    proxyRecipe.setBrowseUnsupportedHandler(browseUnsupportedHandler);
    proxyRecipe.setRoutingHandler(routingRuleHandler);

    proxyRecipe.setSecurityFacet(securityFacet);
    proxyRecipe.setViewFacet(viewFacet);
    proxyRecipe.setHttpClientFacet(httpClientFacet);
    proxyRecipe.setNegativeCacheFacet(negativeCacheFacet);
    proxyRecipe.setProxyFacet(proxyFacet);
    proxyRecipe.setCondaFacet(condaFacet);
    proxyRecipe.setStorageFacet(storageFacet);
    proxyRecipe.setAttributesFacet(attributesFacet);
    proxyRecipe.setComponentMaintenanceFacet(componentMaintenanceFacet);
    proxyRecipe.setSearchFacet(searchFacet);
    proxyRecipe.setPurgeUnusedFacet(purgeUnusedFacet);
    proxyRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);

    proxyRecipe.apply(repository);
    condaViewFacet.attach(repository);
  }

  @Test
  public void testEnabledIfHaCheckPassed() {
    when(highAvailabilitySupportChecker.isSupported(CONDA_NAME)).thenReturn(true);
    assertThat(proxyRecipe.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(CONDA_NAME);
  }

  @Test
  public void testDisabledIfHaCheckReturnsFalse() {
    when(highAvailabilitySupportChecker.isSupported(CONDA_NAME)).thenReturn(false);
    assertThat(proxyRecipe.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(CONDA_NAME);
  }

  @Test
  public void testHighAvailabilitySupportHandlerAdded() throws Exception {
    when(request.getPath()).thenReturn("/main/linux-64/curl/7.55.1/curl-7.55.1-h78862de_4.tar.bz2");
    condaViewFacet.dispatch(request, context);
    verify(highAvailabilitySupportHandler, times(1)).handle(any());
  }

  private static Answer<Response> createPropagationAnswer() {
    return invocation -> {
      Context context = (Context) invocation.getArguments()[0];
      return context.proceed();
    };
  }
}
