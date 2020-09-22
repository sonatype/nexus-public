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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.content.npm.internal.search.legacy.NpmSearchIndexFacetHosted;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.HttpHandlers;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAuditErrorHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAuditFacet;
import org.sonatype.nexus.repository.npm.internal.NpmAuditTarballFacet;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmHandlers;
import org.sonatype.nexus.repository.npm.internal.NpmHostedFacet;
import org.sonatype.nexus.repository.npm.internal.NpmPingHandler;
import org.sonatype.nexus.repository.npm.internal.NpmSecurityFacet;
import org.sonatype.nexus.repository.npm.internal.NpmTokenFacet;
import org.sonatype.nexus.repository.npm.internal.NpmWhoamiHandler;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacetHosted;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
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

import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.*;

/**
 * npm hosted repository recipe.
 *
 * @since 3.next
 */
@Named(NpmHostedRecipe.NAME)
@Singleton
public class NpmHostedRecipe
    extends NpmRecipeSupport
{
  public static final String NAME = "npm-hosted";

  private final Provider<NpmHostedFacet> npmHostedFacetProvider;

  private final Provider<NpmSearchIndexFacetHosted> npmSearchIndexFacetProvider;

  private final Provider<NpmSearchFacetHosted> npmSearchFacetProvider;

  private final NpmWhoamiHandler npmWhoamiHandler;

  private final NpmPingHandler pingHandler;

  @Inject
  public NpmHostedRecipe(
      @Named(HostedType.NAME) final Type type,
      @Named(NpmFormat.NAME) final Format format,
      final Provider<NpmSecurityFacet> securityFacet,
      final Provider<ConfigurableViewFacet> viewFacet,
      final Provider<BrowseFacet> browseFacet,
      final Provider<SearchFacet> searchFacet,
      final Provider<NpmTokenFacet> tokenFacet,
      final Provider<NpmAuditFacet> npmAuditFacetProvider,
      final Provider<NpmAuditTarballFacet> npmAuditTarballFacetProvider,
      final Provider<NpmContentFacet> contentFacet,
      final Provider<NpmHostedFacet> npmHostedFacet,
      final Provider<NpmSearchIndexFacetHosted> npmSearchIndexFacet,
      final Provider<NpmSearchFacetHosted> npmSearchFacet,
      final Provider<LastAssetMaintenanceFacet> lastAssetMaintenanceFacet,
      final ExceptionHandler exceptionHandler,
      final TimingHandler timingHandler,
      final IndexHtmlForwardHandler indexHtmlForwardHandler,
      final SecurityHandler securityHandler,
      final PartialFetchHandler partialFetchHandler,
      final ConditionalRequestHandler conditionalRequestHandler,
      final ContentHeadersHandler contentHeadersHandler,
      final LastDownloadedHandler lastDownloadedHandler,
      final HandlerContributor handlerContributor,
      final RoutingRuleHandler routingHandler,
      final NpmAuditErrorHandler auditErrorHandler,
      final NpmWhoamiHandler npmWhoamiHandler,
      final NpmPingHandler pingHandler,
      @Named("nexus.analytics.npmAuditHandler") @Nullable final Handler auditAnalyticsHandler)
  {
    super(type, format, securityFacet, viewFacet, contentFacet, searchFacet, browseFacet, exceptionHandler,
        timingHandler, indexHtmlForwardHandler, securityHandler, partialFetchHandler, conditionalRequestHandler,
        contentHeadersHandler, lastDownloadedHandler, handlerContributor, tokenFacet, npmAuditFacetProvider,
        npmAuditTarballFacetProvider, lastAssetMaintenanceFacet, routingHandler, auditErrorHandler,
        auditAnalyticsHandler);
    this.npmHostedFacetProvider = npmHostedFacet;
    this.npmSearchIndexFacetProvider = npmSearchIndexFacet;
    this.npmSearchFacetProvider = npmSearchFacet;
    this.npmWhoamiHandler = npmWhoamiHandler;
    this.pingHandler = pingHandler;
  }

  @Override
  public void apply(final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(tokenFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(searchFacet.get());
    repository.attach(npmHostedFacetProvider.get());
    repository.attach(npmSearchIndexFacetProvider.get());
    repository.attach(npmSearchFacetProvider.get());
    repository.attach(contentFacet.get());
    repository.attach(lastAssetMaintenanceFacet.get());
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
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.searchIndex)
        .create());

    // GET /-/v1/search (npm v1 search)
    builder.route(searchV1Matcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
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

    // GET /packageName (npm install)
    builder.route(packageMatcher(GET, HEAD)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.getPackage)
        .create());

    // PUT /packageName (npm publish + npm deprecate)
    builder.route(packageMatcher(PUT)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(NpmHandlers.putPackage)
        .create());

    // PUT /packageName/-rev/revision (npm unpublish)
    builder.route(packageMatcherWithRevision(PUT)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(NpmHandlers.putPackage)
        .create());

    // DELETE /packageName (npm un-publish when last version deleted, npm 1.x)
    builder.route(packageMatcher(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(NpmHandlers.deletePackage)
        .create());

    // DELETE /packageName/-rev/revision (npm un-publish when last version deleted, newer npms)
    builder.route(packageMatcherWithRevision(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(NpmHandlers.deletePackage)
        .create());

    // GET /packageName/-/tarballName (npm install)
    builder.route(tarballMatcher(GET, HEAD)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(NpmHandlers.getTarball)
        .create());

    // DELETE /packageName/-/tarballName (npm un-publish when some versions are left in place)
    builder.route(tarballMatcher(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(NpmHandlers.deleteTarball)
        .create());

    // DELETE /packageName/-/tarballName/-rev/revision (npm un-publish when some versions are left in place)
    builder.route(tarballMatcherWithRevision(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(NpmHandlers.deleteTarball)
        .create());

    // GET /-/package/packageName/dist-tags (npm dist-tag ls pkg)
    builder.route(distTagsMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(NpmHandlers.getDistTags)
        .create());

    // PUT /-/package/packageName/dist-tags (npm dist-tag add pkg@version tag)
    builder.route(distTagsUpdateMatcher(PUT)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(NpmHandlers.putDistTags)
        .create());

    // DELETE /-/package/packageName/dist-tags (npm dist-tag rm pkg tag)
    builder.route(distTagsUpdateMatcher(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(NpmHandlers.deleteDistTags)
        .create());

    createUserRoutes(builder);

    builder.defaultHandlers(HttpHandlers.badRequest());

    facet.configure(builder.create());

    return facet;
  }
}
