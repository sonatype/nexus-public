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
package org.sonatype.nexus.content.maven.internal.recipe;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.maven.MavenArchetypeCatalogFacet;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.maven.ContentDispositionHandler;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.MavenSecurityFacet;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyHandler;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Matcher;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.handlers.BrowseUnsupportedHandler;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import com.google.common.collect.ImmutableList;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class MavenRecipeTestSupport
    extends TestSupport
{
  @Mock
  private SecurityHandler securityHandler;

  @Mock
  private VersionPolicyHandler versionPolicyHandler;

  @Mock
  private LastDownloadedHandler lastDownloadedHandler;

  @Mock
  private BrowseUnsupportedHandler browseUnsupportedHandler;

  @Mock
  private ContentHeadersHandler contentHeadersHandler;

  @Mock
  private PartialFetchHandler partialFetchHandler;

  @Mock
  private TimingHandler timingHandler;

  @Mock
  private MavenPathParser mavenPathParser;

  @Mock
  private RoutingRuleHandler routingRuleHandler;

  @Mock
  private ExceptionHandler exceptionHandler;

  @Mock
  private ConditionalRequestHandler conditionalRequestHandler;

  @Mock
  private MavenMetadataRebuildHandler mavenMetadataRebuildHandler;

  @Mock
  private MavenContentHandler mavenContentHandler;

  @Mock
  private MavenArchetypeCatalogHandler archetypeCatalogHandler;

  @Mock
  private ContentDispositionHandler contentDispositionHandler;

  @Mock
  private HandlerContributor handlerContributor;

  @Mock
  protected ConfigurableViewFacet viewFacet;

  private final Provider<ConfigurableViewFacet> viewFacetProvider = () -> viewFacet;

  @Mock
  protected BrowseFacet browseFacet;

  private final Provider<BrowseFacet> browseFacetProvider = () -> browseFacet;

  @Mock
  protected MavenSecurityFacet securityFacet;

  private final Provider<MavenSecurityFacet> securityFacetProvider = () -> securityFacet;

  @Mock
  protected MavenContentFacet mavenContentFacet;

  private final Provider<MavenContentFacet> mavenContentFacetProvider = () -> mavenContentFacet;

  @Mock
  protected MavenMaintenanceFacet mavenMaintenanceFacet;

  private final Provider<MavenMaintenanceFacet> mavenMaintenanceFacetProvider = () -> mavenMaintenanceFacet;

  @Mock
  protected SearchFacet searchFacet;

  private final Provider<SearchFacet> searchFacetProvider = () -> searchFacet;

  @Mock
  protected RemoveSnapshotsFacet removeSnapshotsFacet;

  private final Provider<RemoveSnapshotsFacet> removeSnapshotsFacetProvider = () -> removeSnapshotsFacet;

  @Mock
  protected MavenMetadataRebuildFacet mavenMetadataRebuildFacet;

  private final Provider<MavenMetadataRebuildFacet> mavenMetadataRebuildFacetProvider = () -> mavenMetadataRebuildFacet;

  @Mock
  protected MavenArchetypeCatalogFacet mavenArchetypeCatalogFacet;

  private final Provider<MavenArchetypeCatalogFacet> mavenArchetypeCatalogFacetProvider =
      () -> mavenArchetypeCatalogFacet;

  protected <T extends MavenRecipeSupport> void mockHandlers(T underTest) {
    underTest.setBrowseUnsupportedHandler(browseUnsupportedHandler);
    when(browseUnsupportedHandler.getRoute()).thenReturn(
        new Route(mock(Matcher.class), ImmutableList.of(securityHandler, browseUnsupportedHandler)));
    underTest.setContentHeadersHandler(contentHeadersHandler);
    underTest.setPartialFetchHandler(partialFetchHandler);
    underTest.setTimingHandler(timingHandler);
    underTest.setMavenPathParser(mavenPathParser);
    underTest.setSecurityHandler(securityHandler);
    underTest.setRoutingHandler(routingRuleHandler);
    underTest.setExceptionHandler(exceptionHandler);
    underTest.setConditionalRequestHandler(conditionalRequestHandler);
    underTest.setHandlerContributor(handlerContributor);
    underTest.setContentDispositionHandler(contentDispositionHandler);
    underTest.setVersionPolicyHandler(versionPolicyHandler);
    underTest.setLastDownloadedHandler(lastDownloadedHandler);
    underTest.setMavenMetadataRebuildHandler(mavenMetadataRebuildHandler);
    underTest.setArchetypeCatalogHandler(archetypeCatalogHandler);
    underTest.setMavenContentHandler(mavenContentHandler);
  }

  protected <T extends MavenRecipeSupport> void mockFacets(T underTest) {
    underTest.setSecurityFacet(securityFacetProvider);
    underTest.setMavenContentFacet(mavenContentFacetProvider);
    underTest.setBrowseFacet(browseFacetProvider);
    underTest.setViewFacet(viewFacetProvider);
    underTest.setMavenMaintenanceFacet(mavenMaintenanceFacetProvider);
    underTest.setSearchFacet(searchFacetProvider);
    underTest.setRemoveSnapshotsFacet(removeSnapshotsFacetProvider);
    underTest.setMavenMetadataRebuildFacet(mavenMetadataRebuildFacetProvider);
    underTest.setMavenArchetypeCatalogFacet(mavenArchetypeCatalogFacetProvider);
  }
}
