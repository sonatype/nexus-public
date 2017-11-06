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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.PeriodicJobService;
import org.sonatype.nexus.blobstore.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import com.amazonaws.services.s3.AmazonS3;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Retains blobstore metrics in memory, periodically writing them out
 * to AWS S3.  Similar to the file blobstore {@link BlobStoreMetricsStore}.
 *
 * @since 3.7
 */
@Named
public class S3BlobStoreMetricsStore
    extends StateGuardLifecycleSupport
{
  private static final String METRICS_PREFIX = "metrics-";

  private static final String METRICS_SUFFIX = ".properties";

  private static final String TOTAL_SIZE_PROP_NAME = "totalSize";

  private static final String BLOB_COUNT_PROP_NAME = "blobCount";

  private static final int METRICS_FLUSH_PERIOD_SECONDS = 2;

  private final PeriodicJobService jobService;

  private AtomicLong blobCount;

  private final NodeAccess nodeAccess;

  private AtomicLong totalSize;

  private AtomicBoolean dirty;

  private PeriodicJob metricsWritingJob;

  private String bucket;

  private S3PropertiesFile propertiesFile;

  private AmazonS3 s3;

  @Inject
  public S3BlobStoreMetricsStore(final PeriodicJobService jobService, final NodeAccess nodeAccess) {
    this.jobService = checkNotNull(jobService);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  protected void doStart() throws Exception {
    blobCount = new AtomicLong();
    totalSize = new AtomicLong();
    dirty = new AtomicBoolean();

    propertiesFile = new S3PropertiesFile(s3, bucket, METRICS_PREFIX + nodeAccess.getId() + METRICS_SUFFIX);
    if (propertiesFile.exists()) {
      log.info("Loading blob store metrics file {}", propertiesFile);
      propertiesFile.load();
      readProperties();
    }
    else {
      log.info("Blob store metrics file {} not found - initializing at zero.", propertiesFile);
      updateProperties();
      propertiesFile.store();
    }

    jobService.startUsing();
    metricsWritingJob = jobService.schedule(() -> {
      try {
        if (dirty.compareAndSet(true, false)) {
          updateProperties();
          log.trace("Writing blob store metrics to {}", propertiesFile);
          propertiesFile.store();
        }
      }
      catch (Exception e) {
        // Don't propagate, as this stops subsequent executions
        log.error("Cannot write blob store metrics", e);
      }
    }, METRICS_FLUSH_PERIOD_SECONDS);
  }

  @Override
  protected void doStop() throws Exception {
    metricsWritingJob.cancel();
    metricsWritingJob = null;
    jobService.stopUsing();

    blobCount = null;
    totalSize = null;
    dirty = null;

    propertiesFile = null;
  }

  public void setBucket(final String bucket) {
    checkState(this.bucket == null, "Do not initialize twice");
    checkNotNull(bucket);
    this.bucket = bucket;
  }

  public void setS3(final AmazonS3 s3) {
    checkState(this.s3 == null, "Do not initialize twice");
    checkNotNull(s3);
    this.s3 = s3;
  }

  @Guarded(by = STARTED)
  public BlobStoreMetrics getMetrics() {
    Stream<S3PropertiesFile> blobStoreMetricsFiles = backingFiles();
    return getCombinedMetrics(blobStoreMetricsFiles);
  }

  private BlobStoreMetrics getCombinedMetrics(final Stream<S3PropertiesFile> blobStoreMetricsFiles) {
    AccumulatingBlobStoreMetrics blobStoreMetrics = new AccumulatingBlobStoreMetrics(0, 0, -1, true);

    blobStoreMetricsFiles.forEach(metricsFile -> {
        try {
          metricsFile.load();
          blobStoreMetrics.addBlobCount(parseLong(metricsFile.getProperty(BLOB_COUNT_PROP_NAME, "0")));
          blobStoreMetrics.addTotalSize(parseLong(metricsFile.getProperty(TOTAL_SIZE_PROP_NAME, "0")));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
    });
    return blobStoreMetrics;
  }

  @Guarded(by = STARTED)
  public void recordAddition(final long size) {
    blobCount.incrementAndGet();
    totalSize.addAndGet(size);
    dirty.set(true);
  }

  @Guarded(by = STARTED)
  public void recordDeletion(final long size) {
    blobCount.decrementAndGet();
    totalSize.addAndGet(-size);
    dirty.set(true);
  }

  public void remove() {
    backingFiles().forEach(metricsFile -> {
        try {
          metricsFile.remove();
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
    });
  }

  private Stream<S3PropertiesFile> backingFiles() {
    if (s3 == null) {
      return Stream.empty();
    } else {
      Stream<S3PropertiesFile> stream = s3.listObjects(bucket, METRICS_PREFIX).getObjectSummaries().stream()
          .filter(summary -> summary.getKey().endsWith(METRICS_SUFFIX))
          .map(summary -> new S3PropertiesFile(s3, bucket, summary.getKey()));
      return stream;
    }
  }

  private void updateProperties() {
    propertiesFile.setProperty(TOTAL_SIZE_PROP_NAME, totalSize.toString());
    propertiesFile.setProperty(BLOB_COUNT_PROP_NAME, blobCount.toString());
  }

  private void readProperties() {
    String size = propertiesFile.getProperty(TOTAL_SIZE_PROP_NAME);
    if (size != null) {
      totalSize.set(parseLong(size));
    }

    String count = propertiesFile.getProperty(BLOB_COUNT_PROP_NAME);
    if (count != null) {
      blobCount.set(parseLong(count));
    }
  }
}
