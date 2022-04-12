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

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

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

  public NexusJavaMigration(final DatabaseMigrationStep dbMigrationStep) {
    this.dbMigrationStep = dbMigrationStep;
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
    return dbMigrationStep.getClass().getSimpleName();
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
}
