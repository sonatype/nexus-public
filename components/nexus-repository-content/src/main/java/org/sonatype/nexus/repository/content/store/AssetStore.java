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
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AttributeChange;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreateEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeleteEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDownloadEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetKindEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgeEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeleteEvent;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.repository.content.AttributesHelper.applyAttributeChange;

/**
 * {@link Asset} store.
 *
 * @since 3.21
 */
@Named
public class AssetStore<T extends AssetDAO>
    extends ContentStoreSupport<T>
{
  private final ContentStoreEventSender eventSender;

  @Inject
  public AssetStore(final DataSessionSupplier sessionSupplier,
                    final ContentStoreEventSender eventSender,
                    @Assisted final String contentStoreName,
                    @Assisted final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
    this.eventSender = checkNotNull(eventSender);
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
  public int countAssets(final int repositoryId,
                         @Nullable final String kind,
                         @Nullable final String filter,
                         @Nullable final Map<String, Object> filterParams)
  {
    return dao().countAssets(repositoryId, kind, filter, filterParams);
  }

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
  @Transactional
  public Continuation<Asset> browseAssets(final int repositoryId,
                                          final int limit,
                                          @Nullable final String continuationToken,
                                          @Nullable final String kind,
                                          @Nullable final String filter,
                                          @Nullable final Map<String, Object> filterParams)
  {
    return dao().browseAssets(repositoryId, limit, continuationToken, kind, filter, filterParams);
  }

  /**
   * Browse all assets associated with the given logical component.
   *
   * @param component the component to browse
   * @return collection of assets
   */
  @Transactional
  public Collection<Asset> browseComponentAssets(final Component component) {
    return dao().browseComponentAssets(component);
  }

  /**
   * Creates the given asset in the content data store.
   *
   * @param asset the asset to create
   */
  @Transactional
  public void createAsset(final AssetData asset) {
    dao().createAsset(asset);

    eventSender.postCommit(
        () -> new AssetCreateEvent(asset));
  }

  /**
   * Retrieves an asset from the content data store.
   *
   * @param assetId the internalId of the asset
   * @return asset if it was found
   */
  @Transactional
  public Optional<Asset> readAsset(final int assetId) {
    return dao().readAsset(assetId);
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
    return dao().readPath(repositoryId, path);
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
    dao().updateAssetKind(asset);

    eventSender.postCommit(
        () -> new AssetKindEvent(asset));
  }

  /**
   * Updates the attributes of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetAttributes(final Asset asset,
                                    final AttributeChange change,
                                    final String key,
                                    final @Nullable Object value)
  {
    // reload latest attributes, apply change, then update database if necessary
    dao().readAssetAttributes(asset).ifPresent(attributes -> {
      ((AssetData) asset).setAttributes(attributes);

      if (applyAttributeChange(attributes, change, key, value)) {
        dao().updateAssetAttributes(asset);

        eventSender.postCommit(
            () -> new AssetAttributesEvent(asset, change, key, value));
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
    dao().updateAssetBlobLink(asset);

    eventSender.postCommit(
        () -> new AssetUploadEvent(asset));
  }

  /**
   * Updates the last downloaded time of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void markAsDownloaded(final Asset asset) {
    dao().markAsDownloaded(asset);

    eventSender.postCommit(
        () -> new AssetDownloadEvent(asset));
  }

  /**
   * Deletes an asset from the content data store.
   *
   * @param asset the asset to delete
   * @return {@code true} if the asset was deleted
   */
  @Transactional
  public boolean deleteAsset(final Asset asset) {
    eventSender.preCommit(
        () -> new AssetDeleteEvent(asset));

    return dao().deleteAsset(asset);
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
    return dao().readPath(repositoryId, path)
        .map(this::deleteAsset)
        .orElse(false);
  }

  /**
   * Deletes all assets in the given repository from the content data store.
   *
   * Events will not be sent for these deletes, instead listen for {@link ContentRepositoryDeleteEvent}.
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
      int[] assetIds = dao().selectNotRecentlyDownloaded(repositoryId, daysAgo, deleteBatchSize());
      if (assetIds.length == 0) {
        break; // nothing left to purge
      }
      if ("H2".equals(thisSession().sqlDialect())) {
        // workaround lack of primitive array support in H2 (should be fixed in H2 1.4.201?)
        purged += dao().purgeSelectedAssets(stream(assetIds).boxed().toArray(Integer[]::new));
      }
      else {
        purged += dao().purgeSelectedAssets(assetIds);
      }

      eventSender.preCommit(
          () -> new AssetPurgeEvent(repositoryId, assetIds));

      commitChangesSoFar();
    }
    return purged;
  }
}
