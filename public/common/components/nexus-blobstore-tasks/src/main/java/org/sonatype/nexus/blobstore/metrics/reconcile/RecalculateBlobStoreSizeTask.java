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
package org.sonatype.nexus.blobstore.metrics.reconcile;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsService;
import org.sonatype.nexus.blobstore.common.BlobStoreTaskSupport;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.joda.time.DateTime;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.logging.task.TaskLogType.TASK_LOG_ONLY;

@AvailabilityVersion(from = "1.0")
@Component
@TaskLogging(TASK_LOG_ONLY)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RecalculateBlobStoreSizeTask
    extends BlobStoreTaskSupport
    implements Cancelable
{
  private static final int LOGGING_INTERVAL = 60;

  private static final int CANCEL_CHECK_INTERVAL = 300;

  @Autowired
  public RecalculateBlobStoreSizeTask(final BlobStoreManager blobStoreManager) {
    super(blobStoreManager);
  }

  @Override
  public String getMessage() {
    return String.format("recalculate blob store storage for '%s'", getBlobStoreField());
  }

  @Override
  protected void execute(final BlobStore blobStore) {
    DateTime currentDate = DateTime.now();

    BlobStoreMetricsService<?> metricsService = blobStore.getMetricsService();
    metricsService.clearCountMetrics();

    AtomicLong totalSize = new AtomicLong();
    AtomicLong totalCount = new AtomicLong();

    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, LOGGING_INTERVAL)) {
      blobStore.getBlobIdStream()
          .map(blobStore::getBlobAttributes)
          .filter(Objects::nonNull)
          .filter(attributes -> isCreatedBefore(attributes, currentDate))
          .filter(attributes -> !attributes.isDeleted())
          .map(attributes -> attributes.getMetrics().getContentSize())
          .forEach(blobSize -> {
            totalSize.addAndGet(blobSize);
            metricsService.recordAddition(blobSize);
            if (totalCount.incrementAndGet() % CANCEL_CHECK_INTERVAL == 0) {
              CancelableHelper.checkCancellation();
            }

            progressLogger.info("Re-calculating size metrics on blob store '{}', size : {} - blobs count : {}",
                blobStore.getBlobStoreConfiguration().getName(), totalSize,
                totalCount);
          });
    }
  }

  @Override
  protected boolean appliesTo(final BlobStore blobStore) {
    return !blobStore.getBlobStoreConfiguration().getType().equals(BlobStoreGroup.TYPE);
  }

  private static boolean isCreatedBefore(final BlobAttributes attributes, final DateTime date) {
    return attributes.getMetrics().getCreationTime().isBefore(date);
  }
}
