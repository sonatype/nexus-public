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
package org.sonatype.nexus.blobstore;

import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.common.math.Math2;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of {@link BlobStoreMetrics} that supports adding to the blobCount and totalSize fields.
 *
 * @since 3.2.1
 */
public class AccumulatingBlobStoreMetrics
    implements BlobStoreMetrics
{
  private long blobCount;

  private long totalSize;

  private final Map<String, Long> availableSpaceByFileStore;

  private final boolean unlimited;

  public AccumulatingBlobStoreMetrics(
      final long blobCount,
      final long totalSize,
      final Map<String, Long> availableSpaceByFileStore,
      final boolean unlimited)
  {
    this.blobCount = blobCount;
    this.totalSize = totalSize;
    this.availableSpaceByFileStore = checkNotNull(availableSpaceByFileStore);
    this.unlimited = unlimited;
  }

  @Override
  public long getBlobCount() {
    return blobCount;
  }

  public void addBlobCount(long blobCount) {
    this.blobCount += blobCount;
  }

  @Override
  public long getTotalSize() {
    return totalSize;
  }

  public void addTotalSize(long totalSize) {
    this.totalSize += totalSize;
  }

  @Override
  public long getAvailableSpace() {
    return availableSpaceByFileStore.values()
        .stream()
        .reduce(Math2::addClamped)
        .orElse(0L);
  }

  @Override
  public boolean isUnlimited() {
    return unlimited;
  }

  @Override
  public Map<String, Long> getAvailableSpaceByFileStore() {
    return availableSpaceByFileStore;
  }

  @Override
  public boolean isUnavailable() {
    return false;
  }
}
