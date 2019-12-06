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
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.transaction.Transactional;

/**
 * {@link Asset} store.
 *
 * @since 3.next
 */
public abstract class AssetStore<T extends AssetDAO>
    extends ContentStoreSupport<T>
{
  public AssetStore(final DataSessionSupplier sessionSupplier, final String storeName) {
    super(sessionSupplier, storeName);
  }

  /**
   * Browse all assets in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Asset> browseAssets(final int repositoryId,
                                          final int limit,
                                          @Nullable final String continuationToken)
  {
    return dao().browseAssets(repositoryId, limit, continuationToken);
  }

  /**
   * Browse all assets associated with the given logical component.
   *
   * @param component the component to browse
   * @return collection of assets
   */
  @Transactional
  public Collection<Asset> browseComponentAssets(final ComponentData component) {
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
   * Updates the attributes of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetAttributes(final AssetData asset) {
    dao().updateAssetAttributes(asset);
  }

  /**
   * Updates the link between the given asset and its {@link AssetBlob} in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetBlobLink(final AssetData asset) {
    dao().updateAssetBlobLink(asset);
  }

  /**
   * Updates the last downloaded time of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void markAsDownloaded(final AssetData asset) {
    dao().markAsDownloaded(asset);
  }

  /**
   * Deletes an asset from the content data store.
   *
   * @param repositoryId the repository containing the asset
   * @param path the path of the asset
   * @return {@code true} if the asset was deleted
   */
  @Transactional
  public boolean deleteAsset(final int repositoryId, final String path) {
    return dao().deleteAsset(repositoryId, path);
  }

  /**
   * Deletes all assets in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the assets
   * @return {@code true} if any assets were deleted
   */
  @Transactional
  public boolean deleteAssets(final int repositoryId) {
    return dao().deleteAssets(repositoryId);
  }
}
