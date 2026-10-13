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
package org.sonatype.nexus.repository.search.sql.store.upgrade.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.sonatype.nexus.common.failure.MultipleFailures;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationUtility;

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Creates text_pattern_ops indexes on search_components columns, replacing default B-tree indexes.
 * <p>
 * The {@code text_pattern_ops} B-tree indexes support both {@code =} (equality) and {@code ^@}
 * (starts_with) operations. The old default B-tree indexes only supported {@code =}, causing {@code ^@} to fall back
 * to sequential scans on large tables. The old indexes are dropped after the new ones are created to ensure
 * zero-downtime during the transition.
 * <p>
 * Uses {@code CREATE INDEX CONCURRENTLY} on PostgreSQL to avoid blocking writes on large tables.
 * If index creation fails for any column, the task continues with remaining columns and throws an aggregated
 * exception so the task scheduler retries on next startup.
 * <p>
 * Affected columns: format, namespace, search_component_name, version
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateSearchTextPatternOpsIndexTask
    extends TaskSupport
{
  private static final String SEARCH_COMPONENTS_TABLE = "search_components";

  private record IndexConfig(String indexName, String column)
  {
  }

  private static final String CHECK_INDEX_INVALID_SQL =
      "SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid "
          + "WHERE i.indrelid = to_regclass(?) AND c.relname = ? AND NOT i.indisvalid";

  private static final String DROP_INDEX_IF_EXISTS_SQL = "DROP INDEX IF EXISTS %s";

  private static final IndexConfig[] INDEX_CONFIGS = {
      new IndexConfig("idx_search_components_format", "format"),
      new IndexConfig("idx_search_components_namespace", "namespace"),
      new IndexConfig("idx_search_components_component_name", "search_component_name"),
      new IndexConfig("idx_search_components_version", "version"),
  };

  private final DataSessionSupplier dataSessionSupplier;

  private final DatabaseMigrationUtility databaseMigrationUtility;

  @Autowired
  public CreateSearchTextPatternOpsIndexTask(
      final DataSessionSupplier dataSessionSupplier,
      final DatabaseMigrationUtility databaseMigrationUtility)
  {
    this.dataSessionSupplier = requireNonNull(dataSessionSupplier);
    this.databaseMigrationUtility = requireNonNull(databaseMigrationUtility);
  }

  @Override
  public String getMessage() {
    return "Creating text_pattern_ops indexes on search_components";
  }

  @Override
  protected Object execute() throws Exception {
    try (Connection connection = dataSessionSupplier.openConnection("nexus")) {
      if (!databaseMigrationUtility.isPostgresql(connection)) {
        log.debug("Not PostgreSQL, skipping text_pattern_ops index creation");
        return 0;
      }

      if (!databaseMigrationUtility.tableExists(connection, SEARCH_COMPONENTS_TABLE)) {
        log.info("Table {} does not exist, skipping text_pattern_ops index creation", SEARCH_COMPONENTS_TABLE);
        return 0;
      }

      return migratePostgresql(connection);
    }
  }

  private int migratePostgresql(final Connection connection) throws Exception {
    MultipleFailures failures = new MultipleFailures();
    int successCount = 0;

    for (IndexConfig config : INDEX_CONFIGS) {
      String tpoIndexName = config.indexName() + "_tpo";

      try {
        dropInvalidIndex(connection, tpoIndexName);
        createTextPatternOpsIndexConcurrently(connection, tpoIndexName, config.column());
        dropIndexIfExists(connection, config.indexName());
        successCount++;
      }
      catch (Exception e) {
        log.warn("Failed to create text_pattern_ops index for column '{}'. Error: {}",
            config.column(), e.getMessage());
        dropInvalidIndex(connection, tpoIndexName);
        failures.add(e);
      }
    }

    log.info("Created {} out of {} text_pattern_ops indexes on {}", successCount, INDEX_CONFIGS.length,
        SEARCH_COMPONENTS_TABLE);

    failures.maybePropagate(
        String.format("Failed to create %d out of %d text_pattern_ops index(es). Will retry on next startup.",
            failures.getFailures().size(), INDEX_CONFIGS.length));

    return successCount;
  }

  private void createTextPatternOpsIndexConcurrently(
      final Connection connection,
      final String indexName,
      final String column) throws SQLException
  {
    if (databaseMigrationUtility.indexExists(connection, SEARCH_COMPONENTS_TABLE, indexName)) {
      log.info("Index {} already exists, skipping creation", indexName);
      return;
    }

    log.info("Creating index {} on {} ({} text_pattern_ops) CONCURRENTLY", indexName, SEARCH_COMPONENTS_TABLE, column);
    runStatement(connection,
        String.format("CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON %s (%s text_pattern_ops)",
            indexName, SEARCH_COMPONENTS_TABLE, column));
    log.info("Created index {}", indexName);
  }

  private void dropInvalidIndex(final Connection connection, final String indexName) {
    try {
      if (databaseMigrationUtility.indexExists(connection, SEARCH_COMPONENTS_TABLE, indexName)) {
        try (PreparedStatement invalidStmt = connection.prepareStatement(CHECK_INDEX_INVALID_SQL)) {
          invalidStmt.setString(1, SEARCH_COMPONENTS_TABLE);
          invalidStmt.setString(2, indexName);
          try (ResultSet invalidRs = invalidStmt.executeQuery()) {
            if (invalidRs.next()) {
              runStatement(connection, "DROP INDEX CONCURRENTLY IF EXISTS " + indexName);
              log.warn("Dropped invalid index: {}", indexName);
            }
          }
        }
      }
    }
    catch (SQLException e) {
      log.warn("Failed to drop invalid index '{}'. Manual cleanup may be required.", indexName, e);
    }
  }

  private void dropIndexIfExists(final Connection connection, final String indexName) throws SQLException {
    runStatement(connection, String.format(DROP_INDEX_IF_EXISTS_SQL, indexName));
  }

  private void runStatement(final Connection connection, final String sql) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.executeUpdate();
    }
  }
}
