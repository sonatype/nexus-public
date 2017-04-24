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
package org.sonatype.nexus.repository.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Encapsulates the operation of deleting a bucket and its contents. Used by {@code StorageFacetManagerImpl}.
 *
 * @since 3.2.1
 */
@Named
@Singleton
public class BucketDeleter
    extends ComponentSupport
{
  private static final long DELETE_BATCH_SIZE = 100L;

  private final Provider<DatabaseInstance> databaseInstanceProvider;

  private final BucketEntityAdapter bucketEntityAdapter;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  private final BlobStoreManager blobStoreManager;

  @Inject
  public BucketDeleter(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstanceProvider,
                       final BucketEntityAdapter bucketEntityAdapter,
                       final ComponentEntityAdapter componentEntityAdapter,
                       final AssetEntityAdapter assetEntityAdapter,
                       final BlobStoreManager blobStoreManager) {
    this.databaseInstanceProvider = checkNotNull(databaseInstanceProvider);
    this.bucketEntityAdapter = checkNotNull(bucketEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  /**
   * Deletes an existing bucket and all components and assets within.
   *
   * NOTE: This is a potentially long-lived and non-atomic operation. Items within the bucket will be
   * sequentially deleted in batches in order to keep memory use within reason. This method will automatically
   * commit a transaction for each batch, and will return after committing the last batch.
   */
  public void deleteBucket(final Bucket bucket) throws InterruptedException {
    checkNotNull(bucket);
    inTxRetry(databaseInstanceProvider).throwing(InterruptedException.class).run(db -> {

      long count = 0;

      List<BlobRef> deletedBlobs = new ArrayList<>();
      Set<String> deletedBlobStores = new HashSet<>();

      // first delete all components and constituent assets
      for (Component component : componentEntityAdapter.browseByBucket(db, bucket)) {
        deleteComponent(db, deletedBlobs, component);
        count++;
        if (count == DELETE_BATCH_SIZE) {
          commitBatch(db, deletedBlobs, deletedBlobStores);
          count = 0;
          if (Thread.interrupted()) {
            throw new InterruptedException();
          }
        }
      }
      commitBatch(db, deletedBlobs, deletedBlobStores);

      // then delete all standalone assets
      for (Asset asset : assetEntityAdapter.browseByBucket(db, bucket)) {
        deleteAsset(db, deletedBlobs, asset);
        count++;
        if (count == DELETE_BATCH_SIZE) {
          commitBatch(db, deletedBlobs, deletedBlobStores);
          count = 0;
          if (Thread.interrupted()) {
            throw new InterruptedException();
          }
        }
      }
      commitBatch(db, deletedBlobs, deletedBlobStores);

      // finally, delete the bucket document
      bucketEntityAdapter.deleteEntity(db, bucket);
      db.commit();
    });
  }

  private void deleteAsset(final ODatabaseDocumentTx db, final List<BlobRef> deletedBlobs, final Asset asset) {
    checkNotNull(db);
    checkNotNull(deletedBlobs);
    checkNotNull(asset);

    BlobRef ref = asset.blobRef();
    if (ref != null) {
      deletedBlobs.add(ref);
    }
    assetEntityAdapter.deleteEntity(db, asset);
  }

  private void deleteComponent(final ODatabaseDocumentTx db,
                               final List<BlobRef> deletedBlobs,
                               final Component component)
  {
    checkNotNull(db);
    checkNotNull(deletedBlobs);
    checkNotNull(component);

    for (Asset asset : assetEntityAdapter.browseByComponent(db, component)) {
      deleteAsset(db, deletedBlobs, asset);
    }
    componentEntityAdapter.deleteEntity(db, component);
  }

  private void commitBatch(final ODatabaseDocumentTx db,
                           final List<BlobRef> deletedBlobs,
                           final Set<String> deletedBlobStores)
  {
    checkNotNull(db);
    checkNotNull(deletedBlobs);
    checkNotNull(deletedBlobStores);

    db.commit();

    for (BlobRef blobRef : deletedBlobs) {
      deleteBlob(deletedBlobStores, blobRef);
    }

    deletedBlobs.clear();
  }

  private void deleteBlob(final Set<String> deletedBlobStores, final BlobRef blobRef) {
    String blobStoreName = blobRef.getStore();
    if (deletedBlobStores.contains(blobStoreName)) {
      return;
    }

    BlobStore blobStore = blobStoreManager.get(blobRef.getStore());
    if (blobStore == null) {
      if (deletedBlobStores.add(blobStoreName)) {
        log.info("Not deleting blobs for blob store {}, blob store not found", blobStoreName);
      }
      return;
    }

    try {
      blobStore.delete(blobRef.getBlobId(), "Deleting Bucket");
    }
    catch (InvalidStateException e) {
      if (deletedBlobStores.add(blobStoreName)) {
        log.info("Not deleting blobs for blob store {}, invalid state {}", blobStoreName, e.getInvalidState(),
            log.isDebugEnabled() ? e : null);
      }
    }
    catch (Exception e) {
      log.warn("Error deleting blob {}, skipping", blobRef, e);
    }
  }
}
