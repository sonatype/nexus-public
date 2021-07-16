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
package org.sonatype.nexus.repository.r.orient.internal.group

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.group.GroupFacetImpl
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.r.AssetKind
import org.sonatype.nexus.repository.r.internal.RCommonHandlers
import org.sonatype.nexus.repository.r.RFormat
import org.sonatype.nexus.repository.r.internal.group.PackagesGroupHandler
import org.sonatype.nexus.repository.r.orient.internal.OrientRRecipeSupport
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router.Builder
import org.sonatype.nexus.repository.view.ViewFacet

/**
 * R group repository recipe.
 * @since 3.28
 */
@Named(OrientRGroupRecipe.NAME)
@Singleton
class OrientRGroupRecipe
    extends OrientRRecipeSupport
{
  public static final String NAME = 'r-group'

  @Inject
  Provider<GroupFacetImpl> groupFacet

  @Inject
  PackagesGroupHandler packagesGroupHandler

  @Inject
  GroupHandler standardGroupHandler

  @Inject
  RCommonHandlers commonHandlers;

  @Inject
  OrientRGroupRecipe(@Named(GroupType.NAME) final Type type, @Named(RFormat.NAME) final Format format) {
    super(type, format)
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
    Builder builder = new Builder()

    builder.route(metadataPackagesRdsMatcher()
        .handler(securityHandler)
        .handler(commonHandlers.notSupportedMetadataRequest)
        .create())

    builder.route(packagesMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.PACKAGES))
        .handler(securityHandler)
        .handler(highAvailabilitySupportHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(packagesGroupHandler)
        .create())

    builder.route(metadataRdsMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.RDS_METADATA))
        .handler(securityHandler)
        .handler(highAvailabilitySupportHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(packagesGroupHandler)
        .create())

    builder.route(archiveMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(AssetKind.ARCHIVE))
        .handler(securityHandler)
        .handler(highAvailabilitySupportHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(standardGroupHandler)
        .create())

    addBrowseUnsupportedRoute(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
