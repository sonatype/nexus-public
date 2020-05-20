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
}
