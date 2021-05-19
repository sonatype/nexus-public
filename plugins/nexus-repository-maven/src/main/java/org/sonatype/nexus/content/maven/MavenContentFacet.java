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
package org.sonatype.nexus.content.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.content.maven.store.GAV;
import org.sonatype.nexus.content.maven.store.Maven2ComponentData;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

/**
 * Provides persistence operations for Maven.
 *
 * @since 3.25
 */
@Facet.Exposed
public interface MavenContentFacet
    extends ContentFacet, MavenFacet
{
  /**
   * Get a maven asset
   *
   * @param mavenPath Path of asset to get
   */
  Optional<Content> get(MavenPath mavenPath) throws IOException;

  /**
   * Put a maven asset
   *
   * @param path    The path of the asset to put
   * @param content The content to put
   */
  Content put(MavenPath path, Payload content) throws IOException;

  /**
   * Delete a maven asset
   *
   * @param path The path of the asset to delete.
   * @return True if asset was deleted or false if not.
   */
  boolean delete(MavenPath path) throws IOException;

  /**
   * Deletes the assets at the specified paths.
   *
   * @since 3.29
   */
  boolean delete(final List<String> paths);

  /**
   * Delete the specified maven asset and its hashes
   *
   * @param path The path of the asset to delete
   * @return The paths of the assets deleted
   */
  Set<String> deleteWithHashes(MavenPath path) throws IOException;

  /**
   * Update component attributes when {@code path} corresponds to a Maven POM
   *
   * @param path for which the corresponding component attributes may be updated
   */
  void maybeUpdateComponentAttributes(MavenPath path) throws IOException;

  int deleteComponents(int[] componentIds);

  /**
   * Delete metadata, when no more components of same coordinates; or
   * Flag metadata for rebuild, when other components share same GAbV (snapshots)
   *
   * @param component for which metadata should be deleted or flagged for rebuild
   * @return paths of deleted assets; empty when just flagging for rebuild
   */
  Set<String> deleteMetadataOrFlagForRebuild(Component component);


  /**
   * Find Snapshot Group Artifact Version(GAVs)
   *
   * @param minimumRetained The minimum number of snapshots to keep.
   *
   * @since 3.30
   */
  Set<GAV> findGavsWithSnaphots(int minimumRetained);

  /**
   * Find Components by Group Artifact Version(GAVs)
   *
   * @param name artifact name
   * @param group artifact group
   * @param baseVersion artifact base version
   * @param releaseVersion artifact release version
   *
   * @since 3.30
   */
  List<Maven2ComponentData> findComponentsForGav(final String name,
                                                 final String group,
                                                 final String baseVersion,
                                                 final String releaseVersion);

  /**
   * Find snapshots to delete for which a release version exists
   *
   * @param gracePeriod an optional period to keep snapshots around.
   * @return array of snapshot components IDs to delete for which a release version exists
   *
   * @since 3.30
   */
  int[] selectSnapshotsAfterRelease(final int gracePeriod);

  /**
   * Create a component and asset for a maven path without attaching a blob to the asset. This is primarily used when
   * the blob will be hard linked to the asset afterwards.
   *
   * @param mavenPath
   * @return the asset
   */
  FluentAsset createComponentAndAsset(final MavenPath mavenPath);

  /**
   * Hard link some content to an asset
   * @param asset
   * @param path
   * @param contentPath
   * @throws IOException
   */
  void hardLink(FluentAsset asset, Path contentPath) throws IOException;
}
