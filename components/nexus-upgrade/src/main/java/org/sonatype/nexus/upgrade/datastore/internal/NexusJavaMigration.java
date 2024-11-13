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

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.sonatype.nexus.upgrade.datastore.DefinedUpgradeRound;

import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;

/**
 * Wrapper for {@link DatabaseMigrationStep} for Flyway
 *
 * @since 3.29
 */
public class NexusJavaMigration implements JavaMigration
{
  private final DatabaseMigrationStep dbMigrationStep;

  private final Integer round;

  public NexusJavaMigration(final DatabaseMigrationStep dbMigrationStep) {
    this(dbMigrationStep, null);
  }

  public NexusJavaMigration(final DatabaseMigrationStep dbMigrationStep, final Integer round) {
    this.dbMigrationStep = dbMigrationStep;

    if (dbMigrationStep instanceof DefinedUpgradeRound) {
      if (round != null) {
        throw new IllegalStateException("Incompatible API, DefinedUpgradeRound cannot be mixd with DependsOn");
      }
      this.round = ((DefinedUpgradeRound) dbMigrationStep).getUpgradeRound();
    }
    else {
      this.round = round;
    }
  }

  @Override
  public boolean canExecuteInTransaction() {
    return dbMigrationStep.canExecuteInTransaction();
  }

  @Override
  public Integer getChecksum() {
    return dbMigrationStep.getChecksum();
  }

  @Override
  public String getDescription() {
    // We need to remove the Guice suffixes to achieve a stable name,
    // e.g. S3BlobStoreMetricsMigrationStep$$EnhancerByGuice$$2c7e99a6
    String className = dbMigrationStep.getClass().getSimpleName().split("\\$\\$")[0];

    if (round != null) {
      return String.format("Z_%03d_%s", round, className);
    }
    return className;
  }

  @Override
  public MigrationVersion getVersion() {
    return dbMigrationStep.version()
        .map(MigrationVersion::fromVersion)
        .orElse(null);
  }

  @Override
  public boolean isBaselineMigration() {
    return false;
  }

  @Override
  public boolean isUndo() {
    return false;
  }

  @Override
  public void migrate(final Context context) throws Exception {
    dbMigrationStep.migrate(context.getConnection());
  }

  /**
   * Provides a {@link Predicate} which will match the against flyway migration descriptions and also accounts for
   * a possible round prefix.
   */
  public static Predicate<String> nameMatcher(final Class<? extends DatabaseMigrationStep> step) {
    return Pattern.compile("^(Z_\\d{3}_)?" + step.getSimpleName() + "$").asPredicate();
  }
}
