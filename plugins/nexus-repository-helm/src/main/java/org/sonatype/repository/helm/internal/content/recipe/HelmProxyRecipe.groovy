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
package org.sonatype.repository.helm.internal.content.recipe

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheHandler
import org.sonatype.nexus.repository.content.browse.BrowseFacet
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet
import org.sonatype.nexus.repository.content.search.SearchFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.proxy.ProxyHandler
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Matcher
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
import org.sonatype.repository.helm.internal.AssetKind
import org.sonatype.repository.helm.internal.HelmFormat
import org.sonatype.repository.helm.internal.content.HelmContentFacet
import org.sonatype.repository.helm.internal.security.HelmSecurityFacet

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.repository.helm.internal.AssetKind.HELM_INDEX
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE

/**
 * @since 3.next
 */
@Named(HelmProxyRecipe.NAME)
@Singleton
class HelmProxyRecipe
    extends RecipeSupport
{
  public static final String NAME = 'helm-proxy'

  @Inject
  Provider<HelmSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  Provider<HelmProxyFacet> proxyFacet

  @Inject
  Provider<HelmContentFacet> helmFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<BrowseFacet> browseFacet

  @Inject
  Provider<LastAssetMaintenanceFacet> maintenanceFacet

  @Inject
  TimingHandler timingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  ExceptionHandler exceptionHandler

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
  HelmProxyRecipe(@Named(ProxyType.NAME) final Type type,
                 @Named(HelmFormat.NAME) final Format format
  )
  {
    super(type, format)
  }

  /**
   * Matcher for index.yaml
   */
  static Matcher indexMatcher() {
    LogicMatchers.and(
        new ActionMatcher(GET, HEAD),
        new LiteralMatcher('/index.yaml'),
        new Matcher() {
          @Override
          boolean matches(final Context context) {
            context.attributes.set(AssetKind.class, HELM_INDEX)
            return true
          }
        }
    )
  }

  /**
   * Matcher for package mapping.
   */
  static Matcher packageMatcher() {
    LogicMatchers.and(
        new ActionMatcher(GET, HEAD),
        new TokenMatcher('/{filename:.+}'),
        new Matcher() {
          @Override
          boolean matches(final Context context) {
            context.attributes.set(AssetKind.class, HELM_PACKAGE)
            return true
          }
        }
    )
  }

  @Override
  void apply(final @Nonnull Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(maintenanceFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(helmFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(browseFacet.get())
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
          .handler(routingRuleHandler)
          .handler(exceptionHandler)
          .handler(handlerContributor)
          .handler(negativeCacheHandler)
          .handler(conditionalRequestHandler)
          .handler(partialFetchHandler)
          .handler(contentHeadersHandler)
          .handler(proxyHandler)
          .create())
    }

    builder.defaultHandlers(notFound())

    facet.configure(builder.create())

    return facet
  }
}
