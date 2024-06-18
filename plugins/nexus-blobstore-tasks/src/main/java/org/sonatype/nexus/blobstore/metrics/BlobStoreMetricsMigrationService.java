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

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsPropertiesReader;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Manage blobstore metrics recalculation.
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class BlobStoreMetricsMigrationService
    extends StateGuardLifecycleSupport
{
  private final BlobStoreManager blobStoreManager;

  private final BlobStoreMetricsStore metricsStore;

  private final Map<String, BlobStoreMetricsPropertiesReader> metricsPropertiesReaders;

  @Inject
  public BlobStoreMetricsMigrationService(
      final BlobStoreManager blobStoreManager,
      final BlobStoreMetricsStore metricsStore,
      final Map<String, BlobStoreMetricsPropertiesReader> metricsPropertiesReaders) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.metricsStore = checkNotNull(metricsStore);
    this.metricsPropertiesReaders = checkNotNull(metricsPropertiesReaders);
  }

  @Override
  protected void doStart() throws Exception {
    blobStoreManager.browse().forEach(blobStore -> {
      String blobStoreName = blobStore.getBlobStoreConfiguration().getName();
      String blobStoreType = blobStore.getBlobStoreConfiguration().getType();

      if (!blobStore.isStarted()) {
        log.info("Blob store {}:{} is not started, skipping it.", blobStoreType, blobStoreName);
        return;
      }

      try {
        BlobStoreMetricsEntity metricsFromDb = metricsStore.get(blobStoreName);
        if (shouldSaveMetricsToDatabase(metricsFromDb)) {
          BlobStoreMetricsPropertiesReader propertiesReader = metricsPropertiesReaders.get(blobStoreType);
          if (propertiesReader == null) {
            log.error("Properties reader not found for {}:{}", blobStoreType, blobStoreName);
            return;
          }
          propertiesReader.initWithBlobStore(blobStore);
          BlobStoreMetrics metricsFromFile = propertiesReader.readMetrics();
          Map<OperationType, OperationMetrics> operationMetrics = propertiesReader.readOperationMetrics();

          if (metricsFromFile != null && operationMetrics != null) {
              log.debug("Found metrics {} for {}:{} should be migrated to DB", metricsFromDb, blobStoreType, blobStoreName);
              metricsStore.initializeMetrics(blobStoreName);
              metricsStore.updateMetrics(toBlobStoreMetricsEntity(blobStoreName, metricsFromFile, operationMetrics));
          }
        }
      }
      catch (Exception e) {
        log.error("Exception during migrating metrics from properties to DB for {}:{}", blobStoreType, blobStoreName);
      }
    });
  }

  private boolean shouldSaveMetricsToDatabase(final BlobStoreMetricsEntity metricsFromDb) {
    return metricsFromDb == null || metricsFromDb.getBlobCount() == 0L;
  }

  private BlobStoreMetricsEntity toBlobStoreMetricsEntity(
      final String blobStoreName,
      final BlobStoreMetrics blobStoreMetrics,
      Map<OperationType, OperationMetrics> operationMetrics)
  {
    OperationMetrics downloadMetrics = operationMetrics.get(DOWNLOAD);
    OperationMetrics uploadMetrics = operationMetrics.get(UPLOAD);

    return new BlobStoreMetricsEntity()
        .setBlobStoreName(blobStoreName)
        .setBlobCount(blobStoreMetrics.getBlobCount())
        .setTotalSize(blobStoreMetrics.getTotalSize())
        .setDownloadBlobSize(downloadMetrics.getBlobSize())
        .setDownloadErrorRequests(downloadMetrics.getErrorRequests())
        .setDownloadSuccessfulRequests(downloadMetrics.getSuccessfulRequests())
        .setDownloadTimeOnRequests(downloadMetrics.getTimeOnRequests())
        .setUploadBlobSize(uploadMetrics.getBlobSize())
        .setUploadErrorRequests(uploadMetrics.getErrorRequests())
        .setUploadSuccessfulRequests(uploadMetrics.getSuccessfulRequests())
        .setUploadTimeOnRequests(uploadMetrics.getTimeOnRequests());
  }
}
