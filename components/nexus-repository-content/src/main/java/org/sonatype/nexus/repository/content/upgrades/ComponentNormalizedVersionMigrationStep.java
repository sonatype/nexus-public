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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTask;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import static java.lang.String.format;

/**
 * Migration step to populate the normalized_version column on the {format}_component tables
 */
@Named
public class ComponentNormalizedVersionMigrationStep
    extends ComponentSupport
    implements RepeatableDatabaseMigrationStep
{
  private final String TABLE_NAME = "{format}_component";

  private final String COLUMN_NAME = "normalized_version";

  private final String INDEX_NAME = "idx_{format}_normalized_version";

  private final String ADD_COLUMN_STATEMENT =
      format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s VARCHAR;", TABLE_NAME, COLUMN_NAME);

  private final String ADD_INDEX_STATEMENT =
      format("CREATE INDEX IF NOT EXISTS %s ON  %s (%s)", INDEX_NAME, TABLE_NAME, COLUMN_NAME);

  private final List<Format> formats;

  private final GlobalKeyValueStore globalKeyValueStore;

  private final TaskScheduler taskScheduler;

  private final UpgradeTaskScheduler startupScheduler;

  @Inject
  public ComponentNormalizedVersionMigrationStep(
      final List<Format> formats,
      final GlobalKeyValueStore globalKeyValueStore,
      final TaskScheduler taskScheduler,
      final UpgradeTaskScheduler startupScheduler)
  {
    this.formats = formats;
    this.globalKeyValueStore = globalKeyValueStore;
    this.taskScheduler = taskScheduler;
    this.startupScheduler = startupScheduler;
  }

  @Override
  public Integer getChecksum() {
    return Objects.hash(formats.stream()
        .map(Format::getValue)
        // Ordered so the hash is consistent
        .sorted()
        .toArray());
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    alterFormats(connection);
    scheduleTask();
  }

  private void scheduleTask() {
    startupScheduler.schedule(taskScheduler.createTaskConfigurationInstance(
        NormalizeComponentVersionTaskDescriptor.TYPE_ID));
  }

  private void alterFormats(final Connection connection) throws SQLException {
    try (Statement alterStmt = connection.createStatement()) {
      for (Format format : formats) {
        if (!isFormatNormalized(format)) {
          alter(connection, alterStmt, format);
        }
      }
    }
  }

  private void alter(final Connection connection, final Statement alterStatement, final Format format)
      throws SQLException
  {
    String formatName = format.getValue();
    log.info("validating {} component table", formatName);

    String tableName = replace(TABLE_NAME, formatName);

    if (!tableExists(connection, tableName)) {
      log.debug("{} component table not found", formatName);
      return;
    }

    if (!columnExists(connection, tableName, COLUMN_NAME)) {
      log.info("adding missing column '{}' to {} format", COLUMN_NAME, formatName);
      alterStatement.execute(replace(ADD_COLUMN_STATEMENT, formatName));

      if (!indexExists(connection, replace(INDEX_NAME, formatName))) {
        log.info("adding missing index '{}' to {} format", replace(INDEX_NAME, formatName), formatName);
        alterStatement.execute(replace(ADD_INDEX_STATEMENT, formatName));
      }
    }
  }

  private boolean isFormatNormalized(final Format format) {
    return globalKeyValueStore
        .getKey(String.format(NormalizeComponentVersionTask.KEY_FORMAT, format.getValue()))
        .map(NexusKeyValue::getAsBoolean)
        .orElse(false);
  }

  private static String replace(final String query, final String format) {
    return query.replaceAll("\\{format\\}", format);
  }
}
