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

import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Provider;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAuditErrorHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAuditFacet;
import org.sonatype.nexus.repository.npm.internal.NpmAuditTarballFacet;
import org.sonatype.nexus.repository.npm.internal.NpmHandlers;
import org.sonatype.nexus.repository.npm.internal.NpmPingHandler;
import org.sonatype.nexus.repository.npm.internal.NpmSecurityFacet;
import org.sonatype.nexus.repository.npm.internal.NpmTokenFacet;
import org.sonatype.nexus.repository.npm.internal.NpmWhoamiHandler;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.IndexHtmlForwardHandler;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.tokenMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.userMatcher;

/**
 * @since 3.next
 */
abstract class NpmRecipeSupport
    extends RecipeSupport
{
  protected final Provider<NpmSecurityFacet> securityFacet;

  protected final Provider<ConfigurableViewFacet> viewFacet;

  protected final Provider<NpmContentFacet> contentFacet;

  protected final Provider<SearchFacet> searchFacet;

  protected final Provider<BrowseFacet> browseFacet;

  protected final ExceptionHandler exceptionHandler;

  protected final TimingHandler timingHandler;

  protected final IndexHtmlForwardHandler indexHtmlForwardHandler;

  protected final SecurityHandler securityHandler;

  protected final PartialFetchHandler partialFetchHandler;

  protected final ConditionalRequestHandler conditionalRequestHandler;

  protected final ContentHeadersHandler contentHeadersHandler;

  protected final LastDownloadedHandler lastDownloadedHandler;

  protected final HandlerContributor handlerContributor;

  protected final Provider<NpmTokenFacet> tokenFacet;

  protected final Provider<NpmAuditFacet> npmAuditFacetProvider;

  protected final Provider<NpmAuditTarballFacet> npmAuditTarballFacetProvider;

  protected final Provider<LastAssetMaintenanceFacet> lastAssetMaintenanceFacet;

  protected final RoutingRuleHandler routingHandler;

  protected final NpmAuditErrorHandler auditErrorHandler;

  protected final Handler auditAnalyticsHandler;

  protected final NpmWhoamiHandler npmWhoamiHandler;

  protected final NpmPingHandler pingHandler;

  protected NpmRecipeSupport(
      final Type type,
      final Format format,
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
      @Nullable final Handler auditAnalyticsHandler,
      final NpmWhoamiHandler npmWhoamiHandler,
      final NpmPingHandler pingHandler)
  {
    super(type, format);
    this.securityFacet = checkNotNull(securityFacet);
    this.viewFacet = checkNotNull(viewFacet);
    this.contentFacet = checkNotNull(contentFacet);
    this.searchFacet = checkNotNull(searchFacet);
    this.browseFacet = checkNotNull(browseFacet);
    this.exceptionHandler = checkNotNull(exceptionHandler);
    this.timingHandler = checkNotNull(timingHandler);
    this.indexHtmlForwardHandler = checkNotNull(indexHtmlForwardHandler);
    this.securityHandler = checkNotNull(securityHandler);
    this.partialFetchHandler = checkNotNull(partialFetchHandler);
    this.conditionalRequestHandler = checkNotNull(conditionalRequestHandler);
    this.contentHeadersHandler = checkNotNull(contentHeadersHandler);
    this.lastDownloadedHandler = checkNotNull(lastDownloadedHandler);
    this.handlerContributor = checkNotNull(handlerContributor);
    this.tokenFacet = checkNotNull(tokenFacet);
    this.npmAuditFacetProvider = checkNotNull(npmAuditFacetProvider);
    this.npmAuditTarballFacetProvider = checkNotNull(npmAuditTarballFacetProvider);
    this.lastAssetMaintenanceFacet = checkNotNull(lastAssetMaintenanceFacet);
    this.routingHandler = checkNotNull(routingHandler);
    this.auditErrorHandler = checkNotNull(auditErrorHandler);
    this.auditAnalyticsHandler = Optional.ofNullable(auditAnalyticsHandler).orElse(Context::proceed);
    this.npmWhoamiHandler = checkNotNull(npmWhoamiHandler);
    this.pingHandler = checkNotNull(pingHandler);
  }

  /**
   * Creates common user related routes to support {@code npm adduser} and {@code npm logout} commands.
   */
  void createUserRoutes(final Router.Builder builder) {
    // PUT /-/user/org.couchdb.user:userName (npm adduser)
    // Note: this happens as anon! No securityHandler here
    builder.route(userMatcher(PUT)
        .handler(timingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(NpmHandlers.createToken)
        .create());

    // DELETE /-/user/token/{token} (npm logout)
    builder.route(tokenMatcher(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(NpmHandlers.deleteToken)
        .create());
  }
}
