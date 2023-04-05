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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.output.MigrateResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
    this.migrations = checkVersionedMigrations(migrations);
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
        .ignoreMigrationPatterns("*:missing") // Removed plugins (e.g. Pro)
        .baselineOnMigrate(true) // create flyway tables the first time migration is run
        .locations(new String[0]) // disable scanning for scripts
        .outOfOrder(true)
        .load();

    MigrateResult result = flyway.migrate();

    checkState(datastoreVersionIsAcceptable(flyway),
        "The database appears to be from a later version of Nexus Repository");

    int repeatableMigrations = migrations.size() - migrations.stream()
        .map(E::version)
        .filter(Optional::isPresent)
        .collect(Collectors.counting())
        .intValue();

    if (result.migrationsExecuted > repeatableMigrations) {
      result.migrations.forEach(m -> log.info("{} migrated to v{} in {}ms", m.description, m.version, m.executionTime));
      result.warnings.forEach(log::warn);
      log.info("Completed migration from v{} to v{}", result.initialSchemaVersion, result.targetSchemaVersion);
    }
    else if (log.isDebugEnabled()) {
      log.debug("No migrations occurred migration of {} from {} to {}", result.schemaName, result.initialSchemaVersion,
          result.targetSchemaVersion);
    }
  }

  /**
   * If migrations with 'future' state exist in db -- newer db schema detected
   * @param flyway migration engine
   * @return <b>true</b> if datastore can be used with current nxrm distribution, <b>false</b> otherwise
   */
  boolean datastoreVersionIsAcceptable(final Flyway flyway) {
    List<String> missingMigrations = Arrays.stream(flyway.info().applied())
        .filter(migrationInfo -> migrationInfo.getState() == MigrationState.FUTURE_SUCCESS)
        .map(MigrationInfo::getDescription)
        .collect(Collectors.toList());

    if (!missingMigrations.isEmpty()) {
      log.error("Missing migrations: {}", missingMigrations);
    }
    return missingMigrations.isEmpty();
  }

  /*
   * Versioned migrations must be part of public source code and not feature flagged.
   */
  private List<E> checkVersionedMigrations(final List<E> migrations) {
    checkNotNull(migrations);

    List<String> failures = migrations.stream()
      .filter(migration -> migration.version().isPresent())
      .map(E::getClass)
      .map(Class::getName)
      .filter(className -> !className.startsWith("org.sonatype"))
      .collect(Collectors.toList());

    if (!failures.isEmpty()) {
      throw new IllegalArgumentException(
          "The following migration steps are invalid: " + failures.stream().collect(Collectors.joining(", ")));
    }

    return migrations;
  }
}
