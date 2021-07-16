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
package org.sonatype.nexus.repository.r.datastore.internal

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.content.browse.BrowseFacet
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet
import org.sonatype.nexus.repository.content.search.SearchFacet
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.r.datastore.RContentFacet
import org.sonatype.nexus.repository.r.AssetKind
import org.sonatype.nexus.repository.r.internal.security.RSecurityFacet
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Matcher
import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.SuffixMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
/**
 * Support for R recipes.
 *
 * @since 3.32
 */
abstract class RRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<BrowseFacet> browseFacet

  @Inject
  Provider<RSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<RContentFacet> contentFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<LastAssetMaintenanceFacet> maintenanceFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  RoutingRuleHandler routingRuleHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  protected RRecipeSupport(final Type type, final Format format) {
    super(type, format)
  }

  Closure assetKindHandler = { Context context, AssetKind value ->
    context.attributes.set(AssetKind, value)
    return context.proceed()
  }

  /**
   * Matcher for all packages mapping.
   */
  static Builder packagesMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.GET, HttpMethods.HEAD),
            packagesTokenMatcher()
        ))
  }

  /**
   * Matcher for PACKAGES.rds metadata mapping.
   *
   * This matcher should be removed after ticket fix: NEXUS-22119
   */
  static Builder metadataPackagesRdsMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.GET, HttpMethods.HEAD),
            packagesRdsTokenMatcher()
        ))
  }

  /**
   * Matcher for all .rds metadata mapping.
   */
  static Builder metadataRdsMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.GET, HttpMethods.HEAD),
            metadataRdsPathMatcher()
        ))
  }

  /**
   * Matcher for archive mapping.
   */
  static Builder archiveMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.GET, HttpMethods.HEAD),
            archivePathMatcher()
        ))
  }

  /**
   * Matcher for upload mapping.
   */
  static Builder uploadMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.PUT),
            allFilesTokenMatcher()
        ))
  }

  /**
   * Matcher for wrong upload mapping.
   */
  static Builder nonRArchiveUploadMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(HttpMethods.PUT),
            LogicMatchers.not(archivePathMatcher())
        ))
  }

  /**
   * Path matcher for archive files.
   */
  static Matcher archivePathMatcher() {
    return LogicMatchers.and(
        allFilesTokenMatcher(),
        LogicMatchers.or(
            suffixMatcherForExtension('.zip'),
            suffixMatcherForExtension('.tgz'),
            suffixMatcherForExtension('.tar.gz')
        )
    )
  }

  /**
   * Path matcher for .rds metadata files.
   */
  static Matcher metadataRdsPathMatcher() {
    return LogicMatchers.and(
        allFilesTokenMatcher(),
        suffixMatcherForExtension('.rds')
    )
  }

  /**
   * Token matcher for all PACKAGES files.
   */
  static TokenMatcher packagesTokenMatcher() {
    return new TokenMatcher('/{path:.+}/PACKAGES{extension:.*}')
  }

  /**
   * Token matcher for PACKAGES.rds files.
   *
   * This matcher should be removed after ticket fix: NEXUS-22119
   */
  static TokenMatcher packagesRdsTokenMatcher() {
    return new TokenMatcher('/{path:.+}/PACKAGES.rds')
  }

  /**
   * Token matcher for all files
   */
  static TokenMatcher allFilesTokenMatcher() {
    return new TokenMatcher('/{pathAndFilename:.+}')
  }

  /**
   * Suffix matcher for files with given extension.
   */
  static SuffixMatcher suffixMatcherForExtension(final String extension) {
    return new SuffixMatcher(extension)
  }
}
