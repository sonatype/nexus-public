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
package org.sonatype.nexus.repository.p2.orient.internal.proxy

import javax.annotation.Nonnull
import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.p2.internal.AssetKind
import org.sonatype.nexus.repository.p2.internal.P2Format
import org.sonatype.nexus.repository.p2.internal.security.P2SecurityFacet
import org.sonatype.nexus.repository.p2.orient.P2Facet
import org.sonatype.nexus.repository.p2.orient.P2RestoreFacet
import org.sonatype.nexus.repository.p2.orient.internal.P2ComponentMaintenance
import org.sonatype.nexus.repository.proxy.ProxyHandler
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Matcher
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.Router.Builder
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.FormatHighAvailabilitySupportHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.p2.internal.AssetKind.*

/**
 * P2 proxy repository recipe.
 */
@Priority(Integer.MAX_VALUE)
@Named(OrientP2ProxyRecipe.NAME)
@Singleton
class OrientP2ProxyRecipe
    extends RecipeSupport
{
  public static final String NAME = 'p2-proxy'

  @Inject
  Provider<P2SecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<AttributesFacet> attributesFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  UnitOfWorkHandler unitOfWorkHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  Provider<P2ComponentMaintenance> componentMaintenanceFacet

  @Inject
  Provider<OrientP2ProxyCacheInvalidatorFacet> cacheInvalidatorFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<OrientP2ProxyFacet> proxyFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  NegativeCacheHandler negativeCacheHandler

  @Inject
  ProxyHandler proxyHandler

  @Inject
  RoutingRuleHandler routingRuleHandler

  @Inject
  FormatHighAvailabilitySupportHandler highAvailabilitySupportHandler;

  @Inject
  HighAvailabilitySupportChecker highAvailabilitySupportChecker

  @Inject
  Provider<P2RestoreFacet> p2RestoreFacet

  @Inject
  Provider<P2Facet> p2Facet

  @Inject
  OrientP2ProxyRecipe(@Named(ProxyType.NAME) final Type type,
                @Named(P2Format.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(componentMaintenanceFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(purgeUnusedFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(p2Facet.get())
    repository.attach(p2RestoreFacet.get())
    repository.attach(cacheInvalidatorFacet.get())
  }

  static Matcher buildTokenMatcherForPatternAndAssetKind(final String pattern,
                                                         final AssetKind assetKind,
                                                         final String... actions) {
    LogicMatchers.and(
        new ActionMatcher(actions),
        new TokenMatcher(pattern),
        new Matcher() {
          @Override
          boolean matches(final Context context) {
            context.attributes.set(AssetKind.class, assetKind)
            return true
          }
        }
    )
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Builder builder = new Builder()

    addBrowseUnsupportedRoute(builder)

    (createMatchers('/{site:[0-9a-f]{64\\}}') + createMatchers('')).each { matcher ->
      builder.route(new Route.Builder().matcher(matcher)
          .handler(timingHandler)
          .handler(securityHandler)
          .handler(highAvailabilitySupportHandler)
          .handler(routingRuleHandler)
          .handler(exceptionHandler)
          .handler(handlerContributor)
          .handler(negativeCacheHandler)
          .handler(conditionalRequestHandler)
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

  List<Matcher> createMatchers(String prefix) {
    String path = '{name:.*}_{version:\\\\d+\\\\.\\\\d+\\\\.\\\\d+(\\\\.[A-Za-z0-9_-]+)?}'
    return [
      buildTokenMatcherForPatternAndAssetKind(prefix + '/p2.index', P2_INDEX, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + '/compositeContent.jar', COMPOSITE_CONTENT, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + '/compositeContent.xml', COMPOSITE_CONTENT, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + '/content.jar', CONTENT_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + '/content.xml.xz', CONTENT_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + '/content.xml', CONTENT_METADATA, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + '/compositeArtifacts.jar', COMPOSITE_ARTIFACTS, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + '/compositeArtifacts.xml', COMPOSITE_ARTIFACTS, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + '/artifacts.jar', ARTIFACTS_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + '/artifacts.xml.xz', ARTIFACTS_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + '/artifacts.xml', ARTIFACTS_METADATA, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + '/{dir:features|plugins}/' + path + '.{extension:.*}',
        BUNDLE, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + '/{dir:binary}/' + path, BINARY_BUNDLE, GET, HEAD)
    ]
  }

  @Override
  boolean isFeatureEnabled() {
    return highAvailabilitySupportChecker.isSupported(getFormat().getValue());
  }
}
