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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.internal.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Stream.iterate;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * A {@link BlobStoreMetricsStore} implementation that retains blobstore metrics in memory, periodically
 * writing them out to a file.
 *
 * @since 3.0
 */
@Named
public class BlobStoreMetricsStoreImpl
    extends StateGuardLifecycleSupport
    implements BlobStoreMetricsStore
{
  @VisibleForTesting
  static final String METRICS_FILENAME = "metrics.properties";

  @VisibleForTesting
  static final String TOTAL_SIZE_PROP_NAME = "totalSize";

  @VisibleForTesting
  static final String BLOB_COUNT_PROP_NAME = "blobCount";

  private static final int METRICS_FLUSH_PERIOD_SECONDS = 2;

  private static final int METRICS_LOADING_DELAY_MILLIS = 200;

  private final PeriodicJobService jobService;

  private AtomicLong blobCount;

  private final NodeAccess nodeAccess;

  private AtomicLong totalSize;

  private AtomicBoolean dirty;

  private PeriodicJob metricsWritingJob;

  private Path storageDirectory;

  private Path metricsDataFile;

  private PropertiesFile propertiesFile;

  @Inject
  public BlobStoreMetricsStoreImpl(final PeriodicJobService jobService, final NodeAccess nodeAccess) {
    this.jobService = checkNotNull(jobService);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  protected void doStart() throws Exception {
    blobCount = new AtomicLong();
    totalSize = new AtomicLong();
    dirty = new AtomicBoolean();

    metricsDataFile = storageDirectory.resolve(nodeAccess.getId() + "-" + METRICS_FILENAME);
    propertiesFile = new PropertiesFile(metricsDataFile.toFile());
    if (Files.exists(metricsDataFile)) {
      log.info("Loading blob store metrics file {}", metricsDataFile);
      propertiesFile.load();
      readProperties();
    }
    else {
      log.info("Blob store metrics file {} not found - initializing at zero.", metricsDataFile);
      updateProperties();
      propertiesFile.store();
    }

    jobService.startUsing();
    metricsWritingJob = jobService.schedule(() -> {
      try {
        if (dirty.compareAndSet(true, false)) {
          updateProperties();
          log.trace("Writing blob store metrics to {}", metricsDataFile);
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

  @Override
  public void setStorageDir(final Path storageDirectory) {
    checkState(this.storageDirectory == null, "Do not initialize twice");
    checkNotNull(storageDirectory);
    checkArgument(Files.isDirectory(storageDirectory));
    this.storageDirectory = storageDirectory;
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetrics getMetrics() {

    File[] blobStoreMetricsFiles = listBackingFiles();
    return getCombinedMetrics(blobStoreMetricsFiles);

  }

  private long getAvailableSpace() {
    try {
      return Files.getFileStore(storageDirectory).getUsableSpace();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private BlobStoreMetrics getCombinedMetrics(final File[] blobStoreMetricsFiles) {
    AccumulatingBlobStoreMetrics blobStoreMetrics = new AccumulatingBlobStoreMetrics(0, 0, getAvailableSpace());
    for (File metricsFile : blobStoreMetricsFiles) {
      PropertiesFile metricsPropertiesFile = new PropertiesFile(metricsFile);

      int maximumTries = 3;
      iterate(1, i -> i + 1)
          .limit(maximumTries)
          .forEach(currentTry -> {
                try {
                  metricsPropertiesFile.load();
                }
                catch (IOException e) {
                  log.debug("Unable to load properties file {}. Try number {} of {}.", metricsPropertiesFile.getFile(),
                      currentTry, maximumTries, e);
                  if (currentTry >= maximumTries) {
                    throw new RuntimeException("Failed to load blob store metrics from " + metricsFile, e);
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

      blobStoreMetrics.addBlobCount(parseLong(metricsPropertiesFile.getProperty(BLOB_COUNT_PROP_NAME, "0")));
      blobStoreMetrics.addTotalSize(parseLong(metricsPropertiesFile.getProperty(TOTAL_SIZE_PROP_NAME, "0")));
    }
    return blobStoreMetrics;
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

  @Override
  public File[] listBackingFiles() {
    return storageDirectory.toFile().listFiles((dir, name) -> name.endsWith(METRICS_FILENAME));
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
