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
import java.util.Optional;

import org.sonatype.nexus.content.testsuite.groups.PostgresTestGroup;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(PostgresTestGroup.class)
public class DatabaseMigrationStepTest
{

  private static final String CUSTOM_SQL = "CREATE TABLE IF NOT EXISTS custom.test (\n"
      + "      domain            VARCHAR(200)   NOT NULL,\n"
      + "      token             VARCHAR(200)   NOT NULL,\n"
      + "      CONSTRAINT pk_domain PRIMARY KEY (domain)\n"
      + "    );\n"
      + "";

  @Rule
  public DataSessionRule customSessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  @Test
  public void testCustomSchemaIndexExists() throws Exception {
    try (Connection conn = customSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(conn, "drop schema if exists custom cascade");
      underTest.runStatement(conn, "create schema custom authorization test");

      // create a table + index in custom schema
      underTest.runStatement(conn, CUSTOM_SQL);
      assertFalse("index does not exist in public schema", underTest.indexExists(conn, "test", "pk_domain"));

      underTest.runStatement(conn, "SET search_path TO custom");
      assertTrue("index exists in custom schema", underTest.indexExists(conn, "test", "pk_domain"));
    }
  }

  /*
   * A backup schema left behind by an earlier upgrade holds a same-named table carrying the column and
   * index a migration step is about to add. The guards must report the schema the DDL will resolve in, not
   * the backup (NEXUS-54867).
   */
  @Test
  public void testGuardsIgnoreOtherSchemas() throws Exception {
    try (Connection conn = customSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(conn, "drop schema if exists live cascade");
      underTest.runStatement(conn, "drop schema if exists backup cascade");
      underTest.runStatement(conn, "create schema live authorization test");
      underTest.runStatement(conn, "create schema backup authorization test");
      underTest.runStatement(conn, "create table live.maven2_component (id int, name varchar)");
      underTest.runStatement(conn, "create table backup.maven2_component (id int, base_version varchar)");
      underTest.runStatement(conn,
          "create index idx_maven2_component_base_version on backup.maven2_component (base_version)");

      underTest.runStatement(conn, "SET search_path TO live");

      assertTrue("live table resolves", underTest.tableExists(conn, "maven2_component"));
      assertFalse("column exists only in the backup schema",
          underTest.columnExists(conn, "maven2_component", "base_version"));
      assertTrue("column of the live table is found",
          underTest.columnExists(conn, "maven2_component", "name"));
      assertFalse("index exists only in the backup schema",
          underTest.indexExists(conn, "maven2_component", "idx_maven2_component_base_version"));

      underTest.runStatement(conn, "SET search_path TO backup");

      assertTrue("column is found once the backup schema is the resolved one",
          underTest.columnExists(conn, "maven2_component", "base_version"));
      assertTrue("index is found once the backup schema is the resolved one",
          underTest.indexExists(conn, "maven2_component", "idx_maven2_component_base_version"));
    }
  }

  // The index exists check should target the current search_path, prevents false positives on idx creation
  @Test
  public void testGuardsIgnoreOtherSchemasOnSearchPath() throws Exception {
    try (Connection conn = customSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(conn, "drop schema if exists live cascade");
      underTest.runStatement(conn, "drop schema if exists backup cascade");
      underTest.runStatement(conn, "create schema live authorization test");
      underTest.runStatement(conn, "create schema backup authorization test");
      underTest.runStatement(conn, "create table live.maven2_component (id int, base_version varchar)");
      underTest.runStatement(conn, "create table backup.maven2_component (id int, base_version varchar)");
      underTest.runStatement(conn,
          "create index idx_maven2_component_base_version on backup.maven2_component (base_version)");

      underTest.runStatement(conn, "SET search_path TO live, backup");

      assertFalse("the index is on the backup table, not the resolved one",
          underTest.indexExists(conn, "maven2_component", "idx_maven2_component_base_version"));

      // the guard-then-create shape of Maven2ComponentBaseVersionMigrationStep_2_29#addIndexIfNotExists
      if (!underTest.indexExists(conn, "maven2_component", "idx_maven2_component_base_version")) {
        underTest.runStatement(conn,
            "create index idx_maven2_component_base_version on maven2_component (base_version)");
      }

      underTest.runStatement(conn, "drop schema backup cascade");

      assertTrue("index was created on the live table",
          underTest.indexExists(conn, "maven2_component", "idx_maven2_component_base_version"));
    }
  }

