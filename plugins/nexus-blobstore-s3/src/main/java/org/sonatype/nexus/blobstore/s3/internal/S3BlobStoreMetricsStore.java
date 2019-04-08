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
package org.sonatype.nexus.blobstore.s3.internal;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.BlobStoreMetricsStoreSupport;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.scheduling.PeriodicJobService;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A S3 specific {@link BlobStoreMetricsStoreSupport} implementation that retains blobstore metrics in memory,
 * periodically writing them out to S3.
 *
 * @since 3.6.1
 */
@Named
public class S3BlobStoreMetricsStore
    extends BlobStoreMetricsStoreSupport<S3PropertiesFile>
{

  static final ImmutableMap<String, Long> AVAILABLE_SPACE_BY_FILE_STORE = ImmutableMap.of("s3", Long.MAX_VALUE);

  private String bucket;

  private String bucketPrefix;

  private AmazonS3 s3;

  @Inject
  public S3BlobStoreMetricsStore(final PeriodicJobService jobService,
                                 final NodeAccess nodeAccess,
                                 final BlobStoreQuotaService quotaService,
                                 @Named("${nexus.blobstore.quota.warnIntervalSeconds:-60}")
                                 final int quotaCheckInterval)
  {
    super(nodeAccess, jobService, quotaService, quotaCheckInterval);
  }

  @Override
  protected S3PropertiesFile getProperties() {
    return new S3PropertiesFile(s3, bucket, bucketPrefix + nodeAccess.getId() + "-" + METRICS_FILENAME);
  }

  @Override
  protected AccumulatingBlobStoreMetrics getAccumulatingBlobStoreMetrics() {
    return new AccumulatingBlobStoreMetrics(0, 0, AVAILABLE_SPACE_BY_FILE_STORE, true);
  }

  @Override
  protected Stream<S3PropertiesFile> backingFiles() {
    if (s3 == null) {
      return Stream.empty();
    }
    else {
      Stream<S3PropertiesFile> stream = s3.listObjects(bucket, bucketPrefix + nodeAccess.getId()).getObjectSummaries()
          .stream()
          .filter(Objects::nonNull)
          .filter(summary -> summary.getKey().endsWith(METRICS_FILENAME))
          .map(summary -> new S3PropertiesFile(s3, bucket, summary.getKey()));
      return stream;
    }
  }

  public void setBucket(final String bucket) {
    checkNotNull(bucket);
    this.bucket = bucket;
  }

  public void setS3(final AmazonS3 s3) {
    checkNotNull(s3);
    this.s3 = s3;
  }

  public void setBucketPrefix(String bucketPrefix) {
    checkNotNull(bucketPrefix);
    this.bucketPrefix = bucketPrefix;
  }

  @Override
  public void remove() {
    backingFiles().forEach(metricsFile -> {
      try {
        log.debug("Removing {}", metricsFile);
        metricsFile.remove();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
