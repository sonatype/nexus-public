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

import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.TransactionalSession;

import com.google.common.hash.HashCode;

/**
 * Represents a session with a {@link BlobStore}.
 *
 * @since 3.20
 */
public interface BlobSession<T extends Transaction>
    extends TransactionalSession<T>
{
  /**
   * @see BlobStore#create(InputStream, Map)
   */
  default Blob create(InputStream blobData, Map<String, String> headers) {
    return create(blobData, headers, null);
  }

  /**
   * @see BlobStore#create(InputStream, Map, BlobId)
   */
  Blob create(InputStream blobData, Map<String, String> headers, @Nullable BlobId blobId);

  /**
   * @see BlobStore#create(Path, Map, long, HashCode)
   */
  Blob create(Path sourceFile, Map<String, String> headers, long size, HashCode sha1);

  /**
   * @see BlobStore#copy(BlobId, Map)
   */
  Blob copy(BlobId blobId, Map<String, String> headers);

  /**
   * @see BlobStore#get(BlobId)
   */
  @Nullable
  default Blob get(BlobId blobId) {
    return get(blobId, false);
  }

  /**
   * @see BlobStore#get(BlobId, boolean)
   */
  @Nullable
  Blob get(BlobId blobId, boolean includeDeleted);

  /**
   * @see BlobStore#exists(BlobId)
   */
  boolean exists(BlobId blobId);

  /**
   * @see BlobStore#delete(BlobId, String)
   */
  boolean delete(BlobId blobId);
}
