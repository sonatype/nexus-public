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
package org.sonatype.nexus.repository.r.datastore.internal.proxy

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.proxy.ProxyHandler
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.r.RFormat
import org.sonatype.nexus.repository.r.datastore.internal.RRecipeSupport
import org.sonatype.nexus.repository.r.AssetKind
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router.Builder
import org.sonatype.nexus.repository.view.ViewFacet

/**
 * R proxy repository recipe.
 *
 * @since 3.32
 */
@Named(RProxyRecipe.NAME)
@Singleton
class RProxyRecipe
    extends RRecipeSupport
{
  public static final String NAME = 'r-proxy'

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  Provider<RProxyFacet> proxyFacet;

  @Inject
  NegativeCacheHandler negativeCacheHandler

  @Inject
  ProxyHandler proxyHandler

  @Inject
  RProxyRecipe(@Named(ProxyType.NAME) final Type type, @Named(RFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(contentFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(purgeUnusedFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(maintenanceFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Builder builder = new Builder()

    builder.route(packagesMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.PACKAGES))
        .handler(securityHandler)
        .handler(routingRuleHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(proxyHandler)
        .create())

    builder.route(metadataRdsMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.RDS_METADATA))
        .handler(securityHandler)
        .handler(routingRuleHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(proxyHandler)
        .create())

    builder.route(archiveMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.ARCHIVE))
        .handler(securityHandler)
        .handler(routingRuleHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(negativeCacheHandler)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(proxyHandler)
        .create())

    addBrowseUnsupportedRoute(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
