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

import javax.annotation.Nonnull;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.hash.HashAlgorithm;

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
  private final BlobRef blobRef;

  private final Blob blob;

  private final long size;

  private final String contentType;

  private final Map<HashAlgorithm, HashCode> hashes;

  private boolean attached;

  private boolean hashesVerified;

  public AssetBlob(final BlobRef blobRef,
                   final Blob blob,
                   final long size,
                   final String contentType,
                   final Map<HashAlgorithm, HashCode> hashes,
                   final boolean hashesVerified)
  {
    this.blobRef = checkNotNull(blobRef);
    this.blob = checkNotNull(blob);
    this.size = size;
    this.contentType = checkNotNull(contentType);
    this.hashes = checkNotNull(hashes);
    this.attached = false;
    this.hashesVerified = hashesVerified;
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
    return blobRef;
  }

  @Nonnull
  public Blob getBlob() {
    return blob;
  }

  /**
   * The blob size in bytes.
   */
  public long getSize() {
    return size;
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
}
