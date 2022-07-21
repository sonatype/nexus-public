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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.squareup.tape.QueueFile;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.DELETIONS_FILENAME;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.REBUILD_DELETED_BLOB_INDEX_KEY;

@Named
public class DatastoreFileBlobDeletionIndex
    extends ComponentSupport
    implements FileBlobDeletionIndex
{
  private final SoftDeletedBlobsStore softDeletedBlobsStore;

  private FileBlobStore blobStore;

  private String blobStoreName;

  private Queue<String> deletedRecordsCache;

  private static final int INTERVAL_IN_SECONDS = 60;

  @Inject
  public DatastoreFileBlobDeletionIndex(final SoftDeletedBlobsStore softDeletedBlobsStore) {
    this.softDeletedBlobsStore = checkNotNull(softDeletedBlobsStore);
  }

  @Override
  public final void initIndex(final PropertiesFile metadata, final FileBlobStore blobStore) throws IOException {
    this.blobStore = blobStore;
    this.blobStoreName = blobStore.getBlobStoreConfiguration().getName();
    migrateDeletionIndexFromFileIfExists(metadata);
    deletedRecordsCache = new ArrayDeque<>();
  }

  @Override
  public void stopIndex() throws IOException {
    //  no special procedure is required
  }

  @Override
  public final void createRecord(final BlobId blobId) {
    softDeletedBlobsStore.createRecord(blobId, blobStoreName);
  }

  /**
   * Reads all records from the corresponding table and save they in a local variable for a future usage
   */
  private void populateInternalCache() {
    List<SoftDeletedBlobsData> response = new ArrayList<>();
    Continuation<SoftDeletedBlobsData> page = softDeletedBlobsStore.readRecords(null, blobStoreName);
    while (!page.isEmpty()) {
      response.addAll(page);
      page = softDeletedBlobsStore.readRecords(page.nextContinuationToken(), blobStoreName);
    }

    deletedRecordsCache.addAll(response.stream()
        .sorted(comparing(SoftDeletedBlobsData::getDeletedDate, nullsLast(naturalOrder())))
        .map(SoftDeletedBlobsData::getBlobId)
        .collect(Collectors.toList()));
  }

  @Override
  public final String readOldestRecord() {
    if (deletedRecordsCache.isEmpty()) {
      populateInternalCache();
    }
    return deletedRecordsCache.peek();
  }

  @Override
  public final void deleteRecord(final BlobId blobId) {
    deletedRecordsCache.remove();
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

  private void migrateDeletionIndexFromFileIfExists(final PropertiesFile metadata) throws IOException {
    QueueFile oldDeletionIndex;

    File oldDeletionIndexFile = getOldDeletionIndexFile(blobStore);
    if (!oldDeletionIndexFile.exists()) {
      log.debug("Skipping deletion file does not exist");
      return;
    }

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
      Set<String> persistedRecords = getPersistedBlobIdsForBlobStore(blobStoreName);

      try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, INTERVAL_IN_SECONDS)) {
        for (int counter = 0, numBlobs = oldDeletionIndex.size(); counter < numBlobs; counter++) {
          byte[] bytes = oldDeletionIndex.peek();
          if (bytes == null) {
            log.debug("Queue indicated no more results {} of {}", counter, numBlobs);
            break;
          }
          BlobId blobId = new BlobId(new String(bytes, UTF_8));
          if (!persistedRecords.contains(blobId.toString())) {
            softDeletedBlobsStore.createRecord(blobId, blobStore.getBlobStoreConfiguration().getName());
            persistedRecords.add(blobId.toString());
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

  private File getOldDeletionIndexFile(final FileBlobStore blobStore) throws IOException {
    Path blobDir = blobStore.getAbsoluteBlobDir();
    File deletedIndexFile = blobDir.resolve(blobStore.getDeletionsFilename()).toFile();
    Path deletedIndexPath = deletedIndexFile.toPath();
    Path legacyDeletionsIndex = deletedIndexPath.getParent().resolve(DELETIONS_FILENAME);
    if (!Files.exists(deletedIndexPath) && Files.exists(legacyDeletionsIndex)) {
      Files.move(legacyDeletionsIndex, deletedIndexPath);
    }
    return deletedIndexFile;
  }

  private Set<String> getPersistedBlobIdsForBlobStore(String blobStoreName) {
    Set<String> persistedRecords = new HashSet<>();
    Continuation<SoftDeletedBlobsData> page = softDeletedBlobsStore.readRecords(null, blobStoreName);
    while (!page.isEmpty()) {
      persistedRecords.addAll(page.stream()
          .map(SoftDeletedBlobsData::getBlobId)
          .collect(Collectors.toSet()));
      page = softDeletedBlobsStore.readRecords(page.nextContinuationToken(), blobStoreName);
    }
    return persistedRecords;
  }
}

