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

import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchGroupHandler

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFacetGroup
import org.sonatype.nexus.repository.Facet
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router

import static org.sonatype.nexus.repository.http.HttpMethods.GET

/**
 * npm group repository recipe.
 *
 * @since 3.0
 */
@Named(NpmGroupRecipe.NAME)
@Singleton
class NpmGroupRecipe
    extends NpmRecipeSupport
{
  public static final String NAME = 'npm-group'

  @Inject
  Provider<NpmGroupFacet> groupFacet

  @Inject
  Provider<NpmSearchIndexFacetGroup> npmSearchIndexFacet

  @Inject
  NpmGroupPackageHandler packageHandler

  @Inject
  GroupHandler tarballHandler

  @Inject
  NpmSearchGroupHandler searchHandler

  @Inject
  NpmGroupRecipe(@Named(GroupType.NAME) final Type type,
                 @Named(NpmFormat.NAME) final Format format)
  {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(groupFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(securityFacet.get())
    repository.attach(tokenFacet.get())
    repository.attach(npmSearchIndexFacet.get())
    repository.attach(npmFacet.get())
    repository.attach(configure(viewFacet.get()))
  }

  Facet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // GET /-/all (npm search)
    builder.route(searchIndexMatcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.searchIndex)
        .create())

    // GET /-/v1/search (npm v1 search)
    builder.route(searchV1Matcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(unitOfWorkHandler)
        .handler(searchHandler)
        .create())

    // GET /packageName (npm install)
    builder.route(packageMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(packageHandler)
        .create())

    // GET /packageName/-/tarballName (npm install)
    builder.route(tarballMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(handlerContributor)
        .handler(tarballHandler)
        .create())

    createUserRoutes(builder)

    builder.defaultHandlers(HttpHandlers.badRequest())

    facet.configure(builder.create())

    return facet
  }


}
