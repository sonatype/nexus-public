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
package org.sonatype.nexus.repository.content.npm.internal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.HttpHandlers;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.npm.internal.NpmAuditErrorHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAuditFacet;
import org.sonatype.nexus.repository.npm.internal.NpmAuditHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAuditQuickHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAuditTarballFacet;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmHandlers;
import org.sonatype.nexus.repository.npm.internal.NpmNegativeCacheHandler;
import org.sonatype.nexus.repository.npm.internal.NpmPingHandler;
import org.sonatype.nexus.repository.npm.internal.NpmProxyCacheInvalidatorFacetImpl;
import org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget;
import org.sonatype.nexus.repository.npm.internal.NpmProxyHandler;
import org.sonatype.nexus.repository.npm.internal.NpmSecurityFacet;
import org.sonatype.nexus.repository.npm.internal.NpmTokenFacet;
import org.sonatype.nexus.repository.npm.internal.NpmWhoamiHandler;
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexFacetProxy;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacetProxy;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.IndexHtmlForwardHandler;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.auditMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.auditQuickMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.distTagsMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.packageMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.pingMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.searchIndexMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.searchV1Matcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.tarballMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.whoamiMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.DIST_TAGS;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.PACKAGE;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.SEARCH_INDEX;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.SEARCH_V1_RESULTS;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.TARBALL;

/**
 * npm proxy repository recipe.
 *
 * @since 3.29
 */
