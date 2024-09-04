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

import javax.inject.Inject;
import javax.inject.Provider;

import org.sonatype.nexus.content.maven.MavenArchetypeCatalogFacet;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.maven.ContentDispositionHandler;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.MavenSecurityFacet;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyHandler;
import org.sonatype.nexus.repository.maven.internal.matcher.MavenArchetypeCatalogMatcher;
import org.sonatype.nexus.repository.maven.internal.matcher.MavenIndexMatcher;
import org.sonatype.nexus.repository.maven.internal.matcher.MavenPathMatcher;
import org.sonatype.nexus.repository.maven.internal.matcher.MavenRepositoryMetadataMatcher;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route.Builder;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers;

/**
 * @since 3.25
 */
public abstract class MavenRecipeSupport
    extends RecipeSupport
{
  @Inject
  private Provider<ConfigurableViewFacet> viewFacet;

  @Inject
  private ExceptionHandler exceptionHandler;

  @Inject
  private TimingHandler timingHandler;

  @Inject
  private RoutingRuleHandler routingHandler;

  @Inject
  private SecurityHandler securityHandler;

  @Inject
  private PartialFetchHandler partialFetchHandler;

  @Inject
  private ConditionalRequestHandler conditionalRequestHandler;

  @Inject
  private ContentHeadersHandler contentHeadersHandler;

  @Inject
  private HandlerContributor handlerContributor;

  @Inject
  private LastDownloadedHandler lastDownloadedHandler;

  @Inject
  private VersionPolicyHandler versionPolicyHandler;

  @Inject
  private Provider<MavenSecurityFacet> securityFacet;

  @Inject
  private MavenPathParser mavenPathParser;

  @Inject
  private MavenContentHandler mavenContentHandler;

  @Inject
  private MavenArchetypeCatalogHandler archetypeCatalogHandler;

  @Inject
  private MavenMetadataRebuildHandler mavenMetadataRebuildHandler;

  @Inject
  private Provider<MavenContentFacet> mavenContentFacet;

  @Inject
  private Provider<SearchFacet> searchFacet;

  @Inject
  private Provider<BrowseFacet> browseFacet;

  @Inject
  private Provider<MavenArchetypeCatalogFacet> mavenArchetypeCatalogFacet;

  @Inject
  private Provider<MavenMetadataRebuildFacet> mavenMetadataRebuildFacet;

  @Inject
  private Provider<MavenMaintenanceFacet> mavenMaintenanceFacet;

  @Inject
  private Provider<RemoveSnapshotsFacet> removeSnapshotsFacet;

  @Inject
  private ContentDispositionHandler contentDispositionHandler;

  protected MavenRecipeSupport(final Type type, final Format format) {
    super(type, format);
  }

  public Builder newMavenPathRouteBuilder() {
    return new Builder().matcher(new MavenPathMatcher(mavenPathParser)).handler(timingHandler)
        .handler(contentDispositionHandler).handler(securityHandler).handler(routingHandler).handler(exceptionHandler)
        .handler(handlerContributor).handler(conditionalRequestHandler);
  }

  public Builder newMetadataRouteBuilder() {
    return new Builder().matcher(new MavenRepositoryMetadataMatcher(mavenPathParser)).handler(timingHandler)
        .handler(securityHandler).handler(routingHandler).handler(exceptionHandler).handler(conditionalRequestHandler);
  }

  /**
   * Only GET, HEAD actions allowed, as nothing publishes the binary index, only consumes.
   */
  public Builder newIndexRouteBuilder() {
    return new Builder().matcher(LogicMatchers.and(new MavenIndexMatcher(mavenPathParser),
            LogicMatchers.or(new ActionMatcher(HttpMethods.GET), new ActionMatcher(HttpMethods.HEAD))))
        .handler(timingHandler).handler(securityHandler).handler(routingHandler).handler(exceptionHandler)
        .handler(conditionalRequestHandler);
  }

  public Builder newArchetypeCatalogRouteBuilder() {
    return new Builder().matcher(new MavenArchetypeCatalogMatcher(mavenPathParser)).handler(timingHandler)
        .handler(securityHandler).handler(routingHandler).handler(exceptionHandler).handler(conditionalRequestHandler);
  }

  public Provider<ConfigurableViewFacet> getViewFacet() {
    return viewFacet;
  }

  public void setViewFacet(Provider<ConfigurableViewFacet> viewFacet) {
    this.viewFacet = viewFacet;
  }

  public ExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public void setExceptionHandler(ExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  public TimingHandler getTimingHandler() {
    return timingHandler;
  }

  public void setTimingHandler(TimingHandler timingHandler) {
    this.timingHandler = timingHandler;
  }

  public RoutingRuleHandler getRoutingHandler() {
    return routingHandler;
  }

  public void setRoutingHandler(RoutingRuleHandler routingHandler) {
    this.routingHandler = routingHandler;
  }

  public SecurityHandler getSecurityHandler() {
    return securityHandler;
  }

  public void setSecurityHandler(SecurityHandler securityHandler) {
    this.securityHandler = securityHandler;
  }

  public PartialFetchHandler getPartialFetchHandler() {
    return partialFetchHandler;
  }

  public void setPartialFetchHandler(PartialFetchHandler partialFetchHandler) {
    this.partialFetchHandler = partialFetchHandler;
  }

  public ConditionalRequestHandler getConditionalRequestHandler() {
    return conditionalRequestHandler;
  }

  public void setConditionalRequestHandler(ConditionalRequestHandler conditionalRequestHandler) {
    this.conditionalRequestHandler = conditionalRequestHandler;
  }

  public ContentHeadersHandler getContentHeadersHandler() {
    return contentHeadersHandler;
  }

  public void setContentHeadersHandler(ContentHeadersHandler contentHeadersHandler) {
    this.contentHeadersHandler = contentHeadersHandler;
  }

  public HandlerContributor getHandlerContributor() {
    return handlerContributor;
  }

  public void setHandlerContributor(HandlerContributor handlerContributor) {
    this.handlerContributor = handlerContributor;
  }

  public LastDownloadedHandler getLastDownloadedHandler() {
    return lastDownloadedHandler;
  }

  public void setLastDownloadedHandler(LastDownloadedHandler lastDownloadedHandler) {
    this.lastDownloadedHandler = lastDownloadedHandler;
  }

  public VersionPolicyHandler getVersionPolicyHandler() {
    return versionPolicyHandler;
  }

  public void setVersionPolicyHandler(VersionPolicyHandler versionPolicyHandler) {
    this.versionPolicyHandler = versionPolicyHandler;
  }

  public Provider<MavenSecurityFacet> getSecurityFacet() {
    return securityFacet;
  }

  public void setSecurityFacet(Provider<MavenSecurityFacet> securityFacet) {
    this.securityFacet = securityFacet;
  }

  public MavenPathParser getMavenPathParser() {
    return mavenPathParser;
  }

  public void setMavenPathParser(MavenPathParser mavenPathParser) {
    this.mavenPathParser = mavenPathParser;
  }

  public MavenContentHandler getMavenContentHandler() {
    return mavenContentHandler;
  }

  public void setMavenContentHandler(MavenContentHandler mavenContentHandler) {
    this.mavenContentHandler = mavenContentHandler;
  }

  public MavenArchetypeCatalogHandler getArchetypeCatalogHandler() {
    return archetypeCatalogHandler;
  }

  public void setArchetypeCatalogHandler(MavenArchetypeCatalogHandler archetypeCatalogHandler) {
    this.archetypeCatalogHandler = archetypeCatalogHandler;
  }

  public MavenMetadataRebuildHandler getMavenMetadataRebuildHandler() {
    return mavenMetadataRebuildHandler;
  }

  public void setMavenMetadataRebuildHandler(MavenMetadataRebuildHandler mavenMetadataRebuildHandler) {
    this.mavenMetadataRebuildHandler = mavenMetadataRebuildHandler;
  }

  public Provider<MavenContentFacet> getMavenContentFacet() {
    return mavenContentFacet;
  }

  public void setMavenContentFacet(Provider<MavenContentFacet> mavenContentFacet) {
    this.mavenContentFacet = mavenContentFacet;
  }

  public Provider<SearchFacet> getSearchFacet() {
    return searchFacet;
  }

  public void setSearchFacet(Provider<SearchFacet> searchFacet) {
    this.searchFacet = searchFacet;
  }

  public Provider<BrowseFacet> getBrowseFacet() {
    return browseFacet;
  }

  public void setBrowseFacet(Provider<BrowseFacet> browseFacet) {
    this.browseFacet = browseFacet;
  }

  public Provider<MavenArchetypeCatalogFacet> getMavenArchetypeCatalogFacet() {
    return mavenArchetypeCatalogFacet;
  }

  public void setMavenArchetypeCatalogFacet(Provider<MavenArchetypeCatalogFacet> mavenArchetypeCatalogFacet) {
    this.mavenArchetypeCatalogFacet = mavenArchetypeCatalogFacet;
  }

  public Provider<MavenMetadataRebuildFacet> getMavenMetadataRebuildFacet() {
    return mavenMetadataRebuildFacet;
  }

  public void setMavenMetadataRebuildFacet(Provider<MavenMetadataRebuildFacet> mavenMetadataRebuildFacet) {
    this.mavenMetadataRebuildFacet = mavenMetadataRebuildFacet;
  }

  public Provider<MavenMaintenanceFacet> getMavenMaintenanceFacet() {
    return mavenMaintenanceFacet;
  }

  public void setMavenMaintenanceFacet(Provider<MavenMaintenanceFacet> mavenMaintenanceFacet) {
    this.mavenMaintenanceFacet = mavenMaintenanceFacet;
  }

  public Provider<RemoveSnapshotsFacet> getRemoveSnapshotsFacet() {
    return removeSnapshotsFacet;
  }

  public void setRemoveSnapshotsFacet(Provider<RemoveSnapshotsFacet> removeSnapshotsFacet) {
    this.removeSnapshotsFacet = removeSnapshotsFacet;
  }

  public ContentDispositionHandler getContentDispositionHandler() {
    return contentDispositionHandler;
  }

  public void setContentDispositionHandler(ContentDispositionHandler contentDispositionHandler) {
    this.contentDispositionHandler = contentDispositionHandler;
  }
}
