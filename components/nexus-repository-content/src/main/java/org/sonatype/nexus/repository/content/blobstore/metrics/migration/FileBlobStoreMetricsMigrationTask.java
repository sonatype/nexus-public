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
package org.sonatype.nexus.repository.content.blobstore.metrics.migration;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;

/**
 * Migrate file blobstore metrics from external metrics files to DB. Applies to all registered file blob stores.
 */
@Named
public class FileBlobStoreMetricsMigrationTask
    extends TaskSupport
    implements Cancelable
{

  private final BlobStoreManager blobStoreManager;

  private final BlobStoreMetricsStore datastoreMetricsStore;

  private final BlobStoreMetricsReader metricsReader;

  @Inject
  public FileBlobStoreMetricsMigrationTask(
      final BlobStoreManager blobStoreManager,
      final BlobStoreMetricsStore datastoreMetricsStore,
      final BlobStoreMetricsReader metricsReader)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.datastoreMetricsStore = checkNotNull(datastoreMetricsStore);
    this.metricsReader = checkNotNull(metricsReader);
  }

  @Override
  protected Object execute() throws Exception {
    for (BlobStore blobStore : blobStoreManager.browse()) {
      BlobStoreMetrics metricsFromFile = metricsReader.readMetrics(blobStore);
      Map<OperationType, OperationMetrics> operationMetrics = metricsReader.readOperationMetrics(blobStore);

      String blobStoreName = blobStore.getBlobStoreConfiguration().getName();

      if (metricsFromFile != null && operationMetrics != null) {
        log.debug("Found metrics for {} in local files", blobStoreName);

        BlobStoreMetricsEntity metricsFromDb = datastoreMetricsStore.get(blobStoreName);
        if (shouldSaveMetricsToDatabase(metricsFromDb)) {
          log.debug("Found metrics {} for {} should be migrated to DB", metricsFromDb, blobStoreName);
          datastoreMetricsStore.initializeMetrics(blobStoreName);
          datastoreMetricsStore.updateMetrics(
              toBlobStoreMetricsEntity(blobStoreName, metricsFromFile, operationMetrics));
        }
      }
    }
    return null;
  }

  private boolean shouldSaveMetricsToDatabase(final BlobStoreMetricsEntity metricsFromDb) {
    return metricsFromDb == null || metricsFromDb.getBlobCount() == 0L;
  }

  private BlobStoreMetricsEntity toBlobStoreMetricsEntity(
      final String blobStoreName,
      final BlobStoreMetrics metricsFromFile,
      Map<OperationType, OperationMetrics> operationMetrics)
  {
    OperationMetrics downloadMetrics = operationMetrics.get(DOWNLOAD);
    OperationMetrics uploadMetrics = operationMetrics.get(UPLOAD);

    return new BlobStoreMetricsEntity()
        .setBlobStoreName(blobStoreName)
        .setBlobCount(metricsFromFile.getBlobCount())
        .setTotalSize(metricsFromFile.getTotalSize())
        .setDownloadBlobSize(downloadMetrics.getBlobSize())
        .setDownloadErrorRequests(downloadMetrics.getErrorRequests())
        .setDownloadSuccessfulRequests(downloadMetrics.getSuccessfulRequests())
        .setDownloadTimeOnRequests(downloadMetrics.getTimeOnRequests())
        .setUploadBlobSize(uploadMetrics.getBlobSize())
        .setUploadErrorRequests(uploadMetrics.getErrorRequests())
        .setUploadSuccessfulRequests(uploadMetrics.getSuccessfulRequests())
        .setUploadTimeOnRequests(uploadMetrics.getTimeOnRequests());
  }

  @Override
  public String getMessage() {
    return "Migrate bob store metrics from external metrics files to DB";
  }
}