@Named(NpmProxyRecipe.NAME)
@Singleton
public class NpmProxyRecipe
    extends NpmRecipeSupport
{
  public static final String NAME = "npm-proxy";

  private final Provider<HttpClientFacet> httpClientFacet;

  private final Provider<NegativeCacheFacet> negativeCacheFacet;

  private final Provider<NpmContentProxyFacet> proxyFacet;

  private final Provider<NpmSearchIndexFacetProxy> npmSearchIndexFacetProxy;

  private final Provider<NpmSearchFacetProxy> npmSearchFacet;

  private final Provider<PurgeUnusedFacet> purgeUnusedFacet;

  private final Provider<NpmProxyCacheInvalidatorFacetImpl> npmProxyCacheInvalidatorFacet;

  private final NpmNegativeCacheHandler negativeCacheHandler;

  private final NpmAuditQuickHandler auditQuickHandler;

  private final NpmAuditHandler auditHandler;

  private final NpmProxyHandler proxyHandler;

  @Inject
  protected NpmProxyRecipe(
      @Named(ProxyType.NAME) final Type type,
      @Named(NpmFormat.NAME) final Format format,
      final Provider<NpmSecurityFacet> securityFacet,
      final Provider<ConfigurableViewFacet> viewFacet,
      final Provider<NpmContentFacet> contentFacet,
      final Provider<SearchFacet> searchFacet,
      final Provider<BrowseFacet> browseFacet,
      final ExceptionHandler exceptionHandler,
      final TimingHandler timingHandler,
      final IndexHtmlForwardHandler indexHtmlForwardHandler,
      final SecurityHandler securityHandler,
      final PartialFetchHandler partialFetchHandler,
      final ConditionalRequestHandler conditionalRequestHandler,
      final ContentHeadersHandler contentHeadersHandler,
      final LastDownloadedHandler lastDownloadedHandler,
      final HandlerContributor handlerContributor,
      final Provider<NpmTokenFacet> tokenFacet,
      final Provider<NpmAuditFacet> npmAuditFacetProvider,
      final Provider<NpmAuditTarballFacet> npmAuditTarballFacetProvider,
      final Provider<LastAssetMaintenanceFacet> lastAssetMaintenanceFacet,
      final RoutingRuleHandler routingHandler,
      final NpmAuditErrorHandler auditErrorHandler,
      @Named("nexus.analytics.npmAuditHandler") @Nullable final Handler auditAnalyticsHandler,
      final Provider<HttpClientFacet> httpClientFacet,
      final Provider<NegativeCacheFacet> negativeCacheFacet,
      final Provider<NpmContentProxyFacet> proxyFacet,
      final Provider<NpmSearchIndexFacetProxy> npmSearchIndexFacetProxy,
      final Provider<NpmSearchFacetProxy> npmSearchFacet,
      final Provider<PurgeUnusedFacet> purgeUnusedFacet,
      final Provider<NpmProxyCacheInvalidatorFacetImpl> npmProxyCacheInvalidatorFacet,
      final NpmNegativeCacheHandler negativeCacheHandler,
      final NpmWhoamiHandler npmWhoamiHandler,
      final NpmAuditQuickHandler auditQuickHandler,
      final NpmAuditHandler auditHandler,
      final NpmPingHandler pingHandler, final NpmProxyHandler proxyHandler)
  {
    super(type, format, securityFacet, viewFacet, contentFacet, searchFacet, browseFacet, exceptionHandler,
        timingHandler, indexHtmlForwardHandler, securityHandler, partialFetchHandler, conditionalRequestHandler,
        contentHeadersHandler, lastDownloadedHandler, handlerContributor, tokenFacet, npmAuditFacetProvider,
        npmAuditTarballFacetProvider, lastAssetMaintenanceFacet, routingHandler, auditErrorHandler,
        auditAnalyticsHandler, npmWhoamiHandler, pingHandler);
    this.httpClientFacet = checkNotNull(httpClientFacet);
    this.negativeCacheFacet = checkNotNull(negativeCacheFacet);
    this.proxyFacet = checkNotNull(proxyFacet);
    this.npmSearchIndexFacetProxy = checkNotNull(npmSearchIndexFacetProxy);
    this.npmSearchFacet = checkNotNull(npmSearchFacet);
    this.purgeUnusedFacet = checkNotNull(purgeUnusedFacet);
    this.npmProxyCacheInvalidatorFacet = checkNotNull(npmProxyCacheInvalidatorFacet);
    this.negativeCacheHandler = checkNotNull(negativeCacheHandler);
    this.auditQuickHandler = checkNotNull(auditQuickHandler);
    this.auditHandler = checkNotNull(auditHandler);
    this.proxyHandler = checkNotNull(proxyHandler);
  }

  @Override
  public void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(tokenFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(httpClientFacet.get());
    repository.attach(negativeCacheFacet.get());
    repository.attach(contentFacet.get());
    repository.attach(proxyFacet.get());
    repository.attach(searchFacet.get());
    repository.attach(npmSearchIndexFacetProxy.get());
    repository.attach(npmSearchFacet.get());
    repository.attach(contentMaintenanceFacetProvider.get());
    repository.attach(purgeUnusedFacet.get());
    repository.attach(npmAuditFacetProvider.get());
    repository.attach(npmAuditTarballFacetProvider.get());
    repository.attach(npmProxyCacheInvalidatorFacet.get());
    repository.attach(browseFacet.get());
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    addBrowseUnsupportedRoute(builder);

    // GET /-/all (npm search)
    builder.route(searchIndexMatcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler(SEARCH_INDEX))
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.searchIndex)
        .create());

    // GET /-/v1/search (npm v1 search)
    builder.route(searchV1Matcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler(SEARCH_V1_RESULTS))
        .handler(NpmHandlers.searchV1)
        .create());

    // GET /-/whoami
    builder.route(whoamiMatcher()
        .handler(timingHandler)
        .handler(npmWhoamiHandler)
        .create());

    // GET /-/ping
    builder.route(pingMatcher()
        .handler(timingHandler)
        .handler(pingHandler)
        .create());

    // POST /-/npm/v1/security/audits
    builder.route(auditMatcher()
        .handler(auditAnalyticsHandler != null ? auditAnalyticsHandler : Context::proceed)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(auditErrorHandler)
        .handler(auditHandler)
        .create());

    // POST /-/npm/v1/security/audits/quick
    builder.route(auditQuickMatcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(auditErrorHandler)
        .handler(auditQuickHandler)
        .create());

    // GET /packageName (npm install)
    builder.route(packageMatcher(GET, HEAD)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler(PACKAGE))
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create());

    // GET /packageName/-/tarballName (npm install)
    builder.route(tarballMatcher(GET, HEAD)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler(TARBALL))
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create());

    // GET /-/package/packageName/dist-tags
    builder.route(distTagsMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler(DIST_TAGS))
        .handler(proxyHandler)
        .create());

    createUserRoutes(builder);

    builder.defaultHandlers(HttpHandlers.notFound());

    facet.configure(builder.create());

    return facet;
  }

  Handler proxyTargetHandler(final ProxyTarget value) {
    return (context) -> {
      context.getAttributes().set(ProxyTarget.class, value);
      return context.proceed();
    };
  }
}
