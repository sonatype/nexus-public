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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeFailedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeStartedEvent;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.sonatype.nexus.upgrade.datastore.UpgradeException;
import org.sonatype.nexus.upgrade.datastore.UpgradeManager;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Support class for upgrade managers.
 *
 * @since 3.29
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ManagedLifecycle(phase = UPGRADE)
public class UpgradeManagerImpl
    extends StateGuardLifecycleSupport
    implements UpgradeManager
{
  protected static final Logger log = LoggerFactory.getLogger(UpgradeManagerImpl.class);

  private final DataStoreManager dataStoreManager;

  private final List<DatabaseMigrationStep> migrations;

  private final PostStartupUpgradeAuditor auditor;

  private Flyway flyway;

  @Autowired
  public UpgradeManagerImpl(
      final DataStoreManager dataStoreManager,
      final PostStartupUpgradeAuditor auditor,
      final List<DatabaseMigrationStep> migrations)
  {
    this.migrations = checkVersionedMigrations(migrations);
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.auditor = checkNotNull(auditor);
  }

  @Override
  protected void doStart() throws Exception {
    flyway = createFlyway(migrations, dataStoreManager);
  }

  @Guarded(by = STARTED)
  @Override
  public void migrate(@Nullable final String user, final Collection<String> nodeIds) throws UpgradeException {
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
      String message = sanitizeFlywayMessage(e.getMessage());
      emitFailed(user, flyway, message);
      RuntimeException sanitizedCause = new RuntimeException(message);
      sanitizedCause.setStackTrace(e.getStackTrace());
      throw new UpgradeException(message, sanitizedCause);
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

  @Guarded(by = STARTED)
  @Override
  public boolean requiresMigration() {
    return flyway.info().pending().length > 0;
  }

  @Guarded(by = STARTED)
  @Override
  public Optional<MigrationVersion> getCurrentVersion() {
    return Optional.ofNullable(flyway.info().current())
        .map(MigrationInfo::getVersion);
  }

  @Guarded(by = STARTED)
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
   * <b>Previous versions</b> are : [1.1 , 1.2 , 1.3 , 1.4 , 1.5]
   * This method will return 1.5 , which is the minimum version allowed to start using 2.0
   * </p>
   *
   * @param target the version to execute
   * @return a String {@link Optional} indicating the baseline version if found
   */
  @VisibleForTesting
  Optional<String> getBaseline(final MigrationVersion target) {
    return migrations.stream()
        .map(NexusJavaMigration::new)
        .map(NexusJavaMigration::getVersion)
        .filter(Objects::nonNull)
        .filter(version -> version.getMajor().compareTo(target.getMajor()) < 0)
        .sorted(Comparator.reverseOrder())
        .map(MigrationVersion::getVersion)
        .findFirst();
  }

  @Guarded(by = STARTED)
  @Override
  public Optional<MigrationVersion> getMaxMigrationVersion() {
    return Stream.of(getMigrations(migrations))
        .map(JavaMigration::getVersion)
        .filter(Objects::nonNull)
        .max(MigrationVersion::compareTo);
  }

  @Guarded(by = STARTED)
  @Override
  public void checkSchemaVersionIsAcceptable() throws UpgradeException {
    checkSchemaVersionIsAcceptable(flyway.info());
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

  private void emitCompleted(
      @Nullable final String user,
      final Collection<String> nodeIds,
      final MigrateResult result)
  {
    if (result.migrationsExecuted > 0) {
      auditor.post(new UpgradeCompletedEvent(user, result.targetSchemaVersion, nodeIds, result.migrations.stream()
          .map(m -> m.description)
          .toArray(String[]::new)));
    }
  }

  /*
   * Versioned migrations must not be feature flagged behind licenses.
   *
   * Note: Package name is no longer restricted. The original org.sonatype filter was intended
   * to prevent license-locked migrations, but this is better enforced by ensuring migrations
   * are defensive (check table existence before altering). PRO migrations in com.sonatype
   * packages are acceptable if they check for table existence and safely no-op when tables
   * don't exist (e.g., in OSS deployments).
   */
  private List<DatabaseMigrationStep> checkVersionedMigrations(final List<DatabaseMigrationStep> migrations) {
    checkNotNull(migrations);

    // Package filter removed - migrations can be in org.sonatype or com.sonatype
    // Real protection is defensive migration code that checks table existence

    return migrations;
  }

  /*
   * Creates a Flyway instances with the known migrations for use within the UpgradeManager
   */
  private static Flyway createFlyway(
      final List<DatabaseMigrationStep> migrations,
      final DataStoreManager dataStoreManager)
  {
    JavaMigration[] flywayMigrations = getMigrations(migrations);

    if (log.isDebugEnabled()) {
      migrations.forEach(m -> log.debug("Found migration: {} version:{}", m.getClass(), m.version()));
    }

    return Flyway.configure()
        .dataSource(dataSource(dataStoreManager))
        .javaMigrations(flywayMigrations)
        .callbacks(log.isTraceEnabled() ? new Callback[]{new TraceLoggingCallback()} : new Callback[0])
        .cleanDisabled(true) // don't empty the database
        .group(false) // don't group all migrations into a single transaction
        .ignoreMigrationPatterns("*:missing") // Removed plugins (e.g. Pro)
        .baselineOnMigrate(true) // create flyway tables the first time migration is run
        .locations(new String[0]) // disable scanning for scripts
        .outOfOrder(true)
        .configuration(Map.of("flyway.postgresql.transactional.lock", "false")) // NEXUS-52081: prevent deadlock with
                                                                                // CREATE INDEX CONCURRENTLY
        .load();
  }

  private static JavaMigration[] getMigrations(final List<DatabaseMigrationStep> migrations) {
    return new SimpleDependencyResolver(migrations).resolve()
        .stream()
        .toArray(JavaMigration[]::new);
  }

  @Override
  public boolean isMigrationApplied(final Class<? extends DatabaseMigrationStep> step) {
    return Arrays.stream(flyway.info().applied())
        .map(MigrationInfo::getDescription)
        .anyMatch(NexusJavaMigration.nameMatcher(step));
  }

  private static DataSource dataSource(final DataStoreManager dataStoreManager) {
    return dataStoreManager.get(DataStoreManager.DEFAULT_DATASTORE_NAME)
        .orElseThrow(
            () -> new IllegalStateException("Missing DataStore named: " + DataStoreManager.DEFAULT_DATASTORE_NAME))
        .getDataSource();
  }

  @VisibleForTesting
  static String sanitizeFlywayMessage(final String message) {
    if (message == null) {
      return null;
    }
    // Strip Flyway advertising: promotional questions followed by "Learn more: URL".
    // [^.!?]* stops at sentence boundaries so only the immediately preceding question is removed.
    // First pattern handles "question? Learn more: URL" (with optional leading whitespace for start-of-message)
    // Second pattern handles standalone "Learn more: URL" without preceding question.
    String sanitized = message
        .replaceAll("(?:^|\\s)[^.!?]*\\?\\s+Learn more:\\s+https://\\S+", "")
        .replaceAll("(?:^|\\s+)Learn more:\\s+https://\\S+", "")
        .trim();
    return sanitized.isEmpty() ? message : sanitized;
  }
}
