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
package org.sonatype.nexus.repository.search.sql.store.upgrade.task;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.sonatype.nexus.common.failure.MultipleFailures.MultipleFailuresException;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CreateSearchTextPatternOpsIndexTask}
 */
@ExtendWith(MockitoExtension.class)
class CreateSearchTextPatternOpsIndexTaskTest
{
  private static final String CHECK_INDEX_EXISTS_SQL = "SELECT 1 FROM pg_indexes WHERE indexname = ?";

  private static final String CHECK_INDEX_INVALID_SQL =
      "SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid "
          + "WHERE c.relname = ? AND NOT i.indisvalid";

  @Mock
  private DataSessionSupplier dataSessionSupplier;

  @Mock
  private Connection connection;

  @Mock
  private DatabaseMetaData databaseMetaData;

  private final List<String> executedStatements = new ArrayList<>();

  @Test
  void testExecute_skipsOnH2() throws Exception {
    setupConnection("H2");

    assertEquals(0, createTask().execute(), "Should skip on H2");
  }

  @Test
  void testExecute_skipsWhenTableDoesNotExist() throws Exception {
    setupConnection("PostgreSQL");
    mockSqlInteractions(false, Set.of());

    assertEquals(0, createTask().execute(), "Should return 0 when table does not exist");
  }

  @Test
  void testExecute_createsAllIndexes() throws Exception {
    setupConnection("PostgreSQL");
    mockSqlInteractions(true, Set.of());

    Object result = createTask().execute();

    assertEquals(4, result, "Should process all 4 index configs");

    long createCount = executedStatements.stream()
        .filter(s -> s.startsWith("CREATE INDEX CONCURRENTLY"))
        .count();
    assertEquals(4, createCount, "Should execute 4 CREATE INDEX CONCURRENTLY statements");

    assertCreatedIndex("idx_search_components_format_tpo", "format");
    assertCreatedIndex("idx_search_components_namespace_tpo", "namespace");
    assertCreatedIndex("idx_search_components_component_name_tpo", "search_component_name");
    assertCreatedIndex("idx_search_components_version_tpo", "version");
  }

  @Test
  void testExecute_dropsOldIndexes() throws Exception {
    setupConnection("PostgreSQL");
    mockSqlInteractions(true, Set.of());

    createTask().execute();

    long dropCount = executedStatements.stream()
        .filter(s -> s.startsWith("DROP INDEX IF EXISTS"))
        .count();
    assertEquals(4, dropCount, "Should drop 4 old indexes");

    assertDroppedIndex("idx_search_components_format");
    assertDroppedIndex("idx_search_components_namespace");
    assertDroppedIndex("idx_search_components_component_name");
    assertDroppedIndex("idx_search_components_version");
  }

  @Test
  void testExecute_skipsExistingIndexes() throws Exception {
    setupConnection("PostgreSQL");
    mockSqlInteractions(true, Set.of(
        "idx_search_components_format_tpo",
        "idx_search_components_namespace_tpo",
        "idx_search_components_component_name_tpo",
        "idx_search_components_version_tpo"));

    Object result = createTask().execute();

    assertEquals(4, result, "Should still count as 4 successes");

    long createCount = executedStatements.stream()
        .filter(s -> s.startsWith("CREATE INDEX CONCURRENTLY"))
        .count();
    assertEquals(0, createCount, "Should not execute any CREATE INDEX when all indexes exist");
  }

  @Test
  void testExecute_createsOnlyMissingIndexes() throws Exception {
    setupConnection("PostgreSQL");
    mockSqlInteractions(true, Set.of("idx_search_components_format_tpo"));

    createTask().execute();

    long createCount = executedStatements.stream()
        .filter(s -> s.startsWith("CREATE INDEX CONCURRENTLY"))
        .count();
    assertEquals(3, createCount, "Should create only 3 indexes (format_tpo already exists)");

    assertTrue(
        executedStatements.stream()
            .noneMatch(s -> s.contains("idx_search_components_format_tpo") && s.startsWith("CREATE INDEX")),
        "Should not create format_tpo index (already exists)");
  }

