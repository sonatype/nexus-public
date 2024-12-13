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
package org.sonatype.nexus.blobstore.file.internal.datastore.metrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.metrics.DatastoreBlobStoreMetricsServiceSupport;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;

import com.google.common.collect.ImmutableMap;

@Named(FileBlobStore.TYPE)
@Priority(Integer.MAX_VALUE)
public class DatastoreFileBlobStoreMetricsService
    extends DatastoreBlobStoreMetricsServiceSupport<FileBlobStore>
{
  @Inject
  public DatastoreFileBlobStoreMetricsService(
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

    if (metricsEntity == null) {
      metricsEntity = new BlobStoreMetricsEntity();
    }

    try {
      FileStore fileStore = Files.getFileStore(blobStore.getAbsoluteBlobDir());

      ImmutableMap<String, Long> availableSpace = ImmutableMap
          .of("fileStore:" + fileStore.name(), fileStore.getUsableSpace());

      return new AccumulatingBlobStoreMetrics(
          metricsEntity.getBlobCount(),
          metricsEntity.getTotalSize(),
          availableSpace,
          false);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
