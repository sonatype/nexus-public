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
import java.util.Map;

import javax.annotation.Nullable;

/**
 * An interface for listening to blob store events.
 *
 * Listeners must be thread-safe, able to receive multiple events concurrently.
 *
 * @since 3.0
 */
public interface BlobStoreListener
{
  /**
   * Fired after the blob store completes {@link BlobStore#create(InputStream, Map) creation of a new blob}.
   *
   * @param message an implementation-specific message (e.g. the file the blob was written to)
   */
  void blobCreated(Blob blob, @Nullable String message);

  /**
   * Fired when the blob store initiates {@link Blob#getInputStream() retrieval of a blob's bytes}.
   *
   * @param message an implementation-specific message (e.g. which file was accessed)
   */
  void blobAccessed(Blob blob, @Nullable String message);

  /**
   * Fired after the blob store {@link BlobStore#delete(BlobId) marks a blob for deletion}.
   *
   * @param message an implementation-specific message (e.g. which file was queued for deletion)
   */
  void blobDeleteRequested(BlobId blobId, @Nullable String message);

  /**
   * Fired after the blob store removes a blob's bytes from the underlying storage mechanism, whether by an internal
   * cleanup process or by a {@link BlobStore#deleteHard(BlobId) hard delete}.
   *
   * @param message an implementation-specific message (e.g. which file was deleted)
   */
  void blobDeleted(BlobId blobId, @Nullable String message);
}
