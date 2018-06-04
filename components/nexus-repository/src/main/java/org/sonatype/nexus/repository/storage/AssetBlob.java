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

import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.node.NodeAccess;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Blob handle, that holds properties and reference of newly created blob that is about to be attached to an {@link
 * Asset}. If this instance is not attached to any {@link Asset} during transaction, it is considered "orphan" and
 * will be cleaned up (deleted).
 *
 * @since 3.0
 */
public class AssetBlob
{
  private final NodeAccess nodeAccess;

  private final BlobStore blobStore;

  private final Function<BlobStore, Blob> blobFunction;

  private final String contentType;

  private final Map<HashAlgorithm, HashCode> hashes;

  private final boolean hashesVerified;

  private boolean attached;

  private Blob canonicalBlob;

  private Blob ingestedBlob;

  public AssetBlob(final NodeAccess nodeAccess,
                   final BlobStore blobStore,
                   final Function<BlobStore, Blob> blobFunction,
                   final String contentType,
                   final Map<HashAlgorithm, HashCode> hashes,
                   final boolean hashesVerified)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.blobStore = checkNotNull(blobStore);
    this.blobFunction = checkNotNull(blobFunction);
    this.contentType = checkNotNull(contentType);
    this.hashes = checkNotNull(hashes);
    this.hashesVerified = hashesVerified;
  }

  /**
   * Returns {@code true} if this {@link AssetBlob} duplicates the old asset's blob.
   *
   * @since 3.4
   */
  public boolean isDuplicate() {
    return canonicalBlob != null;
  }

  /**
   * Redirects this temporary {@link AssetBlob} to a canonical (de-duplicated) blob.
   *
   * @since 3.4
   */
  void setDuplicate(final Blob canonicalBlob) {
    this.canonicalBlob = checkNotNull(canonicalBlob);
  }

  /**
   * Deletes this temporary {@link AssetBlob} by clearing any locally ingested blobs.
   *
   * Note this shouldn't stop the current response from serving back temporary content,
   * it just makes sure non-persisted content is eventually cleaned up from the store.
   *
   * @since 3.4
   */
  void delete(final String reason) {
    if (ingestedBlob != null) {
      if (canonicalBlob != null) {
        // canonical redirect is in place, so it's safe to hard-delete the temp blob
        blobStore.deleteHard(ingestedBlob.getId());
      }
      else {
        // no redirect, so the temp blob is all we have - use soft-delete so the bytes
        // will still be available on disk for streaming back in the current response,
        // while making sure it gets cleaned up on the next compact
        blobStore.delete(ingestedBlob.getId(), reason);
      }
    }
  }

  /**
   * Returns {@code true} if this instance is attached to an {@link Asset}. If {@code false} returned, the blob
   * referenced by this instance is considered "orphan" and will be deleted at the end of TX (whatever outcome is,
   * commit or rollback) if not attached until the end of TX.
   */
  boolean isAttached() {
    return attached;
  }

  /**
   * Sets the attached state or this instance. Only can be invoked once, while this instance is not attached.
   */
  void setAttached(final boolean attached) {
    checkArgument(!this.attached, "Already attached");
    this.attached = attached;
  }

  /**
   * The blob reference this instance is pointing to.
   */
  @Nonnull
  public BlobRef getBlobRef() {
    return new BlobRef(
        nodeAccess.getId(),
        blobStore.getBlobStoreConfiguration().getName(),
        getBlob().getId().asUniqueString());
  }

  @Nonnull
  public Blob getBlob() {
    if (canonicalBlob != null) {
      return canonicalBlob;
    }
    if (ingestedBlob == null) {
      ingestedBlob = checkNotNull(blobFunction.apply(blobStore));
    }
    return ingestedBlob;
  }

  /**
   * The blob size in bytes.
   */
  public long getSize() {
    return getBlob().getMetrics().getContentSize();
  }

  /**
   * The content-type that blob contains.
   */
  @Nonnull
  public String getContentType() {
    return contentType;
  }

  /**
   * Exact hashes for the blob. Typically calculated by storage subsystem while blob was getting saved, but sometimes
   * provided based on precalculated or known values.
   */
  @Nonnull
  public Map<HashAlgorithm, HashCode> getHashes() {
    return hashes;
  }

  /**
   * Returns a boolean indicating whether the hashes associated with this blob have been verified.
   *
   * @since 3.1
   */
  public boolean getHashesVerified() {
    return hashesVerified;
  }

  @VisibleForTesting
  Blob getIngestedBlob() {
    return ingestedBlob;
  }
}
