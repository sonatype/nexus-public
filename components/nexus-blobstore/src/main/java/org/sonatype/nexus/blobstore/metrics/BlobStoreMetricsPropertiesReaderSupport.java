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
package org.sonatype.nexus.blobstore.metrics;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.BlobStoreMetricsNotAvailableException;
import org.sonatype.nexus.blobstore.UnavailableBlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsPropertiesReader;
import org.sonatype.nexus.common.property.ImplicitSourcePropertiesFile;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Stream.iterate;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * @deprecated legacy method for metrics stored in the blob store, only used for migrating data to the db
 */
@Deprecated
public abstract class BlobStoreMetricsPropertiesReaderSupport<B extends BlobStore, T extends ImplicitSourcePropertiesFile>
    extends StateGuardLifecycleSupport
    implements BlobStoreMetricsPropertiesReader<B>
{
  private static final int METRICS_LOADING_DELAY_MILLIS = 200;

  public static final int MAXIMUM_TRIES = 3;

  public static final String METRICS_FILENAME = "metrics.properties";

  @VisibleForTesting
  public static final String TOTAL_SIZE_PROP_NAME = "totalSize";

  @VisibleForTesting
  public static final String BLOB_COUNT_PROP_NAME = "blobCount";

  protected B blobStore;

  private Map<OperationType, OperationMetrics> operationMetrics;

  @Override
  @Guarded(by = NEW)
  public final void init(final B blobStore) throws Exception {
    checkState(this.blobStore == null, "Do not initialize twice");

    this.blobStore = checkNotNull(blobStore);
    doInit(blobStore);

    this.start();
  }

  /**
   * Called during {@link init} for subclasses to derive necessary values from the configured blobstore
   */
  protected abstract void doInit(B blobstore) throws Exception;

  @Override
  protected void doStart() throws Exception {
    operationMetrics = new EnumMap<>(OperationType.class);
    for (OperationType type : OperationType.values()) {
      operationMetrics.put(type, new OperationMetrics());
    }
  }

  @Override
  protected void doStop() throws Exception {
    blobStore = null;

    operationMetrics.clear();
  }

  protected abstract AccumulatingBlobStoreMetrics getAccumulatingBlobStoreMetrics() throws BlobStoreMetricsNotAvailableException;

  protected abstract Stream<T> backingFiles() throws BlobStoreMetricsNotAvailableException;

  protected BlobStoreMetrics getCombinedMetrics(
      final Stream<T> blobStoreMetricsFiles) throws BlobStoreMetricsNotAvailableException
  {
    AccumulatingBlobStoreMetrics blobStoreMetrics = getAccumulatingBlobStoreMetrics();
    blobStoreMetricsFiles.forEach(metricsFile -> {
      iterate(1, i -> i + 1)
          .limit(MAXIMUM_TRIES)
          .forEach(currentTry -> {
            try {
              metricsFile.load();
            }
            catch (IOException e) {
              log.debug("Unable to load properties file {}. Try number {} of {}.", metricsFile, currentTry,
                  MAXIMUM_TRIES, e);
              if (currentTry >= MAXIMUM_TRIES) {
                throw new RuntimeException("Failed to load blob store metrics from " + metricsFile, e);
              }
              try {
                MILLISECONDS.sleep(METRICS_LOADING_DELAY_MILLIS);
              }
              catch (InterruptedException e1) {
                log.warn("Interrupted", e1);
                Thread.currentThread().interrupt();
              }
            }
          });

      blobStoreMetrics.addBlobCount(parseLong(metricsFile.getProperty(BLOB_COUNT_PROP_NAME, "0")));
      blobStoreMetrics.addTotalSize(parseLong(metricsFile.getProperty(TOTAL_SIZE_PROP_NAME, "0")));
    });
    return blobStoreMetrics;
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

}
