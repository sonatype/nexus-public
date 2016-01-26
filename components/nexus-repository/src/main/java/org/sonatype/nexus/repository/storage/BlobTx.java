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
package org.sonatype.nexus.repository.storage;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.MultiHashingInputStream;
import org.sonatype.nexus.common.node.LocalNodeAccess;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Keeps track of added and to-be-deleted blobs so they can be deleted as appropriate when the transaction ends,
 * via commit or rollback.
 *
 * @since 3.0
 */
class BlobTx
{
  private static final Logger log = LoggerFactory.getLogger(BlobTx.class);

  private final LocalNodeAccess localNodeAccess;

  private final BlobStore blobStore;

  private final Set<AssetBlob> newlyCreatedBlobs = Sets.newHashSet();

  private final Set<BlobRef> deletionRequests = Sets.newHashSet();

  public BlobTx(final LocalNodeAccess localNodeAccess, final BlobStore blobStore) {
    this.localNodeAccess = checkNotNull(localNodeAccess);
    this.blobStore = checkNotNull(blobStore);
  }

  public AssetBlob create(final InputStream inputStream,
                          final Map<String, String> headers,
                          final Iterable<HashAlgorithm> hashAlgorithms,
                          final String contentType)
  {
    final MultiHashingInputStream hashingStream = new MultiHashingInputStream(hashAlgorithms, inputStream);
    Blob blob = blobStore.create(hashingStream, headers);
    BlobRef blobRef = new BlobRef(localNodeAccess.getId(), blobStore.getBlobStoreConfiguration().getName(), blob.getId().asUniqueString());
    AssetBlob assetBlob = new AssetBlob(blobRef, blob, hashingStream.count(), contentType, hashingStream.hashes());
    newlyCreatedBlobs.add(assetBlob);
    return assetBlob;
  }

  @Nullable
  public Blob get(BlobRef blobRef) {
    return blobStore.get(blobRef.getBlobId());
  }

  public void delete(BlobRef blobRef) {
    deletionRequests.add(blobRef);
  }

  public void commit() {
    for (BlobRef blobRef : deletionRequests) {
      try {
        blobStore.delete(blobRef.getBlobId());
      }
      catch (Throwable t) {
        log.warn("Unable to delete old blob {} while committing transaction", blobRef, t);
      }
    }
    for (AssetBlob assetBlob : newlyCreatedBlobs) {
      try {
        if (!assetBlob.isAttached()) {
          blobStore.delete(assetBlob.getBlobRef().getBlobId());
        }
      }
      catch (Throwable t) {
        log.warn("Unable to delete new orphan blob {} while committing transaction", assetBlob.getBlobRef(), t);
      }
    }
    clearState();
  }

  public void rollback() {
    for (AssetBlob assetBlob : newlyCreatedBlobs) {
      try {
        blobStore.delete(assetBlob.getBlobRef().getBlobId());
      }
      catch (Throwable t) {
        log.warn("Unable to delete new blob {} while rolling back transaction", assetBlob.getBlobRef(), t);
      }
    }
    clearState();
  }

  private void clearState() {
    newlyCreatedBlobs.clear();
    deletionRequests.clear();
  }
}
