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

import java.io.Closeable;
import java.io.InputStream;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.hash.HashAlgorithm;

import com.google.common.base.Supplier;
import com.google.common.hash.HashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Blob handle that holds information for a temporary blob used in place of temporary files and streams. Instances must
 * be closed by the caller.
 *
 * @since 3.1
 */
public class TempBlob
    implements Closeable, Supplier<InputStream>
{
  private static final Logger log = LoggerFactory.getLogger(TempBlob.class);

  private final Blob blob;

  private final Map<HashAlgorithm, HashCode> hashes;

  private final boolean hashesVerified;

  private final BlobStore blobStore;

  private boolean deleted = false;

  public TempBlob(final Blob blob,
                  final Map<HashAlgorithm, HashCode> hashes,
                  final boolean hashesVerified,
                  final BlobStore blobStore)
  {
    this.blob = checkNotNull(blob);
    this.hashes = checkNotNull(hashes);
    this.hashesVerified = hashesVerified;
    this.blobStore = checkNotNull(blobStore);
  }

  /**
   * The actual blob this instance is pointing to.
   */
  public Blob getBlob() {
    return blob;
  }

  /**
   * Exact hashes for the blob. Typically calculated by storage subsystem while blob was getting saved, but sometimes
   * provided based on precalculated or known values.
   */
  public Map<HashAlgorithm, HashCode> getHashes() {
    return hashes;
  }

  /**
   * Returns a boolean indicating whether the hashes associated with this blob have been verified.
   */
  public boolean getHashesVerified() {
    return hashesVerified;
  }

  @Override
  public void close() {
    if (deleted) {
      return;
    }
    try {
      blobStore.deleteHard(blob.getId());
      deleted = true;
    }
    catch (BlobStoreException e) {
      log.debug("Unable to delete blob {} in blob store {}", blob.getId(),
          blobStore.getBlobStoreConfiguration().getName(), e);
    }
  }

  @Override
  public InputStream get() {
    return blob.getInputStream();
  }
}
