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
package org.sonatype.nexus.repository.content.store;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;

import org.apache.ibatis.annotations.Param;

/**
 * Asset {@link ContentDataAccess}.
 *
 * @since 3.20
 */
@Expects({ ContentRepositoryDAO.class, ComponentDAO.class, AssetBlobDAO.class })
@SchemaTemplate("format")
public interface AssetDAO
    extends ContentDataAccess
{
  /**
   * Count all assets in the given repository.
   *
   * @param repositoryId the repository to count
   * @return count of assets in the repository
   */
  int countAssets(@Param("repositoryId") int repositoryId);

  /**
   * Browse all assets in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param kind the kind of assets to return
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Asset> browseAssets(@Param("repositoryId") int repositoryId,
                                   @Param("kind") @Nullable String kind,
                                   @Param("limit") int limit,
                                   @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Browse all assets associated with the given logical component.
   *
   * @param component the component to browse
   * @return collection of assets
   */
  Collection<Asset> browseComponentAssets(Component component);

  /**
   * Creates the given asset in the content data store.
   *
   * @param asset the asset to create
   */
  void createAsset(AssetData asset);

  /**
   * Retrieves an asset from the content data store.
   *
   * @param repositoryId the repository containing the asset
   * @param path the path of the asset
   * @return asset if it was found
   */
  Optional<Asset> readAsset(@Param("repositoryId") int repositoryId, @Param("path") String path);

  /**
   * Updates the kind of the given asset in the content data store.
   *
   * @param asset the asset to update
   *
   * @since 3.25.0
   */
  void updateAssetKind(Asset asset);

  /**
   * Updates the attributes of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  void updateAssetAttributes(Asset asset);

  /**
   * Updates the link between the given asset and its {@link AssetBlob} in the content data store.
   *
   * @param asset the asset to update
   */
  void updateAssetBlobLink(Asset asset);

  /**
   * Updates the last downloaded time of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  void markAsDownloaded(Asset asset);

  /**
   * Deletes an asset from the content data store.
   *
   * @param asset the asset to delete
   * @return {@code true} if the asset was deleted
   */
  boolean deleteAsset(Asset asset);

  /**
   * Deletes the asset located at the given path in the content data store.
   *
   * @param repositoryId the repository containing the asset
   * @param path the path of the asset
   * @return {@code true} if the asset was deleted
   */
  boolean deletePath(@Param("repositoryId") int repositoryId, @Param("path") String path);

  /**
   * Deletes all assets in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the assets
   * @param limit when positive limits the number of assets deleted per-call
   * @return {@code true} if any assets were deleted
   */
  boolean deleteAssets(@Param("repositoryId") int repositoryId, @Param("limit") int limit);

  /**
   * Purge assets without component in the given repository last downloaded more than given number of days ago
   *
   * @param repositoryId the repository to browse
   * @param daysAgo last downloaded more than this
   * @param limit at most items to delete
   * @return number of assets deleted
   *
   * @since 3.24
   */
  int purgeNotRecentlyDownloaded(@Param("repositoryId") int repositoryId,
                                 @Param("daysAgo") int daysAgo,
                                 @Param("limit") int limit);
}
