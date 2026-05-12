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
import java.sql.SQLException;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.testdb.DataSessionRule;

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
public class AssetBlobMigrationStep_2_18_1Test
{
  private static final String TEST_FORMAT = "test";

  private static final String INDEX_NAME = "idx_test_asset_blob_blob_created_asset_id";

  private static final String ASSET_BLOB_TABLE = "test_asset_blob";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  @Mock
  private Format testFormat;

  private AssetBlobMigrationStep_2_18_1 underTest;

  @Before
  public void setup() {
    when(testFormat.getValue()).thenReturn(TEST_FORMAT);
    underTest = new AssetBlobMigrationStep_2_18_1();
  }

  @Test
  public void testMigration_isNoOp() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create asset_blob table first
      createAssetBlobTable(conn);

      // Verify table exists
      assertTrue("asset_blob table should exist", underTest.tableExists(conn, ASSET_BLOB_TABLE));

      // Verify index does not exist before migration
      assertFalse("index should not exist before migration",
          underTest.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));

      // Run migration (should be a no-op)
      underTest.migrate(conn);

      // Verify index was NOT created (since this is now a no-op)
      assertFalse("index should NOT exist after no-op migration",
          underTest.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testMigration_idempotent() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create asset_blob table first
      createAssetBlobTable(conn);

      // Run migration twice (should be a no-op both times)
      underTest.migrate(conn);
      underTest.migrate(conn);

      // Verify no error occurred
      assertFalse("index should NOT exist after double no-op migration",
          underTest.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testMigration_handlesMultipleFormats() throws Exception {
    Format format2 = org.mockito.Mockito.mock(Format.class);
    when(format2.getValue()).thenReturn("maven");

    AssetBlobMigrationStep_2_18_1 multiFormatMigration = new AssetBlobMigrationStep_2_18_1();

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create both tables
      createAssetBlobTable(conn);
      createMavenAssetBlobTable(conn);

      // Run migration (should be a no-op)
      multiFormatMigration.migrate(conn);

      // Verify no indexes were created (since this is now a no-op)
      assertFalse("test index should NOT exist after no-op migration",
          multiFormatMigration.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
      assertFalse("maven index should NOT exist after no-op migration",
          multiFormatMigration.indexExists(conn, "maven_asset_blob", "idx_maven_asset_blob_blob_created_asset_id"));
    }
  }

  @Test
  public void testMigration_whenTableDoesNotExist() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Verify table doesn't exist
      assertFalse("table should not exist", underTest.tableExists(conn, ASSET_BLOB_TABLE));

      // Run migration (should be a no-op) - should not throw exception
      underTest.migrate(conn);

      // Verify index was NOT created (since this is a no-op)
      assertFalse("index should not exist after no-op migration",
          underTest.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testVersion() {
    assertTrue("version should be present", underTest.version().isPresent());
    assertEquals("version should be 2.18.1", "2.18.1", underTest.version().get());
  }

  @Test
  public void testMigration_withH2Database() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create asset_blob table
      createAssetBlobTable(conn);

      // H2 is the default for tests, so this tests the H2 path
      assertTrue("should be H2 database", underTest.isH2(conn));

      // Run migration (should be a no-op)
      underTest.migrate(conn);

      // Verify index was NOT created (since this is now a no-op)
      assertFalse("index should NOT exist after no-op H2 migration",
          underTest.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  private void createAssetBlobTable(Connection conn) throws SQLException {
    String createTable = "CREATE TABLE IF NOT EXISTS " + ASSET_BLOB_TABLE + " ("
        + "asset_blob_id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
        + "blob_ref VARCHAR NOT NULL,"
        + "blob_size BIGINT NOT NULL,"
        + "blob_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + "created_by VARCHAR,"
        + "created_by_ip VARCHAR"
        + ")";
    underTest.runStatement(conn, createTable);
  }

  private void createMavenAssetBlobTable(Connection conn) throws SQLException {
    String createTable = "CREATE TABLE IF NOT EXISTS maven_asset_blob ("
        + "asset_blob_id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
        + "blob_ref VARCHAR NOT NULL,"
        + "blob_size BIGINT NOT NULL,"
        + "blob_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + "created_by VARCHAR,"
        + "created_by_ip VARCHAR"
        + ")";
    underTest.runStatement(conn, createTable);
  }
}
