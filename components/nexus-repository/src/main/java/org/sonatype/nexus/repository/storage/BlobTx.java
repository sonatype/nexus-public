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
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.MultiHashingInputStream;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.text.Strings2;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Keeps track of added and to-be-deleted blobs so they can be deleted as appropriate when the transaction ends,
 * via commit or rollback.
 *
 * @since 3.0
 */
public class BlobTx
{
  private static final Logger log = LoggerFactory.getLogger(BlobTx.class);

  private final NodeAccess nodeAccess;

  private final BlobStore blobStore;

  private final Set<AssetBlob> newlyCreatedBlobs = Sets.newHashSet();

  private final Map<BlobRef, String> deletionRequests = Maps.newHashMap();

  public BlobTx(final NodeAccess nodeAccess, final BlobStore blobStore) {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.blobStore = checkNotNull(blobStore);
  }

  public AssetBlob create(final InputStream inputStream,
                          final Map<String, String> headers,
                          final Iterable<HashAlgorithm> hashAlgorithms,
                          final String contentType)
  {
    MultiHashingInputStream hashingStream = new MultiHashingInputStream(hashAlgorithms, inputStream);
    Blob streamedBlob = blobStore.create(hashingStream, headers); // pre-fetch to populate hashes
    return createPrefetchedAssetBlob(
        streamedBlob,
        hashingStream.hashes(),
        true,
        contentType);
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
    return createAssetBlob(
        store -> store.create(sourceFile, headers, size, hashes.get(SHA1)),
        hashes,
        false,
        contentType);
  }

  /**
   * Create an asset blob by copying the source blob. Throws an exception if the blob has already been deleted.
   *
   * @param blobId      blobId of a blob already present in the blobstore
   * @param headers     a map of headers to be applied to the resulting blob
   * @param hashes      the algorithms and precalculated hashes of the content
   * @return {@link AssetBlob}
   * @since 3.1
   */
  public AssetBlob createByCopying(final BlobId blobId,
                                   final Map<String, String> headers,
                                   final Map<HashAlgorithm, HashCode> hashes,
                                   final boolean hashesVerified)
  {
    checkArgument(!Strings2.isBlank(headers.get(BlobStore.CONTENT_TYPE_HEADER)), "Blob content type is required");
    // This might be a place where we might consider passing in a BlobRef instead of a BlobId, for a post-fabric world
    // where repositories could be writing/reading from multiple blob stores.
    return createAssetBlob(
        store -> store.copy(blobId, headers),
        hashes,
        hashesVerified,
        headers.get(BlobStore.CONTENT_TYPE_HEADER));
  }

  private PrefetchedAssetBlob createPrefetchedAssetBlob(final Blob blob,
                                                        final Map<HashAlgorithm, HashCode> hashes,
                                                        final boolean hashesVerified,
                                                        final String contentType)
  {
    PrefetchedAssetBlob assetBlob = new PrefetchedAssetBlob(
        nodeAccess,
        blobStore,
        blob,
        contentType,
        hashes,
        hashesVerified);

    newlyCreatedBlobs.add(assetBlob);
    return assetBlob;
  }

  private AssetBlob createAssetBlob(final Function<BlobStore, Blob> blobFunction,
                                    final Map<HashAlgorithm, HashCode> hashes,
                                    final boolean hashesVerified,
                                    final String contentType)
  {
    AssetBlob assetBlob = new AssetBlob(
        nodeAccess,
        blobStore,
        blobFunction,
        contentType,
        hashes,
        hashesVerified);

    newlyCreatedBlobs.add(assetBlob);
    return assetBlob;
  }

  @Nullable
  public Blob get(BlobRef blobRef) {
    return blobStore.get(blobRef.getBlobId());
  }

  public void delete(BlobRef blobRef, final String reason) {
    deletionRequests.put(blobRef, reason);
  }

  public void commit() {
    for (Entry<BlobRef, String> deletionRequestEntry : deletionRequests.entrySet()) {
      try {
        blobStore.delete(deletionRequestEntry.getKey().getBlobId(), deletionRequestEntry.getValue());
      }
      catch (Throwable t) {
        log.warn("Unable to delete old blob {} while committing transaction", deletionRequestEntry.getKey(), t);
      }
    }
    for (AssetBlob assetBlob : newlyCreatedBlobs) {
      try {
        if (!assetBlob.isAttached()) {
          assetBlob.delete("Removing unattached asset");
        }
      }
      catch (Throwable t) {
        log.warn("Unable to delete new orphan blob {} while committing transaction", assetBlob.getBlobRef(), t);
      }
    }
    clearState();
  }

  public void rollback() {
    //
    // No need to undelete deletionRequests here, because rollback is only triggered if the DB commit fails.
    // At that point we haven't done any deletions (just queued up requests) so there's nothing to undelete.
    //
    // This relies on the DB commit happening before the BlobTx commit in StorageTxImpl.commit() as well as
    // BlobTx.commit() not throwing any exception or error, which is why throwables are caught and logged
    // throughout this class rather than being allowed to propagate.
    //
    // Also consider when two threads try to delete the same blob at the same time, only one will succeed
    // and commit. The other thread will rollback - in that case we wouldn't want it to undelete the blob
    // that was just deleted by the first thread, as that would lead to orphaned deleted blobs.
    //
    for (AssetBlob assetBlob : newlyCreatedBlobs) {
      try {
        assetBlob.delete("Rolling back new asset");
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
