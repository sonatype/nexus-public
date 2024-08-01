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
package org.sonatype.nexus.blobstore.file.store;

import java.util.List;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.common.entity.Continuation;

/**
 * Store for accessing the soft deleted blob ID's
 */
public interface SoftDeletedBlobsStore
{
  /**
   * Create new record
   *
   * @param blobId              string representation of {@link BlobId}
   * @param sourceBlobStoreName the blobstore name this record is related to
   */
  void createRecord(BlobId blobId, String sourceBlobStoreName);

  /**
   * Return all records stored in DB, the continuationToken to be used when amount more than single page (>1000 rows)
   *
   * @param continuationToken   the record id used for pagination
   * @param sourceBlobStoreName the blobstore name these records are related to
   * @return all records related to provided sourceBlobStoreName
   */
  Continuation<SoftDeletedBlobsData> readRecords(String continuationToken, int limit, String sourceBlobStoreName);

  Stream<BlobId> readAllBlobIds(String sourceBlobStoreName);

  /**
   * Delete single record by provided 'blobId' related to specified blobstore name
   *
   * @param sourceBlobStoreName the blobstore name this record is related to
   * @param blobId              {@link BlobId} of record that should be deleted
   */
  void deleteRecord(String sourceBlobStoreName, BlobId blobId);

  /**
   * Delete all records related to provided blobstore name
   *
   * @param sourceBlobStoreName the blobstore name these records are related to
   */
  void deleteAllRecords(String sourceBlobStoreName);

  /**
   * Returns the amount of soft deleted blobs records related to provided blobstore
   *
   * @param sourceBlobStoreName the blobstore name these records are related to
   * @return amount of soft deleted blobs
   */
  int count(String sourceBlobStoreName);

  /**
   * Returns a list of the oldest 20 blob_ids in the soft_deleted_blobs table related to the provided blobstore.
   *
   * @param sourceBlobStoreName the blobstore name these records are related to
   * @return oldest 20 blob_id strings
   */
  List<String> readOldestRecords(String sourceBlobStoreName);
}
