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
package org.sonatype.nexus.repository.raw.internal

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.IndexHtmlForwardHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.SuffixMatcher
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and

/**
 * Raw hosted repository recipe.
 *
 * @since 3.0
 */
@Named(RawHostedRecipe.NAME)
@Singleton
class RawHostedRecipe
    extends RecipeSupport
{
  public static final String NAME = 'raw-hosted'

  @Inject
  Provider<RawSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<RawContentFacetImpl> rawContentFacet

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  Provider<AttributesFacet> attributesFacet

  @Inject
  Provider<SingleAssetComponentMaintenance> componentMaintenance

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  IndexHtmlForwardHandler indexHtmlForwardHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  UnitOfWorkHandler unitOfWorkHandler

  @Inject
  RawContentHandler rawContentHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  LastDownloadedHandler lastDownloadedHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  RawHostedRecipe(@Named(HostedType.NAME) final Type type,
                  @Named(RawFormat.NAME) final Format format)
  {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(rawContentFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(componentMaintenance.get())
    repository.attach(searchFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    // Additional handlers, such as the lastDownloadHandler, are intentionally
    // not included on this route because this route forwards to the route below.
    // This route specifically handles GET / and forwards to /index.html.
    builder.route(new Route.Builder()
        .matcher(and(new ActionMatcher(HttpMethods.GET), new SuffixMatcher('/')))
        .handler(timingHandler)
        .handler(indexHtmlForwardHandler)
        .create()
    )

    builder.route(new Route.Builder()
        .matcher(new TokenMatcher('/{name:.+}'))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(rawContentHandler)
        .create())

    builder.defaultHandlers(HttpHandlers.badRequest())

    facet.configure(builder.create())

    return facet
  }
}
