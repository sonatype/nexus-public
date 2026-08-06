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
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.common.BlobStoreTaskSupport;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.kv.upgrade.UpgradeNexusKeyValueStore;
import org.sonatype.nexus.repository.content.blobstore.metrics.upgrade.UpgradeBlobStoreMetricsStore;
import org.sonatype.nexus.repository.internal.blobstore.upgrade.UpgradeBlobStoreConfigurationStore;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.UpgradeCooperation;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Tests {@link S3BlobStoreMetricsMigrationStep}'s reworked, off-runtime-services wiring
 * ({@link UpgradeNexusKeyValueStore} + {@link UpgradeCooperation} in place of the removed
 * {@code GlobalKeyValueStore} / {@code Cooperation2Factory}): it sources its {@link Cooperation2} from
 * {@link UpgradeCooperation}, schedules a single migration task only for S3 blob stores whose DB metrics are
 * absent or zeroed, and clears its KV bookkeeping key after migrating.
 */
@ExtendWith(MockitoExtension.class)
class S3BlobStoreMetricsMigrationStepTest
{
  private static final String NAME = S3BlobStoreMetricsMigrationStep.class.getSimpleName();

  private static final String S3 = "S3";

  @Mock
  private UpgradeNexusKeyValueStore kv;

  @Mock
  private UpgradeCooperation upgradeCooperation;

  @Mock
  private Cooperation2 cooperation;

  @Mock
  private UpgradeBlobStoreMetricsStore metricsStore;

  @Mock
  private UpgradeBlobStoreConfigurationStore blobStoreConfigurationStore;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @Mock
  private TaskConfiguration taskConfiguration;

  private S3BlobStoreMetricsMigrationStep underTest;

  @BeforeEach
  void setUp() {
    when(upgradeCooperation.get(NAME)).thenReturn(cooperation);
    underTest = new S3BlobStoreMetricsMigrationStep(kv, new ObjectMapper(), upgradeCooperation);
    underTest.initDependencies(metricsStore, blobStoreConfigurationStore, upgradeTaskScheduler);
  }

  @Test
  void cooperation_isObtainedFromUpgradeCooperation() {
    // The Cooperation2 instance must come from UpgradeCooperation.get(NAME) (the UPGRADE-safe wrapper),
    // not from the removed runtime Cooperation2Factory.
    verify(upgradeCooperation).get(NAME);
  }

  @Test
  void migrate_schedulesOnlyS3StoresWithAbsentOrZeroMetrics_thenClearsKvKey() throws Exception {
    BlobStoreConfiguration s3NoMetrics = config("s3-empty", S3);
    BlobStoreConfiguration s3WithMetrics = config("s3-populated", S3);
    BlobStoreConfiguration fileStore = config("file-bs", "File");
    when(blobStoreConfigurationStore.list()).thenReturn(List.of(s3NoMetrics, s3WithMetrics, fileStore));
    when(metricsStore.get("s3-empty")).thenReturn(null);
    when(metricsStore.get("s3-populated"))
        .thenReturn(new BlobStoreMetricsEntity().setBlobStoreName("s3-populated").setBlobCount(5L));
    when(upgradeTaskScheduler.createTaskConfigurationInstance(any())).thenReturn(taskConfiguration);

    underTest.migrate(null);

    // only the empty/zeroed S3 store is scheduled; the populated S3 store and the non-S3 store are excluded
    verify(taskConfiguration).setString(BlobStoreTaskSupport.BLOBSTORE_NAME_FIELD_ID, "s3-empty");
    verify(upgradeTaskScheduler).schedule(taskConfiguration);
    // migrate() clears the KV key it used to coordinate name computation across nodes
    verify(kv).removeKey(NAME);
  }

  @Test
  void migrate_noS3StoresNeedMigration_schedulesNothingButStillClearsKvKey() throws Exception {
    BlobStoreConfiguration s3WithMetrics = config("s3-populated", S3);
    when(blobStoreConfigurationStore.list()).thenReturn(List.of(s3WithMetrics));
    when(metricsStore.get("s3-populated"))
        .thenReturn(new BlobStoreMetricsEntity().setBlobStoreName("s3-populated").setBlobCount(10L));

    underTest.migrate(null);

    verify(upgradeTaskScheduler, never()).schedule(any());
    verify(kv).removeKey(NAME);
  }

  private static BlobStoreConfiguration config(final String name, final String type) {
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    lenient().when(configuration.getName()).thenReturn(name);
    lenient().when(configuration.getType()).thenReturn(type);
    return configuration;
  }
}
