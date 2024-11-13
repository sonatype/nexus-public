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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.content.maven.store.GAV;
import org.sonatype.nexus.content.maven.store.Maven2ComponentData;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
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

  int deleteComponents(Stream<FluentComponent> components);

  /**
   * Delete metadata, when no more components of same coordinates; or Flag metadata for rebuild, when other components
   * share same GAbV (snapshots)
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
   * Eagerly fetches {@link org.sonatype.nexus.repository.content.store.AssetBlobData}
   * & {@link org.sonatype.nexus.repository.content.store.AssetData}
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
   * Find jar assets associated with Components in the namespace of kind maven-plugin.
   *
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @param namespace the namespace to find plugins for
   *
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Asset> findMavenPluginAssetsForNamespace(
      int limit,
      @Nullable String continuationToken,
      String namespace);

  /**
   * Find components with the GAbV.
   *
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @param namespace the namespace
   * @param name artifact name
   * @param baseVersion artifact base version
   *
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<FluentComponent> findComponentsForBaseVersion(
      int limit,
      @Nullable String continuationToken,
      String namespace,
      String name,
      String baseVersion);

  /**
   * Find Components in the provided GroupId & ArtifactId.
   *
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @param namespace the namespace
   * @param name artifact name
   *
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<FluentComponent> findComponentsInGA(
      int limit,
      @Nullable String continuationToken,
      String namespace,
      String name);

  /**
   * Retrieve known base versions for a provided GA.
   *
   * @param namespace    the namespace for the components
   * @param name         the name for the components
   *
   * @return a unique set of base versions
   */
  Collection<String> getBaseVersions(String namespace, String name);

  /**
   * Find components with missed base version.
   *
   * @param namespace the namespace
   * @param name artifact name
   *
   * @return collection of component data
   */
  public Iterable<FluentComponent> getComponentsWithMissedBaseVersion();


  /**
   * Updates the maven base_version of the given component in the content data store.
   *
   * @param component the component to update
   */
  public void updateBaseVersion(final Maven2ComponentData component);

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
   * @param source component to copy
   * @return a new component copied from the source to the current repository
   *
   * @since 3.38
   */
  FluentComponent copy(final Component source);
}
