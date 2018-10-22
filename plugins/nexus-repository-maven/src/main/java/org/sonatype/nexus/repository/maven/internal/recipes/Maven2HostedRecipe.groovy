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
package org.sonatype.nexus.repository.maven.internal.recipes

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.maven.MavenPathParser
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet
import org.sonatype.nexus.repository.maven.internal.Maven2Format
import org.sonatype.nexus.repository.maven.internal.MavenSecurityFacet
import org.sonatype.nexus.repository.maven.internal.VersionPolicyHandler
import org.sonatype.nexus.repository.maven.internal.hosted.ArchetypeCatalogHandler
import org.sonatype.nexus.repository.maven.internal.hosted.HostedHandler
import org.sonatype.nexus.repository.maven.internal.hosted.MavenHostedComponentMaintenanceFacet
import org.sonatype.nexus.repository.maven.internal.hosted.MavenHostedFacetImpl
import org.sonatype.nexus.repository.maven.internal.hosted.MavenHostedIndexFacet
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound

/**
 * Maven 2 hosted repository recipe.
 *
 * @since 3.0
 */
@Named(Maven2HostedRecipe.NAME)
@Singleton
class Maven2HostedRecipe
    extends MavenRecipeSupport
{
  public static final String NAME = 'maven2-hosted'

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<MavenHostedFacetImpl> mavenHostedFacet

  @Inject
  Provider<MavenHostedIndexFacet> mavenIndexFacet

  @Inject
  Provider<PurgeUnusedSnapshotsFacet> mavenPurgeSnapshotsFacet

  @Inject
  Provider<MavenHostedComponentMaintenanceFacet> componentMaintenanceFacet

  @Inject
  VersionPolicyHandler versionPolicyHandler

  @Inject
  HostedHandler hostedHandler

  @Inject
  ArchetypeCatalogHandler archetypeCatalogHandler
  
  @Inject
  Provider<RemoveSnapshotsFacet> removeSnapshotsFacet

  @Inject
  Maven2HostedRecipe(@Named(HostedType.NAME) final Type type,
                     @Named(Maven2Format.NAME) final Format format,
                     @Named(Maven2Format.NAME) MavenPathParser mavenPathParser,
                     Provider<MavenSecurityFacet> securityFacet)
  {
    super(type, format, mavenPathParser, securityFacet)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(componentMaintenanceFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(mavenFacet.get())
    repository.attach(mavenHostedFacet.get())
    repository.attach(mavenIndexFacet.get())
    repository.attach(mavenPurgeSnapshotsFacet.get())
    repository.attach(removeSnapshotsFacet.get())
    repository.attach(configure(viewFacet.get()))
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // Note: partialFetchHandler NOT added for Maven metadata
    builder.route(newMetadataRouteBuilder()
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(hostedHandler)
        .create())

    builder.route(newIndexRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(hostedHandler)
        .create())

    builder.route(newArchetypeCatalogRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(archetypeCatalogHandler)
        .create())

    builder.route(newMavenPathRouteBuilder()
        .handler(partialFetchHandler)
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(hostedHandler)
        .create())

    builder.defaultHandlers(notFound())

    facet.configure(builder.create())

    return facet
  }
}
