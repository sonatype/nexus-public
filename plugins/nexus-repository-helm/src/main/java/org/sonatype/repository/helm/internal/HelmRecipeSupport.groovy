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
package org.sonatype.repository.helm.internal

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
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
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
import org.sonatype.repository.helm.HelmRestoreFacet
import org.sonatype.repository.helm.internal.orient.HelmComponentMaintenanceFacet
import org.sonatype.repository.helm.internal.orient.HelmFacet
import org.sonatype.repository.helm.internal.security.HelmSecurityFacet

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.repository.helm.internal.AssetKind.HELM_INDEX
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE

/**
 * Support for Helm recipes.
 *
 * @since 3.next
 */
abstract class HelmRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<HelmSecurityFacet> securityFacet

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
  Provider<HelmComponentMaintenanceFacet> componentMaintenanceFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  protected Provider<HelmFacet> helmFacet

  @Inject
  protected Provider<HelmRestoreFacet> helmRestoreFacet

  @Inject
  FormatHighAvailabilitySupportHandler formatHighAvailabilitySupportHandler

  @Inject
  HighAvailabilitySupportChecker highAvailabilitySupportChecker

  protected HelmRecipeSupport(final Type type, final Format format) {
    super(type, format)
  }

  @Override
  boolean isFeatureEnabled() {
    return highAvailabilitySupportChecker.isSupported(getFormat().getValue())
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
}
