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

class AuditEventsContextPatternIndexMigrationStep_2_157Test
{
  private static final String TABLE_NAME = "audit_events";

  private static final String OLD_INDEX_NAME = "idx_audit_events_context";

  private static final String NEW_INDEX_NAME = "idx_audit_events_context_pattern";

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

  private final AuditEventsContextPatternIndexMigrationStep_2_157 underTest =
      new AuditEventsContextPatternIndexMigrationStep_2_157();

  @Test
  void testVersion() {
    Optional<String> version = underTest.version();
    assertThat(version.isPresent(), is(true));
    assertThat(version.get(), is("2.157"));
  }

  @Test
  void testCanExecuteInTransactionIsFalse() {
    // Required so that PostgreSQL's CREATE INDEX CONCURRENTLY / DROP INDEX CONCURRENTLY are not
    // wrapped in a transaction.
    assertThat(underTest.canExecuteInTransaction(), is(false));
  }

  @Test
  void testMigrateOnH2IsNoOp() throws Exception {
    // H2's default text ordering is codepoint-based, so the plain btree index that _2_116 creates
    // on H2 already serves LIKE 'X:%'. _2_157 should not touch H2 at all.
    try (Connection connection = h2Connection("audit_events_context_pattern_h2_noop")) {
      underTest.runStatement(connection, CREATE_TABLE);

      underTest.migrate(connection);

      assertThat("_2_157 must not create the pattern index on H2",
          underTest.indexExists(connection, TABLE_NAME, NEW_INDEX_NAME), is(false));
    }
  }

  @Test
  void testMigrateOnH2IsIdempotent() throws Exception {
    try (Connection connection = h2Connection("audit_events_context_pattern_h2_idempotent")) {
      underTest.runStatement(connection, CREATE_TABLE);

      underTest.migrate(connection);
      underTest.migrate(connection);

      assertThat("second run must also be a no-op on H2",
          underTest.indexExists(connection, TABLE_NAME, NEW_INDEX_NAME), is(false));
    }
  }

  @Test
  void testMigrateIsNoOpWhenTableMissing() throws Exception {
    try (Connection connection = h2Connection("audit_events_context_pattern_missing_table")) {
      assertThat(underTest.tableExists(connection, TABLE_NAME), is(false));

      underTest.migrate(connection);

      assertThat("no table should be created by the migration",
          underTest.tableExists(connection, TABLE_NAME), is(false));
    }
  }

  @Test
  void testMigrateOnH2ExecutesNoDdl() throws Exception {
    // Pins the H2 no-op behaviour at SQL-statement level: no CREATE INDEX, no DROP INDEX.
    List<String> executedSql = new ArrayList<>();
    Connection connection = h2MockConnection(executedSql, true);

    underTest.migrate(connection);

    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
    assertThat(executedSql, not(hasItem(containsString("DROP INDEX"))));
  }

  @Test
  void testMigrateOnPostgresqlExecutesCreatePatternIndexSql() throws Exception {
    // Pins the exact CREATE statement. Guards against accidental removal of CONCURRENTLY, the
    // text_pattern_ops opclass, or a change of index-key column.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, true);

    underTest.migrate(connection);

    assertThat(executedSql,
        hasItem(is(
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_events_context_pattern"
                + " ON audit_events (context text_pattern_ops)")));
  }

  @Test
  void testMigrateOnPostgresqlExecutesDropPlainIndexSql() throws Exception {
    // Pins the exact DROP statement.
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, true);

    underTest.migrate(connection);

    assertThat(executedSql,
        hasItem(is("DROP INDEX CONCURRENTLY IF EXISTS idx_audit_events_context")));
  }

  @Test
  void testMigrateOnPostgresqlIsNoOpWhenTableMissing() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection connection = postgresqlMockConnection(executedSql, false);

    underTest.migrate(connection);

    assertThat(executedSql, hasItem(containsString("to_regclass")));
    assertThat(executedSql, not(hasItem(containsString("CREATE INDEX"))));
    assertThat(executedSql, not(hasItem(containsString("DROP INDEX"))));
  }

  @Test
  void testMigrateThrowsOnUnsupportedDatabase() throws Exception {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("Oracle");

    assertThrows(UnsupportedOperationException.class, () -> underTest.migrate(connection));
  }

  private static Connection h2Connection(final String name) throws Exception {
    return DriverManager.getConnection("jdbc:h2:mem:" + name);
  }

  private static Connection h2MockConnection(
      final List<String> executedSql,
      final boolean tablePresent) throws SQLException
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

      return statement;
    });

    return connection;
  }

  private static Connection postgresqlMockConnection(
      final List<String> executedSql,
      final boolean tablePresent) throws SQLException
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

      return statement;
    });

    return connection;
  }
}
