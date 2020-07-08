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
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.inject.assistedinject.Assisted;

/**
 * {@link Asset} store.
 *
 * @since 3.21
 */
@Named
public class AssetStore<T extends AssetDAO>
    extends ContentStoreSupport<T>
{
  @Inject
  public AssetStore(final DataSessionSupplier sessionSupplier,
                    @Assisted final String contentStoreName,
                    @Assisted final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
  }

  /**
   * Count all assets in the given repository.
   *
   * @param repositoryId the repository to count
   * @return count of assets in the repository
   */
  @Transactional
  public int countAssets(final int repositoryId) {
    return dao().countAssets(repositoryId);
  }

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
  @Transactional
  public Continuation<Asset> browseAssets(final int repositoryId,
                                          @Nullable final String kind,
                                          final int limit,
                                          @Nullable final String continuationToken)
  {
    return dao().browseAssets(repositoryId, kind, limit, continuationToken);
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
  }

  /**
   * Retrieves an asset from the content data store.
   *
   * @param repositoryId the repository containing the asset
   * @param path the path of the asset
   * @return asset if it was found
   */
  @Transactional
  public Optional<Asset> readAsset(final int repositoryId, final String path) {
    return dao().readAsset(repositoryId, path);
  }

  /**
   * Updates the kind of the given asset in the content data store.
   *
   * @param asset the asset to update
   *
   * @since 3.25.0
   */
  @Transactional
  public void updateAssetKind(final Asset asset) {
    dao().updateAssetKind(asset);
  }

  /**
   * Updates the attributes of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetAttributes(final Asset asset) {
    dao().updateAssetAttributes(asset);
  }

  /**
   * Updates the link between the given asset and its {@link AssetBlob} in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetBlobLink(final Asset asset) {
    dao().updateAssetBlobLink(asset);
  }

  /**
   * Updates the last downloaded time of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void markAsDownloaded(final Asset asset) {
    dao().markAsDownloaded(asset);
  }

  /**
   * Deletes an asset from the content data store.
   *
   * @param asset the asset to delete
   * @return {@code true} if the asset was deleted
   */
  @Transactional
  public boolean deleteAsset(final Asset asset) {
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
    return dao().deletePath(repositoryId, path);
  }

  /**
   * Deletes all assets in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the assets
   * @return {@code true} if any assets were deleted
   */
  @Transactional
  public boolean deleteAssets(final int repositoryId) {
    log.debug("Deleting all assets in repository {}", repositoryId);
    Transaction tx = UnitOfWork.currentTx();
    boolean deleted = false;
    while (dao().deleteAssets(repositoryId, deleteBatchSize())) {
      tx.commit();
      deleted = true;
      tx.begin();
    }
    log.debug("Deleted all assets in repository {}", repositoryId);
    return deleted;
  }

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
  @Transactional
  public int purgeNotRecentlyDownloaded(final int repositoryId, final int daysAgo, final int limit) {
    return dao().purgeNotRecentlyDownloaded(repositoryId, daysAgo, limit);
  }
}
