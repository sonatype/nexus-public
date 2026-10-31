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
@Expects({ContentRepositoryDAO.class, ComponentDAO.class, AssetBlobDAO.class})
@SchemaTemplate("format")
public interface AssetDAO
    extends ContentDataAccess
{
  String FILTER_PARAMS = "filterParams";

  /**
   * See {@link ComponentDAO#SOFT_DELETE_AVAILABLE}.
   */
  String SOFT_DELETE_AVAILABLE = ComponentDAO.SOFT_DELETE_AVAILABLE;

  /**
   * Count all assets in the given repository.
   */
  int countAssets(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

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
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Asset> browseAssets(
      @Param("repositoryId") int repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Backward-compatible overload retained so callers that predate soft-delete keep compiling and behave as
   * before. Delegates with {@code softDeleteAvailable=false}, which omits the {@code deleted_on IS NULL}
   * predicate -- the pre-soft-delete behavior, and safe when the column is not present.
   */
  default Continuation<Asset> browseAssets(
      final int repositoryId,
      final int limit,
      @Nullable final String continuationToken,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return browseAssets(repositoryId, limit, continuationToken, kind, filter, filterParams, false);
  }

  /**
   * Browse all assets with corresponding components and blobs in the given repository in a paged fashion. The returned
   * assets will be sorted by blob_created descending, asset_id ascending for stable pagination.
   * Blob and the Component are eagerly populated.
   *
   * <p>
   * <strong>NOTE:</strong> This method is currently only utilized by migration asset queries and polling operations
   * that require assets ordered by blob creation timestamp in descending order (newest first). It uses a composite
   * continuation token containing both blob_created timestamp and asset_id for stable pagination across pages.
   *
   * @param repositoryId the repository to browse
   * @param continuationToken optional composite token (blob_created + asset_id) to continue from a previous request
   * @param limit maximum number of assets to return
   * @param olderThan optional timestamp to filter assets with blob_created before this time
   * @param newerThan optional timestamp to filter assets with blob_created after this time
   * @return collection of assets and the next continuation token
   * @see Continuation#nextContinuationToken()
   * @see AssetBlobCreatedContinuationToken
   */
  Continuation<Asset> browseEagerAssetsInRepository(
      @Param("repositoryId") int repositoryId,
      @Nullable @Param("continuationToken") AssetBlobCreatedContinuationToken continuationToken,
      @Param("limit") int limit,
      @Nullable @Param("newerThan") OffsetDateTime newerThan,
      @Nullable @Param("olderThan") OffsetDateTime olderThan,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all assets in the given repositories in a paged fashion. The returned assets will be sorted by asset id in
   * ascending order.
   *
   * @param repositoryIds the ids of the repositories to browse
   * @param continuationToken optional token to continue from a previous request
   * @param kind optional kind of assets to return
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @param limit maximum number of assets to return
   * @return collection of assets from the specified repositories and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<Asset> browseAssetsInRepositories(
      @Param("repositoryIds") Set<Integer> repositoryIds,
      @Nullable @Param("continuationToken") String continuationToken,
      @Nullable @Param("kind") String kind,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams,
      @Param("limit") int limit,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all assets associated with the given logical component.
   */
  Collection<Asset> browseComponentAssets(
      @Param("component") Component component,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Browse all assets of a component including soft-deleted ones.
   */
  Collection<Asset> browseAllComponentAssets(Component component);

  /**
   * Browse all assets associated with a component, including soft-deleted.
   */
  Collection<Asset> browseAllComponentAssetsWithSoftDelete(Component component);

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
      @Param("limit") int limit,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

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
      @Param("limit") int limit,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Creates the given asset in the content data store and retrieves the generated assetId.
   *
   * @param asset the asset to create
   * @param updateComponentEntityVersion whether to update the component's entity version
   */
  default int createAsset(
      final AssetData asset,
      final boolean updateComponentEntityVersion)
  {
    return createAsset(asset, updateComponentEntityVersion, false);
  }

  /**
   * Adds a new asset to the content data store.
   *
   * @param asset the asset to create
   * @param updateComponentEntityVersion whether to update the component's entity version
   * @param softDeleteAvailable whether the {@code deleted_on}/{@code deleted_by} columns exist (see
   *          {@link #SOFT_DELETE_AVAILABLE}); when {@code false} those columns are omitted from the write so it
   *          stays valid against a pre-migration schema during a zero-downtime upgrade
   */
  int createAsset(
      @Param("asset") AssetData asset,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Retrieves an asset from the content data store.
   *
   * @param assetId the internal id of the asset
   * @return asset if it was found
   */
  Optional<Asset> readAsset(
      @Param("assetId") int assetId,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Backward-compatible overload retained so callers that predate soft-delete keep compiling and behave as
   * before. Delegates with {@code softDeleteAvailable=false}, which omits the {@code deleted_on IS NULL}
   * predicate -- the pre-soft-delete behavior, and safe when the column is not present.
   */
  default Optional<Asset> readAsset(final int assetId) {
    return readAsset(assetId, false);
  }

  /**
   * Retrieves an asset located at the given path in the content data store.
   */
  Optional<Asset> readPath(
      @Param("repositoryId") int repositoryId,
      @Param("path") String path,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Retrieves a collection of assets located specific paths in the given repository.
   */
  Collection<Asset> readPathsFromRepository(
      @Param("repositoryId") int repositoryId,
      @Param("paths") List<String> paths,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Find an asset based on the blob ref of the associated blob.
   */
  Optional<Asset> findByBlobRef(
      @Param("repositoryId") int repositoryId,
      @Param("blobRef") BlobRef blobRef,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Find assets by their component ids.
   *
   * <p>
   * Important: Aimed to be used ONLY on HA mode, do not use this method in non-HA methods or classes.
   * </p>
   *
   * @param componentIds a set of component ids.
   * @param assetFilter optional filter to apply.
   * @param assetFilterParams parameter map for the optional filter.
   * @param includeNullBlobs whether to include assets without associated blobs (uses LEFT JOIN instead of INNER JOIN)
   * @return collection of {@link AssetInfo}
   */
  Collection<Asset> findByComponentIds(
      @Param("componentIds") Set<Integer> componentIds,
      @Param("assetFilter") final String assetFilter,
      @Param("assetFilterParams") final Map<String, Object> assetFilterParams,
      @Param("includeNullBlobs") boolean includeNullBlobs,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Updates the kind of the given asset in the content data store.
   *
   * @param asset the asset to update
   * @param updateComponentEntityVersion whether to update the component entity version
   * @since 3.25
   */
  void updateAssetKind(
      @Param("asset") Asset asset,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Updates the path of the given asset in the content data store.
   *
   * @param asset the asset to update*
   * @param updateComponentEntityVersion whether to update the component entity version
   */
  void updateAssetPath(
      @Param("asset") Asset asset,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Retrieves the latest attributes of the given asset in the content data store.
   *
   * @param asset the asset to read
   * @return asset attributes if found
   */
  Optional<NestedAttributesMap> readAssetAttributes(
      @Param("asset") Asset asset,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

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
   * Updates the last downloaded time of the given asset, but only when the asset has never been
   * downloaded or its last_downloaded timestamp is older than {@code intervalSeconds}. The
   * conditional WHERE clause makes this safe to call concurrently from multiple threads or nodes —
   * exactly one caller will affect a row per interval.
   *
   * @param asset the asset to update
   * @param intervalSeconds the minimum age (in seconds) the existing last_downloaded must have for
   *          the update to apply; pass {@code 0} for unconditional update
   * @return number of rows affected (0 or 1)
   */
  int markAsDownloaded(@Param("asset") Asset asset, @Param("intervalSeconds") long intervalSeconds);

  /**
   * Deletes an asset from the content data store.
   *
   * @param asset the asset to delete
   * @return {@code true} if the asset was deleted
   */
  boolean deleteAsset(Asset asset);

  /**
   * Hard-deletes an asset only if it is still soft-deleted ({@code deleted_on IS NOT NULL}). Used by the
   * manual Trash purge so a restore landing between selection and delete leaves the now-live asset intact
   * (the guarded delete affects 0 rows).
   *
   * @param asset the asset to purge
   * @return {@code true} if the asset was purged; {@code false} if it was no longer soft-deleted
   */
  boolean purgeDeletedAsset(Asset asset);

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
   * @since 3.26
   */
  int[] selectNotRecentlyDownloaded(
      @Param("repositoryId") int repositoryId,
      @Param("daysAgo") int daysAgo,
      @Param("limit") int limit,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Purges the selected assets.
   * <p>
   * This version of the method is for databases that support primitive arrays.
   *
   * @param assetIds the assets to purge
   * @param updateComponentEntityVersion whether to update the component entity version
   * @return the number of purged assets
   * @since 3.26
   */
  int purgeSelectedAssets(
      @Param("assetIds") int[] assetIds,
      @Param("updateComponentEntityVersion") boolean updateComponentEntityVersion);

  /**
   * Purges the selected assets.
   * <p>
   * This version of the method is for databases that don't yet support primitive arrays.
   *
   * @param assetIds the assets to purge
   * @return the number of purged assets
   * @since 3.26
   */
  int purgeSelectedAssets(@Param("assetIds") Integer[] assetIds);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   * <p>
   * Sets the created time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void created(@Param("assetId") int assetId, @Param("created") OffsetDateTime created);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   * <p>
   * Sets the last download time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void lastDownloaded(@Param("assetId") int assetId, @Param("lastDownloaded") OffsetDateTime lastDownloaded);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   * <p>
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
  int[] selectComponentIds(
      @Param("assetIds") int[] assetIds,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);

  /**
   * Marks a single asset as soft-deleted.
   *
   * @param assetId the internal id of the asset
   * @param deletedBy the user who performed the deletion
   */
  void softDeleteAsset(
      @Param("assetId") int assetId,
      @Param("deletedBy") String deletedBy);

  /**
   * Restores a single soft-deleted asset by clearing deletion metadata.
   *
   * @param assetId the internal id of the asset
   */
  void undoSoftDeleteAsset(@Param("assetId") int assetId);

  /**
   * Cascade soft-deletes all assets belonging to a component.
   *
   * @param componentId the internal id of the component whose assets to soft-delete
   * @param deletedBy the user who performed the deletion
   */
  void softDeleteComponentAssets(
      @Param("componentId") int componentId,
      @Param("deletedBy") String deletedBy);

  /**
   * Re-stamps assets that were individually soft-deleted during a folder delete operation,
   * aligning their deleted_on with the component's timestamp so they are hidden in Trash.
   * Only affects assets deleted at or after folderDeleteStarted (preserving true individual deletes).
   *
   * @param componentId the internal id of the component
   * @param deletedBy the user who performed the deletion
   * @param folderDeleteStarted only re-stamp assets soft-deleted at or after this instant
   */
  void reSoftDeleteComponentAssets(
      @Param("componentId") int componentId,
      @Param("deletedBy") String deletedBy,
      @Param("folderDeleteStarted") OffsetDateTime folderDeleteStarted);

  /**
   * Restores (clears {@code deleted_on}/{@code deleted_by} from) <em>all</em> soft-deleted assets of a
   * component. Under the subsume-all model the component owns every one of its soft-deleted assets, so
   * restoring the component restores them all together -- there is no timestamp guard, which would otherwise
   * strand an asset soft-deleted before the component in an invisible soft-deleted state.
   *
   * @param componentId the internal id of the component whose assets to restore
   */
  void undoSoftDeleteComponentAssets(@Param("componentId") int componentId);

  /**
   * Aligns the {@code deleted_on} of every soft-deleted asset of a component with the component's own
   * {@code deleted_on}. Used when the last live asset's deletion cascades the component into Trash: assets
   * that were soft-deleted individually beforehand predate the component's timestamp, so without this they
   * would be left behind on component restore (an empty shell) and shown as separate Trash entries. Aligning
   * them makes the whole component a single cascade group -- hidden together in Trash and restored together
   * (NEXUS-53984).
   *
   * @param componentId the internal id of the component
   */
  void alignSoftDeletedAssetsWithComponent(@Param("componentId") int componentId);

  /**
   * Reads a single soft-deleted asset by its internal ID.
   *
   * @param assetId the internal id of the asset
   * @return the soft-deleted asset if found
   */
  Optional<Asset> readDeletedAsset(@Param("assetId") int assetId);

  /**
   * Browse soft-deleted assets with optional repository filter.
   *
   * @param repositoryId optional repository to filter by
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of soft-deleted assets
   */
  Continuation<Asset> browseDeletedAssets(
      @Nullable @Param("repositoryId") Integer repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken);

  /**
   * Browse <em>all</em> soft-deleted assets (including those cascade-deleted alongside their component),
   * with optional repository filter. Unlike {@link #browseDeletedAssets}, which hides cascade-deleted
   * assets for the Trash UI, this returns every asset with {@code deleted_on IS NOT NULL} so callers such
   * as format GC can protect blobs/manifests still referenced by soft-deleted (restorable) content.
   *
   * @param repositoryId optional repository to filter by
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of all soft-deleted assets (including cascade-deleted ones)
   */
  Continuation<Asset> browseAllDeletedAssets(
      @Nullable @Param("repositoryId") Integer repositoryId,
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken);

  /**
   * Count soft-deleted assets with optional repository filter.
   *
   * @param repositoryId optional repository to filter by
   * @return count of soft-deleted assets
   */
  int countDeletedAssets(@Nullable @Param("repositoryId") Integer repositoryId);

  /**
   * Hard-delete assets that were soft-deleted before the given cutoff.
   *
   * @param cutoff purge assets soft-deleted strictly before this instant
   * @param limit maximum number of rows to delete in this batch ({@code <= 0} means no limit)
   * @return the number of assets purged
   */
  int purgeDeletedAssetsBefore(
      @Param("cutoff") OffsetDateTime cutoff,
      @Param("limit") int limit);

  /**
   * Checks for existence of asset in all three: <format>_asset, <format>_asset_blob, <format>_component tables
   *
   * @param blobRef the blob reference
   * @param path
   * @param repository
   * @return {@code true} if asset exists in all three: <format>_asset, <format>_asset_blob, <format>_component tables
   */
  boolean assetRecordsExist(
      @Param("blobRef") BlobRef blobRef,
      final @Param("path") String path,
      final @Param("repository") String repository,
      @Param(SOFT_DELETE_AVAILABLE) boolean softDeleteAvailable);
}
