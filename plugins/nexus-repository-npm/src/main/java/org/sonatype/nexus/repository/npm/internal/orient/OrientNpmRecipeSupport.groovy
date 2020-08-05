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
package org.sonatype.nexus.repository.npm.internal.orient

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.npm.internal.NpmAuditErrorHandler
import org.sonatype.nexus.repository.npm.internal.NpmAuditFacet
import org.sonatype.nexus.repository.npm.internal.NpmAuditTarballFacet
import org.sonatype.nexus.repository.npm.internal.NpmHandlers
import org.sonatype.nexus.repository.npm.internal.NpmSecurityFacet
import org.sonatype.nexus.repository.npm.internal.NpmTokenFacet
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Handler
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler

import static org.sonatype.nexus.repository.http.HttpMethods.DELETE
import static org.sonatype.nexus.repository.http.HttpMethods.PUT
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.tokenMatcher
import static org.sonatype.nexus.repository.npm.internal.NpmPaths.userMatcher

/**
 * Common configuration aspects for npm repositories.
 * @since 3.0
 */
abstract class OrientNpmRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<NpmSecurityFacet> securityFacet

  @Inject
  Provider<NpmFacetImpl> npmFacet

  @Inject
  Provider<NpmTokenFacet> tokenFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  Provider<AttributesFacet> attributesFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<NpmAuditFacet> npmAuditFacetProvider

  @Inject
  Provider<NpmAuditTarballFacet> npmAuditTarballFacetProvider

  @Inject
  TimingHandler timingHandler

  @Inject
  RoutingRuleHandler routingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  UnitOfWorkHandler unitOfWorkHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  LastDownloadedHandler lastDownloadedHandler

  @Inject
  NpmAuditErrorHandler auditErrorHandler

  @Inject
  @Named("nexus.analytics.npmAuditHandler")
  @Nullable
  Handler auditAnalyticsHandler

  protected OrientNpmRecipeSupport(final Type type,
                                   final Format format)
  {
    super(type, format)
  }

  /**
   * Creates common user related routes to support {@code npm adduser} and {@code npm logout} commands.
   */
  void createUserRoutes(Router.Builder builder) {
    // PUT /-/user/org.couchdb.user:userName (npm adduser)
    // Note: this happens as anon! No securityHandler here
    builder.route(userMatcher(PUT)
        .handler(timingHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(NpmHandlers.createToken)
        .create())

    // DELETE /-/user/token/{token} (npm logout)
    builder.route(tokenMatcher(DELETE)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(NpmHandlers.npmErrorHandler)
        .handler(NpmHandlers.deleteToken)
        .create())
  }
}
