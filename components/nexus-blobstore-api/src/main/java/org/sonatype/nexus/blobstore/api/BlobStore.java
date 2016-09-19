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
package org.sonatype.nexus.blobstore.api;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;

import com.google.common.hash.HashCode;

/**
 * A generic storage bin for binary objects of all sizes.
 *
 * In general, most methods can throw {@link BlobStoreException} for conditions such as network connectivity problems,
 * or file IO issues, blob store misconfiguration, or internal corruption.
 *
 * @since 3.0
 */
public interface BlobStore
    extends Lifecycle
{
  /**
   * An identifying name for disaster recovery purposes (which isn't required to be strictly unique)
   */
  String BLOB_NAME_HEADER = "BlobStore.blob-name";

  /**
   * An informational header for disaster recovery purposes (describes what blob contains)
   */
  String CONTENT_TYPE_HEADER = "BlobStore.content-type";

  /**
   * Audit information (e.g. the name of a principal that created the blob)
   */
  String CREATED_BY_HEADER = "BlobStore.created-by";

  /**
   * Creates a new blob. The header map must contain at least two keys:
   *
   * <ul>
   * <li>{@link #BLOB_NAME_HEADER}</li>
   * <li>{@link #CREATED_BY_HEADER}</li>
   * </ul>
   *
   * @throws BlobStoreException       (or a subclass) if the input stream can't be read correctly
   * @throws IllegalArgumentException if mandatory headers are missing
   */
  Blob create(InputStream blobData, Map<String, String> headers);

  /**
   * Imports a blob by creating a hard link, throwing {@link BlobStoreException} if that's not supported
   * from the source file's location.
   *
   * Otherwise similar to {@link #create(InputStream, Map)} with the difference that a known file size and sha1 are
   * already provided.
   *
   * @since 3.1
   */
  Blob create(Path sourceFile, Map<String, String> headers, long size, HashCode sha1);

  /**
   * Returns the corresponding {@link Blob}, or {@code null} if the  blob does not exist or has been {@link #delete
   * deleted}.
   */
  @Nullable
  Blob get(BlobId blobId);

  /**
   * Removes a blob from the blob store.  This may not immediately delete the blob from the underlying storage
   * mechanism, but will make it immediately unavailable to future calls to {@link BlobStore#get(BlobId)}.
   *
   * @return {@code true} if the blob has been deleted, {@code false} if no blob was found by that ID.
   */
  boolean delete(BlobId blobId);

  /**
   * Removes a blob from the blob store immediately, disregarding any locking or concurrent access by other threads.
   * This should be considered exceptional (e.g. administrative) usage.
   *
   * @return {@code true} if the blob has been deleted, {@code false} if no blob was found by that ID.
   */
  boolean deleteHard(BlobId blobId);

  /**
   * Provides metrics about the BlobStore's usage.
   */
  BlobStoreMetrics getMetrics();

  /**
   * Perform garbage collection, purging blobs marked for deletion or whatever other periodic, implementation-specific
   * tasks need doing.
   */
  void compact();

  /**
   * Returns the configuration entity for the BlobStore.
   */
  BlobStoreConfiguration getBlobStoreConfiguration();

  /**
   * Initialize the BlobStore.
   */
  void init(BlobStoreConfiguration configuration) throws Exception;

  /**
   * Signifies that the {@link BlobStoreManager} has permanently deleted this blob store.
   */
  void remove();
}
