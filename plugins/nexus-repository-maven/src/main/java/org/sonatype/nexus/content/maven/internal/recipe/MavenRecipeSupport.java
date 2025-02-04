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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.25
 */
public abstract class MavenRecipeSupport
    extends RecipeSupport
{
  protected Provider<ConfigurableViewFacet> viewFacet;

  protected ExceptionHandler exceptionHandler;

  protected TimingHandler timingHandler;

  protected RoutingRuleHandler routingHandler;

  protected SecurityHandler securityHandler;

  protected PartialFetchHandler partialFetchHandler;

  protected ConditionalRequestHandler conditionalRequestHandler;

  protected ContentHeadersHandler contentHeadersHandler;

  protected HandlerContributor handlerContributor;

  protected LastDownloadedHandler lastDownloadedHandler;

  protected VersionPolicyHandler versionPolicyHandler;

  protected Provider<MavenSecurityFacet> securityFacet;

  protected MavenPathParser mavenPathParser;

  protected MavenContentHandler mavenContentHandler;

  protected MavenArchetypeCatalogHandler archetypeCatalogHandler;

  protected MavenMetadataRebuildHandler mavenMetadataRebuildHandler;

  protected Provider<MavenContentFacet> mavenContentFacet;

  protected Provider<SearchFacet> searchFacet;

  protected Provider<BrowseFacet> browseFacet;

  protected Provider<MavenArchetypeCatalogFacet> mavenArchetypeCatalogFacet;

  protected Provider<MavenMetadataRebuildFacet> mavenMetadataRebuildFacet;

  protected Provider<MavenMaintenanceFacet> mavenMaintenanceFacet;

  protected Provider<RemoveSnapshotsFacet> removeSnapshotsFacet;

  protected ContentDispositionHandler contentDispositionHandler;

  protected MavenRecipeSupport(final Type type, final Format format) {
    super(type, format);
  }

  public Builder newArchetypeCatalogRouteBuilder() {
    return new Builder().matcher(new MavenArchetypeCatalogMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler);
  }

  /**
   * Only GET, HEAD actions allowed, as nothing publishes the binary index, only consumes.
   */
  public Builder newIndexRouteBuilder() {
    return new Builder().matcher(LogicMatchers.and(new MavenIndexMatcher(mavenPathParser),
        LogicMatchers.or(new ActionMatcher(HttpMethods.GET), new ActionMatcher(HttpMethods.HEAD))))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler);
  }

  public Builder newMavenPathRouteBuilder() {
    return new Builder().matcher(new MavenPathMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(contentDispositionHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler);
  }

  public Builder newMetadataRouteBuilder() {
    return new Builder().matcher(new MavenRepositoryMetadataMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler);
  }

  @Inject
  public void setArchetypeCatalogHandler(final MavenArchetypeCatalogHandler archetypeCatalogHandler) {
    this.archetypeCatalogHandler = checkNotNull(archetypeCatalogHandler);
  }

  @Inject
  public void setBrowseFacet(final Provider<BrowseFacet> browseFacet) {
    this.browseFacet = checkNotNull(browseFacet);
  }

  @Inject
  public void setConditionalRequestHandler(final ConditionalRequestHandler conditionalRequestHandler) {
    this.conditionalRequestHandler = checkNotNull(conditionalRequestHandler);
  }

  @Inject
  public void setContentDispositionHandler(final ContentDispositionHandler contentDispositionHandler) {
    this.contentDispositionHandler = checkNotNull(contentDispositionHandler);
  }

  @Inject
  public void setContentHeadersHandler(final ContentHeadersHandler contentHeadersHandler) {
    this.contentHeadersHandler = checkNotNull(contentHeadersHandler);
  }

  @Inject
  public void setExceptionHandler(final ExceptionHandler exceptionHandler) {
    this.exceptionHandler = checkNotNull(exceptionHandler);
  }

  @Inject
  public void setHandlerContributor(final HandlerContributor handlerContributor) {
    this.handlerContributor = checkNotNull(handlerContributor);
  }

  @Inject
  public void setLastDownloadedHandler(final LastDownloadedHandler lastDownloadedHandler) {
    this.lastDownloadedHandler = checkNotNull(lastDownloadedHandler);
  }

  @Inject
  public void setMavenArchetypeCatalogFacet(final Provider<MavenArchetypeCatalogFacet> mavenArchetypeCatalogFacet) {
    this.mavenArchetypeCatalogFacet = checkNotNull(mavenArchetypeCatalogFacet);
  }

  @Inject
  public void setMavenContentFacet(final Provider<MavenContentFacet> mavenContentFacet) {
    this.mavenContentFacet = checkNotNull(mavenContentFacet);
  }

  @Inject
  public void setMavenContentHandler(final MavenContentHandler mavenContentHandler) {
    this.mavenContentHandler = checkNotNull(mavenContentHandler);
  }

  @Inject
  public void setMavenMaintenanceFacet(final Provider<MavenMaintenanceFacet> mavenMaintenanceFacet) {
    this.mavenMaintenanceFacet = checkNotNull(mavenMaintenanceFacet);
  }

  @Inject
  public void setMavenMetadataRebuildFacet(final Provider<MavenMetadataRebuildFacet> mavenMetadataRebuildFacet) {
    this.mavenMetadataRebuildFacet = checkNotNull(mavenMetadataRebuildFacet);
  }

  @Inject
  public void setMavenMetadataRebuildHandler(final MavenMetadataRebuildHandler mavenMetadataRebuildHandler) {
    this.mavenMetadataRebuildHandler = checkNotNull(mavenMetadataRebuildHandler);
  }

  @Inject
  public void setMavenPathParser(final MavenPathParser mavenPathParser) {
    this.mavenPathParser = checkNotNull(mavenPathParser);
  }

  @Inject
  public void setPartialFetchHandler(final PartialFetchHandler partialFetchHandler) {
    this.partialFetchHandler = checkNotNull(partialFetchHandler);
  }

  @Inject
  public void setRemoveSnapshotsFacet(final Provider<RemoveSnapshotsFacet> removeSnapshotsFacet) {
    this.removeSnapshotsFacet = checkNotNull(removeSnapshotsFacet);
  }

  @Inject
  public void setRoutingHandler(final RoutingRuleHandler routingHandler) {
    this.routingHandler = checkNotNull(routingHandler);
  }

  @Inject
  public void setSearchFacet(final Provider<SearchFacet> searchFacet) {
    this.searchFacet = checkNotNull(searchFacet);
  }

  @Inject
  public void setSecurityFacet(final Provider<MavenSecurityFacet> securityFacet) {
    this.securityFacet = checkNotNull(securityFacet);
  }

  @Inject
  public void setSecurityHandler(final SecurityHandler securityHandler) {
    this.securityHandler = checkNotNull(securityHandler);
  }

  @Inject
  public void setTimingHandler(final TimingHandler timingHandler) {
    this.timingHandler = checkNotNull(timingHandler);
  }

  @Inject
  public void setVersionPolicyHandler(final VersionPolicyHandler versionPolicyHandler) {
    this.versionPolicyHandler = checkNotNull(versionPolicyHandler);
  }

  @Inject
  public void setViewFacet(final Provider<ConfigurableViewFacet> viewFacet) {
    this.viewFacet = checkNotNull(viewFacet);
  }
}
