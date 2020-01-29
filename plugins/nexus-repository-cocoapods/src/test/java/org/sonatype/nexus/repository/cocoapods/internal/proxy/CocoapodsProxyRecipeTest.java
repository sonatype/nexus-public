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
package org.sonatype.nexus.repository.cocoapods.internal.proxy;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.cocoapods.CocoapodsFacet;
import org.sonatype.nexus.repository.cocoapods.internal.CocoapodsFormat;
import org.sonatype.nexus.repository.cocoapods.internal.CocoapodsSecurityFacet;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.FormatHighAvailabilitySupportHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.view.Router.LOCAL_ATTRIBUTE_PREFIX;

@RunWith(JUnitParamsRunner.class)
public class CocoapodsProxyRecipeTest
    extends TestSupport
{
  @Mock
  private Provider<AttributesFacet> attributesFacet;

  @Mock
  private Provider<CocoapodsFacet> cocoapodsFacet;

  private ConfigurableViewFacet cocoapodsViewFacet = new ConfigurableViewFacet();

  @Mock
  private Provider<SingleAssetComponentMaintenance> componentMaintenance;

  @Mock
  private ConditionalRequestHandler conditionalRequestHandler;

  @Mock
  Context context;

  @Mock
  private ExceptionHandler exceptionHandler;

  @Mock
  private Provider<HttpClientFacet> httpClientFacet;

  @Mock
  CocoapodsFormat format;

  @Mock
  private LastDownloadedHandler lastDownloadedHandler;

  @Mock
  private Provider<NegativeCacheFacet> negativeCacheFacet;

  @Mock
  private NegativeCacheHandler negativeCacheHandler;

  @Mock
  private ContentHeadersHandler ontentHeadersHandler;

  @Mock
  private PartialFetchHandler partialFetchHandler;

  @Mock
  private Provider<CocoapodsProxyFacet> proxyFacet;

  @Mock
  private ProxyHandler proxyHandler;

  @Mock
  private Provider<PurgeUnusedFacet> purgeUnusedFacet;

  @Mock
  private Repository repository;

  @Mock
  Request request;

  @Mock
  private RoutingRuleHandler routingHandler;

  @Mock
  private Provider<SearchFacet> searchFacet;

  @Mock
  private Provider<CocoapodsSecurityFacet> securityFacet;

  @Mock
  SecurityHandler securityHandler;

  @Mock
  private Provider<StorageFacet> storageFace;

  private CocoapodsProxyRecipe underTest;

  @Mock
  private UnitOfWorkHandler unitOfWorkHandler;

  @Mock
  private Provider<ConfigurableViewFacet> viewFacet;

  @Mock
  private HandlerContributor handlerContributor;

  @Mock
  HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  @Mock
  FormatHighAvailabilitySupportHandler highAvailabilitySupportHandler;

  TimingHandler timingHandler;

  final String COCOAPODS_NAME = "cocoapods";

  @Before
  public void setup() throws Exception
  {
    AttributesMap attributesMap = new AttributesMap();
    attributesMap.set(LOCAL_ATTRIBUTE_PREFIX + "some_key", "some_value");
    when(context.getRepository()).thenReturn(repository);
    when(context.getAttributes()).thenReturn(attributesMap);
    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(viewFacet.get()).thenReturn(cocoapodsViewFacet);
    when(format.getValue()).thenReturn(COCOAPODS_NAME);
    timingHandler = spy(new TimingHandler(null));

    underTest = new CocoapodsProxyRecipe(new ProxyType(), format);
    underTest.timingHandler = timingHandler;
    underTest.securityFacet = securityFacet;
    underTest.viewFacet = viewFacet;
    underTest.securityHandler = securityHandler;
    underTest.routingHandler = routingHandler;
    underTest.exceptionHandler = exceptionHandler;
    underTest.negativeCacheHandler = negativeCacheHandler;
    underTest.conditionalRequestHandler = conditionalRequestHandler;
    underTest.partialFetchHandler = partialFetchHandler;
    underTest.contentHeadersHandler = ontentHeadersHandler;
    underTest.unitOfWorkHandler = unitOfWorkHandler;
    underTest.lastDownloadedHandler = lastDownloadedHandler;
    underTest.proxyHandler = proxyHandler;
    underTest.httpClientFacet = httpClientFacet;
    underTest.negativeCacheFacet = negativeCacheFacet;
    underTest.proxyFacet = proxyFacet;
    underTest.cocoapodsFacet = cocoapodsFacet;
    underTest.storageFacet = storageFace;
    underTest.attributesFacet = attributesFacet;
    underTest.componentMaintenance = componentMaintenance;
    underTest.searchFacet = searchFacet;
    underTest.purgeUnusedFacet = purgeUnusedFacet;
    underTest.highAvailabilitySupportHandler = highAvailabilitySupportHandler;
    underTest.highAvailabilitySupportChecker = highAvailabilitySupportChecker;
    underTest.handlerContributor = handlerContributor;

    underTest.apply(repository);
    cocoapodsViewFacet.attach(repository);
  }

  /**
   * For pods fetching Cocoapods Recipe applies security check
   */
  @Test
  public void testPodHandling() throws Exception {
    when(request.getPath()).thenReturn("/pods/any.tar.gz");
    cocoapodsViewFacet.dispatch(request, context);
    verify(securityHandler, times(1)).handle(any());
  }

  /**
   * For CocoaPods-version fetching Cocoapods Recipe doesn't apply security check
   */
  @Test
  public void testCocoaPodsVersionHandling() throws Exception {
    when(request.getPath()).thenReturn("/CocoaPods-version.yml");
    cocoapodsViewFacet.dispatch(request, context);
    verify(securityHandler, times(0)).handle(any());
  }

  /**
   * For all_pods* fetching Cocoapods Recipe doesn't apply security check
   */
  @Test
  public void testAllPodsVersionsHandling() throws Exception {
    when(request.getPath()).thenReturn("/all_pods_versions_5_7_4.txt");
    cocoapodsViewFacet.dispatch(request, context);
    verify(securityHandler, times(0)).handle(any());
  }

  /**
   * For single spec fetching Cocoapods Recipe doesn't apply security check
   */
  @Test
  public void testSingleSpecHandling() throws Exception {
    when(request.getPath()).thenReturn("/Specs/test.podspec.json");
    cocoapodsViewFacet.dispatch(request, context);
    verify(securityHandler, times(0)).handle(any());
  }

  @Test
  public void testEnabledIfHaCheckPassed() {
    when(highAvailabilitySupportChecker.isSupported(COCOAPODS_NAME)).thenReturn(true);
    assertThat(underTest.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(COCOAPODS_NAME);
  }

  @Test
  public void testDisabledIfHaCheckReturnsFalse() {
    when(highAvailabilitySupportChecker.isSupported(COCOAPODS_NAME)).thenReturn(false);
    assertThat(underTest.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(COCOAPODS_NAME);
  }

  @Test
  public void testHighAvailabilitySupportHandlerAdded() throws Exception {
    when(request.getPath()).thenReturn("/CocoaPods-version.yml");
    cocoapodsViewFacet.dispatch(request, context);
    verify(highAvailabilitySupportHandler, times(1)).handle(any());
  }


  @Test
  @Parameters(method = "provideRoutingTestParameters")
  public void testRouting(String route, boolean supported) throws Exception {
    when(request.getPath()).thenReturn(route);
    cocoapodsViewFacet.dispatch(request, context);
    verify(timingHandler, times(supported? 1: 0)).handle(any());
  }

  private static Object[] provideRoutingTestParameters() {
    return new Object[]{
        new Object[]{"/Specs/spec1", "true"},
        new Object[]{"/Specs/segment/spec1", "true"},

        new Object[]{"/CocoaPods-version.yml", "true"},
        new Object[]{"prefix/CocoaPods-version.yml", "true"},

        new Object[]{"/deprecated_podspecs.txt", "true"},
        new Object[]{"prefix/deprecated_podspecs.txt", "true"},

        new Object[]{"/all_pods_any_suffix.txt", "true"},
        new Object[]{"prefix/all_pods_any_suffix.txt", "true"},
        new Object[]{"/all_pods_any_suffix.avi", "false"},

        new Object[]{"/pods/any_suffix", "true"},
        new Object[]{"/unknown_segment/any_suffix", "false"},

        new Object[]{"/unknown_route", "false"},
    };
  }
}
