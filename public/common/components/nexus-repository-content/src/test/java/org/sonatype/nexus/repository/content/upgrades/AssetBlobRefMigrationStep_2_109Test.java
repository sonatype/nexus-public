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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.util.List;

import org.sonatype.nexus.kv.upgrade.UpgradeNexusKeyValueStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.TYPE_ID;

/**
 * Real-database tests for {@link AssetBlobRefMigrationStep_2_109}.
 * <p>
 * The step no longer probes the {@code {format}_asset_blob} tables on the UPGRADE thread (that probe was an
 * unindexable full scan blocking startup). It now schedules the self-guarding background migration task for every
 * format unconditionally, so these tests assert scheduling behaviour rather than the old per-format legacy-data probe.
 */
class AssetBlobRefMigrationStep_2_109Test
{
  private static final String CONTENT_STORE_NAME = "nexus";

  @DataSessionConfiguration(daos = {})
  TestDataSessionSupplier dataSessionSupplier;

  private final UpgradeTaskScheduler upgradeTaskScheduler = mock(UpgradeTaskScheduler.class);

  private final UpgradeNexusKeyValueStore keyValueStore = mock(UpgradeNexusKeyValueStore.class);

  private final TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);

  @DatabaseTest
  void migrate_schedulesTaskUnconditionallyForEveryFormat() throws Exception {
    when(upgradeTaskScheduler.createTaskConfigurationInstance(TYPE_ID)).thenReturn(taskConfiguration);
    AssetBlobRefMigrationStep_2_109 underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler, List.of(format("maven2"), format("npm")), keyValueStore);

    // The step no longer probes the {format}_asset_blob tables on the UPGRADE thread. It schedules the
    // (self-guarding, background) migration task for EVERY format regardless of contents; the task itself
    // no-ops when a format has no legacy blobRefs.
    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
    }

    verify(upgradeTaskScheduler, times(2)).createTaskConfigurationInstance(TYPE_ID);
    verify(taskConfiguration).setString(FORMAT_FIELD_ID, "maven2");
    verify(taskConfiguration).setString(FORMAT_FIELD_ID, "npm");
    verify(taskConfiguration, times(2)).setString(CONTENT_STORE_FIELD_ID, CONTENT_STORE_NAME);
    verify(upgradeTaskScheduler, times(2)).schedule(taskConfiguration);
    // old state keys are cleaned up for every format in a single batched call
    verify(keyValueStore).removeKeys(List.of(
        "assetBlob.blobRef.migration:checked:maven2:nexus",
        "assetBlob.blobRef.migration:checked:npm:nexus"));
  }

  @DatabaseTest
  void migrate_schedulesTaskEvenWhenNoAssetBlobTableExists() throws Exception {
    when(upgradeTaskScheduler.createTaskConfigurationInstance(TYPE_ID)).thenReturn(taskConfiguration);
    AssetBlobRefMigrationStep_2_109 underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler, List.of(format("maven2")), keyValueStore);

    // No maven2_asset_blob table exists, and the step no longer touches it at UPGRADE time, so the
    // task is still scheduled; the background task tolerates a missing/empty format as a no-op.
    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
    }

    verify(upgradeTaskScheduler, times(1)).schedule(taskConfiguration);
    verify(keyValueStore).removeKeys(List.of("assetBlob.blobRef.migration:checked:maven2:nexus"));
  }

  @DatabaseTest
  void migrate_noFormats_schedulesNothingAndDeletesNoKeys() throws Exception {
    AssetBlobRefMigrationStep_2_109 underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler, List.of(), keyValueStore);

    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
    }

    verify(upgradeTaskScheduler, never()).schedule(taskConfiguration);
    verify(keyValueStore).removeKeys(List.of());
  }

  private static Format format(final String value) {
    Format format = mock(Format.class);
    lenient().when(format.getValue()).thenReturn(value);
    return format;
  }
}
