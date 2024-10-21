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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AssetInfo;
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
  String FILTER_PARAMS = "filterParams";

  /**
   * Count all assets in the given repository.
   *
   * @param repositoryId the repository to count
   * @param kind optional kind of assets to count
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return count of assets in the repository
   */
  int countAssets(@Param("repositoryId") int repositoryId,
                  @Nullable @Param("kind") String kind,
                  @Nullable @Param("filter") String filter,
                  @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams);

  /**
   * Browse all assets in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @param kind optional kind of assets to return
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Asset> browseAssets(@Param("repositoryId") int repositoryId,
                                   @Param("limit") int limit,
                                   @Nullable @Param("continuationToken") String continuationToken,
                                   @Nullable @Param("kind") String kind,
                                   @Nullable @Param("filter") String filter,
                                   @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams);

  /**
   * Browse all assets with corresponding components and blobs in the given repository in a paged fashion.
   * The returned assets will be sorted by asset id in ascending order. Blob and the Component are eagerly populated.
   *
   * @param repositoryId      the repository to browse
   * @param continuationToken optional token to continue from a previous request
   * @param limit             maximum number of assets to return
   * @return collection of assets and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Asset> browseEagerAssetsInRepository(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("continuationToken") String continuationToken,
      @Param("limit") int limit);

  /**
   * Browse all assets in the given repositories in a paged fashion. The returned assets will be sorted
   * by asset id in ascending order.
   *
   * @param repositoryIds the ids of the repositories to browse
   * @param continuationToken optional token to continue from a previous request
   * @param kind optional kind of assets to return
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @param limit maximum number of assets to return
   * @return collection of assets from the specified repositories and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Asset> browseAssetsInRepositories(
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Nullable @Param("continuationToken") String continuationToken,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param("limit") int limit);

  /**
   * Browse all assets associated with the given logical component.
   *
   * @param component the component to browse
   * @return collection of assets
   */
  Collection<Asset> browseComponentAssets(Component component);

  /**
   * Finds assets where lastUpdated is greater than or equal to the given value.
   *
   * @param repositoryId the repository to browse
   * @param addedToRepository date that asset content must have been updated after
   * @param regexList list of SQL regex expressions that match on the path column
   * @param limit maximum number of assets to return
   * @return collection of assets
   */
  List<AssetInfo> findGreaterThanOrEqualToAddedToRepository(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("addedToRepository") OffsetDateTime addedToRepository,
      @Param("regexList") List<String> regexList,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param("limit") int limit);

  /**
   * Finds all assets where lastUpdated equals the given value.
   *
   * @param repositoryId the repository to browse
   * @param startAddedToRepository blobCreated on asset content is greater than or equal to this value
   * @param endAddedToRepository blobCreated on asset content is less than this value
   * @param regexList list of SQL regex expressions that match on path column
   * @param limit maximum number of assets to return
   * @return collection of assets
   */
  List<AssetInfo> findAddedToRepositoryWithinRange(
      @Param("repositoryId") int repositoryId,
      @Param("startAddedToRepository") OffsetDateTime startAddedToRepository,
      @Param("endAddedToRepository") OffsetDateTime endAddedToRepository,
      @Param("regexList") List<String> regexList,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param("limit") int limit);

  /**
   * Creates the given asset in the content data store and retrieves the generated assetId.
   *
   * @param asset                        the asset to create
   * @param updateComponentEntityVersion whether to update the component's entity version
   */
  int createAsset(
      @Param("asset") AssetData asset,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Retrieves an asset from the content data store.
   *
   * @param assetId the internal id of the asset
   * @return asset if it was found
   */
  Optional<Asset> readAsset(@Param("assetId") int assetId);

  /**
   * Retrieves an asset located at the given path in the content data store.
   *
   * @param repositoryId the repository containing the asset
   * @param path the path of the asset
   * @return asset if it was found
   */
  Optional<Asset> readPath(@Param("repositoryId") int repositoryId, @Param("path") String path);

  /**
   * Retrieves a collection of assets located specific paths in the given repository.
   *
   * @param repositoryId the repository containing the assets
   * @param paths        a list of asset paths
   * @return collection of assets if found
   * @since 3.30
   */
  Collection<Asset> readPathsFromRepository(@Param("repositoryId") int repositoryId, @Param("paths") List<String> paths);

  /**
   * Find an asset based on the blob ref of the associated blob.
   *
   * @param repositoryId the repository containing the assets
   * @param blobRef the blob ref
   * @return asset if it was found
   */
  Optional<Asset> findByBlobRef(@Param("repositoryId") int repositoryId, @Param("blobRef") BlobRef blobRef);

  /**
   * Find assets by their component ids.
   *
   * @param componentIds      a set of component ids.
   * @param assetFilter       optional filter to apply.
   * @param assetFilterParams parameter map for the optional filter.
   * @return collection of {@link AssetInfo}
   */
  Collection<AssetInfo> findByComponentIds(@Param("componentIds") Set<Integer> componentIds,
                                           @Param("assetFilter") final String assetFilter,
                                           @Param("assetFilterParams") final Map<String, String> assetFilterParams);

  /**
   * Updates the kind of the given asset in the content data store.
   *
   * @param asset the asset to update
   *
   * @param updateComponentEntityVersion whether to update the component entity version
   * @since 3.25
   */
  void updateAssetKind(
      @Param("asset") Asset asset,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Retrieves the latest attributes of the given asset in the content data store.
   *
   * @param asset the asset to read
   * @return asset attributes if found
   */
  Optional<NestedAttributesMap> readAssetAttributes(Asset asset);

  /**
   * Updates the attributes of the given asset in the content data store.
   *
   * @param asset the asset to update
   * @param updateComponentEntityVersion whether to update the component entity version
   */
  void updateAssetAttributes(
      @Param("asset") Asset asset,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Updates the link between the given asset and its {@link AssetBlob} in the content data store.
   *
   * @param asset the asset to update
   * @param updateComponentEntityVersion whether to update the component entity version
   */
  void updateAssetBlobLink(
      @Param("asset") Asset asset,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

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
   * Deletes all assets in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the assets
   * @param limit when positive limits the number of assets deleted per-call
   * @return {@code true} if any assets were deleted
   */
  boolean deleteAssets(@Param("repositoryId") int repositoryId, @Param("limit") int limit);

  /**
   * Selects assets without a component in the given repository last downloaded more than given number of days ago.
   *
   * @param repositoryId the repository to check
   * @param daysAgo the number of days ago to check
   * @param limit when positive limits the number of assets selected per-call
   * @return selected asset ids
   *
   * @since 3.26
   */
  int[] selectNotRecentlyDownloaded(@Param("repositoryId") int repositoryId,
                                    @Param("daysAgo") int daysAgo,
                                    @Param("limit") int limit);

  /**
   * Purges the selected assets.
   *
   * This version of the method is for databases that support primitive arrays.
   *
   * @param assetIds the assets to purge
   * @param updateComponentEntityVersion whether to update the component entity version
   * @return the number of purged assets
   *
   * @since 3.26
   */
  int purgeSelectedAssets(
      @Param("assetIds") int[] assetIds,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Purges the selected assets.
   *
   * This version of the method is for databases that don't yet support primitive arrays.
   *
   * @param assetIds the assets to purge
   * @return the number of purged assets
   *
   * @since 3.26
   */
  int purgeSelectedAssets(@Param("assetIds") Integer[] assetIds);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the created time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void created(@Param("assetId") int assetId, @Param("created") OffsetDateTime created);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the last download time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void lastDownloaded(@Param("assetId") int assetId, @Param("lastDownloaded") OffsetDateTime lastDownloaded);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the last updated time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void lastUpdated(@Param("assetId") int assetId, @Param("lastUpdated") OffsetDateTime lastUpdated);

  /**
   * Updates the entity version for this asset's component
   */
  void updateEntityVersion(
      @Param("componentId") int componentId,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Updates the entity version for the components identified for the specified component ids
   */
  void updateEntityVersions(
      @Param("componentIds") int[] componentIds,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Selects the component ids for the assets identified by the asset ids
   *
   * @param assetIds The asset ids to fetch the component ids for
   * @return The component ids for the assets identified by the asset ids.
   */
  int[] selectComponentIds(@Param("assetIds") int[] assetIds);
}