  @Test
  void testExecute_continuesOnFailureAndThrows() throws Exception {
    setupConnection("PostgreSQL");
    mockSqlInteractionsWithFailure(true, Set.of(), "idx_search_components_format_tpo");

    assertThrows(MultipleFailuresException.class, () -> createTask().execute());

    long createCount = executedStatements.stream()
        .filter(s -> s.startsWith("CREATE INDEX CONCURRENTLY"))
        .count();
    assertEquals(4, createCount, "Should attempt CREATE INDEX for all 4 columns");

    long dropOldCount = executedStatements.stream()
        .filter(s -> s.startsWith("DROP INDEX IF EXISTS idx_search_components_"))
        .count();
    assertEquals(3, dropOldCount, "Should drop only 3 old indexes (format skipped due to failure)");
  }

  private CreateSearchTextPatternOpsIndexTask createTask() throws SQLException {
    when(dataSessionSupplier.openConnection("nexus")).thenReturn(connection);
    return new CreateSearchTextPatternOpsIndexTask(dataSessionSupplier);
  }

  private void setupConnection(final String databaseProductName) throws SQLException {
    when(connection.getMetaData()).thenReturn(databaseMetaData);
    when(databaseMetaData.getDatabaseProductName()).thenReturn(databaseProductName);
  }

  private void mockSqlInteractions(
      final boolean tableExists,
      final Set<String> existingIndexes) throws SQLException
  {
    mockSqlInteractionsWithFailure(tableExists, existingIndexes, null);
  }

  private void mockSqlInteractionsWithFailure(
      final boolean tableExists,
      final Set<String> existingIndexes,
      final String failOnIndex) throws SQLException
  {
    executedStatements.clear();
    when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
      String sql = invocation.getArgument(0);
      executedStatements.add(sql);
      PreparedStatement stmt = mock(PreparedStatement.class);

      if (sql.contains("to_regclass")) {
        mockTableExistsStatement(stmt, tableExists);
      }
      else if (sql.equals(CHECK_INDEX_EXISTS_SQL)) {
        mockIndexExistsStatement(stmt, existingIndexes);
      }
      else if (sql.equals(CHECK_INDEX_INVALID_SQL)) {
        mockAlwaysFalseQuery(stmt);
      }
      else if (sql.startsWith("CREATE INDEX CONCURRENTLY") && failOnIndex != null
          && sql.contains(failOnIndex)) {
        when(stmt.executeUpdate()).thenThrow(new SQLException("Simulated failure for " + failOnIndex));
      }
      else {
        when(stmt.executeUpdate()).thenReturn(0);
      }

      return stmt;
    });
  }

  private void mockTableExistsStatement(
      final PreparedStatement stmt,
      final boolean exists) throws SQLException
  {
    doAnswer(inv -> null).when(stmt).setString(anyInt(), anyString());
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(exists);
    if (exists) {
      when(rs.getObject(1)).thenReturn("search_components");
    }
    when(stmt.executeQuery()).thenReturn(rs);
  }

  private void mockIndexExistsStatement(
      final PreparedStatement stmt,
      final Set<String> existingIndexes) throws SQLException
  {
    AtomicReference<String> paramValue = new AtomicReference<>();
    doAnswer(inv -> {
      paramValue.set(inv.getArgument(1));
      return null;
    }).when(stmt).setString(anyInt(), anyString());
    when(stmt.executeQuery()).thenAnswer(inv -> {
      ResultSet rs = mock(ResultSet.class);
      when(rs.next()).thenReturn(existingIndexes.contains(paramValue.get()));
      return rs;
    });
  }

  private void assertCreatedIndex(final String indexName, final String column) {
    assertTrue(
        executedStatements.stream()
            .anyMatch(s -> s.contains(indexName) && s.contains(column + " text_pattern_ops")),
        "Should create index " + indexName);
  }

  private void assertDroppedIndex(final String indexName) {
    assertTrue(
        executedStatements.stream().anyMatch(s -> s.equals("DROP INDEX IF EXISTS " + indexName)),
        "Should drop index " + indexName);
  }

  private void mockAlwaysFalseQuery(final PreparedStatement stmt) throws SQLException {
    doAnswer(inv -> null).when(stmt).setString(anyInt(), anyString());
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(false);
    when(stmt.executeQuery()).thenReturn(rs);
  }
}
