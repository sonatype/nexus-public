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
package org.sonatype.nexus.blobstore.group.internal;

import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;

import static java.lang.Math.addExact;

/**
 * An implementation of {@link BlobStoreMetrics} that combines metrics
 * from member metrics.
 *
 * @since 3.next
 */
public class BlobStoreGroupMetrics
    implements BlobStoreMetrics
{
  private final long blobCount;

  private final long totalSize;

  private final long availableSpace;

  private final boolean unlimited;

  public BlobStoreGroupMetrics(final Iterable<BlobStoreMetrics> membersMetrics) {
    long aggregatedBlobCount = 0l;
    long aggregatedTotalSize = 0l;
    long aggregatedAvailableSpace = 0l;
    boolean aggregatedUnlimited = false;

    for (BlobStoreMetrics memberMetrics : membersMetrics) {
      aggregatedBlobCount = clampedAdd(aggregatedBlobCount, memberMetrics.getBlobCount());
      aggregatedTotalSize = clampedAdd(aggregatedTotalSize, memberMetrics.getTotalSize());
      aggregatedAvailableSpace = clampedAdd(aggregatedAvailableSpace, memberMetrics.getAvailableSpace());
      aggregatedUnlimited = aggregatedUnlimited || memberMetrics.isUnlimited();
    }

    this.blobCount = aggregatedBlobCount;
    this.totalSize = aggregatedTotalSize;
    this.availableSpace = aggregatedAvailableSpace;
    this.unlimited = aggregatedUnlimited;
  }

  @Override
  public long getBlobCount() {
    return blobCount;
  }

  @Override
  public long getTotalSize() {
    return totalSize;
  }

  @Override
  public long getAvailableSpace() {
    return availableSpace;
  }

  @Override
  public boolean isUnlimited() {
    return unlimited;
  }

  /**
   * Add longs with overflow clamped at {@link Long.MAX_VALUE}.
   */
  private long clampedAdd(final long a, final long b) {
    try {
      return addExact(a, b);
    }
    catch (ArithmeticException e) { // NOSONAR
      return Long.MAX_VALUE;
    }
  }
}
