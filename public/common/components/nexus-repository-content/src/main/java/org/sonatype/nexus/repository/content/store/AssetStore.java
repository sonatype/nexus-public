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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDownloadedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetRestoredEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPathChangedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetKindEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPreDeleteEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPrePurgeEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeletedEvent;
import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;
import org.sonatype.nexus.transaction.Transactional;

import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static java.util.Objects.requireNonNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.repository.content.AttributesHelper.applyAttributeChange;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;

/**
 * {@link Asset} store.
 *
 * @since 3.21
 */
public class AssetStore<T extends AssetDAO>
    extends ContentStoreEventSupport<T>
{
  private static final int LAST_UPDATED_LIMIT = 1000;

  private final boolean clustered;

  private GlobalRepositorySettings globalRepositorySettings;

  private TrashSchemaState trashSchemaState;

  @Autowired
  public AssetStore(
      final DataSessionSupplier sessionSupplier,
      @Value(DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE) final boolean clustered,
      final String contentStoreName,
      final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
    this.clustered = clustered;
  }

  /** See {@code ComponentStore#setTrashSchemaState}. */
  @Autowired(required = false)
  public void setTrashSchemaState(final TrashSchemaState trashSchemaState) {
    this.trashSchemaState = trashSchemaState;
  }

  protected boolean trashAvailable() {
    // Read/insert filtering keys on schema presence, NOT the feature flag: turning Trash off must not resurrect
    // already-Trashed content, so the deleted_on IS NULL filter persists as long as the columns exist
    // (NEXUS-55233). The flag still gates delete-routing (see ContentDeletionService) and Trash UI/REST.
    return trashSchemaState != null && trashSchemaState.isSchemaReached();
  }

  @Autowired
  public void setGlobalRepositorySettings(final GlobalRepositorySettings globalRepositorySettings) {
    this.globalRepositorySettings = requireNonNull(globalRepositorySettings);
  }

  /**
   * Count all assets in the given repository.
   *
   * @param repositoryId the repository to count
   * @param kind optional kind of assets to count
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return count of assets in the repository
   */
  @Transactional
  public int countAssets(
      final int repositoryId,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return dao().countAssets(repositoryId, kind, filter, filterParams, trashAvailable());
  }

  /**
   * Browse all assets in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param continuationToken optional token to continue from a previous request
   * @param kind optional kind of assets to return
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @param limit maximum number of assets to return
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Asset> browseAssets(
      final int repositoryId,
      @Nullable final String continuationToken,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams,
      final int limit)
  {
    return dao().browseAssets(repositoryId, limit, continuationToken, kind, filter, filterParams,
        trashAvailable());
  }

  /**
   * Browse all assets with corresponding components and blobs in the given repository in a paged fashion.
   * The returned assets will be sorted by asset id in ascending order.
   *
   * @param repositoryId the repository to browse
   * @param continuationToken optional token to continue from a previous request
   * @param limit maximum number of assets to return
   * @return collection of assets and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Asset> browseEagerAssets(
      final int repositoryId,
      @Nullable final String continuationToken,
      final int limit)
  {
    AssetBlobCreatedContinuationToken token = AssetBlobCreatedContinuationToken.decode(continuationToken);
    return dao().browseEagerAssetsInRepository(repositoryId, token, limit, null, null, trashAvailable());
  }

  /**
   * Browse all assets with corresponding components and blobs in the given repository in a paged fashion,
   * with optional timestamp filtering. The returned assets will be sorted by blob_created descending,
   * asset_id ascending for stable pagination.
   *
   * <p>
   * <strong>NOTE:</strong> This method is currently only utilized by migration asset queries and polling operations
   * that require assets ordered by blob creation timestamp in descending order (newest first). It uses a composite
   * continuation token containing both blob_created timestamp and asset_id for stable pagination across pages.
   *
   * @param repositoryId the repository to browse
   * @param continuationToken optional composite token (blob_created + asset_id) to continue from a previous request
   * @param limit maximum number of assets to return
   * @param newerThan optional timestamp to filter assets with blob_created after this time
   * @param olderThan optional timestamp to filter assets with blob_created before this time
   * @return collection of assets and the next continuation token
   * @see Continuation#nextContinuationToken()
   * @see AssetBlobCreatedContinuationToken
   */
  @Transactional
  public Continuation<Asset> browseEagerAssets(
      final int repositoryId,
      @Nullable final String continuationToken,
      final int limit,
      @Nullable final OffsetDateTime newerThan,
      @Nullable final OffsetDateTime olderThan)
  {
    AssetBlobCreatedContinuationToken token = AssetBlobCreatedContinuationToken.decode(continuationToken);
    return dao().browseEagerAssetsInRepository(repositoryId, token, limit, newerThan, olderThan, trashAvailable());
  }

  /**
   * Browse all assets in the given repositories in a paged fashion. The returned assets will be sorted
   * by asset id in ascending order.
   *
   * @param repositoryIds the repositories to browse
   * @param continuationToken optional token to continue from a previous request
   * @param kind optional kind of assets to return
   * @param filter optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @param limit maximum number of assets to return
   * @return collection of assets and the next continuation token
   * @see Continuation#nextContinuationToken()
   *
   * @since 3.27
   */
  @Transactional
  public Continuation<Asset> browseAssets(
      final Set<Integer> repositoryIds,
      @Nullable final String continuationToken,
      @Nullable final String kind,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams,
      final int limit)
  {
    return dao().browseAssetsInRepositories(repositoryIds, continuationToken, kind, filter, filterParams, limit,
        trashAvailable());
  }

  /**
   * Browse all assets associated with the given logical component.
   *
   * @param component the component to browse
   * @return collection of assets
   */
  @Transactional
  public Collection<Asset> browseComponentAssets(final Component component) {
    return dao().browseComponentAssets(component, trashAvailable());
  }

  @Transactional
  public Collection<Asset> browseAllComponentAssets(final Component component) {
    return dao().browseAllComponentAssets(component);
  }

  /**
   * Browse all assets associated with a component, including trashed.
   */
  @Transactional
  public Collection<Asset> browseAllComponentAssetsWithTrash(final Component component) {
    return dao().browseAllComponentAssetsWithTrash(component);
  }

  /**
   * Find updated assets. The paging works differently because results are sorted by addedToRepository instead of id.
   * Page
   * through results by passing the addedToRepository value from the last record in the Collection.
   *
   * @param repositoryId the repository to browse
   * @param addedToRepository addedToRepository of asset content from the last record of the previous call.
   * @param regexList list of wildcard expressions to match on path.
   *          Supported special characters are * and ?
   * @param batchSize how many assets to fetch in each call. May return more assets than this if there are
   *          multiple assets with the same addedToRepository value as the last record.
   * @return batch of updated assets
   */
  @Transactional
  public List<AssetInfo> findUpdatedAssets(
      final int repositoryId,
      @Nullable final OffsetDateTime addedToRepository,
      final List<String> regexList,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams,
      final int batchSize)
  {
    // We consider dates the same if they are at the same millisecond. Normalization of the date plus using a >= query
    // has
    // the effect of doing a > query as if the data in the database was truncated to the millisecond.
    OffsetDateTime addedToRepositoryNormalized = null;
    if (addedToRepository != null) {
      addedToRepositoryNormalized = addedToRepository.plus(1, ChronoUnit.MILLIS).truncatedTo(ChronoUnit.MILLIS);
    }

    // Fetch one extra record to check if there are more results with the same addedToRepository value. Most of the time
    // this won't be the case, and we will not need a query to find them all.
    List<AssetInfo> assets =
        dao().findGreaterThanOrEqualToAddedToRepository(repositoryId, addedToRepositoryNormalized, regexList,
            filter, filterParams, batchSize + 1, trashAvailable());

    if (assets.size() == batchSize + 1) {
      if (hasMoreResultsWithSameBlobCreated(assets)) {
        Set<String> knownPaths = assets.stream().map(AssetInfo::path).collect(Collectors.toSet());
        AssetInfo lastAsset = assets.get(assets.size() - 1);

        OffsetDateTime startAddedToRepository = lastAsset.addedToRepository().truncatedTo(ChronoUnit.MILLIS);
        OffsetDateTime endAddedToRepository = lastAsset.addedToRepository().plus(1, ChronoUnit.MILLIS);

        // Add all records that match the timestamp (truncating to millisecond) of the last record. Then we can continue
        // paging with a greater than query.
        List<AssetInfo> matchAddedToRepository =
            dao().findAddedToRepositoryWithinRange(repositoryId, startAddedToRepository, endAddedToRepository,
                regexList, filter, filterParams, LAST_UPDATED_LIMIT, trashAvailable());

        if (matchAddedToRepository.size() == LAST_UPDATED_LIMIT) {
          log.error(
              "Found {} assets with identical last_updated value. Replication is skipping over additional assets with last_updated = {}",
              LAST_UPDATED_LIMIT, lastAsset.addedToRepository());
        }

        assets.addAll(
            matchAddedToRepository.stream()
                .filter(asset -> !knownPaths.contains(asset.path()))
                .collect(Collectors.toList()));
      }
      else {
        // It's not safe to leave the extra record in. There may be more assets with same addedToRepository value as it.
        assets.remove(assets.size() - 1);
      }
    }

    return assets;
  }

  private boolean hasMoreResultsWithSameBlobCreated(final List<AssetInfo> assets) {
    OffsetDateTime lastAddedToRepository =
        assets.get(assets.size() - 1).addedToRepository().truncatedTo(ChronoUnit.MILLIS);

    OffsetDateTime secondToLastAddedToRepository =
        assets.get(assets.size() - 2).addedToRepository().truncatedTo(ChronoUnit.MILLIS);

    return lastAddedToRepository.equals(secondToLastAddedToRepository);
  }

  /**
   * Creates the given asset in the content data store.
   *
   * @param asset the asset to create
   */
  @Transactional
  public void createAsset(final AssetData asset) {
    dao().createAsset(asset, clustered, trashAvailable());

    postCommitEvent(() -> new AssetCreatedEvent(asset));
  }

  /**
   * Retrieves an asset from the content data store.
   *
   * @param assetId the internalId of the asset
   * @return asset if it was found
   */
  @Transactional
  public Optional<Asset> readAsset(final int assetId) {
    return dao().readAsset(assetId, trashAvailable());
  }

  /**
   * Retrieves an asset located at the given path in the content data store.
   *
   * @param repositoryId the repository containing the asset
   * @param path the path of the asset
   * @return asset if it was found
   */
  @Transactional
  public Optional<Asset> readPath(final int repositoryId, final String path) {
    return dao().readPath(repositoryId, path, trashAvailable());
  }

  /**
   * Retrieves an asset associated with the given blob ref.
   *
   * @param repositoryId the repository containing the asset
   * @param blobRef the blob ref
   * @return asset if it was found
   * @since 3.32
   */
  @Transactional
  public Optional<Asset> findByBlobRef(final int repositoryId, final BlobRef blobRef) {
    return dao().findByBlobRef(repositoryId, blobRef, trashAvailable());
  }

  /**
   * Retrieves an assets associated with the given component ids.
   *
   * @param componentIds a set of component ids to search
   * @param assetFilter optional filter to apply.
   * @param assetFilterParams parameter map for the optional filter.
   * @param includeNullBlobs whether to include assets without associated blobs (uses LEFT JOIN instead of INNER JOIN)
   * @return collection of {@link AssetInfo}
   */
  @Transactional
  public Collection<Asset> findByComponentIds(
      final Set<Integer> componentIds,
      final String assetFilter,
      final Map<String, Object> assetFilterParams,
      final boolean includeNullBlobs)
  {
    if (CollectionUtils.isEmpty(componentIds)) {
      return List.of();
    }

    return dao().findByComponentIds(componentIds, assetFilter, assetFilterParams, includeNullBlobs,
        trashAvailable());
  }

  /**
   * Updates the kind of the given asset in the content data store.
   *
   * @param asset the asset to update
   *
   * @since 3.25
   */
  @Transactional
  public void updateAssetKind(final Asset asset) {
    dao().updateAssetKind(asset, clustered);

    postCommitEvent(() -> new AssetKindEvent(asset));
  }

  /**
   * Updates the path of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetPath(final Asset asset) {
    String oldPath = dao().readAsset(InternalIds.internalAssetId(asset), trashAvailable())
        .map(Asset::path)
        .orElse(null);

    dao().updateAssetPath(asset, clustered);

    postCommitEvent(() -> new AssetPathChangedEvent(asset, oldPath, asset.path()));
  }

  /**
   * Updates the attributes of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetAttributes(
      final Asset asset,
      final AttributeChangeSet changeSet)
  {
    // Reload latest attributes, apply change, then update database if necessary. This read must NOT filter out
    // trashed assets (pass false, not trashAvailable()): unlike normal reads, this is a targeted
    // update of a specific asset by id, and stash-and-reapply formats (npm custom dist-tags + deprecated, Cargo
    // registry index line/yanked) write a preservation attribute onto an asset that has just been trashed.
    // Filtering by deleted_on IS NULL here would make readAssetAttributes return empty and silently drop that
    // write, losing the stashed metadata on the asset-level trash/restore path (NEXUS-53911/54381/54382).
    dao().readAssetAttributes(asset, false).ifPresent(attributes -> {
      ((AssetData) asset).setAttributes(attributes);

      boolean changesApplied = changeSet.getChanges()
          .stream()
          .map(change -> applyAttributeChange(attributes, change))
          .reduce((a, b) -> a || b)
          .orElse(false);
      if (changesApplied) {
        dao().updateAssetAttributes(asset, clustered);

        postCommitEvent(() -> new AssetAttributesEvent(asset, changeSet.getChanges()));
      }
    });
  }

  /**
   * Updates the link between the given asset and its {@link AssetBlob} in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetBlobLink(final Asset asset) {
    dao().updateAssetBlobLink(asset, clustered);

    postCommitEvent(() -> new AssetUploadedEvent(asset));
  }

  /**
   * Updates the last downloaded time of the given asset, throttled by the configured
   * lastDownloaded interval. Returns whether the database row actually changed — callers should
   * use this to gate downstream work like blob attribute file writes.
   *
   * @param asset the asset to update
   * @return {@code true} if the asset's last_downloaded was updated; {@code false} if the
   *         throttle skipped the update (i.e. another caller updated within the interval)
   */
  @Transactional
  public boolean markAsDownloaded(final Asset asset) {
    long intervalSeconds = globalRepositorySettings.getLastDownloadedInterval().toSeconds();
    int rows = dao().markAsDownloaded(asset, intervalSeconds);
    if (rows > 0) {
      postCommitEvent(() -> new AssetDownloadedEvent(asset));
    }
    return rows > 0;
  }

  /**
   * Deletes an asset from the content data store.
   *
   * @param asset the asset to delete
   * @return {@code true} if the asset was deleted
   */
  @Transactional
  public boolean deleteAsset(final Asset asset) {
    preCommitEvent(() -> new AssetPreDeleteEvent(asset));
    boolean deleted = dao().deleteAsset(asset);

    if (deleted) {
      asset.component()
          .ifPresent(component -> dao().updateEntityVersion(internalComponentId(component), clustered));
      postCommitEvent(() -> new AssetDeletedEvent(asset));
    }
    return deleted;
  }

  /**
   * Purges (hard-deletes) an asset only if it is still trashed. Used by the manual Trash purge to
   * avoid a read-then-delete race: if the asset is restored between selection and purge, {@code deleted_on}
   * is cleared, the guarded delete removes 0 rows, and the now-live asset is preserved -- no
   * {@link AssetDeletedEvent} (and therefore no blob reclamation) fires for the surviving asset.
   *
   * @return {@code true} if the asset was purged; {@code false} if it was no longer trashed
   */
  @Transactional
  public boolean purgeDeletedAsset(final Asset asset) {
    boolean deleted = dao().purgeDeletedAsset(asset);

    // Only fire delete events when a row was actually purged: on a race-lost no-op (the asset was restored
    // before this ran) we must not fire AssetPreDeleteEvent/AssetDeletedEvent for the surviving live asset,
    // which would reclaim its blob. Registration order within the transaction is irrelevant -- both hooks
    // still fire pre-/post-commit, exactly as in deleteAsset.
    if (deleted) {
      preCommitEvent(() -> new AssetPreDeleteEvent(asset));
      asset.component()
          .ifPresent(component -> dao().updateEntityVersion(internalComponentId(component), clustered));
      postCommitEvent(() -> new AssetDeletedEvent(asset));
    }
    return deleted;
  }

  /**
   * Deletes the asset located at the given path in the content data store.
   *
   * @param repositoryId the repository containing the asset
   * @param path the path of the asset
   * @return {@code true} if the asset was deleted
   */
  @Transactional
  public boolean deletePath(final int repositoryId, final String path) {
    return dao().readPath(repositoryId, path, trashAvailable())
        .map(this::deleteAsset)
        .orElse(false);
  }

  /**
   * Deletes the assets located at the given paths in the content data store.
   *
   * @param repositoryId the repository containing the assets
   * @param paths the paths of the assets to delete
   * @return number of assets deleted
   */
  @Transactional
  public int deleteAssetsByPaths(final int repositoryId, final List<String> paths) {
    if (paths.isEmpty()) {
      return 0;
    }

    Collection<Asset> assets = dao().readPathsFromRepository(repositoryId, paths, trashAvailable());

    if (assets.isEmpty()) {
      return 0;
    }

    for (Asset asset : assets) {
      asset.blob(); // force lazy-load blob while the session is open
    }
    int[] assetIds = assets.stream().mapToInt(InternalIds::internalAssetId).toArray();
    List<Asset> assetList = new ArrayList<>(assets);
    preCommitEvent(() -> new AssetPrePurgeEvent(repositoryId, assetIds));
    postCommitEvent(() -> new AssetPurgedEvent(repositoryId, assetIds, assetList));

    int[] componentIds = new int[0];
    if (clustered) {
      componentIds = Arrays.stream(dao().selectComponentIds(assetIds, trashAvailable())).distinct().toArray();
    }

    int count = purgeAssets(assetIds);
    if (clustered && componentIds.length > 0) {
      dao().updateEntityVersions(componentIds, clustered);
    }
    return count;
  }

  /**
   * Deletes all assets in the given repository from the content data store.
   *
   * Events will not be sent for these deletes, instead listen for {@link ContentRepositoryDeletedEvent}.
   *
   * @param repositoryId the repository containing the assets
   * @return {@code true} if any assets were deleted
   */
  @Transactional
  public boolean deleteAssets(final int repositoryId) {
    log.debug("Deleting all assets in repository {}", repositoryId);
    boolean deleted = false;
    while (dao().deleteAssets(repositoryId, deleteBatchSize())) {
      commitChangesSoFar();
      deleted = true;
    }
    log.debug("Deleted all assets in repository {}", repositoryId);
    return deleted;
  }

  /**
   * Purge assets without a component in the given repository last downloaded more than given number of days ago
   *
   * @param repositoryId the repository to check
   * @param daysAgo the number of days ago to check
   * @return number of purged assets
   *
   * @since 3.24
   */
  @Transactional
  public int purgeNotRecentlyDownloaded(final int repositoryId, final int daysAgo) {
    int purged = 0;
    while (true) {
      int[] assetIds =
          dao().selectNotRecentlyDownloaded(repositoryId, daysAgo, deleteBatchSize(), trashAvailable());
      if (assetIds.length == 0) {
        break; // nothing left to purge
      }
      List<Asset> assetsForAudit = readAssetsForAudit(assetIds);
      preCommitEvent(() -> new AssetPrePurgeEvent(repositoryId, assetIds));
      purged += purgeAssets(assetIds);
      postCommitEvent(() -> new AssetPurgedEvent(repositoryId, assetIds, assetsForAudit));

      commitChangesSoFar();
    }
    return purged;
  }

  private List<Asset> readAssetsForAudit(final int[] assetIds) {
    List<Asset> assets = new ArrayList<>(assetIds.length);
    for (int assetId : assetIds) {
      // trashAvailable() always returns false; hard-purge targets are still readable here
      dao().readAsset(assetId, trashAvailable()).ifPresent(asset -> {
        asset.blob(); // force lazy-load blob while the session is open
        assets.add(asset);
      });
    }
    return assets;
  }

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the created time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  @Transactional
  public void created(final Asset asset, final OffsetDateTime created) {
    dao().created(InternalIds.internalAssetId(asset), created);
  }

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the last download time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  @Transactional
  public void lastDownloaded(final Asset asset, final OffsetDateTime lastDownloaded) {
    dao().lastDownloaded(InternalIds.internalAssetId(asset), lastDownloaded);
  }

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the last updated time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  @Transactional
  public void lastUpdated(final Asset asset, final OffsetDateTime lastUpdated) {
    dao().lastUpdated(InternalIds.internalAssetId(asset), lastUpdated);
  }

  /**
   * Checks for existence of asset in all three: <format>_asset, <format>_asset_blob, <format>_component tables.
   * <p>
   * Delegates with {@code trashAvailable=false} so the {@code deleted_on IS NULL} predicate is omitted, i.e. a
   * present-but-trashed asset (its {@code deleted_on} is set) counts as existing. This is the behavior required by
   * the sole caller, blob-store reconciliation (Data Repair): reconcile must NOT treat a trashed asset as a missing
   * DB record. Filtering trashed rows out here would, on a Trash-enabled instance, make a trashed asset whose blob
   * is intact read as absent and be classified BLOB_RECORD_MISSING -> RECREATE_RECORDS, which on execute clears
   * {@code deleted_on} and silently resurrects (un-trashes) content the user deleted (NEXUS-55233).
   *
   * @param blobRef the blob reference
   * @return {@code true} if a matching asset record exists, including one that is currently trashed
   */
  @Transactional
  public Boolean assetRecordsExist(final BlobRef blobRef, final String path, final String repository) {
    return dao().assetRecordsExist(blobRef, path, repository, false);
  }

  /**
   * Trashs an asset by marking it with deletion metadata.
   */
  @Transactional
  public void trashAsset(final Asset asset, final String deletedBy) {
    int assetId = InternalIds.internalAssetId(asset);
    dao().trashAsset(assetId, deletedBy);
    postCommitEvent(() -> new AssetDeletedEvent(asset));
  }

  /**
   * Restores a trashed asset by clearing deletion metadata.
   */
  @Transactional
  public int undoTrashAsset(final Asset asset) {
    int assetId = InternalIds.internalAssetId(asset);
    int restored = dao().undoTrashAsset(assetId);
    // Only fire the restore event when a row was actually cleared. On a lost restore/purge race the asset was
    // already purged (0 rows), so firing AssetRestoredEvent -- and its webhook/audit/metadata side effects --
    // for a now-nonexistent asset would be misleading (NEXUS-55233).
    if (restored > 0) {
      postCommitEvent(() -> new AssetRestoredEvent(asset));
    }
    return restored;
  }

  /**
   * Cascade trashs all assets belonging to a component.
   */
  @Transactional
  public void trashComponentAssets(final Component component, final String deletedBy) {
    int componentId = internalComponentId(component);
    dao().trashComponentAssets(componentId, deletedBy);
  }

  /**
   * Re-stamps assets individually trashed during a folder delete, aligning their timestamps
   * with the component so they are hidden in Trash. Preserves true pre-existing individual deletes.
   */
  @Transactional
  public void reTrashComponentAssets(
      final Component component,
      final String deletedBy,
      final OffsetDateTime folderDeleteStarted)
  {
    int componentId = internalComponentId(component);
    dao().reTrashComponentAssets(componentId, deletedBy, folderDeleteStarted);
  }

  /**
   * Restores all trashed assets of a component. Under the subsume-all model the component owns every one
   * of its trashed assets (including any deleted individually before the component), so they are all
   * restored together with the component -- no timestamp guard, which would strand the earlier-deleted ones.
   */
  @Transactional
  public void undoTrashComponentAssets(final Component component) {
    int componentId = internalComponentId(component);
    dao().undoTrashComponentAssets(componentId);
  }

  /**
   * Aligns all trashed assets of a component with the component's own deleted_on (see
   * {@link AssetDAO#alignTrashedAssetsWithComponent}). Used on the last-asset cascade so a component
   * emptied piece-by-piece is treated as one cascade delete and restores as a whole (NEXUS-53984).
   */
  @Transactional
  public void alignTrashedAssetsWithComponent(final Component component) {
    int componentId = internalComponentId(component);
    dao().alignTrashedAssetsWithComponent(componentId);
  }

  /**
   * Reads a single trashed asset by its internal ID.
   */
  @Transactional
  public Optional<Asset> readDeletedAsset(final int assetId) {
    return dao().readDeletedAsset(assetId);
  }

  /**
   * Browse trashed assets with optional repository filter.
   */
  @Transactional
  public Continuation<Asset> browseDeletedAssets(
      @Nullable final Integer repositoryId,
      final int limit,
      @Nullable final String continuationToken)
  {
    return dao().browseDeletedAssets(repositoryId, limit, continuationToken);
  }

  /**
   * Browse all trashed assets including cascade-deleted ones (see
   * {@link AssetDAO#browseAllDeletedAssets}). Used by format GC to protect content still referenced by
   * trashed (restorable) components.
   */
  @Transactional
  public Continuation<Asset> browseAllDeletedAssets(
      @Nullable final Integer repositoryId,
      final int limit,
      @Nullable final String continuationToken)
  {
    // NEXUS-54969: unlike the other trash store methods (only reached from the trash plugin or
    // the flag-gated DeleteFolderServiceImpl branch), this is called by Docker/OCI format GC on EVERY run,
    // independent of the trash feature flag. With trash disabled the deleted_on column may not
    // exist, so short-circuit here to avoid a "column deleted_on does not exist" failure during GC.
    if (!trashAvailable()) {
      return emptyContinuation();
    }
    return dao().browseAllDeletedAssets(repositoryId, limit, continuationToken);
  }

  /**
   * Count trashed assets with optional repository filter.
   */
  @Transactional
  public int countDeletedAssets(@Nullable final Integer repositoryId) {
    return dao().countDeletedAssets(repositoryId);
  }

  /**
   * Hard-delete assets that were trashed before the given cutoff.
   */
  @Transactional
  public int purgeDeletedAssetsBefore(final OffsetDateTime cutoff) {
    return dao().purgeDeletedAssetsBefore(cutoff, deleteBatchSize());
  }

  private int purgeAssets(final int[] assetIds) {
    if ("H2".equals(thisSession().sqlDialect())) {
      // workaround lack of primitive array support in H2 (should be fixed in H2 1.4.201?)
      return dao().purgeSelectedAssets(stream(assetIds).boxed().toArray(Integer[]::new));
    }
    else {
      return dao().purgeSelectedAssets(assetIds, clustered);
    }
  }

  /**
   * Empty {@link Continuation} returned by the deleted-asset browse methods when trash is disabled
   * (NEXUS-54969), so callers iterate zero results without issuing SQL that references {@code deleted_on}.
   */
  private static Continuation<Asset> emptyContinuation() {
    return new EmptyContinuation<>();
  }

  private static final class EmptyContinuation<E>
      extends ArrayList<E>
      implements Continuation<E>
  {
    @Override
    public String nextContinuationToken() {
      return null;
    }
  }
}
