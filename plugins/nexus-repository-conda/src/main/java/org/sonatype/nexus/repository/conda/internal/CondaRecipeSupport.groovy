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
package org.sonatype.nexus.repository.conda.internal

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheHandler
import org.sonatype.nexus.repository.conda.internal.security.CondaSecurityFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Matcher
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

import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_CONDA_PACKAGE
import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_INDEX_HTML
import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_REPODATA2_JSON
import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_REPODATA_JSON
import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_REPODATA_JSON_BZ2
import static org.sonatype.nexus.repository.conda.internal.AssetKind.ARCH_TAR_PACKAGE
import static org.sonatype.nexus.repository.conda.internal.AssetKind.CHANNEL_DATA_JSON
import static org.sonatype.nexus.repository.conda.internal.AssetKind.CHANNEL_INDEX_HTML
import static org.sonatype.nexus.repository.conda.internal.AssetKind.CHANNEL_RSS_XML
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.CHANNELDATA_JSON
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.CONDA_EXT
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.INDEX_HTML
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.REPODATA2_JSON
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.REPODATA_JSON
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.REPODATA_JSON_BZ2
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.RSS_XML
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.TAR_BZ2_EXT
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
/**
 * Support for Conda recipes.
 *
 * @since 3.19
 */
abstract class CondaRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<CondaSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<CondaFacetImpl> condaFacet

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
  Provider<CondaComponentMaintenanceFacet> componentMaintenanceFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  NegativeCacheHandler negativeCacheHandler

  @Inject
  HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  @Inject
  FormatHighAvailabilitySupportHandler highAvailabilitySupportHandler;

  @Inject
  RoutingRuleHandler routingHandler

  protected CondaRecipeSupport(final Type type, final Format format) {
    super(type, format)
  }

  static Matcher rootChannelIndexHtmlMatcher() {
    buildTokenMatcherForPatternAndAssetKind("{path:.*}/${INDEX_HTML}", CHANNEL_INDEX_HTML, GET, HEAD)
  }

  static Matcher rootChannelDataJsonMatcher() {
    buildTokenMatcherForPatternAndAssetKind("{path:.*}/${CHANNELDATA_JSON}", CHANNEL_DATA_JSON, GET, HEAD)
  }

  static Matcher rootChannelRssXmlMatcher() {
    buildTokenMatcherForPatternAndAssetKind("{path:.*}/${RSS_XML}", CHANNEL_RSS_XML, GET, HEAD)
  }

  static Matcher archIndexHtmlMatcher() {
    buildTokenMatcherForPatternAndAssetKind("{path:.*}/{arch:.+}/${INDEX_HTML}", ARCH_INDEX_HTML, GET, HEAD)
  }

  static Matcher archRepodataJsonMatcher() {
    buildTokenMatcherForPatternAndAssetKind("{path:.*}/{arch:.+}/${REPODATA_JSON}", ARCH_REPODATA_JSON, GET, HEAD)
  }

  static Matcher archRepodataJsonBz2Matcher() {
    buildTokenMatcherForPatternAndAssetKind("{path:.*}/{arch:.+}/${REPODATA_JSON_BZ2}", ARCH_REPODATA_JSON_BZ2, GET,
        HEAD)
  }

  static Matcher archRepodata2JsonMatcher() {
    buildTokenMatcherForPatternAndAssetKind("{path:.*}/{arch:.+}/${REPODATA2_JSON}", ARCH_REPODATA2_JSON, GET, HEAD)
  }

  static Matcher archTarPackageMatcher() {
    buildTokenMatcherForPatternAndAssetKind(
        "{path:.*}/{arch:.+}/{name:.+}-{version:.+}-{build:.+}{format:${TAR_BZ2_EXT}}",
        ARCH_TAR_PACKAGE, GET, HEAD)
  }

  static Matcher archCondaPackageMatcher() {
    buildTokenMatcherForPatternAndAssetKind(
        "{path:.*}/{arch:.+}/{name:.+}-{version:.+}-{build:.+}{format:${CONDA_EXT}}",
        ARCH_CONDA_PACKAGE, GET, HEAD)
  }

  static Matcher buildTokenMatcherForPatternAndAssetKind(final String pattern, final AssetKind assetKind,
                                                         final String... actions)
  {
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

  @Override
  boolean isFeatureEnabled() {
    return highAvailabilitySupportChecker.isSupported(getFormat().getValue())
  }
}
