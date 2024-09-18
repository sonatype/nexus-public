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
package org.sonatype.nexus.blobstore.file.internal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.file.store.SoftDeletedBlobsData;
import org.sonatype.nexus.blobstore.file.store.SoftDeletedBlobsStore;
import org.sonatype.nexus.blobstore.file.store.internal.SoftDeletedBlobsDAO;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

/**
 * Implementation of {@link SoftDeletedBlobsStore}
 */
@Named("mybatis")
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
  public Continuation<SoftDeletedBlobsData> readRecords(
      final String continuationToken,
      int limit,
      final String sourceBlobStoreName)
  {
    return dao().readRecords(continuationToken, limit, sourceBlobStoreName);
  }

  @Override
  public Stream<BlobId> readAllBlobIds(final String sourceBlobStoreName) {
    return Continuations
        .streamOf((limit, continuationToken) -> readRecords(continuationToken, limit, sourceBlobStoreName))
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
  public List<BlobId> readOldestRecords(final String sourceBlobStoreName) {
    return dao().readOldestRecords(sourceBlobStoreName).stream()
        .map(data -> new BlobId(data.getBlobId(), data.getDatePathRef()))
        .collect(Collectors.toList());
  }
}
