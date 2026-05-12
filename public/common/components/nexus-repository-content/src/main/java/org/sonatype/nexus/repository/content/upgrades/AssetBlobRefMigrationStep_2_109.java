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
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.TYPE_ID;

/**
 * Schedules migration tasks to convert legacy blobRef format (store:id@node) to new format (store@id).
 * <p>
 * This migration step replaces the task-based scheduling approach with upgrade-time task scheduling. Tasks are
 * scheduled via UpgradeTaskScheduler and tracked in the upgrade_tasks table, eliminating the need for
 * GlobalKeyValueStore state tracking. Tasks have REQUEST_RECOVERY enabled for automatic retry on failure.
 * <p>
 * Related to NEXUS-42488 - Migration from task manager-based to upgrade-based scheduling for better reliability and
 * consistency with other upgrade-time data migrations.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AssetBlobRefMigrationStep_2_109
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String CONTENT_STORE_NAME = "nexus";

  private final UpgradeTaskScheduler upgradeTaskScheduler;

  private final List<Format> formats;

  private final Map<String, FormatStoreManager> formatStoreManagers;

  private final GlobalKeyValueStore globalKeyValueStore;

  @Autowired
  public AssetBlobRefMigrationStep_2_109(
      final UpgradeTaskScheduler upgradeTaskScheduler,
      final List<Format> formats,
      final List<FormatStoreManager> formatStoreManagersList,
      final GlobalKeyValueStore globalKeyValueStore)
  {
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
    this.formats = checkNotNull(formats);
    this.formatStoreManagers = QualifierUtil.buildQualifierBeanMap(checkNotNull(formatStoreManagersList));
    this.globalKeyValueStore = checkNotNull(globalKeyValueStore);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.109");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    for (Format format : formats) {
      String formatName = format.getValue();

      FormatStoreManager formatStoreManager = formatStoreManagers.get(formatName);
      if (formatStoreManager != null) {
        AssetBlobStore<?> assetBlobStore = formatStoreManager.assetBlobStore(CONTENT_STORE_NAME);

        // Check if this format has legacy blobRef data
        if (assetBlobStore.notMigratedAssetBlobRefsExists()) {
          log.info("Found legacy blobRef data for format: {}", formatName);
          scheduleTask(formatName);
        }
        else {
          log.debug("No legacy blobRef data for format: {}", formatName);
        }
      }
      else {
        log.debug("No FormatStoreManager available for format: {}", formatName);
      }
    }

    // Delete old GlobalKeyValueStore keys from previous implementation
    deleteOldStateKeys();
  }

  private void scheduleTask(final String format) {
    TaskConfiguration config = upgradeTaskScheduler.createTaskConfigurationInstance(TYPE_ID);
    config.setString(FORMAT_FIELD_ID, format);
    config.setString(CONTENT_STORE_FIELD_ID, CONTENT_STORE_NAME);
    upgradeTaskScheduler.schedule(config);
    log.info("Scheduled blobRef migration task for {} format on {}", format,
        CONTENT_STORE_NAME);
  }

  private void deleteOldStateKeys() {
    int deletedCount = 0;

    for (Format format : formats) {
      String formatName = format.getValue();
      String key = buildStateKey(formatName, CONTENT_STORE_NAME);

      if (globalKeyValueStore.removeKey(key)) {
        deletedCount++;
        log.debug("Deleted old migration state key for format: {}", formatName);
      }
    }

    if (deletedCount > 0) {
      log.info("Deleted {} old migration state keys from GlobalKeyValueStore", deletedCount);
    }
  }

  private String buildStateKey(final String format, final String contentStore) {
    return TYPE_ID + ":checked:" + format + ":" + contentStore;
  }
}
