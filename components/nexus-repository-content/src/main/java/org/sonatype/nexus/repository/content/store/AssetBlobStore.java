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
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

/**
 * {@link AssetBlob} store.
 *
 * @since 3.21
 */
@Named
public class AssetBlobStore<T extends AssetBlobDAO>
    extends ContentStoreSupport<T>
{
  @Inject
  public AssetBlobStore(final DataSessionSupplier sessionSupplier,
                        @Assisted final String contentStoreName,
                        @Assisted final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
  }

  /**
   * Browse unused asset blobs in the content data store in a paged fashion.
   *
   * @param limit maximum number of asset blobs to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of asset blobs and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<AssetBlob> browseUnusedAssetBlobs(final int limit, @Nullable final String continuationToken) {
    return dao().browseUnusedAssetBlobs(limit, continuationToken);
  }

  /**
   * Creates the given asset blob in the content data store.
   *
   * @param assetBlob the asset blob to create
   */
  @Transactional
  public void createAssetBlob(final AssetBlobData assetBlob) {
    dao().createAssetBlob(assetBlob);
  }

  /**
   * Retrieves an asset blob from the content data store.
   *
   * @param blobRef the blob reference
   * @return asset blob if it was found
   */
  @Transactional
  public Optional<AssetBlob> readAssetBlob(final BlobRef blobRef) {
    return dao().readAssetBlob(blobRef);
  }

  /**
   * Deletes an asset blob from the content data store.
   *
   * @param blobRef the blob reference
   * @return {@code true} if the asset blob was deleted
   */
  @Transactional
  public boolean deleteAssetBlob(final BlobRef blobRef) {
    return dao().deleteAssetBlob(blobRef);
  }
}