  // Confirms the index is created even when it exists on another schema (NEXUS-54867)
  @Test
  public void testIndexGuardsDoNotSkipCreationOnTheLiveTable() throws Exception {
    try (Connection conn = customSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(conn, "drop schema if exists live cascade");
      underTest.runStatement(conn, "drop schema if exists backup cascade");
      underTest.runStatement(conn, "create schema live authorization test");
      underTest.runStatement(conn, "create schema backup authorization test");
      underTest.runStatement(conn, "create table live.distributed_cache"
          + " (id int, name varchar, key bytea, expiration_date timestamptz)");
      underTest.runStatement(conn, "create table backup.distributed_cache"
          + " (id int, name varchar, key bytea, expiration_date timestamptz)");
      underTest.runStatement(conn,
          "create unique index idx_distributed_cache_name_key on backup.distributed_cache (name, key)");
      underTest.runStatement(conn,
          "create index idx_distributed_cache_expiration_date on backup.distributed_cache (expiration_date)");

      underTest.runStatement(conn, "SET search_path TO live");

      assertTrue("the live table resolves, so the step does not return early",
          underTest.tableExists(conn, "distributed_cache"));

      // the guard-then-create shape of DistributedCacheIndexesMigration#createIndexes
      if (!underTest.indexExists(conn, "distributed_cache", "idx_distributed_cache_name_key")) {
        underTest.runStatement(conn,
            "create unique index idx_distributed_cache_name_key on distributed_cache (name, key)");
      }
      if (!underTest.indexExists(conn, "distributed_cache", "idx_distributed_cache_expiration_date")) {
        underTest.runStatement(conn,
            "create index idx_distributed_cache_expiration_date on distributed_cache (expiration_date)");
      }

      underTest.runStatement(conn, "drop schema backup cascade");

      assertTrue("unique index was created on the live table",
          underTest.indexExists(conn, "distributed_cache", "idx_distributed_cache_name_key"));
      assertTrue("plain index was created on the live table",
          underTest.indexExists(conn, "distributed_cache", "idx_distributed_cache_expiration_date"));
    }
  }

  /**
   * Plain indexes must be found, and constraint-backed ones must keep being found - callers such as
   * {@code ApiKeyUpgrade_1_31} gate destructive DDL on a primary key name.
   */
  @Test
  public void testIndexExistsFindsPlainAndConstraintBackedIndexes() throws Exception {
    try (Connection conn = customSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(conn, "drop schema if exists live cascade");
      underTest.runStatement(conn, "create schema live authorization test");
      underTest.runStatement(conn, "create table live.api_key (id int, name varchar,"
          + " constraint pk_api_key primary key (id), constraint uk_api_key_name unique (name))");
      underTest.runStatement(conn, "create index idx_api_key_name on live.api_key (name)");

      underTest.runStatement(conn, "SET search_path TO live");

      assertTrue("plain index", underTest.indexExists(conn, "api_key", "idx_api_key_name"));
      assertTrue("primary key backed index", underTest.indexExists(conn, "api_key", "pk_api_key"));
      assertTrue("unique constraint backed index", underTest.indexExists(conn, "api_key", "uk_api_key_name"));
      assertFalse("absent index", underTest.indexExists(conn, "api_key", "idx_does_not_exist"));
      assertTrue("constraint", underTest.constraintExists(conn, "api_key", "pk_api_key"));
      assertFalse("absent constraint", underTest.constraintExists(conn, "api_key", "no_such_constraint"));
    }
  }

  /**
   * A guard naming a table that does not resolve returns false rather than throwing.
   */
  @Test
  public void testGuardsOnMissingTable() throws Exception {
    try (Connection conn = customSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      assertFalse(underTest.tableExists(conn, "no_such_table"));
      assertFalse(underTest.columnExists(conn, "no_such_table", "any_column"));
      assertFalse(underTest.indexExists(conn, "no_such_table", "any_index"));
      assertFalse(underTest.constraintExists(conn, "no_such_table", "any_constraint"));
    }
  }

  private final DatabaseMigrationStep underTest = new DatabaseMigrationStep()
  {
    public Optional<String> version() {
      return Optional.of("0.0");
    }

    @Override
    public void migrate(final Connection connection) {
    }
  };
}
