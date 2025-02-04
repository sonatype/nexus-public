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
package org.sonatype.nexus.blobstore.api.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;

/**
 * Class to hold and manage the metrics for a blobstore.
 */
public class DatastoreBlobStoreMetricsContainer
{
  public final AtomicLong blobstoreUsageDelta = new AtomicLong(0L);

  public final AtomicLong blobCountDelta = new AtomicLong(0L);

  public final Map<OperationType, OperationMetrics> operationMetricsDelta = new EnumMap<>(OperationType.class);

  public DatastoreBlobStoreMetricsContainer() {
    operationMetricsDelta.put(OperationType.UPLOAD, new OperationMetrics());
    operationMetricsDelta.put(OperationType.DOWNLOAD, new OperationMetrics());
  }

  public boolean metricsNeedFlushing() {
    if (blobCountDelta.get() != 0 || blobstoreUsageDelta.get() != 0) {
      return true;
    }

    return operationMetricsDelta
        .values()
        .stream()
        .anyMatch(
            operationMetrics -> operationMetrics.getBlobSize() > 0 ||
                operationMetrics.getErrorRequests() > 0 ||
                operationMetrics.getSuccessfulRequests() > 0 ||
                operationMetrics.getTimeOnRequests() > 0);
  }

  public Map<OperationType, OperationMetrics> getOperationMetricsDelta() {
    return operationMetricsDelta;
  }

  public void recordAddition(final long size) {
    blobstoreUsageDelta.getAndAdd(size);
    blobCountDelta.incrementAndGet();
  }

  public void recordDeletion(final long size) {
    blobstoreUsageDelta.getAndAdd(-size);
    blobCountDelta.decrementAndGet();
  }
}
