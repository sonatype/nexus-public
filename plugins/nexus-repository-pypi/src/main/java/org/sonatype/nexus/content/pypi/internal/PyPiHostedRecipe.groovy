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
package org.sonatype.nexus.content.pypi.internal

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.pypi.internal.AssetKind
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet

/**
 * PyPI hosted repository recipe.
 *
 * @since 3.next
 */
@Named(PyPiHostedRecipe.NAME)
@Singleton
class PyPiHostedRecipe
    extends PyPiRecipeSupport
{
  public static final String NAME = 'pypi-hosted'

  @Inject
  Provider<PyPiHostedFacet> hostedFacet

  @Inject
  PyPiHostedHandlers hostedHandlers

  @Inject
  PyPiHostedRecipe(@Named(HostedType.NAME) final Type type, @Named(PyPiFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(hostedFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(contentFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(indexFacet.get())
    repository.attach(lastAssetMaintenanceFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    builder.route(rootIndexMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.ROOT_INDEX))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(hostedHandlers.getRootIndex)
        .create())

    builder.route(indexMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.INDEX))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(hostedHandlers.getIndex)
        .create())

    builder.route(searchMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.SEARCH))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(hostedHandlers.search())
        .create())

    builder.route(packagesMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.PACKAGE))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(hostedHandlers.getPackage)
        .create())

    builder.route(baseMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.PACKAGE))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(hostedHandlers.postContent)
        .create())

    addBrowseUnsupportedRoute(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
