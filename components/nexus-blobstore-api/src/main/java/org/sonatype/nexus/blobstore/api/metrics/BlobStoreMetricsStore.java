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

/**
 * Store for managing DB based blobstore metrics.
 */
public interface BlobStoreMetricsStore
{
  /**
   * Update metrics for the given {@link BlobStoreMetricsEntity}.
   */
  void updateMetrics(BlobStoreMetricsEntity blobStoreMetricsEntity);

  /**
   * Retrieve metrics for the given blobstore name.
   */
  BlobStoreMetricsEntity get(String blobStoreName);

  /**
   * Remove metrics for the given blobstore name.
   */
  void remove(String blobStoreName);

  /**
   * Clear the operation metrics for the given blobstore name.
   */
  void clearOperationMetrics(String blobStoreName);

  /**
   * Clear the summary metrics for the given blob store name.
   * (total size , blob count)
   */
  void clearCountMetrics(String blobStoreName);

  /**
   * Initialize the metrics for the given blobstore.
   */
  void initializeMetrics(String blobStoreName);
}
