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
package org.sonatype.nexus.repository.p2.datastore.internal.proxy;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.HttpHandlers;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.p2.datastore.P2ContentFacet;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.internal.security.P2SecurityFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Matcher;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router.Builder;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.ARTIFACTS_METADATA;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.BINARY_BUNDLE;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.BUNDLE;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.COMPOSITE_ARTIFACTS;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.COMPOSITE_CONTENT;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.CONTENT_METADATA;
import static org.sonatype.nexus.repository.p2.internal.AssetKind.P2_INDEX;

/**
 * @since 3.next
 */
@Named(P2ProxyRecipe.NAME)
@Singleton
public class P2ProxyRecipe
    extends RecipeSupport
{
  public static final String NAME = "p2-proxy";

  private final Provider<ConfigurableViewFacet> viewFacet;

  private final Provider<HttpClientFacet> httpClientFacet;

  private final Provider<NegativeCacheFacet> negativeCacheFacet;

  private final Provider<P2ContentFacet> contentFacet;

  private final Provider<P2ProxyCacheInvalidatorFacet> cacheInvalidatorFacet;

  private final Provider<P2ProxyFacet> proxyFacet;

  private final Provider<P2SecurityFacet> securityFacet;

  private final Provider<PurgeUnusedFacet> purgeUnusedFacet;

  private final Provider<SearchFacet> searchFacet;

  private final Provider<LastAssetMaintenanceFacet> maintenanceFacet;

  private final Provider<BrowseFacet> browseFacet;

  private final ConditionalRequestHandler conditionalRequestHandler;

  private final ContentHeadersHandler contentHeadersHandler;

  private final ExceptionHandler exceptionHandler;

  private final HandlerContributor handlerContributor;

  private final NegativeCacheHandler negativeCacheHandler;

  private final PartialFetchHandler partialFetchHandler;

  private final ProxyHandler proxyHandler;

  private final RoutingRuleHandler routingRuleHandler;

  private final SecurityHandler securityHandler;

  private final TimingHandler timingHandler;

  @Inject
  public P2ProxyRecipe(
      @Named(ProxyType.NAME) final Type type,
      @Named(P2Format.NAME) final Format format,
      final Provider<ConfigurableViewFacet> viewFacet,
      final Provider<HttpClientFacet> httpClientFacet,
      final Provider<NegativeCacheFacet> negativeCacheFacet,
      final Provider<P2ContentFacet> contentFacet,
      final Provider<P2ProxyCacheInvalidatorFacet> cacheInvalidatorFacet,
      final Provider<P2ProxyFacet> proxyFacet,
      final Provider<P2SecurityFacet> securityFacet,
      final Provider<PurgeUnusedFacet> purgeUnusedFacet,
      final Provider<SearchFacet> searchFacet,
      final Provider<LastAssetMaintenanceFacet> maintenanceFacet,
      final Provider<BrowseFacet> browseFacet,
      final ConditionalRequestHandler conditionalRequestHandler,
      final ContentHeadersHandler contentHeadersHandler,
      final ExceptionHandler exceptionHandler,
      final HandlerContributor handlerContributor,
      final NegativeCacheHandler negativeCacheHandler,
      final PartialFetchHandler partialFetchHandler,
      final ProxyHandler proxyHandler,
      final RoutingRuleHandler routingRuleHandler,
      final SecurityHandler securityHandler,
      final TimingHandler timingHandler)
  {
    super(type, format);
    this.viewFacet = checkNotNull(viewFacet);
    this.httpClientFacet = checkNotNull(httpClientFacet);
    this.negativeCacheFacet = checkNotNull(negativeCacheFacet);
    this.contentFacet = checkNotNull(contentFacet);
    this.cacheInvalidatorFacet = checkNotNull(cacheInvalidatorFacet);
    this.proxyFacet = checkNotNull(proxyFacet);
    this.securityFacet = checkNotNull(securityFacet);
    this.purgeUnusedFacet = checkNotNull(purgeUnusedFacet);
    this.searchFacet = checkNotNull(searchFacet);
    this.maintenanceFacet = checkNotNull(maintenanceFacet);
    this.browseFacet = checkNotNull(browseFacet);

    this.conditionalRequestHandler = checkNotNull(conditionalRequestHandler);
    this.contentHeadersHandler = checkNotNull(contentHeadersHandler);
    this.exceptionHandler = checkNotNull(exceptionHandler);
    this.handlerContributor = checkNotNull(handlerContributor);
    this.negativeCacheHandler = checkNotNull(negativeCacheHandler);
    this.partialFetchHandler = checkNotNull(partialFetchHandler);
    this.proxyHandler = checkNotNull(proxyHandler);
    this.routingRuleHandler = checkNotNull(routingRuleHandler);
    this.securityHandler = checkNotNull(securityHandler);
    this.timingHandler = checkNotNull(timingHandler);
  }

  @Override
  public void apply(final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(httpClientFacet.get());
    repository.attach(negativeCacheFacet.get());
    repository.attach(maintenanceFacet.get());
    repository.attach(proxyFacet.get());
    repository.attach(contentFacet.get());
    repository.attach(searchFacet.get());
    repository.attach(purgeUnusedFacet.get());
    repository.attach(cacheInvalidatorFacet.get());
    repository.attach(browseFacet.get());
}

  static Matcher buildTokenMatcherForPatternAndAssetKind(
      final String pattern,
      final AssetKind assetKind,
      final String... actions)
  {
    return LogicMatchers.and(
        new ActionMatcher(actions),
        new TokenMatcher(pattern),
        context -> {
          context.getAttributes().set(AssetKind.class, assetKind);
          return true;
        }
    );
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Builder builder = new Builder();

    addBrowseUnsupportedRoute(builder);

    createMatchers("/{site:[0-9a-f]{64\\}}").forEach(createRoute(builder));
    createMatchers("").forEach(createRoute(builder));

    builder.defaultHandlers(HttpHandlers.notFound());

    facet.configure(builder.create());

    return facet;
  }

  private Consumer<Matcher> createRoute(final Builder builder) {
    return matcher -> builder.route(new Route.Builder().matcher(matcher)
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
        .create());
  }

  private static List<Matcher> createMatchers(final String prefix) {
    String path = "{name:.*}_{version:\\\\d+\\\\.\\\\d+\\\\.\\\\d+(\\\\.[A-Za-z0-9_-]+)?}";
    return Arrays.asList(
      buildTokenMatcherForPatternAndAssetKind(prefix + "/p2.index", P2_INDEX, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + "/compositeContent.jar", COMPOSITE_CONTENT, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + "/compositeContent.xml", COMPOSITE_CONTENT, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + "/content.jar", CONTENT_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + "/content.xml.xz", CONTENT_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + "/content.xml", CONTENT_METADATA, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + "/compositeArtifacts.jar", COMPOSITE_ARTIFACTS, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + "/compositeArtifacts.xml", COMPOSITE_ARTIFACTS, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + "/artifacts.jar", ARTIFACTS_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + "/artifacts.xml.xz", ARTIFACTS_METADATA, GET, HEAD),
      buildTokenMatcherForPatternAndAssetKind(prefix + "/artifacts.xml", ARTIFACTS_METADATA, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + "/{dir:features|plugins}/" + path + ".{extension:.*}",
        BUNDLE, GET, HEAD),

      buildTokenMatcherForPatternAndAssetKind(prefix + "/{dir:binary}/" + path, BINARY_BUNDLE, GET, HEAD)
    );
  }
}
