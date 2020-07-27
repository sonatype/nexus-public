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
package org.sonatype.nexus.repository.maven.internal.orient

import javax.annotation.Nonnull
import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.maven.MavenPathParser
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet
import org.sonatype.nexus.repository.maven.internal.Maven2Format
import org.sonatype.nexus.repository.maven.internal.MavenSecurityFacet
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet
import org.sonatype.nexus.repository.maven.internal.group.MergingGroupHandler
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2GroupRecipe
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound

/**
 * Maven 2 group repository recipe.
 *
 * @since 3.0
 */
@Named(Maven2GroupRecipe.NAME)
@Priority(Integer.MAX_VALUE)
@Singleton
class OrientMaven2GroupRecipe
    extends OrientMavenRecipeSupport
    implements Maven2GroupRecipe
{
  @Inject
  Provider<MavenGroupFacet> mavenGroupFacet

  @Inject
  Provider<OrientMavenGroupIndexFacet> mavenIndexFacet

  @Inject
  Provider<PurgeUnusedSnapshotsFacet> mavenPurgeSnapshotsFacet

  @Inject
  GroupHandler groupHandler

  @Inject
  MergingGroupHandler mergingGroupHandler

  @Inject
  OrientIndexGroupHandler indexGroupHandler

  @Inject
  Provider<RemoveSnapshotsFacet> removeSnapshotsFacet

  @Inject
  OrientMaven2GroupRecipe(
      @Named(GroupType.NAME) Type type,
      @Named(Maven2Format.NAME) Format format,
      @Named(Maven2Format.NAME) MavenPathParser mavenPathParser,
      Provider<MavenSecurityFacet> securityFacet)
  {
    super(type, format, mavenPathParser, securityFacet)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(mavenFacet.get())
    repository.attach(mavenGroupFacet.get())
    repository.attach(mavenIndexFacet.get())
    repository.attach(mavenPurgeSnapshotsFacet.get())
    repository.attach(removeSnapshotsFacet.get())
    repository.attach(configure(viewFacet.get()))
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet viewFacet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // Note: partialFetchHandler NOT added for Maven metadata
    builder.route(newMetadataRouteBuilder()
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(mergingGroupHandler)
        .create())

    builder.route(newIndexRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(indexGroupHandler)
        .create())

    builder.route(newArchetypeCatalogRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(mergingGroupHandler)
        .create())


    builder.route(newMavenPathRouteBuilder()
        .handler(partialFetchHandler)
        .handler(groupHandler)
        .create())

    builder.defaultHandlers(notFound())

    viewFacet.configure(builder.create())

    return viewFacet
  }
}
