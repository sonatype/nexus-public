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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.kv.upgrade.UpgradeNexusKeyValueStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.TYPE_ID;

/**
 * Schedules background migration tasks to convert legacy blobRef format (store:id@node) to the new format
 * (store@id).
 * <p>
 * A migration task is scheduled <em>unconditionally</em> for every format. The step deliberately does not inspect the
 * {@code {format}_asset_blob} tables during the UPGRADE phase: detecting legacy blobRefs requires a
 * {@code blob_ref LIKE '%:%'} predicate that cannot use an index, so on large tables it degrades into a full-table
 * scan. Running that scan on the UPGRADE thread blocked startup for a long time on large instances. Scheduling is a
 * cheap {@code upgrade_tasks} insert; the task itself runs after Nexus has started, on a background thread, and is
 * self-guarding — it no-ops for any format that has no legacy blobRefs.
 * <p>
 * Tasks are tracked in the {@code upgrade_tasks} table and have REQUEST_RECOVERY enabled for automatic retry on
 * failure.
 */
@Component
public class AssetBlobRefMigrationStep_2_109
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String CONTENT_STORE_NAME = "nexus";

  private final UpgradeTaskScheduler upgradeTaskScheduler;

  private final List<Format> formats;

  private final UpgradeNexusKeyValueStore keyValueStore;

  @Autowired
  public AssetBlobRefMigrationStep_2_109(
      final UpgradeTaskScheduler upgradeTaskScheduler,
      final List<Format> formats,
      final UpgradeNexusKeyValueStore keyValueStore)
  {
    this.upgradeTaskScheduler = checkNotNull(upgradeTaskScheduler);
    this.formats = checkNotNull(formats);
    this.keyValueStore = checkNotNull(keyValueStore);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.109");
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * The {@code connection} parameter is part of the {@link DatabaseMigrationStep} contract but is intentionally
   * unused: this step only schedules background tasks and never touches the {@code {format}_asset_blob} tables during
   * the UPGRADE phase (see the class Javadoc for why the legacy-blobRef probe was removed).
   */
  @Override
  public void migrate(final Connection connection) throws Exception {
    // Do NOT probe the {format}_asset_blob tables here. The probe (blob_ref LIKE '%:%' ...) is an unindexable
    // full-table scan that ran on the jetty-main-1 UPGRADE thread, blocking startup for a long time on large
    // instances. Instead, schedule the migration task unconditionally per format. Scheduling is a cheap upgrade_tasks
    // insert; the task runs after Nexus starts (background) and no-ops when a format has no legacy blobRefs.
    for (Format format : formats) {
      scheduleTask(format.getValue());
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
    List<String> keys = new ArrayList<>(formats.size());
    for (Format format : formats) {
      keys.add(buildStateKey(format.getValue(), CONTENT_STORE_NAME));
    }

    // Batch the deletes into a single connection/transaction rather than one connection acquisition +
    // commit per format (NEXUS-53442 review): matters on a cold pool during the UPGRADE hot path.
    int deletedCount = keyValueStore.removeKeys(keys);
    if (deletedCount > 0) {
      log.info("Deleted {} old migration state keys from nexus_key_value", deletedCount);
    }
  }

  private String buildStateKey(final String format, final String contentStore) {
    return TYPE_ID + ":checked:" + format + ":" + contentStore;
  }
}
