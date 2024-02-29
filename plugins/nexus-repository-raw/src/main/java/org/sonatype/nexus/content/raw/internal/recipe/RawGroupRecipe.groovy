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
package org.sonatype.nexus.content.raw.internal.recipe

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.common.upgrade.AvailabilityVersion
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.raw.internal.RawFormat
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

/**
 * Raw group repository recipe.
 *
 * @since 3.24
 */
@AvailabilityVersion(from = "1.0")
@Named(RawGroupRecipe.NAME)
@Singleton
class RawGroupRecipe
    extends RawRecipeSupport
{
  public static final String NAME = 'raw-group'

  @Inject
  Provider<GroupFacet> groupFacet

  @Inject
  GroupHandler groupHandler

  @Inject
  RawGroupRecipe(@Named(GroupType.NAME) final Type type,
                 @Named(RawFormat.NAME) final Format format)
  {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(groupFacet.get())
    repository.attach(contentFacet.get())
    repository.attach(browseFacet.get())
  }


  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet viewFacet) {
    Router.Builder builder = new Router.Builder()

    builder.route(new Route.Builder()
        .matcher(new TokenMatcher('{path:/.*}'))
        .handler(timingHandler)
        .handler(contentDispositionHandler)
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(groupHandler)
        .create())

    builder.defaultHandlers(HttpHandlers.badRequest())

    viewFacet.configure(builder.create())

    return viewFacet
  }
}
