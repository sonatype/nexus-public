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
import java.util.Optional;

import org.sonatype.nexus.common.app.softdelete.SoftDeleteSchemaVersions;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Adds soft-delete columns ({@code deleted_on}, {@code deleted_by}) on the {@code repository} table
 * and the partial index for querying of soft-deleted repositories.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConfigurationSoftDeleteMigrationStep_2_171
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "repository";

  public static final String VERSION = SoftDeleteSchemaVersions.REPOSITORY;

  @Override
  public Optional<String> version() {
    return Optional.of(VERSION);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, TABLE_NAME)) {
      return;
    }

    addDeletedOnColumn(connection);
    addDeletedByColumn(connection);
    createPartialIndex(connection);
  }

  private void addDeletedOnColumn(final Connection connection) throws Exception {
    if (columnExists(connection, TABLE_NAME, "deleted_on")) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, """
          DO $$
          BEGIN
            BEGIN
              ALTER TABLE repository ADD COLUMN deleted_on TIMESTAMP WITH TIME ZONE NULL;
            EXCEPTION
              WHEN duplicate_column THEN NULL;
            END;
          END $$;
          """);
    }
    else if (isH2(connection)) {
      runStatement(connection,
          "ALTER TABLE repository ADD COLUMN IF NOT EXISTS deleted_on TIMESTAMP WITH TIME ZONE NULL");
    }
  }

  private void addDeletedByColumn(final Connection connection) throws Exception {
    if (columnExists(connection, TABLE_NAME, "deleted_by")) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, """
          DO $$
          BEGIN
            BEGIN
              ALTER TABLE repository ADD COLUMN deleted_by VARCHAR(200) NULL;
            EXCEPTION
              WHEN duplicate_column THEN NULL;
            END;
          END $$;
          """);
    }
    else if (isH2(connection)) {
      runStatement(connection,
          "ALTER TABLE repository ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(200) NULL");
    }
  }

  private void createPartialIndex(final Connection connection) throws Exception {
    if (indexExists(connection, TABLE_NAME, "idx_repository_soft_deleted")) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, """
          CREATE INDEX IF NOT EXISTS idx_repository_soft_deleted
          ON repository (deleted_on)
          WHERE deleted_on IS NOT NULL
          """);
    }
    else if (isH2(connection)) {
      runStatement(connection,
          "CREATE INDEX IF NOT EXISTS idx_repository_soft_deleted ON repository (deleted_on)");
    }
  }
}
