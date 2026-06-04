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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BrowseNodeMigrationStep_1_38
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final List<Format> formats;

  @Autowired
  public BrowseNodeMigrationStep_1_38(final List<Format> formats) {
    this.formats = formats;
  }

  private static final String CREATE_PARENT_ID_INDEX_PG = "CREATE INDEX CONCURRENTLY IF NOT EXISTS ";

  private static final String CREATE_PARENT_ID_INDEX_H2 = "CREATE INDEX IF NOT EXISTS ";

  private static final String INDEX_NAME = "idx_%s_browse_node_parent_id ON %s_browse_node (parent_id);";

  @Override
  public Optional<String> version() {
    return Optional.of("1.38");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    boolean isH2 = isH2(connection);
    String finalIndexClause = isH2
        ? CREATE_PARENT_ID_INDEX_H2 + INDEX_NAME
        : CREATE_PARENT_ID_INDEX_PG + INDEX_NAME;

    List<Exception> collectedExceptions = new ArrayList<>();
    int successCount = 0;

    for (Format format : formats) {
      String formatValue = format.getValue();
      String tableName = formatValue + "_browse_node";
      String indexName = "idx_" + formatValue + "_browse_node_parent_id";

      try {
        if (!tableExists(connection, tableName)) {
          log.debug("Table {} does not exist, skipping index creation", tableName);
          continue;
        }

        if (indexExists(connection, tableName, indexName)) {
          log.debug("Index {} already exists on table {}, skipping", indexName, tableName);
          continue;
        }

        String sqlStatement = String.format(finalIndexClause, formatValue, formatValue);
        executeStatement(connection, sqlStatement);
        successCount++;
      }
      catch (SQLException e) {
        log.warn("Failed to create index '{}' on table '{}'. The index creation may have timed out or " +
            "failed due to concurrent operations. Error: {}", indexName, tableName, e.getMessage());

        if (!isH2) {
          dropInvalidIndex(connection, indexName);
        }

        collectedExceptions.add(e);
      }
    }

    log.info("Created {} out of {} browse_node parent_id indexes", successCount, formats.size());

    if (!collectedExceptions.isEmpty()) {
      log.warn("Failed to create {} index(es). The upgrade will continue, but these indexes " +
          "should be created manually for optimal performance.", collectedExceptions.size());

      for (Exception e : collectedExceptions) {
        log.debug("Index creation failure details:", e);
      }
    }
  }

  private void executeStatement(final Connection connection, final String sqlStatement) throws SQLException {
    try (PreparedStatement select = connection.prepareStatement(sqlStatement)) {
      select.executeUpdate();
    }
  }

  private void dropInvalidIndex(final Connection connection, final String indexName) {
    try {
      String checkInvalidSql = "SELECT 1 FROM pg_indexes WHERE indexname = ?";
      try (PreparedStatement stmt = connection.prepareStatement(checkInvalidSql)) {
        stmt.setString(1, indexName.toLowerCase(java.util.Locale.ENGLISH));
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            String dropSql = "DROP INDEX CONCURRENTLY IF EXISTS " + indexName;
            executeStatement(connection, dropSql);
            log.info("Dropped invalid index: {}", indexName);
          }
        }
      }
    }
    catch (SQLException e) {
      log.warn("Failed to drop invalid index '{}'. Manual cleanup may be required.", indexName, e);
    }
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }
}
