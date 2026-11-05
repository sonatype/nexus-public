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
package org.sonatype.nexus.upgrade.datastore;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationUtilityTest
{
  @DataSessionConfiguration(daos = {})
  TestDataSessionSupplier supplier;

  private final DatabaseMigrationUtility underTest = new DatabaseMigrationUtility();

  @DatabaseTest(postgresql = true)
  void testExactlyOneSupportedDatabaseIsDetected() throws Exception {
    try (Connection conn = open()) {
      assertTrue(underTest.isH2(conn) ^ underTest.isPostgresql(conn), "H2 or PostgreSQL");
    }
  }

  @DatabaseTest(postgresql = true)
  void testTableExists() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table util_exists (id int)");

      assertTrue(underTest.tableExists(conn, "util_exists"));
      assertTrue(underTest.tableExists(conn, "UTIL_EXISTS"), "lookup ignores case");
      assertFalse(underTest.tableExists(conn, "util_missing"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testColumnExists() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table util_columns (id int, base_version varchar)");

      assertTrue(underTest.columnExists(conn, "util_columns", "base_version"));
      assertTrue(underTest.columnExists(conn, "UTIL_COLUMNS", "BASE_VERSION"), "lookup ignores case");
      assertFalse(underTest.columnExists(conn, "util_columns", "missing"));
      assertFalse(underTest.columnExists(conn, "util_no_table", "id"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testConstraintExists() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table util_constraints (id int, constraint pk_util_constraints primary key (id))");

      assertTrue(underTest.constraintExists(conn, "util_constraints", "pk_util_constraints"));
      assertTrue(underTest.constraintExists(conn, "UTIL_CONSTRAINTS", "PK_UTIL_CONSTRAINTS"), "lookup ignores case");
      assertFalse(underTest.constraintExists(conn, "util_constraints", "pk_missing"));
      assertFalse(underTest.constraintExists(conn, "util_no_table", "pk_util_constraints"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testIndexExists() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table util_indexes (id int, name varchar)");
      execute(conn, "create index idx_util_indexes_name on util_indexes (name)");

      assertTrue(underTest.indexExists(conn, "util_indexes", "idx_util_indexes_name"));
      assertTrue(underTest.indexExists(conn, "UTIL_INDEXES", "IDX_UTIL_INDEXES_NAME"), "lookup ignores case");
      assertFalse(underTest.indexExists(conn, "util_indexes", "idx_missing"));
      assertFalse(underTest.indexExists(conn, "util_other", "idx_util_indexes_name"),
          "index belongs to a different table");
    }
  }

  @DatabaseTest(postgresql = true)
  void testCreateIndexIsIdempotent() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table util_create (id int, name varchar)");
      assertFalse(underTest.indexExists(conn, "util_create", "idx_util_create_name"));

      underTest.createIndex(conn, "idx_util_create_name", "util_create", "name");
      assertTrue(underTest.indexExists(conn, "util_create", "idx_util_create_name"));

      // IF NOT EXISTS: a second call must not fail
      underTest.createIndex(conn, "idx_util_create_name", "util_create", "name");
      assertTrue(underTest.indexExists(conn, "util_create", "idx_util_create_name"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testCreateIndexOnMultipleColumns() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table util_multi (id int, name varchar, key_value varchar)");

      underTest.createIndex(conn, "idx_util_multi", "util_multi", "name, key_value");

      assertTrue(underTest.indexExists(conn, "util_multi", "idx_util_multi"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testDropIndex() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table util_drop (id int, name varchar)");
      execute(conn, "create index idx_util_drop_name on util_drop (name)");
      assertTrue(underTest.indexExists(conn, "util_drop", "idx_util_drop_name"));

      underTest.dropIndex(conn, "idx_util_drop_name");
      assertFalse(underTest.indexExists(conn, "util_drop", "idx_util_drop_name"));

      // IF EXISTS: dropping again must not fail
      underTest.dropIndex(conn, "idx_util_drop_name");
    }
  }

  @DatabaseTest(postgresql = true)
  void testFindTablesBySuffixMatchesSuffixOnly() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table maven2_component (id int)");
      execute(conn, "create table npm_component (id int)");
      execute(conn, "create table maven2_component_tag (id int)");
      execute(conn, "create table maven2_asset (id int)");

      List<String> tables = underTest.findTablesBySuffix(conn, "_component");

      assertThat(normalise(tables), containsInAnyOrder("maven2_component", "npm_component"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testFindTablesBySuffixIgnoresCaseOfSuffix() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table raw_component (id int)");

      assertThat(normalise(underTest.findTablesBySuffix(conn, "_COMPONENT")), containsInAnyOrder("raw_component"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testFindTablesBySuffixTreatsUnderscoreLiterally() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table raw_component (id int)");
      // 'x' where the suffix has '_' would match if '_' were left as a LIKE wildcard
      execute(conn, "create table rawxcomponent (id int)");

      assertThat(normalise(underTest.findTablesBySuffix(conn, "_component")), containsInAnyOrder("raw_component"));
    }
  }

  @DatabaseTest(postgresql = true)
  void testFindTablesBySuffixNoMatches() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table raw_asset (id int)");

      assertThat(underTest.findTablesBySuffix(conn, "_no_such_suffix"), empty());
    }
  }

  @DatabaseTest(postgresql = true)
  void testFindTablesBySuffixIncludesBlobTables() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "create table raw_asset_blob (id int)");

      assertThat(normalise(underTest.findTablesBySuffix(conn, "_asset_blob")), containsInAnyOrder("raw_asset_blob"));
    }
  }

  @DatabaseTest(h2 = false, postgresql = true)
  void testFindTablesBySuffixIsScopedToCurrentSchemaOnPostgres() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "drop schema if exists live cascade");
      execute(conn, "drop schema if exists backup cascade");
      try {
        execute(conn, "create schema live");
        execute(conn, "create schema backup");
        execute(conn, "create table live.maven2_component (id int)");
        execute(conn, "create table backup.npm_component (id int)");

        execute(conn, "set search_path to live");
        assertThat(underTest.findTablesBySuffix(conn, "_component"), containsInAnyOrder("maven2_component"));

        execute(conn, "set search_path to backup");
        assertThat(underTest.findTablesBySuffix(conn, "_component"), containsInAnyOrder("npm_component"));
      }
      finally {
        execute(conn, "drop schema if exists live cascade");
        execute(conn, "drop schema if exists backup cascade");
      }
    }
  }

  @DatabaseTest(h2 = false, postgresql = true)
  void testTableExistsHonoursSearchPathOnPostgres() throws Exception {
    try (Connection conn = open()) {
      execute(conn, "drop schema if exists live cascade");
      try {
        execute(conn, "create schema live");
        execute(conn, "create table live.util_scoped (id int)");

        assertFalse(underTest.tableExists(conn, "util_scoped"));
        execute(conn, "set search_path to live");
        assertTrue(underTest.tableExists(conn, "util_scoped"));
      }
      finally {
        execute(conn, "drop schema if exists live cascade");
      }
    }
  }

  private Connection open() throws SQLException {
    return supplier.openConnection();
  }

  private static void execute(final Connection conn, final String sql) throws SQLException {
    try (Statement statement = conn.createStatement()) {
      statement.execute(sql);
    }
  }

  /** H2 reports unquoted names in upper case; PostgreSQL in lower case. */
  private static List<String> normalise(final List<String> names) {
    return names.stream().map(String::toLowerCase).toList();
  }
}
