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
package org.sonatype.nexus.audit.internal.store;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditEventsTimestampIndexMigrationStep_2_156Test
{
  private static final String TABLE_NAME = "audit_events";

  private static final String INDEX_NAME = "idx_audit_events_timestamp";

  private static final String CREATE_TABLE = """
      CREATE TABLE IF NOT EXISTS audit_events (
        id         BIGINT AUTO_INCREMENT PRIMARY KEY,
        domain     VARCHAR(255) NOT NULL,
        type       VARCHAR(255) NOT NULL,
        context    VARCHAR(500),
        timestamp  TIMESTAMP WITH TIME ZONE NOT NULL,
        initiator  VARCHAR(255),
        node_id    VARCHAR(100),
        attributes JSON
      )
      """;

  private final AuditEventsTimestampIndexMigrationStep_2_156 underTest =
      new AuditEventsTimestampIndexMigrationStep_2_156();

  @Test
  void testVersion() {
    Optional<String> version = underTest.version();
    assertThat(version.isPresent(), is(true));
    assertThat(version.get(), is("2.156"));
  }

  @Test
  void testCanExecuteInTransactionIsFalse() {
    // Required so that PostgreSQL's CREATE INDEX CONCURRENTLY is not wrapped in a transaction.
    assertThat(underTest.canExecuteInTransaction(), is(false));
  }

  @Test
  void testMigrateOnH2CreatesIndex() throws Exception {
    try (Connection connection = h2Connection("audit_events_timestamp_index_create")) {
      underTest.runStatement(connection, CREATE_TABLE);

      assertThat(underTest.tableExists(connection, TABLE_NAME), is(true));
      assertThat(underTest.indexExists(connection, TABLE_NAME, INDEX_NAME), is(false));

      underTest.migrate(connection);

      assertThat("index should exist after migration",
          underTest.indexExists(connection, TABLE_NAME, INDEX_NAME), is(true));
    }
  }

  @Test
  void testMigrateOnH2IsIdempotent() throws Exception {
    // The second migrate exercises the indexExists == true early-return branch.
    try (Connection connection = h2Connection("audit_events_timestamp_index_idempotent")) {
      underTest.runStatement(connection, CREATE_TABLE);

      underTest.migrate(connection);
      underTest.migrate(connection);

      assertThat("index should still exist after a second migration",
          underTest.indexExists(connection, TABLE_NAME, INDEX_NAME), is(true));
    }
  }

  @Test
  void testMigrateIsNoOpWhenTableMissing() throws Exception {
    try (Connection connection = h2Connection("audit_events_timestamp_index_missing_table")) {
      assertThat(underTest.tableExists(connection, TABLE_NAME), is(false));

      underTest.migrate(connection);

      assertThat("no table should be created by the migration",
          underTest.tableExists(connection, TABLE_NAME), is(false));
    }
  }

  @Test
  void testMigrateOnH2ExecutesCreateIndexIfNotExistsSql() throws Exception {
    // Pins the exact H2-branch SQL, which the real-H2 tests cannot verify because indexExists
    // matches on index/table name only.
    List<String> executedSql = new ArrayList<>();
    Connection connection = h2MockConnection(executedSql, true, false);

    underTest.migrate(connection);

    assertThat(executedSql,
        hasItem(is("CREATE INDEX IF NOT EXISTS idx_audit_events_timestamp ON audit_events (timestamp DESC)")));
  }

  @Test
  void testMigrateOnH2SkipsCreateWhenIndexExists() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection connection = h2MockConnection(executedSql, true, true);

    underTest.migrate(connection);

    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
  }

  @Test
  void testMigrateOnPostgresqlExecutesCreateIndexConcurrentlySql() throws Exception {
    // Pins the exact PostgreSQL-branch SQL, guarding against accidental removal of CONCURRENTLY
    // or a change of index-key columns.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, true, false);

    underTest.migrate(connection);

    assertThat(executedSql,
        hasItem(is(
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_events_timestamp ON audit_events (timestamp DESC)")));
  }

  @Test
  void testMigrateOnPostgresqlSkipsCreateWhenIndexExists() throws Exception {
    // The index already exists -> indexExists early-return; CREATE INDEX must not run.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, true, true);

    underTest.migrate(connection);

    assertThat(executedSql, hasItem(containsString("pg_indexes")));
    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
  }

  @Test
  void testMigrateOnPostgresqlIsNoOpWhenTableMissing() throws Exception {
    // tableExists is false -> migrate must not probe for the index or attempt creation.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, false, false);

    underTest.migrate(connection);

    assertThat(executedSql, hasItem(containsString("to_regclass")));
    assertThat(executedSql, not(hasItem(containsString("pg_indexes"))));
    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
  }

  @Test
  void testMigrateThrowsOnUnsupportedDatabase() throws Exception {
    // tableExists throws UnsupportedOperationException for any non-H2/non-PostgreSQL database.
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("Oracle");

    assertThrows(UnsupportedOperationException.class, () -> underTest.migrate(connection));
  }

  private static Connection h2Connection(final String name) throws Exception {
    return DriverManager.getConnection("jdbc:h2:mem:" + name);
  }

  /**
   * Builds a mocked H2 {@link Connection} that records every executed SQL statement, so the H2-specific
   * branch can be exercised without depending on a real database.
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
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(tablePresent);
        when(statement.executeQuery()).thenReturn(results);
      }
      else if (sql.contains("INFORMATION_SCHEMA.TABLE_CONSTRAINTS")
          || sql.contains("information_schema.indexes")) {
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(indexPresent);
        when(statement.executeQuery()).thenReturn(results);
      }

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
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(tablePresent);
        when(results.getObject(1)).thenReturn(tablePresent ? TABLE_NAME : null);
        when(statement.executeQuery()).thenReturn(results);
      }
      else if (sql.contains("current_schema")) {
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(true);
        when(results.getString(1)).thenReturn("public");
        when(statement.executeQuery()).thenReturn(results);
      }
      else if (sql.contains("pg_indexes")) {
        ResultSet results = mock(ResultSet.class);
        when(results.next()).thenReturn(indexPresent);
        when(statement.executeQuery()).thenReturn(results);
      }

      return statement;
    });

    return connection;
  }
}
