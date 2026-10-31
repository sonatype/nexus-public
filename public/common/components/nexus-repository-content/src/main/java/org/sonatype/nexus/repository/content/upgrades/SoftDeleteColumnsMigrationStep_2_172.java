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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.sonatype.nexus.common.app.softdelete.SoftDeleteSchemaVersions;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Re-adds the soft-delete columns ({@code deleted_on}, {@code deleted_by}) to every existing
 * {@code {format}_asset} and {@code {format}_component} table after the NEXUS-54969 neutralization
 * (NEXUS-54973).
 *
 * <p>
 * The column add is done by this versioned step (schema anchor {@value #VERSION}) rather than a separate
 * anchor plus repeatable step, so that reaching {@value #VERSION} guarantees the columns exist: stores gate
 * their {@code deleted_on} predicate on {@code DatabaseCheck.isAtLeast(VERSION)}, which must never read
 * {@code true} before the columns are present. Under ZDU a new-version node serves traffic while migrations
 * run, and Flyway runs versioned steps before repeatable ones, so a no-op anchor gated ahead of a repeatable
 * column-add would open the gate against a missing column. The {@code deleted_on} indexes are created
 * separately by the {@code {Asset,Component}SoftDeleteRepeatableMigrationStep}s (index absence only affects
 * performance, never correctness).
 *
 * <p>
 * Safe to run live because NEXUS-54969 converted the affected reads to explicit column lists, so the
 * {@code ALTER} cannot change a warmed prepared statement's result shape. The PostgreSQL {@code DO} block
 * guards against the NEXUS-49154 lock-before-check outage. Future formats get the columns from
 * {@code createSchema}.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SoftDeleteColumnsMigrationStep_2_172
    implements DatabaseMigrationStep
{
  public static final String VERSION = SoftDeleteSchemaVersions.CONTENT;

  private static final Logger log = LoggerFactory.getLogger(SoftDeleteColumnsMigrationStep_2_172.class);

  private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

  @Override
  public Optional<String> version() {
    return Optional.of(VERSION);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    List<String> tables = new ArrayList<>();
    tables.addAll(findTables(connection, "asset", true));
    tables.addAll(findTables(connection, "component", false));

    if (tables.isEmpty()) {
      log.info("No content tables found, skipping soft-delete column reintroduction");
      return;
    }

    log.info("Ensuring soft-delete columns on {} content tables", tables.size());
    for (String tableName : tables) {
      addColumn(connection, tableName, "deleted_on", "TIMESTAMP WITH TIME ZONE");
      addColumn(connection, tableName, "deleted_by", "VARCHAR(200)");
    }
  }

  private List<String> findTables(
      final Connection connection,
      final String suffix,
      final boolean excludeBlob) throws Exception
  {
    List<String> tables = new ArrayList<>();
    String sql;
    if (isPostgresql(connection)) {
      sql = "SELECT table_name FROM information_schema.tables "
          + "WHERE table_schema = 'public' AND table_name LIKE '%\\_" + suffix + "' ESCAPE '\\'"
          + (excludeBlob ? " AND table_name NOT LIKE '%\\_asset\\_blob' ESCAPE '\\'" : "");
    }
    else {
      String upper = suffix.toUpperCase();
      sql = "SELECT table_name FROM information_schema.tables "
          + "WHERE table_name LIKE '%\\_" + upper + "' ESCAPE '\\'"
          + (excludeBlob ? " AND table_name NOT LIKE '%\\_ASSET\\_BLOB' ESCAPE '\\'" : "");
    }

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        String name = rs.getString("table_name");
        if (SAFE_TABLE_NAME.matcher(name).matches()) {
          tables.add(name);
        }
        else {
          log.warn("Skipping content table with unexpected name: {}", name);
        }
      }
    }
    return tables;
  }

  private void addColumn(
      final Connection connection,
      final String tableName,
      final String column,
      final String type) throws Exception
  {
    if (columnExists(connection, tableName, column)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              ALTER TABLE %s ADD COLUMN %s %s NULL;
            EXCEPTION
              WHEN duplicate_column THEN NULL;
            END;
          END $$;
          """, tableName, column, type));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s %s NULL", tableName, column, type));
    }
  }
}
