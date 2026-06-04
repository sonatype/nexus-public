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
package org.sonatype.nexus.repository.search.sql.store.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Adds indexes to search_components and search_assets tables for existing databases.
 *
 * Related to NEXUS-49154 - Migration created to allow removal of CREATE INDEX
 * from MyBatis createSchema() which could cause lock contention on startup.
 * <p>
 * This is a copy of SearchTableIndexesMigrationStep_2_53 which was made a no-op because
 * the migration may have run when the tables did not exist.
 */
@Component
public class SearchTableIndexesMigrationStep_2_77
    implements DatabaseMigrationStep
{
  private static final String SEARCH_COMPONENTS_TABLE = "search_components";

  private static final String SEARCH_ASSETS_TABLE = "search_assets";

  @Override
  public Optional<String> version() {
    return Optional.of("2.77");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, SEARCH_COMPONENTS_TABLE)) {
      createSearchComponentsIndexes(connection);
    }
    if (tableExists(connection, SEARCH_ASSETS_TABLE)) {
      createSearchAssetsIndexes(connection);
    }
  }

  private void createSearchComponentsIndexes(final Connection connection) throws SQLException {
    if (isPostgresql(connection)) {
      createPostgreSQLSearchComponentsIndexes(connection);
    }
    else if (isH2(connection)) {
      createH2SearchComponentsIndexes(connection);
    }
  }

  private void createPostgreSQLSearchComponentsIndexes(final Connection connection) throws SQLException {
    // Regular indexes
    createIndexIfNotExists(connection, "idx_search_components_normalised_version",
        SEARCH_COMPONENTS_TABLE, "normalised_version");
    createIndexIfNotExists(connection, "idx_search_components_namespace",
        SEARCH_COMPONENTS_TABLE, "namespace");
    createIndexIfNotExists(connection, "idx_search_components_component_name",
        SEARCH_COMPONENTS_TABLE, "search_component_name");
    createIndexIfNotExists(connection, "idx_search_components_format",
        SEARCH_COMPONENTS_TABLE, "format");
    createIndexIfNotExists(connection, "idx_search_components_repository_name",
        SEARCH_COMPONENTS_TABLE, "search_repository_name");
    createIndexIfNotExists(connection, "idx_search_components_prerelease",
        SEARCH_COMPONENTS_TABLE, "prerelease");
    createIndexIfNotExists(connection, "idx_search_components_entity_version",
        SEARCH_COMPONENTS_TABLE, "entity_version");

    // pg_trgm extension and GIST index for path search (PostgreSQL 13+)
    createPgTrgmIndexIfSupported(connection);

    // GIN indexes for TSVECTOR columns
    createGinIndexIfNotExists(connection, "idx_search_components_keywords",
        SEARCH_COMPONENTS_TABLE, "keywords");
    createGinIndexIfNotExists(connection, "idx_search_components_md5",
        SEARCH_COMPONENTS_TABLE, "md5");
    createGinIndexIfNotExists(connection, "idx_search_components_sha1",
        SEARCH_COMPONENTS_TABLE, "sha1");
    createGinIndexIfNotExists(connection, "idx_search_components_sha256",
        SEARCH_COMPONENTS_TABLE, "sha256");
    createGinIndexIfNotExists(connection, "idx_search_components_sha512",
        SEARCH_COMPONENTS_TABLE, "sha512");
    createGinIndexIfNotExists(connection, "idx_search_components_format_field_values_1",
        SEARCH_COMPONENTS_TABLE, "format_field_values_1");
    createGinIndexIfNotExists(connection, "idx_search_components_format_field_values_2",
        SEARCH_COMPONENTS_TABLE, "format_field_values_2");
    createGinIndexIfNotExists(connection, "idx_search_components_format_field_values_3",
        SEARCH_COMPONENTS_TABLE, "format_field_values_3");
    createGinIndexIfNotExists(connection, "idx_search_components_format_field_values_4",
        SEARCH_COMPONENTS_TABLE, "format_field_values_4");
    createGinIndexIfNotExists(connection, "idx_search_components_format_field_values_5",
        SEARCH_COMPONENTS_TABLE, "format_field_values_5");
    createGinIndexIfNotExists(connection, "idx_search_components_format_field_values_6",
        SEARCH_COMPONENTS_TABLE, "format_field_values_6");
    createGinIndexIfNotExists(connection, "idx_search_components_format_field_values_7",
        SEARCH_COMPONENTS_TABLE, "format_field_values_7");
    createGinIndexIfNotExists(connection, "idx_search_components_format_uploaders",
        SEARCH_COMPONENTS_TABLE, "uploaders");
    createGinIndexIfNotExists(connection, "idx_search_components_format_uploader_ips",
        SEARCH_COMPONENTS_TABLE, "uploader_ips");
    createGinIndexIfNotExists(connection, "idx_search_components_tsvector_paths",
        SEARCH_COMPONENTS_TABLE, "tsvector_paths");
    createGinIndexIfNotExists(connection, "idx_search_components_tsvector_format",
        SEARCH_COMPONENTS_TABLE, "tsvector_format");
    createGinIndexIfNotExists(connection, "idx_search_components_tsvector_namespace",
        SEARCH_COMPONENTS_TABLE, "tsvector_namespace");
    createGinIndexIfNotExists(connection, "idx_search_components_tsvector_search_component_name",
        SEARCH_COMPONENTS_TABLE, "tsvector_search_component_name");
    createGinIndexIfNotExists(connection, "idx_search_components_tsvector_version",
        SEARCH_COMPONENTS_TABLE, "tsvector_version");
    createGinIndexIfNotExists(connection, "idx_search_components_tsvector_search_repository_name",
        SEARCH_COMPONENTS_TABLE, "tsvector_search_repository_name");
    createGinIndexIfNotExists(connection, "idx_search_components_tsvector_tags",
        SEARCH_COMPONENTS_TABLE, "tsvector_tags");
  }

  private void createH2SearchComponentsIndexes(final Connection connection) throws SQLException {
    // H2 only has simple indexes (no TSVECTOR columns)
    createIndexIfNotExists(connection, "idx_search_components_normalised_version",
        SEARCH_COMPONENTS_TABLE, "normalised_version");
    createIndexIfNotExists(connection, "idx_search_components_namespace",
        SEARCH_COMPONENTS_TABLE, "namespace");
    createIndexIfNotExists(connection, "idx_search_components_component_name",
        SEARCH_COMPONENTS_TABLE, "search_component_name");
    createIndexIfNotExists(connection, "idx_search_components_format",
        SEARCH_COMPONENTS_TABLE, "format");
    createIndexIfNotExists(connection, "idx_search_components_repository_name",
        SEARCH_COMPONENTS_TABLE, "search_repository_name");
    createIndexIfNotExists(connection, "idx_search_components_prerelease",
        SEARCH_COMPONENTS_TABLE, "prerelease");
    createIndexIfNotExists(connection, "idx_search_components_uploaders",
        SEARCH_COMPONENTS_TABLE, "uploaders");
    createIndexIfNotExists(connection, "idx_search_components_uploader_ips",
        SEARCH_COMPONENTS_TABLE, "uploader_ips");
  }

  private void createSearchAssetsIndexes(final Connection connection) throws SQLException {
    // Create indexes for all 20 asset_format_value columns (H2 and PostgreSQL)
    for (int i = 1; i <= 20; i++) {
      createIndexIfNotExists(connection,
          "idx_search_assets_asset_format_value_" + i,
          SEARCH_ASSETS_TABLE,
          "asset_format_value_" + i);
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

  private void createGinIndexIfNotExists(
      final Connection connection,
      final String indexName,
      final String tableName,
      final String column) throws SQLException
  {
    if (indexExists(connection, tableName, indexName)) {
      return;
    }

    // GIN indexes are only for PostgreSQL
    if (isPostgresql(connection)) {
      runStatement(connection, String.format("""
          DO $$
          BEGIN
            BEGIN
              CREATE INDEX %s ON %s USING GIN (%s);
            EXCEPTION
              WHEN duplicate_table THEN NULL;
            END;
          END $$;
          """, indexName, tableName, column));
    }
  }

  /**
   * Creates the pg_trgm extension and GIST index for path search if PostgreSQL 13+.
   * Migrations check table existence, safely becoming no-ops when tables do not exist.
   */
  private void createPgTrgmIndexIfSupported(final Connection connection) throws SQLException {
    if (!isPostgresql(connection)) {
      return;
    }

    // This function creates pg_trgm extension if available (PostgreSQL 13+)
    // and then creates a GIST index for efficient path pattern matching
    runStatement(connection, """
        DO $$
        DECLARE
            version_number INTEGER;
            db_version VARCHAR;
        BEGIN
            SELECT LOWER(version()) INTO db_version;
            IF db_version ~ '^postgresql \\d\\d' THEN
                SELECT SUBSTRING(db_version, CHAR_LENGTH('postgresql') + 2, 2) INTO version_number;
                IF version_number >= 13 THEN
                    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
                        CREATE EXTENSION pg_trgm;
                    END IF;
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_indexes
                        WHERE indexname = 'trgm_idx_search_components_paths'
                    ) THEN
                        CREATE INDEX trgm_idx_search_components_paths
                        ON search_components USING GIST (paths gist_trgm_ops);
                    END IF;
                END IF;
            END IF;
        END $$;
        """);
  }
}
