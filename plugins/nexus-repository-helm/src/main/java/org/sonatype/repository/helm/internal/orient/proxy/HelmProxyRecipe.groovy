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
package org.sonatype.repository.helm.internal.orient.proxy

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
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.repository.helm.internal.HelmFormat
import org.sonatype.repository.helm.internal.HelmRecipeSupport

/**
 * Helm proxy repository recipe.
 *
 * @since 3.next
 */
@Named(HelmProxyRecipe.NAME)
@Singleton
class HelmProxyRecipe
  extends HelmRecipeSupport
{
  public static final String NAME = 'helm-proxy'

  @Inject
  Provider<HelmProxyFacetImpl> proxyFacet

  @Inject
  ProxyHandler proxyHandler

  @Inject
  RoutingRuleHandler routingRuleHandler

  @Inject
  NegativeCacheHandler negativeCacheHandler

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  HelmProxyRecipe(@Named(ProxyType.NAME) final Type type,
                  @Named(HelmFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(componentMaintenanceFacet.get())
    repository.attach(helmFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(helmRestoreFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(purgeUnusedFacet.get())
    repository.attach(attributesFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    [indexMatcher(), packageMatcher()].each { matcher ->
      builder.route(new Route.Builder().matcher(matcher)
          .handler(timingHandler)
          .handler(securityHandler)
          .handler(formatHighAvailabilitySupportHandler)
          .handler(routingRuleHandler)
          .handler(exceptionHandler)
          .handler(handlerContributor)
          .handler(negativeCacheHandler)
          .handler(partialFetchHandler)
          .handler(contentHeadersHandler)
          .handler(unitOfWorkHandler)
          .handler(proxyHandler)
          .create())
    }

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
