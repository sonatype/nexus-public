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
package org.sonatype.nexus.repository.r.orient.internal.hosted

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.r.AssetKind
import org.sonatype.nexus.repository.r.orient.OrientRHostedFacet
import org.sonatype.nexus.repository.r.RPackagesBuilderFacet
import org.sonatype.nexus.repository.r.internal.RCommonHandlers
import org.sonatype.nexus.repository.r.RFormat
import org.sonatype.nexus.repository.r.orient.internal.OrientRRecipeSupport
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router.Builder
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

/**
 * R proxy repository recipe.
 *
 * @since 3.28
 */
@Named(OrientRHostedRecipe.NAME)
@Singleton
class OrientRHostedRecipe
    extends OrientRRecipeSupport
{
  public static final String NAME = 'r-hosted'

  @Inject
  Provider<OrientRHostedFacet> hostedFacet

  @Inject
  Provider<RPackagesBuilderFacet> packagesBuilderFacet

  @Inject
  OrientHostedHandlers hostedHandlers

  @Inject
  RCommonHandlers commonHandlers

  @Inject
  OrientRHostedRecipe(@Named(HostedType.NAME) final Type type, @Named(RFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(storageFacet.get())
    repository.attach(httpClientFacet.get())
    repository.attach(componentMaintenanceFacet.get())
    repository.attach(hostedFacet.get())
    repository.attach(packagesBuilderFacet.get())
    repository.attach(rFacet.get())
    repository.attach(rRestoreFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(attributesFacet.get())
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
        .handler(highAvailabilitySupportHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(hostedHandlers.getPackages)
        .create())

    builder.route(archiveMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.ARCHIVE))
        .handler(securityHandler)
        .handler(highAvailabilitySupportHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(hostedHandlers.getArchive)
        .create())

    builder.route(uploadMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.ARCHIVE))
        .handler(securityHandler)
        .handler(highAvailabilitySupportHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
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
        org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and(
            new ActionMatcher(org.sonatype.nexus.repository.http.HttpMethods.GET, org.sonatype.nexus.repository.http.
                HttpMethods.HEAD),
            packagesGzTokenMatcher()
        ))
  }

  static Route.Builder notSupportedMetadataMatcher() {
    new Route.Builder().matcher(
        org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and(
            new ActionMatcher(org.sonatype.nexus.repository.http.HttpMethods.GET, org.sonatype.nexus.repository.http.
                HttpMethods.HEAD),
            org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.
                or(metadataRdsPathMatcher(), packagesTokenMatcher()),
            org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.not(packagesGzTokenMatcher())
        ))
  }

  static TokenMatcher packagesGzTokenMatcher() {
    return new TokenMatcher('/{path:.+}/PACKAGES.gz')
  }
}
