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
import java.sql.Statement;
import java.util.Optional;

import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConfigurationSoftDeleteMigrationStep_2_168Test
{
  @DataSessionConfiguration(daos = {})
  TestDataSessionSupplier dataSessionSupplier;

  @DatabaseTest
  void testVersion() {
    ConfigurationSoftDeleteMigrationStep_2_168 underTest = new ConfigurationSoftDeleteMigrationStep_2_168();
    assertEquals(Optional.of("2.168"), underTest.version());
  }

  /**
   * NEXUS-54969: the migration is now a no-op. It must NOT add the soft-delete columns to the repository table.
   */
  @DatabaseTest
  void testMigrate_doesNotAddColumns() throws Exception {
    ConfigurationSoftDeleteMigrationStep_2_168 underTest = new ConfigurationSoftDeleteMigrationStep_2_168();

    try (Connection conn = dataSessionSupplier.openConnection(); Statement statement = conn.createStatement()) {
      statement.execute(createRepositoryTable(underTest.isH2(conn)));

      underTest.migrate(conn);

      assertFalse(underTest.columnExists(conn, "repository", "deleted_on"),
          "deleted_on column should NOT be added (migration disabled)");
      assertFalse(underTest.columnExists(conn, "repository", "deleted_by"),
          "deleted_by column should NOT be added (migration disabled)");
    }
  }

  @DatabaseTest
  void testMigrate_isIdempotentNoOp() throws Exception {
    ConfigurationSoftDeleteMigrationStep_2_168 underTest = new ConfigurationSoftDeleteMigrationStep_2_168();

    try (Connection conn = dataSessionSupplier.openConnection(); Statement statement = conn.createStatement()) {
      statement.execute(createRepositoryTable(underTest.isH2(conn)));

      underTest.migrate(conn);
      assertDoesNotThrow(() -> underTest.migrate(conn),
          "Migration should be idempotent");

      assertFalse(underTest.columnExists(conn, "repository", "deleted_on"),
          "deleted_on column should still be absent after second run");
      assertFalse(underTest.columnExists(conn, "repository", "deleted_by"),
          "deleted_by column should still be absent after second run");
    }
  }

  @DatabaseTest
  void testMigrate_noTable() throws Exception {
    ConfigurationSoftDeleteMigrationStep_2_168 underTest = new ConfigurationSoftDeleteMigrationStep_2_168();

    try (Connection conn = dataSessionSupplier.openConnection()) {
      assertDoesNotThrow(() -> underTest.migrate(conn),
          "Migration should handle missing table gracefully");
    }
  }

  private String createRepositoryTable(final boolean isH2) {
    return String.format("""
        CREATE TABLE IF NOT EXISTS repository (
          id              %s NOT NULL,
          name            VARCHAR(200) NOT NULL,
          recipe_name     VARCHAR(200) NOT NULL,
          online          BOOLEAN      NOT NULL,
          routing_rule_id %s NULL,
          attributes      %s NOT NULL,

          CONSTRAINT pk_repository_id PRIMARY KEY (id),
          CONSTRAINT uk_repository_name UNIQUE (name)
        )
        """,
        isH2 ? "UUID" : "UUID",
        isH2 ? "UUID" : "UUID",
        isH2 ? "VARCHAR" : "JSONB");
  }
}
