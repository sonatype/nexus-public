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
package org.sonatype.nexus.repository.content.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTask;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationUtility;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Check if the {format}_component table has the normalized_version column. If not schedule the
 * NormalizeComponentVersionTask to run after startup is complete.
 */
@Named
@Singleton
public class ComponentNormalizationIntegrityChecker
    extends ComponentSupport
    implements DatabaseIntegrityChecker
{
  private final String TABLE_NAME = "%s_component";

  private final String COLUMN_NAME = "normalized_version";

  private final String INDEX_NAME = "idx_%s_normalized_version";

  private final String ADD_COLUMN_STATEMENT =
      format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s VARCHAR;", TABLE_NAME, COLUMN_NAME);

  private final String ADD_INDEX_STATEMENT =
      "CREATE INDEX IF NOT EXISTS %s ON %s (" + COLUMN_NAME + ")";

  private final List<Format> formats;

  private final TaskScheduler taskScheduler;

  private final UpgradeTaskScheduler startupScheduler;

  private final Map<String, FormatStoreManager> managersByFormat;

  private final GlobalKeyValueStore globalKeyValueStore;

  private final DatabaseMigrationUtility databaseMigrationUtility;

  @Inject
  public ComponentNormalizationIntegrityChecker(
      final List<Format> formats,
      final TaskScheduler taskScheduler,
      final UpgradeTaskScheduler startupScheduler,
      final Map<String, FormatStoreManager> managersByFormat,
      final GlobalKeyValueStore globalKeyValueStore,
      final DatabaseMigrationUtility databaseMigrationUtility)
  {
    this.formats = checkNotNull(formats);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.startupScheduler = checkNotNull(startupScheduler);
    this.managersByFormat = checkNotNull(managersByFormat);
    this.globalKeyValueStore = checkNotNull(globalKeyValueStore);
    this.databaseMigrationUtility = checkNotNull(databaseMigrationUtility);
  }

  @Override
  public void checkAndRepair(Connection connection) throws SQLException {
    log.info("validating normalized_version columns");
    alterFormats(connection);

    Set<Format> formatsNeedingNormalization = formats.stream().filter(format ->
        !managersByFormat.get(format.getValue())
            .componentStore(DEFAULT_DATASTORE_NAME)
            .browseUnnormalized(1, null)
            .isEmpty()).collect(toSet());
    boolean needsNormalization = !formatsNeedingNormalization.isEmpty();

    if (needsNormalization) {
      log.info("Formats detected needing normalization {}", formatsNeedingNormalization);
      formatsNeedingNormalization.forEach(this::markFormatAsNeedingNormalization);
      scheduleTask();
    }
  }

  private void markFormatAsNeedingNormalization(final Format format) {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(getFormatKey(format));
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(false);

    globalKeyValueStore.setKey(kv);
  }

  private void scheduleTask() {
    startupScheduler.schedule(taskScheduler.createTaskConfigurationInstance(
        NormalizeComponentVersionTaskDescriptor.TYPE_ID));
  }

  private void alterFormats(final Connection connection) throws SQLException {
    try (Statement alterStmt = connection.createStatement()) {
      for (Format format : formats) {
        alter(connection, alterStmt, format);
      }
    }
  }

  private void alter(final Connection connection, final Statement alterStatement, final Format format)
      throws SQLException
  {
    String formatName = format.getValue();
    String tableName = format(TABLE_NAME, formatName).toUpperCase();

    if (!databaseMigrationUtility.tableExists(connection, tableName)) {
      log.debug("{} component table not found", formatName);
      throw new SQLException("Unable to repair " + tableName + " because it wasn't yet created");
    }

    if (!databaseMigrationUtility.columnExists(connection, tableName, COLUMN_NAME)) {
      log.info("adding missing column '{}' to {} format", COLUMN_NAME, formatName);
      alterStatement.execute(format(ADD_COLUMN_STATEMENT, formatName));

      if (!databaseMigrationUtility.indexExists(connection, format(INDEX_NAME, formatName))) {
        log.info("adding missing index '{}' to {} format", format(INDEX_NAME, formatName), formatName);
        alterStatement.execute(format(ADD_INDEX_STATEMENT, format(INDEX_NAME, formatName), tableName));
      }
    }
  }

  private String getFormatKey(final Format format) {
    return format(NormalizeComponentVersionTask.KEY_FORMAT, format.getValue());
  }
}
