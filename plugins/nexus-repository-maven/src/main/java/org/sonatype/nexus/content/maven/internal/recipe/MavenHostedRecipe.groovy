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
package org.sonatype.nexus.content.maven.internal.recipe

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.maven.internal.Maven2Format
import org.sonatype.nexus.repository.maven.internal.hosted.MavenHostedIndexFacet
import org.sonatype.nexus.repository.maven.internal.matcher.MavenArchetypeCatalogMatcher
import org.sonatype.nexus.repository.maven.internal.matcher.MavenIndexMatcher
import org.sonatype.nexus.repository.maven.internal.matcher.MavenPathMatcher
import org.sonatype.nexus.repository.maven.internal.matcher.MavenRepositoryMetadataMatcher
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2HostedRecipe
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound

/**
 * @since 3.25
 */
@Named(Maven2HostedRecipe.NAME)
@Singleton
class MavenHostedRecipe
    extends MavenRecipeSupport
    implements Maven2HostedRecipe
{
  @Inject
  Provider<MavenHostedIndexFacet> mavenIndexFacet

  @Inject
  MavenHostedRecipe(@Named(HostedType.NAME) final Type type, @Named(Maven2Format.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(mavenMetadataRebuildFacet.get())
    repository.attach(mavenContentFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(mavenArchetypeCatalogFacet.get())
    repository.attach(mavenIndexFacet.get())
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // Note: partialFetchHandler NOT added for Maven metadata
    builder.route(newMetadataRouteBuilder()
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(mavenMetadataRebuildHandler)
        .handler(mavenContentHandler)
        .create())

    builder.route(newIndexRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(mavenContentHandler)
        .create())

    builder.route(newArchetypeCatalogRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(archetypeCatalogHandler)
        .handler(mavenContentHandler)
        .create())

    builder.route(newMavenPathRouteBuilder()
        .handler(partialFetchHandler)
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(mavenContentHandler)
        .create())

    builder.defaultHandlers(notFound())

    facet.configure(builder.create())

    return facet
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
