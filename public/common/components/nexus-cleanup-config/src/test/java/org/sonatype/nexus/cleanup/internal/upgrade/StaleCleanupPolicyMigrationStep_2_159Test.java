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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaleCleanupPolicyMigrationStep_2_159Test
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String CREATE_REPOSITORY_TABLE = """
      CREATE TABLE IF NOT EXISTS repository (
        id           UUID         NOT NULL,
        name         VARCHAR(200) NOT NULL,
        recipe_name  VARCHAR(200) NOT NULL,
        online       BOOLEAN      NOT NULL DEFAULT TRUE,
        attributes   JSON         NULL,
        CONSTRAINT pk_repository_id PRIMARY KEY (id)
      )
      """;

  private static final String CREATE_CLEANUP_POLICY_TABLE = """
      CREATE TABLE IF NOT EXISTS cleanup_policy (
        name     VARCHAR(200) NOT NULL,
        notes    VARCHAR(400) NULL,
        format   VARCHAR(100) NOT NULL,
        mode     VARCHAR(100) NOT NULL,
        criteria VARCHAR(4000) NOT NULL,
        CONSTRAINT pk_cleanup_policy_name PRIMARY KEY (name)
      )
      """;

  private final StaleCleanupPolicyMigrationStep_2_159 underTest = new StaleCleanupPolicyMigrationStep_2_159();

  // ── version ────────────────────────────────────────────────────────────────

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();
    assertThat(version.isPresent(), is(true));
    assertThat(version.get(), is("2.159"));
  }

  // ── real H2 behavioural tests ───────────────────────────────────────────────

  @Test
  public void testMigrate_noCleanupBlock_unchanged() throws Exception {
    try (Connection conn = h2Connection("no_cleanup_block")) {
      createTables(conn);
      UUID id = randomId();
      insertRepository(conn, id, "no-cleanup-repo", "{\"proxy\":{\"remoteUrl\":\"https://example.com\"}}");

      underTest.migrate(conn);

      assertThat(readAttributesJson(conn, id).has("cleanup"), is(false));
    }
  }

  @Test
  public void testMigrate_validPolicy_unchanged() throws Exception {
    try (Connection conn = h2Connection("valid_policy")) {
      createTables(conn);
      insertCleanupPolicy(conn, "real-policy");

      UUID id = randomId();
      insertRepository(conn, id, "valid-repo",
          "{\"cleanup\":{\"policyName\":[\"real-policy\"]}}");

      underTest.migrate(conn);

      JsonNode cleanup = readAttributesJson(conn, id).get("cleanup");
      assertThat(cleanup.get("policyName").get(0).asText(), is("real-policy"));
    }
  }

  @Test
  public void testMigrate_swaggerSentinel_cleanupBlockRemoved() throws Exception {
    try (Connection conn = h2Connection("swagger_sentinel")) {
      createTables(conn);
      // no cleanup_policy rows — "string" will not match anything
      UUID id = randomId();
      insertRepository(conn, id, "swagger-repo",
          "{\"cleanup\":{\"policyName\":[\"string\"]}}");

      underTest.migrate(conn);

      assertThat("cleanup block should be removed when all entries are stale",
          readAttributesJson(conn, id).has("cleanup"), is(false));
    }
  }

  @Test
  public void testMigrate_noneSentinel_cleanupBlockRemoved() throws Exception {
    try (Connection conn = h2Connection("none_sentinel")) {
      createTables(conn);
      UUID id = randomId();
      insertRepository(conn, id, "legacy-repo",
          "{\"cleanup\":{\"policyName\":[\"None\"]}}");

      underTest.migrate(conn);

      assertThat("cleanup block should be removed for legacy None sentinel",
          readAttributesJson(conn, id).has("cleanup"), is(false));
    }
  }

  @Test
  public void testMigrate_deletedPolicy_cleanupBlockRemoved() throws Exception {
    try (Connection conn = h2Connection("deleted_policy")) {
      createTables(conn);
      // policy was deleted — no matching row in cleanup_policy
      UUID id = randomId();
      insertRepository(conn, id, "orphaned-repo",
          "{\"cleanup\":{\"policyName\":[\"deleted-policy\"]}}");

      underTest.migrate(conn);

      assertThat("cleanup block should be removed for deleted policy reference",
          readAttributesJson(conn, id).has("cleanup"), is(false));
    }
  }

  @Test
  public void testMigrate_mixedPolicies_onlyStaleRemoved() throws Exception {
    try (Connection conn = h2Connection("mixed_policies")) {
      createTables(conn);
      insertCleanupPolicy(conn, "real-policy");

      UUID id = randomId();
      insertRepository(conn, id, "mixed-repo",
          "{\"cleanup\":{\"policyName\":[\"real-policy\",\"string\"]}}");

      underTest.migrate(conn);

      JsonNode policyNames = readAttributesJson(conn, id).get("cleanup").get("policyName");
      assertThat("only the valid policy should remain", policyNames.size(), is(1));
      assertThat(policyNames.get(0).asText(), is("real-policy"));
    }
  }

  @Test
  public void testMigrate_multipleStale_allRemoved() throws Exception {
    try (Connection conn = h2Connection("multiple_stale")) {
      createTables(conn);
      UUID id = randomId();
      insertRepository(conn, id, "multi-stale-repo",
          "{\"cleanup\":{\"policyName\":[\"string\",\"None\",\"deleted-policy\"]}}");

      underTest.migrate(conn);

      assertThat("cleanup block should be removed when all entries are stale",
          readAttributesJson(conn, id).has("cleanup"), is(false));
    }
  }

  @Test
  public void testMigrate_multipleValidPoliciesAllRetained() throws Exception {
    try (Connection conn = h2Connection("multiple_valid")) {
      createTables(conn);
      insertCleanupPolicy(conn, "policy-a");
      insertCleanupPolicy(conn, "policy-b");

      UUID id = randomId();
      insertRepository(conn, id, "multi-valid-repo",
          "{\"cleanup\":{\"policyName\":[\"policy-a\",\"policy-b\"]}}");

      underTest.migrate(conn);

      JsonNode policyNames = readAttributesJson(conn, id).get("cleanup").get("policyName");
      assertThat("both valid policies should remain", policyNames.size(), is(2));
    }
  }

  @Test
  public void testMigrate_isIdempotent() throws Exception {
    try (Connection conn = h2Connection("idempotent")) {
      createTables(conn);
      UUID id = randomId();
      insertRepository(conn, id, "stale-repo",
          "{\"cleanup\":{\"policyName\":[\"string\"]}}");

      underTest.migrate(conn);
      underTest.migrate(conn); // second run must not throw

      assertThat("cleanup block should still be absent after second run",
          readAttributesJson(conn, id).has("cleanup"), is(false));
    }
  }

  @Test
  public void testMigrate_missingRepositoryTable_isNoOp() throws Exception {
    try (Connection conn = h2Connection("missing_repo_table")) {
      // only cleanup_policy table — no repository table
      underTest.runStatement(conn, CREATE_CLEANUP_POLICY_TABLE);

      underTest.migrate(conn); // must not throw

      assertThat(underTest.tableExists(conn, "cleanup_policy"), is(true));
      assertThat(underTest.tableExists(conn, "repository"), is(false));
    }
  }

  @Test
  public void testMigrate_missingCleanupPolicyTable_isNoOp() throws Exception {
    try (Connection conn = h2Connection("missing_cleanup_table")) {
      // only repository table — no cleanup_policy table
      underTest.runStatement(conn, CREATE_REPOSITORY_TABLE);

      underTest.migrate(conn); // must not throw

      assertThat(underTest.tableExists(conn, "repository"), is(true));
      assertThat(underTest.tableExists(conn, "cleanup_policy"), is(false));
    }
  }

  @Test
  public void testMigrate_noTablesPresent_isNoOp() throws Exception {
    try (Connection conn = h2Connection("no_tables")) {
      underTest.migrate(conn); // must not throw
    }
  }

  @Test
  public void testMigrate_nonArrayPolicyName_isNoOp() throws Exception {
    try (Connection conn = h2Connection("non_array_policy_name")) {
      createTables(conn);
      UUID id = randomId();
      // bare string instead of array — may occur in OrientDB-migrated databases
      insertRepository(conn, id, "non-array-repo",
          "{\"cleanup\":{\"policyName\":\"None\"}}");

      underTest.migrate(conn);

      // non-array shape must be left unchanged (no cleanup block removal)
      JsonNode cleanup = readAttributesJson(conn, id).get("cleanup");
      assertThat("non-array policyName should be left unchanged", cleanup != null, is(true));
      assertThat(cleanup.get("policyName").asText(), is("None"));
    }
  }

  @Test
  public void testMigrate_emptyPolicyNameArray_isNoOp() throws Exception {
    try (Connection conn = h2Connection("empty_policy_name_array")) {
      createTables(conn);
      UUID id = randomId();
      insertRepository(conn, id, "empty-array-repo",
          "{\"cleanup\":{\"policyName\":[]}}");

      underTest.migrate(conn);

      JsonNode cleanup = readAttributesJson(conn, id).get("cleanup");
      assertThat("empty policyName array should be left unchanged", cleanup != null, is(true));
      assertThat(cleanup.get("policyName").size(), is(0));
    }
  }

  @Test
  public void testMigrate_nonTextualEntry_treatedAsStale() throws Exception {
    // Non-string entries (null, number, object) in policyName are treated as stale and removed.
    // The guard prevents asText() silently coercing e.g. null → "null" into a policy lookup.
    try (Connection conn = h2Connection("non_textual_entry")) {
      createTables(conn);
      insertCleanupPolicy(conn, "real-policy");

      UUID id = randomId();
      // policyName contains a valid string and a JSON null — null should be dropped as stale
      insertRepository(conn, id, "non-textual-repo",
          "{\"cleanup\":{\"policyName\":[\"real-policy\",null]}}");

      underTest.migrate(conn);

      JsonNode policyNames = readAttributesJson(conn, id).get("cleanup").get("policyName");
      assertThat("valid policy should be retained", policyNames.size(), is(1));
      assertThat(policyNames.get(0).asText(), is("real-policy"));
    }
  }

  // ── SQL-capture mock tests ─────────────────────────────────────────────────

  @Test
  public void testMigrate_h2_executesSelectFromBothTables() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection conn = h2MockConnection(executedSql, /* tablesPresent= */true,
        /* validPolicies= */List.of(), /* repoRows= */List.of());

    underTest.migrate(conn);

    assertThat("must query cleanup_policy for valid names",
        executedSql, hasItem(containsString("cleanup_policy")));
    assertThat("must query repository table",
        executedSql, hasItem(containsString("repository")));
  }

  @Test
  public void testMigrate_postgresql_executesSelectFromBothTables() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection conn = postgresqlMockConnection(executedSql, /* tablesPresent= */true,
        /* validPolicies= */List.of(), /* repoRows= */List.of());

    underTest.migrate(conn);

    assertThat("must query cleanup_policy for valid names",
        executedSql, hasItem(containsString("cleanup_policy")));
    assertThat("must query repository table",
        executedSql, hasItem(containsString("repository")));
  }

  @Test
  public void testMigrate_h2_tablesMissing_onlyChecksExistence() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection conn = h2MockConnection(executedSql, /* tablesPresent= */false,
        List.of(), List.of());

    underTest.migrate(conn);

    assertThat("should not query repository when tables are absent",
        executedSql, not(hasItem(containsString("SELECT id, name, attributes FROM repository"))));
    assertThat("should not query cleanup_policy when tables are absent",
        executedSql, not(hasItem(is("SELECT name FROM cleanup_policy"))));
  }

  @Test
  public void testMigrate_postgresql_tablesMissing_onlyChecksExistence() throws Exception {
    List<String> executedSql = new ArrayList<>();
    Connection conn = postgresqlMockConnection(executedSql, /* tablesPresent= */false,
        List.of(), List.of());

    underTest.migrate(conn);

    assertThat("should not query data when tables are absent",
        executedSql, not(hasItem(containsString("SELECT id, name, attributes FROM repository"))));
  }

  @Test
  public void testMigrate_postgresql_stalePolicy_updatesViaStringPath() throws Exception {
    // The PostgreSQL path reads attributes via rs.getString() (not rs.getBytes() like H2).
    // This confirms the stale-removal logic runs through the string-read branch.
    List<String> executedSql = new ArrayList<>();
    String repoId = UUID.randomUUID().toString();
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[]{repoId, "legacy-repo", "{\"cleanup\":{\"policyName\":[\"None\"]}}"});

    Connection conn = postgresqlMockConnection(executedSql, /* tablesPresent= */true,
        /* validPolicies= */List.of(), rows);

    underTest.migrate(conn);

    assertThat("PostgreSQL string-read path should trigger UPDATE for stale cleanup ref",
        executedSql, hasItem(containsString("UPDATE repository SET attributes")));
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private static Connection h2Connection(final String name) throws Exception {
    return DriverManager.getConnection("jdbc:h2:mem:" + name);
  }

  private void createTables(final Connection conn) throws SQLException {
    underTest.runStatement(conn, CREATE_REPOSITORY_TABLE);
    underTest.runStatement(conn, CREATE_CLEANUP_POLICY_TABLE);
  }

  private static void insertCleanupPolicy(final Connection conn, final String name) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO cleanup_policy (name, notes, format, mode, criteria) VALUES (?, '', 'all', 'deletion', '{}')")) {
      ps.setString(1, name);
      ps.executeUpdate();
    }
  }

  private static void insertRepository(
      final Connection conn,
      final UUID id,
      final String name,
      final String attributesJson) throws SQLException
  {
    try (PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO repository (id, name, recipe_name, online, attributes) VALUES (?, ?, 'maven2-proxy', true, ?)")) {
      ps.setObject(1, id);
      ps.setString(2, name);
      // Use setBytes to match the production write path (setJsonParameter on H2 calls setBytes).
      ps.setBytes(3, attributesJson.getBytes(StandardCharsets.UTF_8));
      ps.executeUpdate();
    }
  }

  private static JsonNode readAttributesJson(final Connection conn, final UUID id) throws Exception {
    try (PreparedStatement ps = conn.prepareStatement(
        "SELECT attributes FROM repository WHERE id = ?")) {
      ps.setObject(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat("repository row should exist", rs.next(), is(true));
        // Use getBytes to match H2's storage format (bytes, not wrapped JSON string).
        byte[] bytes = rs.getBytes("attributes");
        return MAPPER.readTree(bytes);
      }
    }
  }

  private static UUID randomId() {
    return UUID.randomUUID();
  }

  /**
   * Mock H2 connection that records executed SQL so SQL-branch tests can assert on exact statements
   * without requiring a real database. The mock returns configurable valid policy names and
   * repository rows.
   */
  private static Connection h2MockConnection(
      final List<String> executedSql,
      final boolean tablesPresent,
      final List<String> validPolicies,
      final List<String[]> repoRows) throws SQLException
  {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);
    when(meta.getDatabaseProductName()).thenReturn("H2");

    when(conn.prepareStatement(anyString())).thenAnswer(invocation -> {
      String sql = invocation.getArgument(0);
      executedSql.add(sql);
      PreparedStatement ps = mock(PreparedStatement.class);

      if (sql.contains("INFORMATION_SCHEMA.TABLES")) {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(tablesPresent);
        when(ps.executeQuery()).thenReturn(rs);
      }
      else if (sql.equals("SELECT name FROM cleanup_policy")) {
        ResultSet rs = buildStringResultSet(validPolicies);
        when(ps.executeQuery()).thenReturn(rs);
      }
      else if (sql.contains("SELECT id, name, attributes FROM repository")) {
        ResultSet rs = buildRepoResultSet(repoRows);
        when(ps.executeQuery()).thenReturn(rs);
      }

      return ps;
    });

    return conn;
  }

  /**
   * Mock PostgreSQL connection that records executed SQL for PostgreSQL-branch assertions.
   */
  private static Connection postgresqlMockConnection(
      final List<String> executedSql,
      final boolean tablesPresent,
      final List<String> validPolicies,
      final List<String[]> repoRows) throws SQLException
  {
    Connection conn = mock(Connection.class);
    DatabaseMetaData meta = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(meta);
    when(meta.getDatabaseProductName()).thenReturn("PostgreSQL");

    when(conn.prepareStatement(anyString())).thenAnswer(invocation -> {
      String sql = invocation.getArgument(0);
      executedSql.add(sql);
      PreparedStatement ps = mock(PreparedStatement.class);

      if (sql.contains("to_regclass")) {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(tablesPresent);
        when(rs.getObject(1)).thenReturn(tablesPresent ? "repository" : null);
        when(ps.executeQuery()).thenReturn(rs);
      }
      else if (sql.equals("SELECT name FROM cleanup_policy")) {
        ResultSet rs = buildStringResultSet(validPolicies);
        when(ps.executeQuery()).thenReturn(rs);
      }
      else if (sql.contains("SELECT id, name, attributes FROM repository")) {
        ResultSet rs = buildRepoResultSet(repoRows);
        when(ps.executeQuery()).thenReturn(rs);
      }

      return ps;
    });

    return conn;
  }

  private static ResultSet buildStringResultSet(final List<String> values) throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    int[] idx = {-1};
    when(rs.next()).thenAnswer(inv -> {
      idx[0]++;
      return idx[0] < values.size();
    });
    when(rs.getString("name")).thenAnswer(inv -> values.get(idx[0]));
    return rs;
  }

  // repoRows: each element is String[]{id, name, attributesJson}
  private static ResultSet buildRepoResultSet(final List<String[]> rows) throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    int[] idx = {-1};
    when(rs.next()).thenAnswer(inv -> {
      idx[0]++;
      return idx[0] < rows.size();
    });
    when(rs.getString("id")).thenAnswer(inv -> rows.get(idx[0])[0]);
    when(rs.getString("name")).thenAnswer(inv -> rows.get(idx[0])[1]);
    when(rs.getString("attributes")).thenAnswer(inv -> rows.get(idx[0])[2]);
    return rs;
  }
}
