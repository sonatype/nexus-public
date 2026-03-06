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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.TYPE_ID;

@ExtendWith(MockitoExtension.class)
class AssetBlobRefMigrationStep_2_109Test
    extends Test5Support
{
  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @Mock
  private Format mavenFormat;

  @Mock
  private Format npmFormat;

  @Mock
  private Format dockerFormat;

  @Mock
  private FormatStoreManager mavenFormatStoreManager;

  @Mock
  private FormatStoreManager npmFormatStoreManager;

  @Mock
  private FormatStoreManager dockerFormatStoreManager;

  @Mock
  private AssetBlobStore<?> mavenAssetBlobStore;

  @Mock
  private AssetBlobStore<?> npmAssetBlobStore;

  @Mock
  private AssetBlobStore<?> dockerAssetBlobStore;

  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private Connection connection;

  @Mock
  private TaskConfiguration taskConfiguration;

  private AssetBlobRefMigrationStep_2_109 underTest;

  private MockedStatic<QualifierUtil> mockedQualifierUtil;

  @BeforeEach
  void setUp() {
    lenient().when(mavenFormat.getValue()).thenReturn("maven2");
    lenient().when(npmFormat.getValue()).thenReturn("npm");
    lenient().when(dockerFormat.getValue()).thenReturn("docker");

    lenient().when(upgradeTaskScheduler.createTaskConfigurationInstance(TYPE_ID)).thenReturn(taskConfiguration);

    mockedQualifierUtil = mockStatic(QualifierUtil.class);
  }

  @AfterEach
  void tearDown() {
    if (mockedQualifierUtil != null) {
      mockedQualifierUtil.close();
    }
  }

  @Test
  void testMigrate_schedulesTasksForFormatsWithLegacyData() throws Exception {
    setupFormatStoreManagers(mavenFormatStoreManager, npmFormatStoreManager);

    when(mavenFormatStoreManager.assetBlobStore("nexus")).thenReturn(mavenAssetBlobStore);
    when(npmFormatStoreManager.assetBlobStore("nexus")).thenReturn(npmAssetBlobStore);

    when(mavenAssetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(true);
    when(npmAssetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(false);

    underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler,
        List.of(mavenFormat, npmFormat),
        List.of(mavenFormatStoreManager, npmFormatStoreManager),
        globalKeyValueStore);

    underTest.migrate(connection);

    // Verify task scheduled only for maven2 (which has legacy data)
    verify(upgradeTaskScheduler, times(1)).createTaskConfigurationInstance(TYPE_ID);
    verify(taskConfiguration).setString(FORMAT_FIELD_ID, "maven2");
    verify(taskConfiguration).setString(CONTENT_STORE_FIELD_ID, "nexus");
    verify(upgradeTaskScheduler, times(1)).schedule(taskConfiguration);

    // Verify old state keys are deleted for both formats
    verify(globalKeyValueStore).removeKey("assetBlob.blobRef.migration:checked:maven2:nexus");
    verify(globalKeyValueStore).removeKey("assetBlob.blobRef.migration:checked:npm:nexus");
  }

  @Test
  void testMigrate_skipsWhenNoLegacyData() throws Exception {
    setupFormatStoreManagers(mavenFormatStoreManager, npmFormatStoreManager);

    when(mavenFormatStoreManager.assetBlobStore("nexus")).thenReturn(mavenAssetBlobStore);
    when(npmFormatStoreManager.assetBlobStore("nexus")).thenReturn(npmAssetBlobStore);

    when(mavenAssetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(false);
    when(npmAssetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(false);

    underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler,
        List.of(mavenFormat, npmFormat),
        List.of(mavenFormatStoreManager, npmFormatStoreManager),
        globalKeyValueStore);

    underTest.migrate(connection);

    // Verify no tasks scheduled
    verify(upgradeTaskScheduler, never()).schedule(taskConfiguration);

    // Verify old state keys are still deleted
    verify(globalKeyValueStore).removeKey("assetBlob.blobRef.migration:checked:maven2:nexus");
    verify(globalKeyValueStore).removeKey("assetBlob.blobRef.migration:checked:npm:nexus");
  }

  @Test
  void testMigrate_handlesMissingFormatStoreManager() throws Exception {
    // Only provide FormatStoreManager for maven2, not for npm
    setupFormatStoreManagers(mavenFormatStoreManager);

    when(mavenFormatStoreManager.assetBlobStore("nexus")).thenReturn(mavenAssetBlobStore);
    when(mavenAssetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(true);

    underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler,
        List.of(mavenFormat, npmFormat),
        List.of(mavenFormatStoreManager), // Only maven, not npm
        globalKeyValueStore);

    // Should not throw exception
    underTest.migrate(connection);

    // Verify task scheduled only for maven2
    verify(upgradeTaskScheduler, times(1)).schedule(taskConfiguration);
    verify(taskConfiguration).setString(FORMAT_FIELD_ID, "maven2");
    verify(taskConfiguration).setString(CONTENT_STORE_FIELD_ID, "nexus");

    // Verify old state keys are deleted for both formats
    verify(globalKeyValueStore).removeKey("assetBlob.blobRef.migration:checked:maven2:nexus");
    verify(globalKeyValueStore).removeKey("assetBlob.blobRef.migration:checked:npm:nexus");
  }

  @Test
  void testMigrate_deletesOldStateKeys() throws Exception {
    setupFormatStoreManagers(mavenFormatStoreManager);

    when(mavenFormatStoreManager.assetBlobStore("nexus")).thenReturn(mavenAssetBlobStore);
    when(mavenAssetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(false);

    when(globalKeyValueStore.removeKey("assetBlob.blobRef.migration:checked:maven2:nexus")).thenReturn(true);

    underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler,
        List.of(mavenFormat),
        List.of(mavenFormatStoreManager),
        globalKeyValueStore);

    underTest.migrate(connection);

    // Verify old state key deletion was attempted
    verify(globalKeyValueStore).removeKey("assetBlob.blobRef.migration:checked:maven2:nexus");
  }

  @Test
  void testMigrate_noFormats() throws Exception {
    setupFormatStoreManagers();

    underTest = new AssetBlobRefMigrationStep_2_109(
        upgradeTaskScheduler,
        List.of(), // Empty formats list
        List.of(),
        globalKeyValueStore);

    underTest.migrate(connection);

    // Verify no tasks scheduled and no keys deleted
    verify(upgradeTaskScheduler, never()).schedule(taskConfiguration);
    verify(globalKeyValueStore, never()).removeKey(anyString());
  }

  private void setupFormatStoreManagers(FormatStoreManager... managers) {
    Map<String, FormatStoreManager> managerMap = new HashMap<>();
    for (FormatStoreManager manager : managers) {
      if (manager == mavenFormatStoreManager) {
        managerMap.put("maven2", manager);
      }
      else if (manager == npmFormatStoreManager) {
        managerMap.put("npm", manager);
      }
      else if (manager == dockerFormatStoreManager) {
        managerMap.put("docker", manager);
      }
    }
    mockedQualifierUtil.when(() -> QualifierUtil.buildQualifierBeanMap(anyList())).thenReturn(managerMap);
  }
}
