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
package org.sonatype.nexus.repository.content;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;

/**
 * Details of the {@link Blob} containing the binary content of an {@link Asset}.
 *
 * Apart from the {@link BlobRef} the rest of these properties are also stored in
 * the blob store, but copies of them are persisted here for performance reasons.
 *
 * @since 3.20
 */
public interface AssetBlob
{
  /**
   * Reference to the blob.
   */
  BlobRef blobRef();

  /**
   * Size of the blob.
   */
  long blobSize();

  /**
   * Content-type of the blob.
   */
  String contentType();

  /**
   * Checksums for the blob.
   *
   * @since 3.24
   */
  Map<String, String> checksums();

  /**
   * When the blob was created.
   */
  OffsetDateTime blobCreated();

  /**
   * When the blob was added to repository.
   */
  OffsetDateTime addedToRepository();

  /**
   * The user that triggered creation of this blob; empty if it was an internal request.
   */
  Optional<String> createdBy();

  /**
   * The client IP that triggered creation of this blob; empty if it was an internal request.
   */
  Optional<String> createdByIp();

  /**
   * Indicates blob should be located by date-based in the blobstore
   */
  boolean useDatePath();
}
