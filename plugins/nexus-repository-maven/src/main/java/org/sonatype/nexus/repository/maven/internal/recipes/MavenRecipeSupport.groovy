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
package org.sonatype.nexus.repository.maven.internal.recipes

import org.sonatype.nexus.repository.attributes.AttributesFacet

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.maven.MavenPathParser
import org.sonatype.nexus.repository.maven.internal.MavenFacetImpl
import org.sonatype.nexus.repository.maven.internal.matcher.MavenArchetypeCatalogMatcher
import org.sonatype.nexus.repository.maven.internal.matcher.MavenIndexMatcher
import org.sonatype.nexus.repository.maven.internal.matcher.MavenPathMatcher
import org.sonatype.nexus.repository.maven.internal.matcher.MavenRepositoryMetadataMatcher
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.security.SecurityFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers

/**
 * Maven recipe support.
 *
 * @since 3.0
 */
abstract class MavenRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  Provider<AttributesFacet> attributesFacet

  @Inject
  Provider<MavenFacetImpl> mavenFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  RoutingRuleHandler routingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  UnitOfWorkHandler unitOfWorkHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  HandlerContributor handlerContributor
  
  @Inject
  LastDownloadedHandler lastDownloadedHandler

  final MavenPathParser mavenPathParser

  final Provider<SecurityFacet> securityFacet

  MavenRecipeSupport(Type type, Format format, MavenPathParser mavenPathParser, Provider<SecurityFacet> securityFacet) {
    super(type, format)
    this.mavenPathParser = mavenPathParser
    this.securityFacet = securityFacet
  }

  Builder newMavenPathRouteBuilder() {
    return new Builder()
        .matcher(new MavenPathMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
  }

  Builder newMetadataRouteBuilder() {
    return new Builder()
        .matcher(new MavenRepositoryMetadataMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler)
  }

  /**
   * Only GET, HEAD actions allowed, as nothing publishes the binary index, only consumes.
   */
  Builder newIndexRouteBuilder() {
    return new Builder()
        .matcher(
            LogicMatchers.and(
                new MavenIndexMatcher(mavenPathParser),
                LogicMatchers.or(
                    new ActionMatcher(HttpMethods.GET),
                    new ActionMatcher(HttpMethods.HEAD)
                )
            )
        )
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler)
  }

  Builder newArchetypeCatalogRouteBuilder() {
    return new Builder()
        .matcher(new MavenArchetypeCatalogMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler)
  }
}
