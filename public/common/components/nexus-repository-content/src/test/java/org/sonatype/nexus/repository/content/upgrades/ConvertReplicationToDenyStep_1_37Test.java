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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ConvertReplicationToDenyStep_1_37Test
{
  private static final String CREATE_REPOSITORY_TABLE =
      "CREATE TABLE IF NOT EXISTS repository ("
          + "id VARCHAR(200) NOT NULL,"
          + "attributes VARBINARY(100000) NOT NULL,"
          + "CONSTRAINT pk_repository PRIMARY KEY (id)"
          + ")";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  @Mock
  private DatabaseCheck databaseCheck;

  private final ObjectMapper mapper = new ObjectMapper();

  private ConvertReplicationToDenyStep_1_37 underTest;

  @Before
  public void setUp() {
    // H2 is not PostgreSQL
    when(databaseCheck.isPostgresql()).thenReturn(false);
    underTest = new ConvertReplicationToDenyStep_1_37(databaseCheck);
  }

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();
    assertTrue(version.isPresent());
    assertEquals("1.37", version.get());
  }

  @Test
  public void testMigrate_convertsReplicationOnlyToDeny() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-1",
          "{\"storage\":{\"writePolicy\":\"REPLICATION_ONLY\",\"blobStoreName\":\"default\"}}");

      underTest.migrate(conn);

      String attributes = getAttributes(conn, "repo-1");
      assertTrue("writePolicy should be DENY", attributes.contains("\"writePolicy\":\"DENY\""));
      assertFalse("REPLICATION_ONLY should not exist", attributes.contains("REPLICATION_ONLY"));
    }
  }

  @Test
  public void testMigrate_doesNotConvertOtherPolicies() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-allow",
          "{\"storage\":{\"writePolicy\":\"ALLOW\",\"blobStoreName\":\"default\"}}");
      insertRepository(conn, "repo-deny",
          "{\"storage\":{\"writePolicy\":\"DENY\",\"blobStoreName\":\"default\"}}");
      insertRepository(conn, "repo-allow-once",
          "{\"storage\":{\"writePolicy\":\"ALLOW_ONCE\",\"blobStoreName\":\"default\"}}");

      underTest.migrate(conn);

      assertTrue(getAttributes(conn, "repo-allow").contains("\"writePolicy\":\"ALLOW\""));
      assertTrue(getAttributes(conn, "repo-deny").contains("\"writePolicy\":\"DENY\""));
      assertTrue(getAttributes(conn, "repo-allow-once").contains("\"writePolicy\":\"ALLOW_ONCE\""));
    }
  }

  @Test
  public void testMigrate_handlesMultipleReplicationOnlyRepos() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-1",
          "{\"storage\":{\"writePolicy\":\"REPLICATION_ONLY\"}}");
      insertRepository(conn, "repo-2",
          "{\"storage\":{\"writePolicy\":\"REPLICATION_ONLY\"}}");
      insertRepository(conn, "repo-3",
          "{\"storage\":{\"writePolicy\":\"ALLOW\"}}");

      underTest.migrate(conn);

      assertTrue(getAttributes(conn, "repo-1").contains("\"writePolicy\":\"DENY\""));
      assertTrue(getAttributes(conn, "repo-2").contains("\"writePolicy\":\"DENY\""));
      assertTrue(getAttributes(conn, "repo-3").contains("\"writePolicy\":\"ALLOW\""));
    }
  }

  @Test
  public void testMigrate_handlesNoStorageAttributes() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-no-storage", "{\"other\":{\"key\":\"value\"}}");

      underTest.migrate(conn);

      // Should not fail, and attributes remain unchanged
      String attributes = getAttributes(conn, "repo-no-storage");
      assertTrue(attributes.contains("\"other\""));
    }
  }

  @Test
  public void testMigrate_handlesNoWritePolicyInStorage() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-no-policy",
          "{\"storage\":{\"blobStoreName\":\"default\"}}");

      underTest.migrate(conn);

      String attributes = getAttributes(conn, "repo-no-policy");
      assertTrue(attributes.contains("\"blobStoreName\":\"default\""));
      assertFalse(attributes.contains("writePolicy"));
    }
  }

  @Test
  public void testMigrate_handlesEmptyTable() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);

      // Should not fail with empty table
      underTest.migrate(conn);
    }
  }

  @Test
  public void testMigrate_preservesOtherAttributes() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-1",
          "{\"storage\":{\"writePolicy\":\"REPLICATION_ONLY\",\"blobStoreName\":\"default\"},"
              + "\"cleanup\":{\"policyName\":[\"weekly-cleanup\"]}}");

      underTest.migrate(conn);

      String attributes = getAttributes(conn, "repo-1");
      assertTrue("writePolicy should be DENY", attributes.contains("\"writePolicy\":\"DENY\""));
      assertTrue("blobStoreName should be preserved", attributes.contains("\"blobStoreName\":\"default\""));
      assertTrue("cleanup should be preserved", attributes.contains("\"policyName\""));
    }
  }

  @Test
  public void testMigrate_idempotent() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-1",
          "{\"storage\":{\"writePolicy\":\"REPLICATION_ONLY\"}}");

      underTest.migrate(conn);
      // Run again - should not fail
      underTest.migrate(conn);

      assertTrue(getAttributes(conn, "repo-1").contains("\"writePolicy\":\"DENY\""));
    }
  }

  @Test
  public void testMigrate_withPostgresqlMode() throws Exception {
    when(databaseCheck.isPostgresql()).thenReturn(true);
    underTest = new ConvertReplicationToDenyStep_1_37(databaseCheck);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTable(conn);
      insertRepository(conn, "repo-1",
          "{\"storage\":{\"writePolicy\":\"REPLICATION_ONLY\"}}");

      underTest.migrate(conn);

      String attributes = getAttributes(conn, "repo-1");
      assertTrue("writePolicy should be DENY", attributes.contains("\"writePolicy\":\"DENY\""));
    }
  }

  private void createTable(final Connection conn) throws Exception {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(CREATE_REPOSITORY_TABLE);
    }
  }

  private void insertRepository(final Connection conn, final String id, final String attributes) throws Exception {
    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO repository (id, attributes) VALUES (?, ?)")) {
      ps.setString(1, id);
      ps.setBytes(2, attributes.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      ps.executeUpdate();
    }
  }

  private String getAttributes(final Connection conn, final String id) throws Exception {
    try (PreparedStatement ps = conn.prepareStatement("SELECT attributes FROM repository WHERE id = ?")) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue("Row should exist for id: " + id, rs.next());
        byte[] bytes = rs.getBytes("attributes");
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
      }
    }
  }
}
