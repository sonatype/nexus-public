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

/**
 * A handle for binary data stored within a {@link BlobStore}.
 *
 * @since 3.0
 */
public interface Blob
{
  BlobId getId();

  /**
   * An immutable map of the headers that were provided when the blob was created.
   *
   * @throws BlobStoreException may be thrown if the blob is {@link BlobStore#delete deleted} or
   *                            {@link BlobStore#delete hard deleted}.
   */
  Map<String, String> getHeaders();

  /**
   * Opens an input stream to the blob's content. The returned stream may be closed asynchronously if the blob is
   * {@link BlobStore#deleteHard(BlobId) hard deleted}.
   *
   * @throws BlobStoreException may be thrown if the blob is {@link BlobStore#delete deleted} or
   *                            {@link BlobStore#delete hard deleted}.
   */
  InputStream getInputStream();

  /**
   * Provides metrics about this Blob.
   *
   * @throws BlobStoreException may be thrown if the blob is {@link BlobStore#delete deleted} or
   *                            {@link BlobStore#delete hard deleted}.
   */
  BlobMetrics getMetrics();
}
