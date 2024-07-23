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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.BlobStoreMetricsNotAvailableException;
import org.sonatype.nexus.blobstore.BlobStoreMetricsStoreSupport;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.scheduling.PeriodicJobService;

import com.amazonaws.SdkClientException;
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
public class LegacyS3BlobStoreMetricsStore
    extends BlobStoreMetricsStoreSupport<S3PropertiesFile>
    implements S3BlobStoreMetricsService
{

  static final ImmutableMap<String, Long> AVAILABLE_SPACE_BY_FILE_STORE = ImmutableMap.of("s3", Long.MAX_VALUE);

  private String bucket;

  private String bucketPrefix;

  private AmazonS3 s3;

  @Inject
  public LegacyS3BlobStoreMetricsStore(
      final PeriodicJobService jobService,
      final NodeAccess nodeAccess)
  {
    super(nodeAccess, jobService);
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
  protected Stream<S3PropertiesFile> backingFiles() throws BlobStoreMetricsNotAvailableException {
    try {
      if (s3 == null) {
        return Stream.empty();
      }
      else {
        return s3.listObjects(bucket, bucketPrefix + nodeAccess.getId()).getObjectSummaries()
            .stream()
            .filter(Objects::nonNull)
            .filter(summary -> summary.getKey().endsWith(METRICS_FILENAME))
            .map(summary -> new S3PropertiesFile(s3, bucket, summary.getKey()));
      }
    }
    catch (SdkClientException e) {
      throw new BlobStoreMetricsNotAvailableException(e);
    }
  }

  @Override
  public void setBucket(final String bucket) {
    checkNotNull(bucket);
    this.bucket = bucket;
  }

  @Override
  public void setS3(final AmazonS3 s3) {
    checkNotNull(s3);
    this.s3 = s3;
  }

  @Override
  public void setBucketPrefix(final String bucketPrefix) {
    checkNotNull(bucketPrefix);
    this.bucketPrefix = bucketPrefix;
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetricsDelta() {
    return super.getOperationMetrics();
  }

  @Override
  public void flush() throws IOException {
    super.flushProperties();
  }

  @Override
  public void remove() {
    try {
      backingFiles().forEach(this::removeQuietly);
    }
    catch (BlobStoreMetricsNotAvailableException e) {
      log.warn("Unable to remove metrics files", e);
    }
  }

  private void removeQuietly(final S3PropertiesFile file) {
    try {
      log.debug("Removing {}", file);
      file.remove();
    }
    catch (IOException e) {
      log.warn("Unable to remove metrics file {}", file, e);
    }
  }
}
