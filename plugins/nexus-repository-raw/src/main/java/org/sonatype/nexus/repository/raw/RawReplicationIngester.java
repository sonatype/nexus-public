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
package org.sonatype.nexus.repository.raw;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.replication.BlobEventType;
import org.sonatype.nexus.repository.replication.ReplicationIngester;
import org.sonatype.nexus.repository.replication.ReplicationIngesterHelper;
import org.sonatype.nexus.repository.replication.ReplicationIngesterSupport;
import org.sonatype.nexus.repository.replication.ReplicationIngestionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;

/**
 * @since 3.31
 */
@Named(RawFormat.NAME)
@Singleton
public class RawReplicationIngester
    extends ReplicationIngesterSupport
    implements ReplicationIngester
{
  private final BlobStoreManager blobStoreManager;

  private final ReplicationIngesterHelper replicationIngesterHelper;

  @Inject
  public RawReplicationIngester(final BlobStoreManager blobstoreManager,
                                final ReplicationIngesterHelper replicationIngesterHelper)
  {
    this.blobStoreManager = checkNotNull(blobstoreManager);
    this.replicationIngesterHelper = checkNotNull(replicationIngesterHelper);
  }

  @Override
  public String getFormat() {
    return RawFormat.NAME;
  }

  @Override
  public void ingestBlob(final String blobIdString,
                         final String blobStoreId,
                         final String repositoryName,
                         final BlobEventType eventType)
      throws ReplicationIngestionException
  {
    BlobId blobId = new BlobId(blobIdString);
    BlobStore blobStore = blobStoreManager.get(blobStoreId);
    if (blobStore == null) {
      throw new ReplicationIngestionException(
          String.format("Can't ingest blob %s, the blob store %s doesn't exist", blobIdString, blobStoreId));
    }
    Blob blob = blobStore.get(blobId);
    if (blob == null) {
      throw new ReplicationIngestionException(
          String.format("Can't ingest blob %s, the blob doesn't exist", blobIdString));
    }
    BlobAttributes blobAttributes = blobStore.getBlobAttributes(blobId);
    if (blobAttributes == null) {
      throw new ReplicationIngestionException(
          String.format("Can't ingest blob %s, the blob doesn't have related attributes", blobIdString));
    }

    if (eventType.equals(BlobEventType.DELETED)) {
      log.info("Ingesting a delete for blob {} in repository {} and blob store {}.", blobIdString, repositoryName,
          blobStoreId);
      String path = blobAttributes.getHeaders().get(BLOB_NAME_HEADER);
      replicationIngesterHelper.deleteReplication(path, repositoryName);
      return;
    }

    Map<String, Object> assetAttributes = extractAssetAttributesFromProperties(blobAttributes.getProperties());
    Map<String, Object> componentAttributes = extractComponentAttributesFromProperties(blobAttributes.getProperties());

    try {
      log.debug("Ingesting blob {} in repository {} and blob store {}.", blobIdString, repositoryName,
          blobStoreId);
      replicationIngesterHelper.replicate(blobStoreId, blob, assetAttributes, componentAttributes, repositoryName, blobStoreId);
    }
    catch (IOException e) {
      throw new ReplicationIngestionException(String
          .format("Could not ingest blob %s for repository %s in blobstore %s.", blobIdString, repositoryName,
              blobStoreId), e);
    }
  }
}
