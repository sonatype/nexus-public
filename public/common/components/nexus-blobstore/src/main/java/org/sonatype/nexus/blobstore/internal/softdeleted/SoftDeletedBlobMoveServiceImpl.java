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
package org.sonatype.nexus.blobstore.internal.softdeleted;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.softdeleted.BlobLocationUpdate;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobMoveService;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobsStore;

import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link SoftDeletedBlobMoveService} that updates soft_deleted_blobs records
 * after blobs are moved between blob stores.
 */
@Service
public class SoftDeletedBlobMoveServiceImpl
    implements SoftDeletedBlobMoveService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final int DEFAULT_BATCH_SIZE = 100;

  private final SoftDeletedBlobsStore softDeletedBlobsStore;

  private final int batchSize;

  @Autowired
  public SoftDeletedBlobMoveServiceImpl(
      final SoftDeletedBlobsStore softDeletedBlobsStore,
      @Value("${nexus.blobstore.softdeleted.move.batchSize:100}") final int batchSize)
  {
    this.softDeletedBlobsStore = checkNotNull(softDeletedBlobsStore);
    if (batchSize <= 0) {
      log.warn("Invalid batchSize {} configured for soft-deleted blob moves, using default: {}",
          batchSize, DEFAULT_BATCH_SIZE);
      this.batchSize = DEFAULT_BATCH_SIZE;
    }
    else {
      this.batchSize = batchSize;
    }
  }

  @Override
  public void updateMovedBlobRecords(final Map<BlobId, String> movedBlobs, final String oldBlobStoreName) {
    if (movedBlobs.isEmpty()) {
      log.info("No blobs were moved, skipping soft_deleted_blobs update");
      return;
    }

    log.info("Updating soft_deleted_blobs records for {} moved blobs from {}",
        movedBlobs.size(), oldBlobStoreName);

    int totalUpdated = 0;
    int totalFailed = 0;
    int totalNotFound = 0;

    for (List<Map.Entry<BlobId, String>> partition : Iterables.partition(movedBlobs.entrySet(), batchSize)) {
      List<BlobLocationUpdate> batch = partition.stream()
          .map(this::asBlobLocationUpdate)
          .toList();

      try {
        int updated = softDeletedBlobsStore.batchUpdateBlobLocation(batch, oldBlobStoreName);
        totalUpdated += updated;

        int notFound = batch.size() - updated;
        totalNotFound += notFound;
        log.debug("Batch updated {} soft_deleted_blobs records from {} ({} not found)",
            updated, oldBlobStoreName, notFound);
      }
      catch (Exception e) {
        totalFailed += batch.size();
        log.warn("Failed to batch update soft_deleted_blobs records from {}: {}",
            oldBlobStoreName, e.getMessage(), e);
      }
    }

    log.info("Updated {} soft_deleted_blobs records from {} ({} not found, {} failed)",
        totalUpdated, oldBlobStoreName, totalNotFound, totalFailed);

    if (totalFailed > 0) {
      log.warn("Failed to update {} soft_deleted_blobs records - these blobs may not be cleaned up by Compact task",
          totalFailed);
    }
  }

  private BlobLocationUpdate asBlobLocationUpdate(Map.Entry<BlobId, String> softDeletedMovedBlob) {
    return new BlobLocationUpdate(
        softDeletedMovedBlob.getKey().toString(),
        softDeletedMovedBlob.getValue(),
        softDeletedMovedBlob.getKey().getBlobCreatedRef());
  }
}
