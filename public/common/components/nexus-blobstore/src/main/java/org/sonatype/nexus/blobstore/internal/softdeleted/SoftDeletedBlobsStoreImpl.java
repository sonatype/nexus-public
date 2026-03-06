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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.softdeleted.BlobLocationUpdate;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlob;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobsStore;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Implementation of {@link SoftDeletedBlobsStore}
 */
@Component
@Qualifier("mybatis")
@Singleton
public class SoftDeletedBlobsStoreImpl
    extends ConfigStoreSupport<SoftDeletedBlobsDAO>
    implements SoftDeletedBlobsStore
{
  @Inject
  public SoftDeletedBlobsStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Transactional
  @Override
  public void createRecord(final BlobId blobId, final String sourceBlobStoreName) {
    dao().createRecord(sourceBlobStoreName, blobId.toString(), blobId.getBlobCreatedRef());
  }

  @Transactional
  @Override
  public Continuation<SoftDeletedBlob> readRecords(
      final String continuationToken,
      final int limit,
      final String sourceBlobStoreName)
  {
    return dao().readRecords(continuationToken, limit, sourceBlobStoreName);
  }

  @Transactional
  public Continuation<SoftDeletedBlob> readRecordsBefore(
      final String continuationToken,
      final int limit,
      final String sourceBlobStoreName,
      final OffsetDateTime upperBound)
  {
    return dao().readRecordsBefore(continuationToken, limit, sourceBlobStoreName, upperBound);
  }

  @Override
  public Stream<BlobId> readAllBlobIds(final String sourceBlobStoreName) {
    return Continuations
        .streamOf((limit, continuationToken) -> readRecords(continuationToken, limit, sourceBlobStoreName))
        .map(data -> new BlobId(data.getBlobId(), data.getDatePathRef()));
  }

  @Override
  public Stream<BlobId> getBlobsBefore(final String sourceBlobStoreName, final OffsetDateTime blobsDeletedBefore) {
    return Continuations
        .streamOf((limit, token) -> readRecordsBefore(token, limit, sourceBlobStoreName, blobsDeletedBefore))
        .map(data -> new BlobId(data.getBlobId(), data.getDatePathRef()));
  }

  @Transactional
  @Override
  public void deleteRecord(final String sourceBlobStoreName, final BlobId blobId) {
    dao().deleteRecord(sourceBlobStoreName, blobId.toString());
  }

  @Override
  public void deleteAllRecords(final String sourceBlobStoreName) {
    while (doDeleteAllBlobs(sourceBlobStoreName) != 0) {
      log.trace("Deleted page for {}", sourceBlobStoreName);
    }
  }

  @Transactional
  public int doDeleteAllBlobs(final String sourceBlobName) {
    return dao().deleteAllRecords(sourceBlobName, "1000");
  }

  @Transactional
  @Override
  public int count(final String sourceBlobStoreName) {
    return dao().count(sourceBlobStoreName);
  }

  @Transactional
  @Override
  public int countBefore(final String blobStoreName, final OffsetDateTime blobsDeletedBefore) {
    return dao().countBefore(blobStoreName, blobsDeletedBefore);
  }

  @Transactional
  @Override
  public int batchUpdateBlobLocation(final List<BlobLocationUpdate> updates, final String oldSourceBlobStoreName) {
    if (updates.isEmpty()) {
      return 0;
    }
    return dao().batchUpdateBlobLocation(updates, oldSourceBlobStoreName);
  }
}
