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

/**
 * Provides usage metrics for a blob-store.
 *
 * @since 3.0
 */
public interface BlobStoreMetrics
{
  /**
   * Get an approximate count of the number of blobs in the blob store.
   */
  long getBlobCount();

  /**
   * Get the approximate total storage space consumed by this blob store in bytes, including blobs, headers, and any
   * other metadata required by the store.
   */
  long getTotalSize();

  /**
   * An estimate of the remaining space available in the blob store, in bytes. For certain blob stores like S3, this
   * may return a value set by a policy rather than some hard storage limit.
   */
  long getAvailableSpace();
}
