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
package org.sonatype.nexus.internal.security.apikey.upgrade;

import java.sql.Connection;
import java.sql.Statement;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ApiKeyCreatorTrackingMigrationStep_2_108}.
 *
 * Verifies:
 * - Column addition for creator tracking (created_by_user_id, created_by_realm)
 * - Idempotency (can run multiple times without errors)
 * - Graceful handling of missing tables
 * - Compatibility with both H2 and PostgreSQL
 */
@ExtendWith(DatabaseExtension.class)
class ApiKeyCreatorTrackingMigrationStep_2_108Test
    extends Test5Support
{
  private static final String TABLE_NAME = "api_key_v2";

  @DataSessionConfiguration(daos = {})
  TestDataSessionSupplier dataSessionSupplier;

  @Test
  void testMigrate_addsCreatorTrackingColumns() throws Exception {
    ApiKeyCreatorTrackingMigrationStep_2_108 underTest = new ApiKeyCreatorTrackingMigrationStep_2_108();

    try (Connection conn = dataSessionSupplier.openConnection(); Statement statement = conn.createStatement()) {
      // Create api_key_v2 table without creator tracking columns (simulating pre-migration schema)
      statement.execute(createApiKeyV2TableWithoutCreatorTracking(underTest.isH2(conn)));

      // Verify columns don't exist before migration
      assertFalse(underTest.columnExists(conn, TABLE_NAME, "created_by_user_id"),
          "created_by_user_id column should not exist before migration");
      assertFalse(underTest.columnExists(conn, TABLE_NAME, "created_by_realm"),
          "created_by_realm column should not exist before migration");

      // Run migration
      underTest.migrate(conn);

      // Verify columns were added
      assertTrue(underTest.columnExists(conn, TABLE_NAME, "created_by_user_id"),
          "created_by_user_id column should exist after migration");
      assertTrue(underTest.columnExists(conn, TABLE_NAME, "created_by_realm"),
          "created_by_realm column should exist after migration");
    }
  }

  @Test
  void testMigrate_idempotent() throws Exception {
    ApiKeyCreatorTrackingMigrationStep_2_108 underTest = new ApiKeyCreatorTrackingMigrationStep_2_108();

    try (Connection conn = dataSessionSupplier.openConnection(); Statement statement = conn.createStatement()) {
      // Create api_key_v2 table without creator tracking columns
      statement.execute(createApiKeyV2TableWithoutCreatorTracking(underTest.isH2(conn)));

      // Run migration twice to verify idempotency
      underTest.migrate(conn);
      assertDoesNotThrow(() -> underTest.migrate(conn),
          "Migration should be idempotent and not fail when columns already exist");

      // Verify columns still exist after second migration
      assertTrue(underTest.columnExists(conn, TABLE_NAME, "created_by_user_id"),
          "created_by_user_id column should exist after second migration");
      assertTrue(underTest.columnExists(conn, TABLE_NAME, "created_by_realm"),
          "created_by_realm column should exist after second migration");
    }
  }

  @Test
  void testMigrate_missingTable() throws Exception {
    ApiKeyCreatorTrackingMigrationStep_2_108 underTest = new ApiKeyCreatorTrackingMigrationStep_2_108();

    try (Connection conn = dataSessionSupplier.openConnection()) {
      // No table created, should not throw exception
      assertDoesNotThrow(() -> underTest.migrate(conn),
          "Migration should handle missing api_key_v2 table gracefully");
    }
  }

  @Test
  void testMigrate_withExistingData() throws Exception {
    ApiKeyCreatorTrackingMigrationStep_2_108 underTest = new ApiKeyCreatorTrackingMigrationStep_2_108();

    try (Connection conn = dataSessionSupplier.openConnection(); Statement statement = conn.createStatement()) {
      // Create api_key_v2 table without creator tracking columns
      statement.execute(createApiKeyV2TableWithoutCreatorTracking(underTest.isH2(conn)));

      // Insert test data
      statement.execute("""
          INSERT INTO api_key_v2 (username, principals, domain, access_key, secret, created)
          VALUES ('testuser', 'test-principals', 'test-domain', 'test-access-key', 'test-secret',
                  CURRENT_TIMESTAMP)
          """);

      // Run migration
      underTest.migrate(conn);

      // Verify columns were added and existing data preserved
      assertTrue(underTest.columnExists(conn, TABLE_NAME, "created_by_user_id"),
          "created_by_user_id column should exist after migration");
      assertTrue(underTest.columnExists(conn, TABLE_NAME, "created_by_realm"),
          "created_by_realm column should exist after migration");

      // Verify existing row still exists
      var rs = statement.executeQuery("SELECT COUNT(*) FROM api_key_v2");
      assertTrue(rs.next(), "Should have result");
      assertTrue(rs.getInt(1) == 1, "Should have 1 row after migration");
    }
  }

  @Test
  void testMigrate_doesNotCreateIndexes() throws Exception {
    ApiKeyCreatorTrackingMigrationStep_2_108 underTest = new ApiKeyCreatorTrackingMigrationStep_2_108();

    try (Connection conn = dataSessionSupplier.openConnection(); Statement statement = conn.createStatement()) {
      // Create api_key_v2 table without creator tracking columns
      statement.execute(createApiKeyV2TableWithoutCreatorTracking(underTest.isH2(conn)));

      // Run migration
      underTest.migrate(conn);

      // Verify indexes were NOT created (Issue #17 fix - no queries filter on these columns)
      assertFalse(underTest.indexExists(conn, TABLE_NAME, "idx_api_key_v2_created_by_user_id"),
          "idx_api_key_v2_created_by_user_id index should NOT be created");
      assertFalse(underTest.indexExists(conn, TABLE_NAME, "idx_api_key_v2_created_by_realm"),
          "idx_api_key_v2_created_by_realm index should NOT be created");
    }
  }

  /**
   * Creates api_key_v2 table without creator tracking columns to simulate pre-migration schema.
   */
  private String createApiKeyV2TableWithoutCreatorTracking(final boolean isH2) {
    return """
        CREATE TABLE IF NOT EXISTS api_key_v2 (
          username      VARCHAR(200) NOT NULL,
          principals    VARCHAR NOT NULL,
          domain        VARCHAR(200) NOT NULL,
          access_key    VARCHAR(200) NOT NULL,
          secret        VARCHAR NOT NULL,
          created       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

          CONSTRAINT pk_api_key_v2 PRIMARY KEY (domain, access_key),
          CONSTRAINT uk_api_key_v2_principals UNIQUE (domain, principals)
        );
        """;
  }
}
