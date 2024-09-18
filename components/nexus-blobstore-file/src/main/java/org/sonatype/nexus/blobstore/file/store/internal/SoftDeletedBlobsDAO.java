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
package org.sonatype.nexus.blobstore.file.store.internal;

import java.time.OffsetDateTime;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.file.store.SoftDeletedBlobsData;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.annotations.Param;

/**
 * {@link SoftDeletedBlobsData} DataAccess
 */
public interface SoftDeletedBlobsDAO
    extends DataAccess
{
  /**
   * Create new record
   *
   * @param sourceBlobStoreName the blobstore name this record is related to
   * @param blobId              string representation of {@link BlobId}
   * @param datePathRef         the {@link OffsetDateTime} of the blob creation
   */
  void createRecord(
      @Param("sourceBlobStoreName") String sourceBlobStoreName,
      @Param("blobId") String blobId,
      @Param("datePathRef") OffsetDateTime datePathRef);

  /**
   * Return all records stored in DB, the continuationToken to be used when amount more than single page (>1000 rows)
   *
   * @param continuationToken   the record id used for pagination
   * @param sourceBlobStoreName the blobstore name these records are related to
   * @return all records related to provided sourceBlobStoreName
   */
  Continuation<SoftDeletedBlobsData> readRecords(
      @Nullable @Param("continuationToken") String continuationToken,
      @Param("limit") int limit,
      @Param("sourceBlobStoreName") String sourceBlobStoreName);

  /**
   * Delete single record by provided 'blobId' related to specified blobstore name
   *
   * @param sourceBlobStoreName the blobstore name this record is related to
   * @param blobId              {@link BlobId} of record that should be deleted
   */
  void deleteRecord(
      @Param("sourceBlobStoreName") String sourceBlobStoreName,
      @Param("blobId") String blobId);

  /**
   * Delete all records related to provided blobstore name
   *
   * @param sourceBlobStoreName the blobstore name these records are related to
   * @param limit               maximum amount of rows to be deleted
   * @return numbers of deleted rows
   */
  int deleteAllRecords(
      @Param("sourceBlobStoreName") String sourceBlobStoreName,
      @Param("limit") String limit);

  /**
   * Returns the amount of soft deleted blobs records related to provided blobstore
   *
   * @param sourceBlobStoreName the blobstore name these records are related to
   * @return amount of soft deleted blobs
   */
  int count(@Param("sourceBlobStoreName") String sourceBlobStoreName);

  /**
   * Returns a list of the oldest 20 blob_ids in the soft_deleted_blobs table related to the provided blobstore.
   *
   * @param sourceBlobStoreName the blobstore name these records are related to
   * @return oldest 20 blob_id strings
   */
  List<SoftDeletedBlobsData> readOldestRecords(@Param("sourceBlobStoreName") String sourceBlobStoreName);
}
