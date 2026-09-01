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
package org.sonatype.nexus.blobstore.internal.metrics;

import java.util.List;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.common.BlobStoreTaskSupport;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.repository.content.blobstore.metrics.upgrade.UpgradeBlobStoreMetricsStore;
import org.sonatype.nexus.repository.internal.blobstore.upgrade.UpgradeBlobStoreConfigurationStore;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the shared {@link BlobStoreMetricsDatabaseMigrationStepSupport} migrate() path (via the File
 * subclass): it lists blob store configurations through {@link UpgradeBlobStoreConfigurationStore},
 * filters by type, and schedules the migration task via {@link UpgradeTaskScheduler}.
 */
@ExtendWith(MockitoExtension.class)
class FileBlobStoreMetricsMigrationStepTest
{
  @Mock
  private UpgradeBlobStoreMetricsStore metricsStore;

  @Mock
  private UpgradeBlobStoreConfigurationStore blobStoreConfigurationStore;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @Mock
  private TaskConfiguration taskConfiguration;

  private FileBlobStoreMetricsMigrationStep underTest;

  @BeforeEach
  void setUp() {
    underTest = new FileBlobStoreMetricsMigrationStep();
    underTest.initDependencies(metricsStore, blobStoreConfigurationStore, upgradeTaskScheduler);
  }

  @Test
  void migrate_schedulesTaskForFileBlobStores() throws Exception {
    BlobStoreConfiguration fileConfig = config("file-bs", FileBlobStore.TYPE);
    BlobStoreConfiguration s3Config = config("s3-bs", "S3");
    when(blobStoreConfigurationStore.list()).thenReturn(List.of(fileConfig, s3Config));
    when(upgradeTaskScheduler.createTaskConfigurationInstance(any())).thenReturn(taskConfiguration);

    underTest.migrate(null);

    verify(taskConfiguration).setString(BlobStoreTaskSupport.BLOBSTORE_NAME_FIELD_ID, "file-bs");
    verify(upgradeTaskScheduler).schedule(taskConfiguration);
  }

  @Test
  void migrate_schedulesSingleTaskForMultipleFileBlobStores() throws Exception {
    BlobStoreConfiguration file1 = config("file-bs-1", FileBlobStore.TYPE);
    BlobStoreConfiguration file2 = config("file-bs-2", FileBlobStore.TYPE);
    BlobStoreConfiguration s3Config = config("s3-bs", "S3");
    when(blobStoreConfigurationStore.list()).thenReturn(List.of(file1, file2, s3Config));
    when(upgradeTaskScheduler.createTaskConfigurationInstance(any())).thenReturn(taskConfiguration);

    underTest.migrate(null);

    // both File stores are joined into one comma-separated task field; the S3 store is excluded
    verify(taskConfiguration).setString(BlobStoreTaskSupport.BLOBSTORE_NAME_FIELD_ID, "file-bs-1,file-bs-2");
    verify(upgradeTaskScheduler).schedule(taskConfiguration);
  }

  @Test
  void migrate_noMatchingBlobStores_doesNotSchedule() throws Exception {
    BlobStoreConfiguration s3Config = config("s3-bs", "S3");
    when(blobStoreConfigurationStore.list()).thenReturn(List.of(s3Config));

    underTest.migrate(null);

    verify(upgradeTaskScheduler, never()).schedule(any());
  }

  private static BlobStoreConfiguration config(final String name, final String type) {
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    lenient().when(configuration.getName()).thenReturn(name);
    lenient().when(configuration.getType()).thenReturn(type);
    return configuration;
  }
}
