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

import java.io.IOException;
import java.util.Map;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;

/**
 * Service for handling metrics for blobstores. They are NOT singletons, there is an instance for each blobstore.
 */
public interface BlobStoreMetricsService<B extends BlobStore>
    extends Lifecycle
{
  /**
   * Retrieve the metrics for the attached blobstore.
   */
  BlobStoreMetrics getMetrics();

  void init(B blobStore) throws Exception;

  /**
   * Record a new blob being added to the blob store.
   *
   * @param size Size of the newly added blob in bytes.
   */
  void recordAddition(final long size);

  /**
   * Record a blob being deleted from the from blobstore.
   *
   * @param size Size of the deleted blob in bytes.
   */
  void recordDeletion(final long size);

  /**
   * Get the operation metrics for the attached blobstore.
   */
  Map<OperationType, OperationMetrics> getOperationMetrics();

  /**
   * Get the current delta set of operation metrics. This used to track operation metric data to be flushed.
   */
  Map<OperationType, OperationMetrics> getOperationMetricsDelta();

  /**
   * Reset the total metrics (blob count / size)
   */
  void clearCountMetrics();

  /**
   * Reset the operational metrics.
   */
  void clearOperationMetrics();

  /**
   * Flush the metrics to the metric store.
   */
  void flush() throws IOException;

  /**
   * Remove metrics for the given blobstore.
   */
  void remove();
}
