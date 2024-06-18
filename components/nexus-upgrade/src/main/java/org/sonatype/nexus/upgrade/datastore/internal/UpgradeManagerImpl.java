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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.sonatype.nexus.upgrade.datastore.UpgradeException;
import org.sonatype.nexus.upgrade.datastore.UpgradeManager;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeCompletedEvent;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeFailedEvent;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeStartedEvent;

import com.google.common.annotations.VisibleForTesting;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.output.MigrateResult;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support class for upgrade managers.
 *
 * @since 3.29
 */
@Named
@Singleton
public class UpgradeManagerImpl
    extends ComponentSupport
    implements UpgradeManager
{
  private final List<DatabaseMigrationStep> migrations;

  private final PostStartupUpgradeAuditor auditor;

  private final DataSource dataSource;

  @Inject
  public UpgradeManagerImpl(
      final DataStoreManager dataStoreManager,
      final PostStartupUpgradeAuditor auditor,
      final List<DatabaseMigrationStep> migrations)
  {
    this.dataSource = checkNotNull(dataStoreManager).get(DataStoreManager.DEFAULT_DATASTORE_NAME)
        .orElseThrow(() -> new IllegalStateException("Missing DataStore named: " + DataStoreManager.DEFAULT_DATASTORE_NAME)).getDataSource();
    this.migrations = checkVersionedMigrations(migrations);
    this.auditor = checkNotNull(auditor);
  }

  @Override
  public void migrate(@Nullable final String user, final Collection<String> nodeIds) throws UpgradeException {
    Flyway flyway = createFlyway();

    // Compute current state
    MigrationInfoService info = flyway.info();

    // Ensure we're not an old version of Nexus
    checkSchemaVersionIsAcceptable(info);

    if (info.pending().length == 0) {
      log.debug("No pending migrations, skipping");
      return;
    }

    emitStarted(user, info);

    MigrateResult result;
    try {
      result = flyway.migrate();
    }
    catch (FlywayException e) {
      emitFailed(user, flyway, e.getMessage());
      throw new UpgradeException(e.getMessage(), e);
    }

    emitCompleted(user, nodeIds, result);

    int repeatableMigrations = migrations.size() - ((Long) migrations.stream()
        .map(DatabaseMigrationStep::version)
        .filter(Optional::isPresent)
        .count())
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

  @Override
  public boolean requiresMigration() {
    return createFlyway().info().pending().length > 0;
  }

  @Override
  public Optional<MigrationVersion> getCurrentVersion() {
    return Optional.ofNullable(createFlyway().info().current())
        .map(MigrationInfo::getVersion);
  }

  @Override
  public void checkBaseline(final String target) {
    Optional<MigrationVersion> version = getCurrentVersion();
    Optional<String> baseline = getBaseline(MigrationVersion.fromVersion(target));

    if (version.isPresent() && baseline.isPresent() && !version.get().isAtLeast(baseline.get())) {
      log.error("expected schema to be on baseline version (at least '{}') , shutting down", baseline.get());
      System.exit(1);
    }
  }

  /**
   * Finds the baseline version required to be able to execute the target version i.e:
   * <p>
   * <b>IF</b> Target is 2.0 and
   * <br>
   * <b>Previous versions</b>  are : [1.1 , 1.2 , 1.3 , 1.4 , 1.5]
   * This method will return 1.5 , which is the minimum version allowed to start using 2.0
   * </p>
   *
   * @param target the version to execute
   * @return a String {@link Optional} indicating the baseline version if found
   */
  @VisibleForTesting
  Optional<String> getBaseline(MigrationVersion target) {
    return migrations.stream()
        .map(NexusJavaMigration::new)
        .map(NexusJavaMigration::getVersion)
        .filter(Objects::nonNull)
        .filter(version -> version.getMajor().compareTo(target.getMajor()) < 0)
        .sorted(Comparator.reverseOrder())
        .map(MigrationVersion::getVersion)
        .findFirst();
  }

  @Override
  public Optional<MigrationVersion> getMaxMigrationVersion() {
    return Arrays.stream(getMigrations())
        .map(JavaMigration::getVersion)
        .filter(Objects::nonNull)
        .max(MigrationVersion::compareTo);
  }

  @Override
  public void checkSchemaVersionIsAcceptable() throws UpgradeException {
    checkSchemaVersionIsAcceptable(createFlyway().info());
  }

  /**
   * If migrations with 'future' state exist in db -- newer db schema detected
   *
   * @param flyway migration engine
   * @return <b>true</b> if datastore can be used with current nxrm distribution, <b>false</b> otherwise
   * @throws UpgradeException if the current schema does not match nexus.
   */
  private void checkSchemaVersionIsAcceptable(final MigrationInfoService info) throws UpgradeException {
    List<String> missingMigrations = Arrays.stream(info.applied())
        .filter(migrationInfo -> migrationInfo.getState() == MigrationState.FUTURE_SUCCESS)
        .map(MigrationInfo::getDescription)
        .collect(Collectors.toList());

    if (!missingMigrations.isEmpty()) {
      log.error("Missing migrations: {}", missingMigrations);
    }

   if (!missingMigrations.isEmpty()) {
     log.error("Missing migrations: {}", missingMigrations);
     throw new UpgradeException("The database appears to be from a later version of Nexus Repository");
   }
  }

  private void emitStarted(@Nullable final String user, final MigrationInfoService info) {
    auditor.post(new UpgradeStartedEvent(user, info.getInfoResult().schemaVersion, Arrays.stream(info.pending())
        .map(MigrationInfo::getDescription)
        .toArray(String[]::new)));
  }

  private void emitFailed(@Nullable final String user, final Flyway flyway, final String errorMessage) {
    // We invoke flyway.info() here to get the current state rather than pre-migration state.
    auditor.post(new UpgradeFailedEvent(user, flyway.info().getInfoResult().schemaVersion, errorMessage));
  }

  private void emitCompleted(@Nullable final String user, final Collection<String> nodeIds, final MigrateResult result) {
    if (result.migrationsExecuted > 0) {
      auditor.post(new UpgradeCompletedEvent(user, result.targetSchemaVersion, nodeIds, result.migrations.stream()
          .map(m -> m.description)
          .toArray(String[]::new)));
    }
  }

  /*
   * Versioned migrations must be part of public source code and not feature flagged.
   */
  private List<DatabaseMigrationStep> checkVersionedMigrations(final List<DatabaseMigrationStep> migrations) {
    checkNotNull(migrations);

    List<String> failures = migrations.stream()
      .filter(migration -> migration.version().isPresent())
      .map(Object::getClass)
      .map(Class::getName)
      .filter(className -> !className.startsWith("org.sonatype"))
      .collect(Collectors.toList());

    if (!failures.isEmpty()) {
      throw new IllegalArgumentException(
          "The following migration steps are invalid: " + failures.stream().collect(Collectors.joining(", ")));
    }

    return migrations;
  }

  /*
   * Creates a Flyway instances with the known migrations for use within the UpgradeManager
   */
  private Flyway createFlyway() {
    JavaMigration[] flywayMigrations = getMigrations();

    if (log.isDebugEnabled()) {
      migrations.forEach(m -> log.debug("Found migration: {} version:{}", m.getClass(), m.version()));
    }

    return Flyway.configure()
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
  }

  private JavaMigration[] getMigrations() {
    return migrations.stream().map(NexusJavaMigration::new).toArray(JavaMigration[]::new);
  }
}
