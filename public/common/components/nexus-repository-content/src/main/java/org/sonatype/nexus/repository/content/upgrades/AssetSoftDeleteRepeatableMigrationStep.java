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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Creates the soft-delete {@code deleted_on} indexes on every {@code {format}_asset} table (NEXUS-54973).
 * The columns themselves are added by the versioned {@link SoftDeleteColumnsMigrationStep_2_172}; this step
 * only builds the indexes, which are performance-only (their absence never breaks a query).
 *
 * <p>
 * Repeatable and keyed on the registered {@link Format} set, so a newly-registered format's tables are
 * indexed automatically. Runs non-transactionally so PostgreSQL can use {@code CREATE INDEX CONCURRENTLY}
 * (NEXUS-51733); all statements are idempotent.
 */
@Component
public class AssetSoftDeleteRepeatableMigrationStep
    implements RepeatableDatabaseMigrationStep
{
  private static final Logger log = LoggerFactory.getLogger(AssetSoftDeleteRepeatableMigrationStep.class);

  private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

  /**
   * Bump when the index logic changes in a way that must re-run on already-migrated databases (the format
   * set alone may be unchanged). 3 = split from column work (columns moved to the versioned step).
   */
  private static final int LOGIC_VERSION = 3;

  private final List<Format> formats;

  @Autowired
  public AssetSoftDeleteRepeatableMigrationStep(final List<Format> formats) {
    this.formats = formats;
  }

  @Override
  public Integer getChecksum() {
    int formatsHash = Arrays.hashCode(formats.stream()
        .map(Format::getValue)
        .sorted()
        .toArray());
    return Objects.hash(LOGIC_VERSION, formatsHash);
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    List<String> assetTables = findAssetTables(connection);
    if (assetTables.isEmpty()) {
      log.info("No asset tables found, skipping soft-delete index creation");
      return;
    }

    log.info("Ensuring soft-delete indexes on {} asset tables", assetTables.size());
    for (String tableName : assetTables) {
      // The indexes reference deleted_on, which the versioned SoftDeleteColumnsMigrationStep_2_172 adds. Flyway
      // runs that versioned step before this repeatable one, so the column normally exists here; guard defensively
      // and skip (rather than fail the migration) if a table somehow predates the column.
      if (!columnExists(connection, tableName, "deleted_on")) {
        log.warn("Skipping soft-delete indexes on {}: deleted_on column not present yet", tableName);
        continue;
      }
      createPartialIndex(connection, tableName);
      createActiveIndex(connection, tableName, "idx_" + tableName + "_active",
          "repository_id, asset_id", "repository_id, deleted_on, asset_id");
    }
  }

  private List<String> findAssetTables(final Connection connection) throws Exception {
    List<String> tables = new ArrayList<>();
    String sql;
    if (isPostgresql(connection)) {
      sql = "SELECT table_name FROM information_schema.tables "
          + "WHERE table_schema = 'public' AND table_name LIKE '%\\_asset' ESCAPE '\\'"
          + " AND table_name NOT LIKE '%\\_asset\\_blob' ESCAPE '\\'";
    }
    else {
      sql = "SELECT table_name FROM information_schema.tables "
          + "WHERE table_name LIKE '%\\_ASSET' ESCAPE '\\'"
          + " AND table_name NOT LIKE '%\\_ASSET\\_BLOB' ESCAPE '\\'";
    }

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        String name = rs.getString("table_name");
        if (SAFE_TABLE_NAME.matcher(name).matches()) {
          tables.add(name);
        }
        else {
          log.warn("Skipping asset table with unexpected name: {}", name);
        }
      }
    }
    return tables;
  }

  private void createPartialIndex(final Connection connection, final String tableName) throws Exception {
    String indexName = "idx_" + tableName + "_soft_deleted";
    if (indexExists(connection, tableName, indexName)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, String.format(
          "CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON %s (deleted_on) WHERE deleted_on IS NOT NULL",
          indexName, tableName));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("CREATE INDEX IF NOT EXISTS %s ON %s (deleted_on)", indexName, tableName));
    }
  }

  /**
   * Active-path index for the hot {@code deleted_on IS NULL} browse/count access pattern; without it the
   * added predicate defeats index-only scans and forces per-row heap fetches (NEXUS-51733). Point/path
   * lookups already narrow to one row via {@code uk_${format}_asset_repository_path}, so they need no
   * dedicated active index. PostgreSQL uses a partial index ({@code WHERE deleted_on IS NULL}); H2 carries
   * {@code deleted_on} as a leading column instead.
   */
  private void createActiveIndex(
      final Connection connection,
      final String tableName,
      final String indexName,
      final String postgresColumns,
      final String h2Columns) throws Exception
  {
    if (indexExists(connection, tableName, indexName)) {
      return;
    }

    if (isPostgresql(connection)) {
      runStatement(connection, String.format(
          "CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON %s (%s) WHERE deleted_on IS NULL",
          indexName, tableName, postgresColumns));
    }
    else if (isH2(connection)) {
      runStatement(connection,
          String.format("CREATE INDEX IF NOT EXISTS %s ON %s (%s)", indexName, tableName, h2Columns));
    }
  }
}
