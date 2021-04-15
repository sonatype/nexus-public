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
package org.sonatype.nexus.content.raw.internal.recipe

import javax.annotation.Nonnull
import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheHandler
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.proxy.ProxyHandler
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.raw.internal.RawFormat
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.SuffixMatcher
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound
import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and

/**
 * Raw proxy repository recipe.
 *
 * @since 3.24
 */
@Named(RawProxyRecipe.NAME)
@Priority(Integer.MAX_VALUE)
@Singleton
class RawProxyRecipe
    extends RawRecipeSupport
{
  public static final String NAME = 'raw-proxy'
  public static final String PATH_NAME = 'path'

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  Provider<RawProxyFacet> proxyFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  NegativeCacheHandler negativeCacheHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ProxyHandler proxyHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  RoutingRuleHandler routingRuleHandler

  @Inject
  RawProxyRecipe(@Named(ProxyType.NAME) final Type type,
                 @Named(RawFormat.NAME) final Format format
  )
  {
    super(type, format)
  }

  @Override
  void apply(final @Nonnull Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(contentFacet.get())
    repository.attach(maintenanceFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(purgeUnusedFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    // Additional handlers, such as the lastDownloadHandler, are intentionally
    // not included on this route because this route forwards to the route below.
    // This route specifically handles GET / and forwards to /index.html.
    builder.route(new Route.Builder()
        .matcher(and(new ActionMatcher(HttpMethods.GET), new SuffixMatcher('/')))
        .handler(timingHandler)
        .handler(indexHtmlForwardHandler)
        .create()
    )

    builder.route(new Route.Builder()
        .matcher(new TokenMatcher('{' + PATH_NAME + ':/.+}'))
        .handler(timingHandler)
        .handler(contentDispositionHandler)
        .handler(securityHandler)
        .handler(routingRuleHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(negativeCacheHandler)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create())

    builder.defaultHandlers(notFound())

    facet.configure(builder.create())

    return facet
  }
}
