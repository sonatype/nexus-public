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
package org.sonatype.nexus.repository.npm.internal

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFacetHosted
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacetHosted

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler

import static org.sonatype.nexus.repository.http.HttpMethods.DELETE
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.PUT

/**
 * npm hosted repository recipe.
 *
 * @since 3.0
 */
@Named(NpmHostedRecipe.NAME)
@Singleton
class NpmHostedRecipe
    extends NpmRecipeSupport
{
  public static final String NAME = 'npm-hosted'

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  Provider<NpmHostedFacet> npmHostedFacet

  @Inject
  Provider<NpmHostedComponentMaintenanceImpl> npmHostedComponentMaintenanceProvider

  @Inject
  Provider<NpmSearchIndexFacetHosted> npmSearchIndexFacet

  @Inject
  Provider<NpmSearchFacetHosted> npmSearchFacet

  @Inject
  NpmHostedRecipe(@Named(HostedType.NAME) final Type type,
                  @Named(NpmFormat.NAME) final Format format)
  {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(tokenFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(searchFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(npmFacet.get())
    repository.attach(npmHostedFacet.get())
    repository.attach(npmHostedComponentMaintenanceProvider.get())
    repository.attach(npmSearchIndexFacet.get())
    repository.attach(npmSearchFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // GET /-/all (npm search)
    builder.route(searchIndexMatcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.searchIndex)
        .create())

    // GET /-/v1/search (npm v1 search)
    builder.route(searchV1Matcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.searchV1)
        .create())

    // GET /packageName (npm install)
    builder.route(packageMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.getPackage)
        .create())

    // PUT /packageName (npm publish + npm deprecate)
    builder.route(packageMatcher(PUT)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.putPackage)
        .create())

    // PUT /packageName/-rev/revision (npm unpublish)
    builder.route(packageMatcherWithRevision(PUT)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.putPackage)
        .create())

    // DELETE /packageName (npm un-publish when last version deleted, npm 1.x)
    builder.route(packageMatcher(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.deletePackage)
        .create())

    // DELETE /packageName/-rev/revision (npm un-publish when last version deleted, newer npms)
    builder.route(packageMatcherWithRevision(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.deletePackage)
        .create())

    // GET /packageName/-/tarballName (npm install)
    builder.route(tarballMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.getTarball)
        .create())

    // DELETE /packageName/-/tarballName (npm un-publish when some versions are left in place)
    builder.route(tarballMatcher(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.deleteTarball)
        .create())

    // DELETE /packageName/-/tarballName/-rev/revision (npm un-publish when some versions are left in place)
    builder.route(tarballMatcherWithRevision(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.deleteTarball)
        .create())

    createUserRoutes(builder)

    builder.defaultHandlers(HttpHandlers.badRequest())

    facet.configure(builder.create())

    return facet
  }


}
