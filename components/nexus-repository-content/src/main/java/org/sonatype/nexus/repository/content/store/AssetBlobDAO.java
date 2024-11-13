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
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.AssetBlob;

import org.apache.ibatis.annotations.Param;

/**
 * Asset blob {@link ContentDataAccess}.
 *
 * @since 3.20
 */
@SchemaTemplate("format")
public interface AssetBlobDAO
    extends ContentDataAccess
{
  /**
   * Browse unused asset blobs in the content data store in a paged fashion.
   *
   * @param limit maximum number of asset blobs to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of asset blobs and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  Continuation<AssetBlob> browseUnusedAssetBlobs(
      @Param("limit") int limit,
      @Param("blobCreatedDelayMinute") int blobCreatedDelayMinute,
      @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Browse asset blobs in the content data store in a paged fashion.
   *
   * @param limit maximum number of asset blobs to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of asset blobs and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<AssetBlob> browseAssetBlobs(
      @Param("limit") int limit,
      @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Browse asset blobs in the content data store in a paged fashion.
   *
   * @param limit             maximum number of asset blobs to return
   * @param start             start date
   * @param end               end date
   * @param continuationToken optional token to continue from a previous request
   * @return collection of asset blobs and the next continuation token
   * @see Continuation#nextContinuationToken()
   */
  Continuation<AssetBlob> browseAssetBlobsWithinDuration(
      @Param("limit") int limit,
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end,
      @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Creates the given asset blob in the content data store.
   *
   * @param assetBlob the asset blob to create
   */
  void createAssetBlob(AssetBlobData assetBlob);

  /**
   * Retrieves an asset blob from the content data store.
   *
   * @param blobRef the blob reference
   * @return asset blob if it was found
   */
  Optional<AssetBlob> readAssetBlob(@Param("blobRef") BlobRef blobRef);

  /**
   * Deletes an asset blob from the content data store.
   *
   * @param blobRef the blob reference
   * @return {@code true} if the asset blob was deleted
   */
  boolean deleteAssetBlob(@Param("blobRef") BlobRef blobRef);

  /**
   * Deletes batch of asset blobs from the content data store.
   *
   * @param blobRefIds the array of String with blobRefs
   * @return {@code true} if the asset blob was deleted
   */
  boolean deleteAssetBlobBatch(@Param("blobRefIds") String[] blobRefIds);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value.
   *
   * @since 3.29
   */
  void setBlobCreated(@Param("blobRef") BlobRef blobRef, @Param("blobCreated") OffsetDateTime blobCreated);

  /**
   * Sets added to repository on the asset blob.
   */
  void setAddedToRepository(@Param("blobRef") BlobRef blobRef, @Param("addedToRepository") OffsetDateTime addedToRepository);

  /**
   * Sets the content type on the asset
   * 
   * @param blobRef
   * @param contentType
   */
  void setContentType(@Param("blobRef") BlobRef blobRef, @Param("contentType") String contentType);

  /**
   * Sets the checksums on the asset blob
   *
   * @param blobRef
   * @param checksums
   */
  void setChecksums(@Param("blobRef") BlobRef blobRef, @Param("checksums") Map<String, String> checksums);

  /**
   * Sets the 'created by' on the asset
   */
  void setCreatedBy(@Param("blobRef") BlobRef blobRef, @Param("createdBy") String createdBy);

  /**
   * Sets the 'created by IP' on the asset
   */
  void setCreatedByIP(@Param("blobRef") BlobRef blobRef, @Param("createdByIP") String createdByIP);

  /**
   * Browse asset blobs with legacy blobRef format {@code store-name:blob-id@node-id} in a paged fashion.
   *
   * @param limit maximum number of asset blobs to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of asset blobs and the next continuation token
   */
  Continuation<AssetBlob> browseAssetsWithLegacyBlobRef(
      @Param("limit") int limit,
      @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Update asset blobs in a batch fashion.
   *
   * @param assetBlobs asset blobs for update
   * @return {code true} if asset blobs were updated
   */
  boolean updateBlobRefs(@Param("assetBlobs") Collection<AssetBlob> assetBlobs);

  /**
   * Update asset blob.
   *
   * @param assetBlob asset blob for update
   * @return {code true} if asset blob was updated
   */
  boolean updateBlobRef(@Param("assetBlobData") AssetBlob assetBlob);

  /**
   * Count asset blobs with legacy blobRef format {@code store-name:blob-id@node-id}.
   *
   * @return asset blobs count
   */
  int countNotMigratedAssetBlobs();

  /**
   * Get repository name by blob reference.
   *
   * @param blobRef the blob reference
   * @return the repository name
   */
  String getRepositoryName(@Param("blobRef") BlobRef blobRef);

  /**
   * Get path by blob reference.
   *
   * @param blobRef the blob reference
   * @return the path
   */
  String getPathByBlobRef(@Param("blobRef") BlobRef blobRef);
}
