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
package org.sonatype.nexus.upgrade.datastore;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Nullable;

import org.flywaydb.core.api.MigrationVersion;

/**
 * Manages database upgrades for SQL
 */
public interface UpgradeManager
{
  /**
   * Trigger database migration as the system user.
   *
   * @throws UpgradeException when a failure occurs
   */
  default void migrate() throws UpgradeException {
    migrate(null, Collections.emptyList());
  }

  /**
   * Trigger database migration with a specified user.
   *
   * @param user the user who requested database migration occur
   * @throws UpgradeException when a failure occurs
   */
  void migrate(@Nullable String user, final Collection<String> nodeIds) throws UpgradeException;

  /**
   * Validates that the current database schema version is supported by the running Nexus.
   *
   * @throws UpgradeException when the current schema version is not acceptable
   */
  void checkSchemaVersionIsAcceptable() throws UpgradeException;

  /**
   * @return {@code true} when there are pending migrations, {@code false} otherwise. For single node instances this
   *         should always be false after startup.
   */
  boolean requiresMigration();

  /**
   * Get the current database schema version
   */
  Optional<MigrationVersion> getCurrentVersion();

  /**
   * Get the latest available schema version known
   */
  Optional<MigrationVersion> getMaxMigrationVersion();

  void checkBaseline(final String target);

  /**
   * Checks whether the provided upgrade step has been applied
   */
  boolean isMigrationApplied(Class<? extends DatabaseMigrationStep> step);
}
