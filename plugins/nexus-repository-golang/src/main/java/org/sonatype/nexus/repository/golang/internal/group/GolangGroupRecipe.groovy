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
package org.sonatype.nexus.repository.golang.internal.group

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.golang.GolangFormat
import org.sonatype.nexus.repository.golang.internal.GolangRecipeSupport
import org.sonatype.nexus.repository.group.GroupFacetImpl
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker

/**
 * Go group repository recipe.
 *
 * @since 3.17
 */
@Named(GolangGroupRecipe.NAME)
@Singleton
class GolangGroupRecipe
    extends GolangRecipeSupport
{
  private static final String NAME = 'go-group'

  @Inject
  Provider<GroupFacetImpl> groupFacet

  @Inject
  GroupHandler groupHandler

  @Inject
  GolangGroupRecipe(final HighAvailabilitySupportChecker highAvailabilitySupportChecker,
                    @Named(GroupType.NAME) final Type type,
                    @Named(GolangFormat.NAME) final Format format)
  {
    super(highAvailabilitySupportChecker, type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(groupFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(attributesFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    [infoMatcher(), packageMatcher(), moduleMatcher(), listMatcher(), latestMatcher()].each { matcher ->
      builder.route(new Route.Builder().matcher(matcher)
          .handler(timingHandler)
          .handler(securityHandler)
          .handler(exceptionHandler)
          .handler(handlerContributor)
          .handler(groupHandler)
          .create())
    }

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
