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
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.MultiHashingInputStream;
import org.sonatype.nexus.common.node.NodeAccess;

import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Keeps track of added and to-be-deleted blobs so they can be deleted as appropriate when the transaction ends,
 * via commit or rollback.
 *
 * @since 3.0
 */
class BlobTx
{
  private static final Logger log = LoggerFactory.getLogger(BlobTx.class);

  private final NodeAccess nodeAccess;

  private final BlobStore blobStore;

  private final Set<AssetBlob> newlyCreatedBlobs = Sets.newHashSet();

  private final Set<BlobRef> deletionRequests = Sets.newHashSet();

  public BlobTx(final NodeAccess nodeAccess, final BlobStore blobStore) {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.blobStore = checkNotNull(blobStore);
  }

  public AssetBlob create(final InputStream inputStream,
                          final Map<String, String> headers,
                          final Iterable<HashAlgorithm> hashAlgorithms,
                          final String contentType)
  {
    final MultiHashingInputStream hashingStream = new MultiHashingInputStream(hashAlgorithms, inputStream);
    Blob blob = blobStore.create(hashingStream, headers);
    BlobRef blobRef = new BlobRef(nodeAccess.getId(), blobStore.getBlobStoreConfiguration().getName(), blob.getId().asUniqueString());
    AssetBlob assetBlob = new AssetBlob(blobRef, blob, hashingStream.count(), contentType, hashingStream.hashes(), true);
    newlyCreatedBlobs.add(assetBlob);
    return assetBlob;
  }

  /**
   * Create an asset blob by hard linking to the {@code sourceFile}.
   *
   * @param sourceFile the file to be hard linked
   * @param headers a map of headers to be applied to the resulting blob
   * @param hashes the algorithms and precalculated hashes of the content
   * @param contentType content type
   * @param size precalculated size for the blob
   * @return {@link AssetBlob}
   */
  public AssetBlob createByHardLinking(final Path sourceFile,
                                       final Map<String, String> headers,
                                       final Map<HashAlgorithm, HashCode> hashes,
                                       final String contentType,
                                       final long size)
  {
    Blob blob = blobStore.create(sourceFile, headers, size, hashes.get(SHA1));
    BlobRef blobRef = new BlobRef(nodeAccess.getId(), blobStore.getBlobStoreConfiguration().getName(), blob.getId().asUniqueString());
    long bytes = blob.getMetrics().getContentSize();
    AssetBlob assetBlob = new AssetBlob(blobRef, blob, bytes, contentType, hashes, false);
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
