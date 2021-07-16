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
package org.sonatype.nexus.repository.r.datastore.internal.hosted

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.r.RFormat
import org.sonatype.nexus.repository.r.datastore.RContentFacet
import org.sonatype.nexus.repository.r.datastore.internal.RRecipeSupport
import org.sonatype.nexus.repository.r.AssetKind
import org.sonatype.nexus.repository.r.internal.RCommonHandlers
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router.Builder
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

/**
 * R hosted repository recipe.
 *
 * @since 3.32
 */
@Singleton
@Named(RHostedRecipe.NAME)
class RHostedRecipe
    extends RRecipeSupport
{
  public static final String NAME = 'r-hosted'

  @Inject
  Provider<RContentFacet> contentFacet

  @Inject
  Provider<RHostedMetadataFacet> rHostedMetadataFacet

  @Inject
  RHostedHandlers hostedHandlers

  @Inject
  RCommonHandlers commonHandlers

  @Inject
  LastDownloadedHandler lastDownloadedHandler

  @Inject
  RHostedRecipe(@Named(HostedType.NAME) final Type type,
                @Named(RFormat.NAME) final Format format)
  {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(contentFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(maintenanceFacet.get())
    repository.attach(rHostedMetadataFacet.get())
    repository.attach(searchFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Builder builder = new Builder()

    // PACKAGES.gz is the only supported metadata in hosted for now
    builder.route(packagesGzMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.PACKAGES))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(hostedHandlers.getPackages)
        .create())

    builder.route(archiveMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.ARCHIVE))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(hostedHandlers.getArchive)
        .create())

    builder.route(uploadMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.ARCHIVE))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(hostedHandlers.putArchive)
        .create())

    builder.route(notSupportedMetadataMatcher()
        .handler(securityHandler)
        .handler(commonHandlers.notSupportedMetadataRequest)
        .create())

    addBrowseUnsupportedRoute(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }

  static Route.Builder packagesGzMatcher() {
    new Route.Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.GET, HttpMethods.HEAD),
            packagesGzTokenMatcher()
        ))
  }

  static Route.Builder notSupportedMetadataMatcher() {
    new Route.Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.GET, HttpMethods.HEAD),
            LogicMatchers.or(metadataRdsPathMatcher(), packagesTokenMatcher()),
            LogicMatchers.not(packagesGzTokenMatcher())
        ))
  }

  static TokenMatcher packagesGzTokenMatcher() {
    return new TokenMatcher('/{path:.+}/PACKAGES.gz')
  }
}
