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
package org.sonatype.nexus.blobstore.s3.internal.datastore;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.metrics.DatastoreBlobStoreMetricsServiceSupport;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreMetricsService;
import org.sonatype.nexus.scheduling.PeriodicJobService;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.collect.ImmutableMap;

@Named
@Priority(Integer.MAX_VALUE)
public class DatastoreS3BlobStoreMetricsService
    extends DatastoreBlobStoreMetricsServiceSupport
    implements S3BlobStoreMetricsService
{
  private static final ImmutableMap<String, Long> AVAILABLE_SPACE_BY_FILE_STORE = ImmutableMap.of("s3", Long.MAX_VALUE);

  @Inject
  public DatastoreS3BlobStoreMetricsService(
      @Named("${nexus.blobstore.metrics.flushInterval:-2}") final int metricsFlushPeriodSeconds,
      final BlobStoreMetricsStore blobStoreMetricsStore,
      final PeriodicJobService jobService)
  {
    super(metricsFlushPeriodSeconds, jobService, blobStoreMetricsStore);
  }

  @Override
  public BlobStoreMetrics getMetrics() {
    BlobStoreMetricsEntity metricsEntity =
        blobStoreMetricsStore.get(blobStore.getBlobStoreConfiguration().getName());

    return new AccumulatingBlobStoreMetrics(
        metricsEntity.getBlobCount(),
        metricsEntity.getTotalSize(),
        AVAILABLE_SPACE_BY_FILE_STORE,
        true
    );
  }

  @Override
  public void setBucket(final String configuredBucket) {
    //no op - this implementation doesn't require a configuredBucket
  }

  @Override
  public void setBucketPrefix(final String bucketPrefix) {
    //no op - this implementation doesn't require a bucketPrefix
  }

  @Override
  public void setS3(final AmazonS3 s3) {
    //no op - this implementation doesn't require an s3 client
  }
}
