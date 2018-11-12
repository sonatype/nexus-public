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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.common.math.Math2;

import static java.lang.Math.addExact;
import static java.util.Collections.unmodifiableMap;

/**
 * An implementation of {@link BlobStoreMetrics} that combines metrics
 * from member metrics.
 *
 * @since 3.14
 */
public class BlobStoreGroupMetrics
    implements BlobStoreMetrics
{
  private final long blobCount;

  private final long totalSize;

  private final Map<String, Long> availableSpaceByFileStore;

  private final boolean unlimited;

  public BlobStoreGroupMetrics(final Iterable<BlobStoreMetrics> membersMetrics) {
    long aggregatedBlobCount = 0L;
    long aggregatedTotalSize = 0L;
    Map<String, Long> aggregatedAvailableSpaceByFileStore = new HashMap<>();
    boolean aggregatedUnlimited = false;

    for (BlobStoreMetrics memberMetrics : membersMetrics) {
      aggregatedBlobCount = Math2.addClamped(aggregatedBlobCount, memberMetrics.getBlobCount());
      aggregatedTotalSize = Math2.addClamped(aggregatedTotalSize, memberMetrics.getTotalSize());
      aggregatedAvailableSpaceByFileStore.putAll(memberMetrics.getAvailableSpaceByFileStore());
      aggregatedUnlimited = aggregatedUnlimited || memberMetrics.isUnlimited();
    }

    this.blobCount = aggregatedBlobCount;
    this.totalSize = aggregatedTotalSize;
    this.availableSpaceByFileStore = unmodifiableMap(aggregatedAvailableSpaceByFileStore);
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
    return availableSpaceByFileStore.values().stream()
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
}
