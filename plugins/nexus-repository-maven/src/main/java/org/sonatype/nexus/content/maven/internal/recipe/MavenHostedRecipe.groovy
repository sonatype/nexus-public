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
package org.sonatype.nexus.content.maven.internal.recipe

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.common.upgrade.AvailabilityVersion
import org.sonatype.nexus.content.maven.internal.index.MavenContentHostedIndexFacet
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet
import org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet
import org.sonatype.nexus.repository.maven.internal.Maven2Format
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2HostedRecipe
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound

/**
 * @since 3.25
 */
@AvailabilityVersion(from = "1.0")
@Named(Maven2HostedRecipe.NAME)
@Singleton
class MavenHostedRecipe
    extends MavenRecipeSupport
    implements Maven2HostedRecipe
{
  @Inject
  Provider<MavenContentHostedIndexFacet> mavenIndexFacet

  @Inject
  Provider<PurgeUnusedSnapshotsFacet> mavenPurgeSnapshotsFacet

  @Inject
  MavenHostedRecipe(@Named(HostedType.NAME) final Type type, @Named(Maven2Format.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(mavenMetadataRebuildFacet.get())
    repository.attach(mavenContentFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(mavenArchetypeCatalogFacet.get())
    repository.attach(mavenIndexFacet.get())
    repository.attach(mavenMaintenanceFacet.get())
    repository.attach(removeSnapshotsFacet.get())
    repository.attach(mavenPurgeSnapshotsFacet.get())
    repository.attach(mavenReplicationFacet.get())
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // Note: partialFetchHandler NOT added for Maven metadata
    builder.route(newMetadataRouteBuilder()
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(mavenMetadataRebuildHandler)
        .handler(mavenContentHandler)
        .create())

    builder.route(newIndexRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(mavenContentHandler)
        .create())

    builder.route(newArchetypeCatalogRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(archetypeCatalogHandler)
        .handler(mavenContentHandler)
        .create())

    builder.route(newMavenPathRouteBuilder()
        .handler(partialFetchHandler)
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(mavenContentHandler)
        .create())

    builder.defaultHandlers(notFound())

    facet.configure(builder.create())

    return facet
  }
}
