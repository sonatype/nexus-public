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
package org.sonatype.nexus.upgrade.datastore.internal;

import java.util.List;

import javax.sql.DataSource;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Support class for upgrade managers.
 *
 * @since 3.29
 */
abstract class UpgradeManagerSupport<E extends DatabaseMigrationStep>
    extends ComponentSupport
{
  private final List<E> migrations;

  private final DataSource dataSource;

  protected UpgradeManagerSupport(
      final DataStoreManager dataStoreManager,
      final String dataStoreName,
      final List<E> migrations)
  {
    this.dataSource = dataStoreManager.get(dataStoreName)
        .orElseThrow(() -> new IllegalStateException("Missing DataStore named: " + dataStoreName)).getDataSource();
    this.migrations = migrations;
  }

  void migrate() {
    JavaMigration[] flywayMigrations = migrations.stream().map(NexusJavaMigration::new).toArray(JavaMigration[]::new);

    if (log.isDebugEnabled()) {
      migrations.forEach(m -> log.debug("Found migration: {} version:{}", m.getClass(), m.version()));
    }

    Flyway flyway = Flyway.configure()
        .dataSource(dataSource)
        .javaMigrations(flywayMigrations)
        .callbacks(log.isTraceEnabled() ? new Callback[]{new TraceLoggingCallback()} : new Callback[0])
        .cleanDisabled(true) // don't empty the database
        .group(false) // don't group all migrations into a single transaction
        .ignoreMissingMigrations(true) // Removed plugins (e.g. Pro)
        .baselineOnMigrate(true) // create flyway tables the first time migration is run
        .locations(new String[0]) // disable scanning for scripts
        .load();

    MigrateResult result = flyway.migrate();

    if (result.migrationsExecuted > 0) {
      result.migrations.forEach(m -> log.info("{} migrated to v{} in {}s", m.description, m.version, m.executionTime));
      result.warnings.forEach(log::warn);
      log.info("Completed migration from v{} to v{}", result.initialSchemaVersion, result.targetSchemaVersion);
    }
    else if (log.isDebugEnabled()) {
      log.debug("No migrations occurred migration of {} from {} to {}", result.schemaName, result.initialSchemaVersion,
          result.targetSchemaVersion);
    }
  }
}
