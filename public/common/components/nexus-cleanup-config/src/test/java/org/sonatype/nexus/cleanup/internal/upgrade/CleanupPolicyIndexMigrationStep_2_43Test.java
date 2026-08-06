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
package org.sonatype.nexus.cleanup.internal.upgrade;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CleanupPolicyIndexMigrationStep_2_43Test
{
  private static final String TABLE_NAME = "cleanup_policy";

  private static final String INDEX_NAME = "idx_cleanup_policy_format";

  private static final String CREATE_TABLE = """
      CREATE TABLE IF NOT EXISTS cleanup_policy (
        name     VARCHAR(200)  NOT NULL,
        notes    VARCHAR(400)  NULL,
        format   VARCHAR(100)  NOT NULL,
        mode     VARCHAR(100)  NOT NULL,
        criteria VARCHAR(4000) NOT NULL,
        CONSTRAINT pk_cleanup_policy_name PRIMARY KEY (name)
      )
      """;

  private final CleanupPolicyIndexMigrationStep_2_43 underTest = new CleanupPolicyIndexMigrationStep_2_43();

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();
    assertThat(version.isPresent(), is(true));
    assertThat(version.get(), is("2.43"));
    assertThat(version, is(Optional.of("2.43")));
  }

  @Test
  public void testMigrateOnH2CreatesIndex() throws Exception {
    try (Connection connection = h2Connection("cleanup_policy_index_create")) {
      underTest.runStatement(connection, CREATE_TABLE);

      assertThat(underTest.tableExists(connection, TABLE_NAME), is(true));
      assertThat(underTest.indexExists(connection, TABLE_NAME, INDEX_NAME), is(false));

      underTest.migrate(connection);

      assertThat("index should exist after migration",
          underTest.indexExists(connection, TABLE_NAME, INDEX_NAME), is(true));
    }
  }

  @Test
  public void testMigrateOnH2IsIdempotent() throws Exception {
    // The second migrate exercises the indexExists == true early-return branch.
    try (Connection connection = h2Connection("cleanup_policy_index_idempotent")) {
      underTest.runStatement(connection, CREATE_TABLE);

      underTest.migrate(connection);
      underTest.migrate(connection);

      assertThat("index should still exist after a second migration",
          underTest.indexExists(connection, TABLE_NAME, INDEX_NAME), is(true));
    }
  }

  @Test
  public void testMigrateIsNoOpWhenTableMissing() throws Exception {
    try (Connection connection = h2Connection("cleanup_policy_index_missing_table")) {
      assertThat(underTest.tableExists(connection, TABLE_NAME), is(false));

      underTest.migrate(connection);

      assertThat("no table should be created by the migration",
          underTest.tableExists(connection, TABLE_NAME), is(false));
    }
  }

  @Test
  public void testMigrateOnH2ExecutesCreateIndexIfNotExistsSql() throws Exception {
    // Pins the exact H2-branch SQL (including the "IF NOT EXISTS" clause and the "format" column),
    // which the real-H2 tests cannot verify because indexExists matches on index/table name only.
    List<String> executedSql = new ArrayList<>();
    Connection connection = h2MockConnection(executedSql, true, false);

    underTest.migrate(connection);

    assertThat(executedSql,
        hasItem(is("CREATE INDEX IF NOT EXISTS idx_cleanup_policy_format ON cleanup_policy (format)")));
  }

  @Test
  public void testMigrateOnH2SkipsCreateWhenIndexExists() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection connection = h2MockConnection(executedSql, true, true);

    underTest.migrate(connection);

    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
  }

  @Test
  public void testMigrateOnPostgresqlExecutesCreateIndexDoBlock() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, true, false);

    underTest.migrate(connection);

    assertThat(executedSql, hasItem(allOf(
        containsString("DO $$"),
        containsString("CREATE INDEX idx_cleanup_policy_format ON cleanup_policy (format)"),
        containsString("WHEN duplicate_table THEN NULL"))));
  }

  @Test
  public void testMigrateOnPostgresqlExecutesExactDoBlockSql() throws Exception {
    // Pins the full DO-block verbatim so any change to its structure/whitespace is caught.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, true, false);

    underTest.migrate(connection);

    String expectedDoBlock = """
        DO $$
        BEGIN
          BEGIN
            CREATE INDEX idx_cleanup_policy_format ON cleanup_policy (format);
          EXCEPTION
            WHEN duplicate_table THEN NULL;
          END;
        END $$;
        """;
    assertThat(executedSql, hasItem(is(expectedDoBlock)));
  }

  @Test
  public void testMigrateOnPostgresqlSkipsCreateWhenIndexExists() throws Exception {
    // The index already exists -> indexExists early-return; the DO-block must not run.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, true, true);

    underTest.migrate(connection);

    assertThat(executedSql, hasItem(containsString("pg_indexes")));
    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
    assertThat(executedSql, not(hasItem(containsString("DO $$"))));
  }

  @Test
  public void testMigrateOnPostgresqlIsNoOpWhenTableMissing() throws Exception {
    // tableExists is false -> migrate must not even probe for the index or attempt creation.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, false, false);

    underTest.migrate(connection);

    assertThat(executedSql, hasItem(containsString("to_regclass")));
    assertThat(executedSql, not(hasItem(containsString("pg_indexes"))));
    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testMigrateThrowsOnUnsupportedDatabase() throws Exception {
    // tableExists throws UnsupportedOperationException for any non-H2/non-PostgreSQL database.
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("Oracle");

    underTest.migrate(connection);
  }

  private static Connection h2Connection(final String name) throws Exception {
    return DriverManager.getConnection("jdbc:h2:mem:" + name);
  }

  /**
   * Builds a mocked H2 {@link Connection} that records every executed SQL statement, so the H2-specific
   * branch can be exercised without depending on a real database.
   * <p>
   * NOTE for maintainers: this mock assumes production issues ALL SQL (including the
   * {@code CREATE INDEX IF NOT EXISTS} DDL) via {@link Connection#prepareStatement(String)}. If the
   * production code is ever changed to use {@code createStatement()}/{@code Statement.execute(...)} for the
   * DDL, the captured SQL would no longer appear in {@code executedSql} and the dependent SQL-capture tests
   * must be updated accordingly. The real-H2 test ({@code testMigrateOnH2CreatesIndex}) guards actual behavior.
   */
  private static Connection h2MockConnection(
      final List<String> executedSql,
      final boolean tablePresent,
      final boolean indexPresent) throws SQLException
  {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("H2");

    when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
      String sql = invocation.getArgument(0);
      executedSql.add(sql);
      PreparedStatement statement = mock(PreparedStatement.class);

      if (sql.contains("INFORMATION_SCHEMA.TABLES")) {
        // tableExists check
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(tablePresent);
        when(statement.executeQuery()).thenReturn(results);
      }
      else if (sql.contains("information_schema.indexes")) {
        // indexExists check
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(indexPresent);
        when(statement.executeQuery()).thenReturn(results);
      }
      // otherwise (the CREATE INDEX statement) statement.execute() defaults to false

      return statement;
    });

    return connection;
  }

  /**
   * Builds a mocked PostgreSQL {@link Connection} that records every executed SQL statement, so the
   * PostgreSQL-specific branch can be exercised without depending on a real database.
   */
  private static Connection postgresqlMockConnection(
      final List<String> executedSql,
      final boolean tablePresent,
      final boolean indexPresent) throws SQLException
  {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

    when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
      String sql = invocation.getArgument(0);
      executedSql.add(sql);
      PreparedStatement statement = mock(PreparedStatement.class);

      if (sql.contains("to_regclass")) {
        // tableExists check
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(tablePresent);
        when(results.getObject(1)).thenReturn(tablePresent ? TABLE_NAME : null);
        when(statement.executeQuery()).thenReturn(results);
      }
      else if (sql.contains("current_schema")) {
        // currentSchema lookup used by indexExists on PostgreSQL
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(true);
        when(results.getString(1)).thenReturn("public");
        when(statement.executeQuery()).thenReturn(results);
      }
      else if (sql.contains("pg_indexes")) {
        // indexExists check
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(indexPresent);
        when(statement.executeQuery()).thenReturn(results);
      }
      // otherwise (the CREATE INDEX DO-block) statement.execute() defaults to false

      return statement;
    });

    return connection;
  }
}
