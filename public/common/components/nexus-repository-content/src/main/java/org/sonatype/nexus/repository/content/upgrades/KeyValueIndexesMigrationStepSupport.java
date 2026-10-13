
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
import java.util.List;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static java.util.Objects.requireNonNull;

/**
 * Adds indexes to {format}_key_value tables for existing databases.
 */
public abstract class KeyValueIndexesMigrationStepSupport
    implements DatabaseMigrationStep
{
  private final List<String> formats;

  /**
   * @param formats a list of formats to create indexes for if required
   */
  protected KeyValueIndexesMigrationStepSupport(final List<String> formats) {
    this.formats = requireNonNull(formats);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    for (String format : formats) {
      String tableName = format + "_key_value";
      if (tableExists(connection, tableName)) {
        createIndexes(connection, format, tableName);
      }
    }
  }

  private void createIndexes(
      final Connection connection,
      final String format,
      final String tableName) throws SQLException
  {
    // Note: 'key' is a reserved keyword in H2, so we need to escape it with backticks
    String keyColumn = isH2(connection) ? "`key`" : "key";

    String uniqueIndexName = "uk_" + format + "_repository_category_key";
    String regularIndexName = "idx_" + format + "_key_value_repository_key";

    createUniqueIndexIfNotExists(connection, uniqueIndexName, tableName, "repository_id, category, " + keyColumn);

    createIndexIfNotExists(connection, regularIndexName, tableName, "repository_id, " + keyColumn);
  }

  private void createUniqueIndexIfNotExists(
      final Connection connection,
      final String indexName,
      final String tableName,
      final String columns) throws SQLException
  {
    if (indexExists(connection, tableName, indexName)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              CREATE UNIQUE INDEX %s ON %s (%s);
            EXCEPTION
              WHEN duplicate_table THEN NULL;
            END;
          END $$;
          """, indexName, tableName, columns));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("CREATE UNIQUE INDEX IF NOT EXISTS %s ON %s (%s)", indexName, tableName, columns));
    }
  }

  private void createIndexIfNotExists(
      final Connection connection,
      final String indexName,
      final String tableName,
      final String columns) throws SQLException
  {
    if (indexExists(connection, tableName, indexName)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              CREATE INDEX %s ON %s (%s);
            EXCEPTION
              WHEN duplicate_table THEN NULL;
            END;
          END $$;
          """, indexName, tableName, columns));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s)", indexName, tableName, columns));
    }
  }
}
