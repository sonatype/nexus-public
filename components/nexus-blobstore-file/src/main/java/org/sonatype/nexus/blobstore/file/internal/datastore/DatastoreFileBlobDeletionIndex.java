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
package org.sonatype.nexus.blobstore.file.internal.datastore;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.file.FileBlobDeletionIndex;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.file.store.SoftDeletedBlobsData;
import org.sonatype.nexus.blobstore.file.store.SoftDeletedBlobsStore;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.scheduling.PeriodicJobService;

import com.squareup.tape.QueueFile;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.REBUILD_DELETED_BLOB_INDEX_KEY;

@Named
public class DatastoreFileBlobDeletionIndex
    extends ComponentSupport
    implements FileBlobDeletionIndex
{
  private static final int INTERVAL_IN_SECONDS = 60;

  private final SoftDeletedBlobsStore softDeletedBlobsStore;

  private final PeriodicJobService periodicJobService;

  private final Duration migrationDelay;

  private final int deletedFileCacheLimit;

  private FileBlobStore blobStore;

  private String blobStoreName;

  private Iterator<BlobId> currentBatchIterator;

  private Continuation<SoftDeletedBlobsData> deletionContinuation;

  @Inject
  public DatastoreFileBlobDeletionIndex(
      final SoftDeletedBlobsStore softDeletedBlobsStore,
      final PeriodicJobService periodicJobService,
      @Named("${nexus.file.deletion.migrate.delay:-60s}") final Duration migrationDelay,
      @Named("${nexus.file.deletion.buffer.size:-1000}") final int deletedFileBufferSize)
  {
    this.softDeletedBlobsStore = checkNotNull(softDeletedBlobsStore);
    this.periodicJobService = checkNotNull(periodicJobService);
    this.migrationDelay = checkNotNull(migrationDelay);
    this.deletedFileCacheLimit = deletedFileBufferSize;
    checkArgument(!migrationDelay.isNegative(), "Non-negative nexus.file.deletion.migrate.delay required");
  }

  @Override
  public final void initIndex(final PropertiesFile metadata, final FileBlobStore blobStore) throws IOException {
    this.blobStore = blobStore;
    this.blobStoreName = blobStore.getBlobStoreConfiguration().getName();
    scheduleMigrateIndex(metadata);
  }

  @Override
  public void stopIndex() throws IOException {
    //  no special procedure is required
  }

  @Override
  public final void createRecord(final BlobId blobId) {
    softDeletedBlobsStore.createRecord(blobId, blobStoreName);
  }

  private void populateInternalCache() {
    String token = Optional.ofNullable(deletionContinuation)
        .filter(value -> !value.isEmpty())
        .map(Continuation::nextContinuationToken)
        .orElse("0");

    deletionContinuation = softDeletedBlobsStore.readRecords(token,
        Math.abs(deletedFileCacheLimit), blobStoreName);

    if(!deletionContinuation.isEmpty()) {
      currentBatchIterator = this.deletionContinuation.stream()
          .map(value -> new BlobId(value.getBlobId(), value.getDatePathRef()))
          .iterator();
    } else {
      currentBatchIterator = null;
    }
  }

  @Override
  public final BlobId getNextAvailableRecord() {
    if(Objects.isNull(currentBatchIterator) || !currentBatchIterator.hasNext() ) {
      populateInternalCache();
    }

    return Objects.nonNull(this.currentBatchIterator) ? currentBatchIterator.next() : null;
  }

  @Override
  public final void deleteRecord(final BlobId blobId) {
    softDeletedBlobsStore.deleteRecord(blobStoreName, blobId);
  }

  @Override
  public final void deleteAllRecords() {
    softDeletedBlobsStore.deleteAllRecords(blobStoreName);
  }

  @Override
  public final int size() throws IOException {
    return softDeletedBlobsStore.count(blobStoreName);
  }

  private void scheduleMigrateIndex(final PropertiesFile metadata) {
    invoke(periodicJobService::startUsing);
    periodicJobService.runOnce(() -> {
      try {
        migrateDeletionIndexFromFiles(metadata);
      }
      catch (IOException e) {
        log.error("Failed to migrate soft deleted blobs to the database", e);
      }
      invoke(periodicJobService::stopUsing);
    }, (int) migrationDelay.getSeconds());
  }

  private void migrateDeletionIndexFromFiles(final PropertiesFile metadata) throws IOException {
    blobStore.getDeletionIndexFiles()
        .forEach(deletionIndexFile -> {
          try {
            migrateDeletionIndexFromFile(metadata, deletionIndexFile);
          }
          catch (IOException e) {
            log.error("An error occurred while attempting to migrate the deletions index {} for {}", deletionIndexFile,
                blobStoreName);
          }
        });
  }

  private void migrateDeletionIndexFromFile(
      final PropertiesFile metadata,
      final @Nullable File oldDeletionIndexFile) throws IOException
  {
    QueueFile oldDeletionIndex;
    log.debug("Starting migration in {} for {}", blobStoreName, oldDeletionIndexFile);

    try {
      oldDeletionIndex = new QueueFile(oldDeletionIndexFile);
    }
    catch (IOException e) {
      log.error(
          "Unable to load deletions index file {}, run the compact blobstore task to rebuild", oldDeletionIndexFile, e
      );
      oldDeletionIndex = null;
      metadata.setProperty(REBUILD_DELETED_BLOB_INDEX_KEY, "true");
      metadata.store();
    }

    if (Objects.nonNull(oldDeletionIndex) && !oldDeletionIndex.isEmpty()) {
      log.info("Processing blobstore {}, discovered file-based deletion index. Migrating to DB-based",
          blobStore.getBlobStoreConfiguration().getName());
      Set<BlobId> persistedRecords = softDeletedBlobsStore.readAllBlobIds(blobStoreName)
          .collect(Collectors.toSet());

      try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, INTERVAL_IN_SECONDS)) {
        for (int counter = 0, numBlobs = oldDeletionIndex.size(); counter < numBlobs; counter++) {
          byte[] bytes = oldDeletionIndex.peek();
          if (bytes == null) {
            log.debug("Queue indicated no more results {} of {}", counter, numBlobs);
            break;
          }
          BlobId blobId = new BlobId(new String(bytes, UTF_8));
          if (!persistedRecords.contains(blobId)) {
            softDeletedBlobsStore.createRecord(blobId, blobStore.getBlobStoreConfiguration().getName());
            persistedRecords.add(blobId);
          } else {
            log.debug("Old deletion index contain duplicate entry with blobId - {} for blobstore - {}, " +
                    "duplicate record will be skipped", blobId, blobStoreName);
          }

          oldDeletionIndex.remove();
          progressLogger.info("Elapsed time: {}, processed: {}/{}", progressLogger.getElapsed(),
              counter + 1, numBlobs);
        }
      }
    }

    if (oldDeletionIndexFile.exists() && !oldDeletionIndexFile.delete()) {
      log.error("Unable to delete 'deletion index' file, path = {}", oldDeletionIndexFile.getAbsolutePath());
    }
  }

  private void invoke(final ThrowingRunnable callable) {
    try {
      callable.run();
    }
    catch (Exception e) {
      log.debug("Failed to start or stop using the PeriodicJobService", e);
    }
  }

  @FunctionalInterface
  private static interface ThrowingRunnable
  {
    void run() throws Exception;
  }
}

