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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.SingleAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.raw.ContentDispositionHandler;
import org.sonatype.nexus.repository.raw.internal.RawIndexHtmlForwardHandler;
import org.sonatype.nexus.repository.raw.internal.RawSecurityFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import org.mockito.Mock;

public abstract class RawRecipeTestSupport
    extends TestSupport
{
  @Mock
  private ExceptionHandler exceptionHandler;

  @Mock
  private TimingHandler timingHandler;

  @Mock
  private RawIndexHtmlForwardHandler indexHtmlForwardHandler;

  @Mock
  private SecurityHandler securityHandler;

  @Mock
  private PartialFetchHandler partialFetchHandler;

  @Mock
  private RawContentHandler contentHandler;

  @Mock
  private ConditionalRequestHandler conditionalRequestHandler;

  @Mock
  private ContentHeadersHandler contentHeadersHandler;

  @Mock
  private LastDownloadedHandler lastDownloadedHandler;

  @Mock
  private HandlerContributor handlerContributor;

  @Mock
  private ContentDispositionHandler contentDispositionHandler;

  @Mock
  protected RawSecurityFacet securityFacet;

  private final Provider<RawSecurityFacet> securityFacetProvider = () -> securityFacet;

  @Mock
  protected ConfigurableViewFacet viewFacet;

  private final Provider<ConfigurableViewFacet> viewFacetProvider = () -> viewFacet;

  @Mock
  protected RawContentFacet contentFacet;

  private final Provider<RawContentFacet> contentFacetProvider = () -> contentFacet;

  @Mock
  protected SingleAssetMaintenanceFacet maintenanceFacet;

  private final Provider<SingleAssetMaintenanceFacet> maintenanceFacetProvider = () -> maintenanceFacet;

  @Mock
  protected SearchFacet searchFacet;

  private final Provider<SearchFacet> searchFacetProvider = () -> searchFacet;

  @Mock
  protected BrowseFacet browseFacet;

  private final Provider<BrowseFacet> browseFacetProvider = () -> browseFacet;

  protected <T extends RawRecipeSupport> void mockDependencies(final T underTest) {
    underTest.setDependencies(securityFacetProvider, viewFacetProvider, contentFacetProvider, maintenanceFacetProvider,
        searchFacetProvider, browseFacetProvider, exceptionHandler, timingHandler, indexHtmlForwardHandler,
        securityHandler, partialFetchHandler, contentHandler, conditionalRequestHandler, contentHeadersHandler,
        lastDownloadedHandler, handlerContributor, contentDispositionHandler);
  }
}
