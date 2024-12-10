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
package org.sonatype.nexus.content.maven.internal.recipe;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.content.maven.internal.index.MavenContentGroupIndexFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.maven.internal.group.MergingGroupHandler;
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2GroupRecipe;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * @since 3.25
 */
@AvailabilityVersion(from = "1.0")
@Named(Maven2GroupRecipe.NAME)
@Singleton
public class MavenGroupRecipe
    extends MavenRecipeSupport
    implements Maven2GroupRecipe
{
  private final Provider<MavenContentGroupIndexFacet> mavenGroupIndexFacet;

  private final Provider<MavenGroupFacet> mavenGroupFacet;

  private final Provider<PurgeUnusedSnapshotsFacet> mavenPurgeSnapshotsFacet;

  private final GroupHandler groupHandler;

  private final MergingGroupHandler mergingGroupHandler;

  private final MavenContentIndexGroupHandler indexGroupHandler;

  @Inject
  public MavenGroupRecipe(
      @Named(GroupType.NAME) final Type type,
      @Named(Maven2Format.NAME) final Format format,
      final Provider<MavenContentGroupIndexFacet> mavenGroupIndexFacet,
      final Provider<MavenGroupFacet> mavenGroupFacet,
      final Provider<PurgeUnusedSnapshotsFacet> mavenPurgeSnapshotsFacet,
      final GroupHandler groupHandler,
      final MergingGroupHandler mergingGroupHandler,
      final MavenContentIndexGroupHandler indexGroupHandler)
  {
    super(type, format);
    this.mavenGroupIndexFacet = checkNotNull(mavenGroupIndexFacet);
    this.mavenGroupFacet = checkNotNull(mavenGroupFacet);
    this.mavenPurgeSnapshotsFacet = checkNotNull(mavenPurgeSnapshotsFacet);
    this.groupHandler = checkNotNull(groupHandler);
    this.mergingGroupHandler = checkNotNull(mergingGroupHandler);
    this.indexGroupHandler = checkNotNull(indexGroupHandler);
  }

  @Override
  public void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(mavenGroupFacet.get());
    repository.attach(mavenContentFacet.get());
    repository.attach(mavenGroupIndexFacet.get());
    repository.attach(browseFacet.get());
    repository.attach(mavenPurgeSnapshotsFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(mavenMaintenanceFacet.get());
    repository.attach(removeSnapshotsFacet.get());
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    addBrowseUnsupportedRoute(builder);

    // Note: partialFetchHandler NOT added for Maven metadata
    builder.route(newMetadataRouteBuilder()
        .handler(contentHeadersHandler)
        .handler(mergingGroupHandler)
        .create());

    builder.route(newIndexRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(indexGroupHandler)
        .create());

    builder.route(newArchetypeCatalogRouteBuilder()
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(mergingGroupHandler)
        .create());

    builder.route(newMavenPathRouteBuilder()
        .handler(partialFetchHandler)
        .handler(groupHandler)
        .create());

    builder.defaultHandlers(notFound());

    facet.configure(builder.create());

    return facet;
  }
}
