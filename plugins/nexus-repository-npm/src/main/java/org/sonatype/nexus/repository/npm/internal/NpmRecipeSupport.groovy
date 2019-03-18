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
package org.sonatype.nexus.repository.npm.internal

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import static org.sonatype.nexus.repository.http.HttpMethods.DELETE
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.PUT

/**
 * Common configuration aspects for npm repositories.
 * @since 3.0
 */
abstract class NpmRecipeSupport
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

  protected NpmRecipeSupport(final Type type,
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

  /**
   * Matcher for npm package search index.
   */
  static Builder searchIndexMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET),
            LogicMatchers.or(
                new LiteralMatcher('/-/all'),
                new LiteralMatcher('/-/all/since')
            )
        )
    )
  }

  /**
   * Matcher for npm package v1 search.
   */
  static Builder searchV1Matcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET),
            new LiteralMatcher('/-/v1/search')
        )
      )
  }

  /**
   * Matcher for npm package metadata.
   */
  static Builder packageMatcher(String httpMethod) {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher('/{' + NpmHandlers.T_PACKAGE_NAME + '}'),
                new TokenMatcher('/@{' + NpmHandlers.T_PACKAGE_SCOPE + '}/{' + NpmHandlers.T_PACKAGE_NAME + '}'),
                new TokenMatcher('/{' + NpmHandlers.T_PACKAGE_NAME + '}/{' + NpmHandlers.T_PACKAGE_VERSION + '}')
            )
        )
    )
  }

  /**
   * Matcher for npm package metadata.
   */
  static Builder packageMatcherWithRevision(String httpMethod) {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher('/{' + NpmHandlers.T_PACKAGE_NAME + '}/-rev/{' + NpmHandlers.T_REVISION + '}'),
                new TokenMatcher('/@{' + NpmHandlers.T_PACKAGE_SCOPE + '}/{' + NpmHandlers.T_PACKAGE_NAME + '}/-rev/{' +
                    NpmHandlers.T_REVISION + '}')
            )
        )
    )
  }

  /**
   * Matcher for npm package tarballs.
   */
  static Builder tarballMatcher(String httpMethod) {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher('/{' + NpmHandlers.T_PACKAGE_NAME + '}/-/{' + NpmHandlers.T_TARBALL_NAME + '}'),
                new TokenMatcher('/@{' + NpmHandlers.T_PACKAGE_SCOPE + '}/{' + NpmHandlers.T_PACKAGE_NAME + '}/-/{' +
                    NpmHandlers.T_TARBALL_NAME + '}'),
            )
        )
    )
  }

  /**
   * Matcher for npm package tarballs.
   */
  static Builder tarballMatcherWithRevision(String httpMethod) {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher('/{' + NpmHandlers.T_PACKAGE_NAME + '}/-/{' + NpmHandlers.T_TARBALL_NAME + '}/-rev/{' +
                    NpmHandlers.T_REVISION + '}'),
                new TokenMatcher('/@{' + NpmHandlers.T_PACKAGE_SCOPE + '}/{' + NpmHandlers.T_PACKAGE_NAME + '}/-/{' +
                    NpmHandlers.T_TARBALL_NAME + '}/-rev/{' + NpmHandlers.T_REVISION + '}')
            )
        )
    )
  }

  /**
   * Matcher for {@code npm adduser}.
   */
  static Builder userMatcher(String httpMethod) {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            new TokenMatcher(NpmHandlers.USER_LOGIN_PREFIX + '{' + NpmHandlers.T_USERNAME + '}')
        )
    )
  }

  /**
   * Matcher for {@code npm logout}.
   */
  static Builder tokenMatcher(String httpMethod) {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            new TokenMatcher('/-/user/token/{' + NpmHandlers.T_TOKEN + '}')
        )
    )
  }
}
