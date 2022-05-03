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

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsService;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.ImplicitSourcePropertiesFile;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.scheduling.PeriodicJobService.PeriodicJob;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Stream.iterate;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.createQuotaCheckJob;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

public abstract class BlobStoreMetricsStoreSupport<T extends ImplicitSourcePropertiesFile>
    extends StateGuardLifecycleSupport
    implements BlobStoreMetricsService
{
  private static final int METRICS_LOADING_DELAY_MILLIS = 200;

  public static final int MAXIMUM_TRIES = 3;

  @VisibleForTesting
  public static final String METRICS_FILENAME = "metrics.properties";

  @VisibleForTesting
  public static final String TOTAL_SIZE_PROP_NAME = "totalSize";

  @VisibleForTesting
  public static final String BLOB_COUNT_PROP_NAME = "blobCount";

  private static final String TOTAL_SUCCESSFUL_REQUESTS_PROP_NAME = "_totalSuccessfulRequests";

  private static final String TOTAL_ERROR_REQUESTS_PROP_NAME = "_totalErrorRequests";

  private static final String TOTAL_TIME_ON_REQUEST_PROP_NAME = "_totalTimeOnRequests";

  public static final String TOTAL_SIZE_ON_REQUEST_PROP_NAME = "_totalSizeOnRequests";

  private static final int METRICS_FLUSH_PERIOD_SECONDS = 2;

  protected AtomicLong blobCount;

  protected AtomicLong totalSize;

  protected AtomicBoolean dirty;

  protected BlobStore blobStore;

  protected final NodeAccess nodeAccess;

  protected T properties;

  protected final PeriodicJobService jobService;

  protected PeriodicJob metricsWritingJob;

  private Map<OperationType, OperationMetrics> operationMetrics;

  public BlobStoreMetricsStoreSupport(final NodeAccess nodeAccess,
                                      final PeriodicJobService jobService)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.jobService = checkNotNull(jobService);
  }

  @Override
  protected void doStart() throws Exception {
    blobCount = new AtomicLong();

    operationMetrics = new EnumMap<>(OperationType.class);
    for (OperationType type : OperationType.values()) {
      operationMetrics.put(type, new OperationMetrics());
    }
    totalSize = new AtomicLong();
    dirty = new AtomicBoolean();

    properties = getProperties();
    if (properties.exists()) {
      log.info("Loading blob store metrics file {}", properties);
      properties.load();
      readProperties();
    }
    else {
      log.info("Blob store metrics file {} not found - initializing at zero.", properties);
      flushProperties();
    }

    jobService.startUsing();
    metricsWritingJob = jobService.schedule(() -> {
      try {
        if (dirty.compareAndSet(true, false)) {
          flushProperties();
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
    blobStore = null;
    metricsWritingJob.cancel();
    metricsWritingJob = null;
    jobService.stopUsing();

    blobCount = null;
    operationMetrics.clear();
    totalSize = null;
    dirty = null;

    properties = null;
  }

  protected abstract T getProperties();

  protected abstract AccumulatingBlobStoreMetrics getAccumulatingBlobStoreMetrics() throws BlobStoreMetricsNotAvailableException;

  protected abstract Stream<T> backingFiles() throws BlobStoreMetricsNotAvailableException;

  @VisibleForTesting
  public synchronized void flushProperties() throws IOException {
    updateProperties();
    log.trace("Writing blob store metrics to {}", properties);
    properties.store();
  }

  protected BlobStoreMetrics getCombinedMetrics(final Stream<T> blobStoreMetricsFiles) throws BlobStoreMetricsNotAvailableException {
    AccumulatingBlobStoreMetrics blobStoreMetrics = getAccumulatingBlobStoreMetrics();
    blobStoreMetricsFiles.forEach(metricsFile -> {
      iterate(1, i -> i + 1)
          .limit(MAXIMUM_TRIES)
          .forEach(currentTry -> {
                try {
                  metricsFile.load();
                }
                catch (IOException e) {
                  log.debug("Unable to load properties file {}. Try number {} of {}.", metricsFile,
                      currentTry, MAXIMUM_TRIES, e);
                  if (currentTry >= MAXIMUM_TRIES) {
                    throw new RuntimeException("Failed to load blob store metrics from " + metricsFile,
                        e);
                  }
                  try {
                    MILLISECONDS.sleep(METRICS_LOADING_DELAY_MILLIS);
                  }
                  catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                  }
                }
              }
          );

      blobStoreMetrics.addBlobCount(parseLong(metricsFile.getProperty(BLOB_COUNT_PROP_NAME, "0")));
      blobStoreMetrics.addTotalSize(parseLong(metricsFile.getProperty(TOTAL_SIZE_PROP_NAME, "0")));
    });
    return blobStoreMetrics;
  }

  @Override
  public void setBlobStore(final BlobStore blobStore) {
    checkState(this.blobStore == null, "Do not initialize twice");
    checkNotNull(blobStore);
    this.blobStore = blobStore;
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetrics getMetrics() {
    try {
      return getCombinedMetrics(backingFiles());
    }
    catch (BlobStoreMetricsNotAvailableException e) {
      log.error("Blob store metrics cannot be accessed", e);
      return UnavailableBlobStoreMetrics.getInstance();
    }
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetrics() {
    return operationMetrics;
  }

  @Override
  @Guarded(by = STARTED)
  public void recordAddition(final long size) {
    blobCount.incrementAndGet();
    totalSize.addAndGet(size);
    dirty.set(true);
  }

  @Override
  @Guarded(by = STARTED)
  public void recordDeletion(final long size) {
    blobCount.decrementAndGet();
    totalSize.addAndGet(-size);
    dirty.set(true);
  }

  private void updateProperties() {
    properties.setProperty(TOTAL_SIZE_PROP_NAME, totalSize.toString());
    properties.setProperty(BLOB_COUNT_PROP_NAME, blobCount.toString());

    for (Entry<OperationType, OperationMetrics> operationMetricByType : operationMetrics.entrySet()) {
      OperationType type = operationMetricByType.getKey();
      OperationMetrics operationMetric = operationMetricByType.getValue();
      properties.setProperty(type + TOTAL_SUCCESSFUL_REQUESTS_PROP_NAME,
          String.valueOf(operationMetric.getSuccessfulRequests()));
      properties.setProperty(type + TOTAL_ERROR_REQUESTS_PROP_NAME,
          String.valueOf(operationMetric.getErrorRequests()));
      properties.setProperty(type + TOTAL_TIME_ON_REQUEST_PROP_NAME,
          String.valueOf(operationMetric.getTimeOnRequests()));
      properties.setProperty(type + TOTAL_SIZE_ON_REQUEST_PROP_NAME,
          String.valueOf(operationMetric.getBlobSize()));
    }
  }

  private void readProperties() {
    String size = properties.getProperty(TOTAL_SIZE_PROP_NAME);
    if (size != null) {
      totalSize.set(parseLong(size));
    }

    String count = properties.getProperty(BLOB_COUNT_PROP_NAME);
    if (count != null) {
      blobCount.set(parseLong(count));
    }

    for (Entry<OperationType, OperationMetrics> operationMetricByType : operationMetrics.entrySet()) {
      OperationType type = operationMetricByType.getKey();
      OperationMetrics operationMetric = operationMetricByType.getValue();

      String totalSuccessfulRequests = properties.getProperty(type + TOTAL_SUCCESSFUL_REQUESTS_PROP_NAME);
      if (totalSuccessfulRequests != null) {
        operationMetric.setSuccessfulRequests(parseLong(totalSuccessfulRequests));
      }

      String totalErrorRequests = properties.getProperty(type + TOTAL_ERROR_REQUESTS_PROP_NAME);
      if (totalErrorRequests != null) {
        operationMetric.setErrorRequests(parseLong(totalErrorRequests));
      }

      String totalTimeOnRequests = properties.getProperty(type + TOTAL_TIME_ON_REQUEST_PROP_NAME);
      if (totalTimeOnRequests != null) {
        operationMetric.setTimeOnRequests(parseLong(totalTimeOnRequests));
      }

      String totalSizeOnRequests = properties.getProperty(type + TOTAL_SIZE_ON_REQUEST_PROP_NAME);
      if (totalSizeOnRequests != null) {
        operationMetric.setBlobSize(parseLong(totalSizeOnRequests));
      }
    }
  }

  @Override
  public void clearOperationMetrics() {
    getOperationMetricsDelta().values().forEach(OperationMetrics::clear);
    getOperationMetrics().values().forEach(OperationMetrics::clear);
  }

  @Override
  public abstract void remove();
}
