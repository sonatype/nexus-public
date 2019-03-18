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

import org.sonatype.nexus.repository.npm.internal.NpmProxyFacetImpl.ProxyTarget
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFacetProxy

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacetProxy
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler

import static org.sonatype.nexus.repository.http.HttpMethods.GET

/**
 * npm proxy repository recipe.
 *
 * @since 3.0
 */
@Named(NpmProxyRecipe.NAME)
@Singleton
class NpmProxyRecipe
    extends NpmRecipeSupport
{
  public static final String NAME = 'npm-proxy'

  @Inject
  Provider<NpmProxyFacetImpl> proxyFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  Provider<NpmSearchIndexFacetProxy> npmSearchIndexFacet

  @Inject
  Provider<NpmSearchFacetProxy> npmSearchFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  Provider<SingleAssetComponentMaintenance> singleAssetComponentMaintenanceProvider

  @Inject
  NpmNegativeCacheHandler negativeCacheHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  NpmProxyHandler proxyHandler

  @Inject
  NpmProxyRecipe(@Named(ProxyType.NAME) final Type type,
                 @Named(NpmFormat.NAME) final Format format)
  {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(tokenFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(npmFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(npmSearchIndexFacet.get())
    repository.attach(npmSearchFacet.get())
    repository.attach(singleAssetComponentMaintenanceProvider.get())
    repository.attach(purgeUnusedFacet.get())
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
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.SEARCH_INDEX))
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.searchIndex)
        .create())

    // GET /-/v1/search (npm v1 search)
    builder.route(searchV1Matcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.SEARCH_V1_RESULTS))
        .handler(unitOfWorkHandler)
        .handler(NpmHandlers.searchV1)
        .create())

    // GET /packageName (npm install)
    builder.route(packageMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.PACKAGE))
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create())

    // GET /packageName/-/tarballName (npm install)
    builder.route(tarballMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.TARBALL))
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create())

    createUserRoutes(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }

  Closure proxyTargetHandler = {
    Context context, ProxyTarget value ->
      context.attributes.set(ProxyTarget, value)
      return context.proceed()
  }
}
