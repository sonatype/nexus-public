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
package org.sonatype.nexus.repository.storage.internal;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketDeleter;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentDatabase;
import org.sonatype.nexus.repository.storage.MissingBlobException;
import org.sonatype.nexus.repository.storage.StorageFacetManager;
import org.sonatype.nexus.transaction.RetryController;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_PENDING_DELETION;

/**
 * @since 3.2.1
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class StorageFacetManagerImpl
    extends StateGuardLifecycleSupport
    implements StorageFacetManager
{
  private final Provider<DatabaseInstance> databaseInstanceProvider;

  private final BucketEntityAdapter bucketEntityAdapter;

  private final BucketDeleter bucketDeleter;

  @Inject
  public StorageFacetManagerImpl(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstanceProvider,
                                 final BucketEntityAdapter bucketEntityAdapter,
                                 final BucketDeleter bucketDeleter,
                                 final RetryController retryController)
  {
    this.databaseInstanceProvider = checkNotNull(databaseInstanceProvider);
    this.bucketEntityAdapter = checkNotNull(bucketEntityAdapter);
    this.bucketDeleter = checkNotNull(bucketDeleter);

    // extend retry delay when blobs are missing to account for slow blob-stores
    retryController.addAsMajorException(MissingBlobException.class);
  }

  @Override
  @Guarded(by = STARTED)
  public void enqueueDeletion(final Repository repository, final BlobStore blobStore, final Bucket bucket)
  {
    checkNotNull(repository);
    checkNotNull(blobStore);
    checkNotNull(bucket);

    // The bucket associated with repository needs a new "repository name" created for it in order to avoid clashes.
    // Consider what happens if the bucket is still being deleted and someone tries to reuse that repository name.
    String generatedRepositoryName = repository.getName() + '$' + UUID.randomUUID();
    bucket.setRepositoryName(generatedRepositoryName);
    bucket.attributes().set(P_PENDING_DELETION, true);

    updateBucket(bucket);
  }

  @Override
  @Guarded(by = STARTED)
  public long performDeletions() {
    List<Bucket> buckets = findBucketsForDeletion();
    return buckets.stream().filter((bucket) -> {
      try {
        log.info("Deleting bucket for repository {}", bucket.getRepositoryName());
        deleteBucket(bucket);
        return true;
      }
      catch (Exception e) {
        log.warn("Unable to delete bucket with repository name {}, will require manual cleanup",
            bucket.getRepositoryName(), e);
        return false;
      }
    }).count();
  }

  /**
   * Returns a list of buckets queued for deletion.
   */
  private List<Bucket> findBucketsForDeletion() {
    return inTx(databaseInstanceProvider).call(db -> {
      return StreamSupport
          .stream(bucketEntityAdapter.browse(db).spliterator(), false)
          .filter((bucket) -> bucket.attributes().contains(P_PENDING_DELETION))
          .collect(Collectors.toList());
    });
  }

  /**
   * Deletes the specified bucket.
   */
  protected void deleteBucket(final Bucket bucket) throws Exception {
    bucketDeleter.deleteBucket(bucket);
  }

  /**
   * Updates the specified bucket.
   */
  protected void updateBucket(final Bucket bucket) {
    inTxRetry(databaseInstanceProvider).run(db -> {
      bucketEntityAdapter.editEntity(db, bucket);
    });
  }
}
