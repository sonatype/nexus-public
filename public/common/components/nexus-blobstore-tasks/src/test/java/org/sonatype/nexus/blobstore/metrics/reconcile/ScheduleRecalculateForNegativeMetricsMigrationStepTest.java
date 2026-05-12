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

import java.sql.Connection;

import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.repository.content.blobstore.metrics.BlobStoreMetricsDAO;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.mockito.InjectMocks;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith(MockitoExtension.class)
class ScheduleRecalculateForNegativeMetricsMigrationStepTest

{
  @DataSessionConfiguration(daos = {})
  TestDataSessionSupplier dataSessionSupplier;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @InjectMocks
  private ScheduleRecalculateForNegativeMetricsMigrationStep underTest;

  @DatabaseTest
  void testMigrate_withMultipleNegativeMetrics() throws Exception {
    createTable();
    createMetricsRecord("blob-store-1", -1000L, 100L);
    createMetricsRecord("blob-store-2", 5000L, -50L);
    createMetricsRecord("blob-store-3", -1000L, -100L);
    createMetricsRecord("blob-store-4", 10000L, 200L); // positive, should not schedule

    TaskConfiguration taskConfiguration1 = mock();
    TaskConfiguration taskConfiguration2 = mock();
    TaskConfiguration taskConfiguration3 = mock();
    when(upgradeTaskScheduler.createTaskConfigurationInstance(RecalculateBlobStoreSizeTaskDescriptor.TYPE_ID))
        .thenReturn(taskConfiguration1, taskConfiguration2, taskConfiguration3);

    try (Connection connection = dataSessionSupplier.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection);
    }

    verify(upgradeTaskScheduler, times(3))
        .createTaskConfigurationInstance(RecalculateBlobStoreSizeTaskDescriptor.TYPE_ID);
    verify(taskConfiguration1).setString(eq("blobstoreName"), eq("blob-store-1"));
    verify(taskConfiguration2).setString(eq("blobstoreName"), eq("blob-store-2"));
    verify(taskConfiguration3).setString(eq("blobstoreName"), eq("blob-store-3"));
    verify(upgradeTaskScheduler).schedule(taskConfiguration1);
    verify(upgradeTaskScheduler).schedule(taskConfiguration2);
    verify(upgradeTaskScheduler).schedule(taskConfiguration3);
  }

  @DatabaseTest
  void testMigrate_noTable() throws Exception {
    try (Connection connection = dataSessionSupplier.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection);
    }
    verifyNoInteractions(upgradeTaskScheduler);
  }

  @DatabaseTest
  void testMigrate_emptyTable() throws Exception {
    createTable();

    try (Connection connection = dataSessionSupplier.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(connection);
    }
    verifyNoInteractions(upgradeTaskScheduler);
  }

  private void createMetricsRecord(final String blobStoreName, final long totalSize, final long blobCount) {
    dataSessionSupplier.callDAO(BlobStoreMetricsDAO.class, dao -> {
      dao.initializeMetrics(blobStoreName);
      dao.updateMetrics(
          new BlobStoreMetricsEntity().setBlobStoreName(blobStoreName).setBlobCount(blobCount).setTotalSize(totalSize));
    });
  }

  private void createTable() {
    dataSessionSupplier.register(BlobStoreMetricsDAO.class);
  }
}
