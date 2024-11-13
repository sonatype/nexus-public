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

import java.sql.Connection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.common.BlobStoreTaskSupport;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base task which will read the information from a metrics property file then adds it to the existing metrics total.
 *
 * This task should only be run manually for a particular blob store if it has previously failed, other usages may
 * result in double counting historical file based metric data.
 */
public abstract class BlobStoreMetricsDatabaseMigrationStepSupport
    extends StateGuardLifecycleSupport
    implements RepeatableDatabaseMigrationStep
{
  private final String blobStoreType;

  protected BlobStoreMetricsStore metricsStore;

  protected BlobStoreConfigurationStore blobStoreConfigurationStore;

  private UpgradeTaskScheduler upgradeTaskScheduler;

  protected BlobStoreMetricsDatabaseMigrationStepSupport(final String blobStoreType) {
    this.blobStoreType = checkNotNull(blobStoreType);
  }

  @Inject
  public final void initDependencies(
      final BlobStoreMetricsStore metricsStore,
      final BlobStoreConfigurationStore blobStoreConfigurationStore,
      final UpgradeTaskScheduler upgradeTaskScheduler)
  {
    this.metricsStore = checkNotNull(metricsStore);
    this.blobStoreConfigurationStore = checkNotNull(blobStoreConfigurationStore);
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    String names = getBlobStoreConfigurations()
        .filter(this::shouldSaveMetricsToDatabase)
        .collect(Collectors.joining(","));

    if (names.isEmpty()) {
      log.debug("No blobstores of type {} found", blobStoreType);
      return;
    }

    TaskConfiguration configuration =
        upgradeTaskScheduler.createTaskConfigurationInstance(BlobStoreMetricsMigrationTask.TYPE_ID);

    configuration.setString(BlobStoreTaskSupport.BLOBSTORE_NAME_FIELD_ID, names);

    upgradeTaskScheduler.schedule(configuration);
  }

  /**
   * Implementations should override this if additional filtering should apply, e.g. invoking {@link emptyOrZero}
   */
  protected boolean shouldSaveMetricsToDatabase(final String blobStoreName) {
    return true;
  }

  protected Stream<String> getBlobStoreConfigurations() {
    return blobStoreConfigurationStore.list().stream()
        .filter(store -> blobStoreType.equals(store.getType()))
        .map(BlobStoreConfiguration::getName);
  }
}
