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
package org.sonatype.nexus.repository.content.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationUtility;

import static java.util.Objects.requireNonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Creates composite index on {format}_asset_blob tables for blob_created DESC and asset_blob_id ASC.
 * This task only creates the index if it doesn't already exist.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateAssetBlobIndexTask
    extends TaskSupport
{
  private static final String CREATE_INDEX_PG = "CREATE INDEX CONCURRENTLY ";

  private static final String CREATE_INDEX_H2 = "CREATE INDEX ";

  private static final String INDEX_NAME_PATTERN = "idx_%s_asset_blob_blob_created_asset_id";

  private static final String INDEX_DEF_PATTERN = " ON %s_asset_blob (blob_created DESC, asset_blob_id ASC);";

  private final List<Format> formats;

  private final DataSessionSupplier dataSessionSupplier;

  private final DatabaseMigrationUtility databaseMigrationUtility;

  @Autowired
  public CreateAssetBlobIndexTask(
      final List<Format> formats,
      final DataSessionSupplier dataSessionSupplier,
      final DatabaseMigrationUtility databaseMigrationUtility)
  {
    this.formats = requireNonNull(formats);
    this.dataSessionSupplier = requireNonNull(dataSessionSupplier);
    this.databaseMigrationUtility = requireNonNull(databaseMigrationUtility);
  }

  @Override
  public String getMessage() {
    return "Creating asset blob indexes for blob_created and asset_blob_id";
  }

  @Override
  protected Object execute() throws Exception {
    int indexesCreated = 0;
    List<Exception> collectedExceptions = new java.util.ArrayList<>();

    try (Connection connection = dataSessionSupplier.openConnection("nexus")) {
      boolean isH2 = databaseMigrationUtility.isH2(connection);
      String createIndexClause = isH2 ? CREATE_INDEX_H2 : CREATE_INDEX_PG;

      for (Format format : formats) {
        String tableName = format.getValue() + "_asset_blob";
        String indexName = INDEX_NAME_PATTERN.formatted(format.getValue());

        try {
          if (databaseMigrationUtility.tableExists(connection, tableName)
              && !databaseMigrationUtility.indexExists(connection, tableName, indexName)) {
            String indexDefinition = INDEX_NAME_PATTERN.formatted(format.getValue()) +
                INDEX_DEF_PATTERN.formatted(format.getValue());

            try {
              executeStatement(connection, createIndexClause + indexDefinition);
              indexesCreated++;
              log.info("Created index {} on table {}", indexName, tableName);
            }
            catch (SQLException e) {
              log.error("Failed to create index '{}' on table '{}'. Attempting to clean up invalid index.",
                  indexName, tableName, e);
              // If CREATE INDEX CONCURRENTLY fails, it may leave behind an invalid index that needs cleanup
              dropInvalidIndex(connection, indexName, isH2);
              collectedExceptions.add(e);
            }
          }
          else {
            log.debug("Index {} already exists on table {} or table doesn't exist", indexName, tableName);
          }
        }
        catch (SQLException e) {
          log.error("Failed to create index '{}' on table '{}'", indexName, tableName, e);
          collectedExceptions.add(e);
        }
      }
    }

    log.info("Created {} asset blob indexes", indexesCreated);

    // If there were any failures, throw an exception with all causes attached, this will allow task to
    // execute again when instance restarts
    if (!collectedExceptions.isEmpty()) {
      Exception aggregatedException = new Exception(
          "Failed to create %d index(es) out of %d format(s). Task will try again later.".formatted(
              collectedExceptions.size(), formats.size()));
      for (Exception e : collectedExceptions) {
        aggregatedException.addSuppressed(e);
      }
      throw aggregatedException;
    }

    return indexesCreated;
  }

  private void executeStatement(final Connection connection, final String sqlStatement) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
      statement.executeUpdate();
    }
  }

  private void dropInvalidIndex(final Connection connection, final String indexName, final boolean isH2) {
    try {
      if (!isH2) {
        // PostgreSQL: Check if index exists and is invalid, then drop it
        String checkInvalidSql = "SELECT 1 FROM pg_indexes WHERE indexname = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkInvalidSql)) {
          stmt.setString(1, indexName.toLowerCase(java.util.Locale.ENGLISH));
          try (java.sql.ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
              String dropSql = "DROP INDEX CONCURRENTLY IF EXISTS " + indexName;
              executeStatement(connection, dropSql);
              log.info("Dropped invalid index: {}", indexName);
            }
          }
        }
      }
      else {
        // H2: Simply try to drop the index if it exists
        String dropSql = "DROP INDEX IF EXISTS " + indexName;
        executeStatement(connection, dropSql);
        log.info("Dropped invalid index: {}", indexName);
      }
    }
    catch (SQLException e) {
      log.warn("Failed to drop invalid index '{}', it may need manual cleanup", indexName, e);
      // Don't throw - this is best-effort cleanup
    }
  }
}
