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
package org.sonatype.nexus.blobstore.group.internal;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.group.FillPolicy;
import org.sonatype.nexus.blobstore.group.InvalidBlobStoreGroupConfiguration;

import org.slf4j.Logger;

/**
 * This policy uses the first 2 blob stores in a group. - The first blob store is used to create temporary blobs. - The
 * second blob store is used to copy temporary blobs to, or to create non-temporary blobs.
 */
@Named(FirstBlobStoreCacheFillPolicy.TYPE)
public class FirstBlobStoreCacheFillPolicy
    implements FillPolicy
{
  private final Logger logger = Loggers.getLogger(this);

  public static final String TYPE = "firstBlobStoreCacheFillPolicy";

  protected static final String NAME = "First Blob Store Cache";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void validateBlobStoreGroup(final BlobStoreGroup blobStoreGroup) {
    if(blobStoreGroup.getMembers().size() < 2) {
      throw new InvalidBlobStoreGroupConfiguration(NAME + " fill policy requires at least 2 blob store group members");
    }
  }

  @Nullable
  @Override
  public BlobStore chooseBlobStoreForCreate(
      final BlobStoreGroup blobStoreGroup, final Map<String, String> headers)
  {
    if (headers.containsKey(BlobStore.TEMPORARY_BLOB_HEADER)) {
      return getBlobStoreForTempBlobs(blobStoreGroup);
    }

    return getBlobStoreForPermanentBlobs(blobStoreGroup);
  }

  @Nullable
  @Override
  public BlobStore chooseBlobStoreForCopy(
      final BlobStoreGroup blobStoreGroup, final BlobStore sourceBlobStore, final Map<String, String> headers)
  {
    if (headers.containsKey(BlobStore.TEMPORARY_BLOB_HEADER)) {
      return getBlobStoreForTempBlobs(blobStoreGroup);
    }

    return getBlobStoreForPermanentBlobs(blobStoreGroup);
  }

  private BlobStore getBlobStoreForTempBlobs(BlobStoreGroup blobStoreGroup){
    BlobStore blobStore = blobStoreGroup.getMembers().get(0);

    if(!blobStore.isWritable() || !blobStore.isStorageAvailable()) {
      logger.warn("TempBlob blob store {} is unavailable, writing to permanent blob store", blobStore.getBlobStoreConfiguration().getName());

      return getBlobStoreForPermanentBlobs(blobStoreGroup);
    }

    return blobStore;
  }

  private BlobStore getBlobStoreForPermanentBlobs(BlobStoreGroup blobStoreGroup){
    BlobStore blobStore = blobStoreGroup.getMembers().get(1);

    if(!blobStore.isWritable() || !blobStore.isStorageAvailable()) {
      throw new BlobStoreException(
          "Blob store " + blobStore.getBlobStoreConfiguration().getName() +
              " cannot is unavailable", null);
    }
    return blobStore;
  }
}
