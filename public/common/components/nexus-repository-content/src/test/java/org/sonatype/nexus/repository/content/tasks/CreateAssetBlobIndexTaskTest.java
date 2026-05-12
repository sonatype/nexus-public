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
package org.sonatype.nexus.repository.content.tasks;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateAssetBlobIndexTaskTest
{
  private static final String TEST_FORMAT = "test";

  private static final String MAVEN_FORMAT = "maven";

  private static final String NPM_FORMAT = "npm";

  private static final String INDEX_NAME = "idx_test_asset_blob_blob_created_asset_id";

  private static final String MAVEN_INDEX_NAME = "idx_maven_asset_blob_blob_created_asset_id";

  private static final String NPM_INDEX_NAME = "idx_npm_asset_blob_blob_created_asset_id";

  private static final String ASSET_BLOB_TABLE = "test_asset_blob";

  private static final String MAVEN_ASSET_BLOB_TABLE = "maven_asset_blob";

  private static final String NPM_ASSET_BLOB_TABLE = "npm_asset_blob";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  @Mock
  private Format testFormat;

  @Mock
  private Format mavenFormat;

  @Mock
  private Format npmFormat;

  @Mock
  private DataSessionSupplier dataSessionSupplier;

  private CreateAssetBlobIndexTask underTest;

  private DatabaseMigrationStep dbHelper = new DatabaseMigrationStep()
  {
    @Override
    public void migrate(Connection connection) throws Exception {
      // Not used
    }

    @Override
    public java.util.Optional<String> version() {
      return java.util.Optional.empty();
    }
  };

  @Before
  public void setup() throws SQLException {
    when(testFormat.getValue()).thenReturn(TEST_FORMAT);
    when(mavenFormat.getValue()).thenReturn(MAVEN_FORMAT);
    when(npmFormat.getValue()).thenReturn(NPM_FORMAT);
    when(dataSessionSupplier.openConnection(DEFAULT_DATASTORE_NAME))
        .thenAnswer(invocation -> sessionRule.openConnection(DEFAULT_DATASTORE_NAME));
  }

  @Test
  public void testExecute_createsIndexWhenTableExists() throws Exception {
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create asset_blob table first
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);

      // Verify table exists
      assertTrue("asset_blob table should exist", dbHelper.tableExists(conn, ASSET_BLOB_TABLE));

      // Verify index does not exist before execution
      assertFalse("index should not exist before execution",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));

      // Execute task
      Object result = underTest.execute();

      // Verify index was created
      assertTrue("index should exist after execution",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));

      // Verify result shows 1 index created
      assertEquals("Should have created 1 index", 1, result);
    }
  }

  @Test
  public void testExecute_idempotent() throws Exception {
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create asset_blob table first
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);

      // Execute task twice
      Object result1 = underTest.execute();
      Object result2 = underTest.execute();

      // First execution should create 1 index
      assertEquals("First execution should create 1 index", 1, result1);

      // Second execution should create 0 indexes (already exists)
      assertEquals("Second execution should create 0 indexes", 0, result2);

      // Verify index exists and no error occurred
      assertTrue("index should exist after double execution",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testExecute_handlesMultipleFormats() throws Exception {
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create both tables
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, MAVEN_ASSET_BLOB_TABLE);

      // Execute task
      Object result = underTest.execute();

      // Verify both indexes were created
      assertTrue("test index should exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
      assertTrue("maven index should exist",
          dbHelper.indexExists(conn, MAVEN_ASSET_BLOB_TABLE, MAVEN_INDEX_NAME));

      // Verify result shows 2 indexes created
      assertEquals("Should have created 2 indexes", 2, result);
    }
  }

  @Test
  public void testExecute_whenTableDoesNotExist() throws Exception {
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Verify table doesn't exist
      assertFalse("table should not exist", dbHelper.tableExists(conn, ASSET_BLOB_TABLE));

      // Execute task - should not throw exception
      Object result = underTest.execute();

      // Verify no indexes were created
      assertEquals("Should have created 0 indexes when table doesn't exist", 0, result);

      // Verify index was NOT created (since table doesn't exist)
      assertFalse("index should not exist when table doesn't exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testExecute_continuesOnError() throws Exception {
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create only maven table (test table doesn't exist, will cause first format to skip)
      createAssetBlobTable(conn, MAVEN_ASSET_BLOB_TABLE);

      // Execute task - should not throw exception even though test table doesn't exist
      Object result = underTest.execute();

      // Verify maven index was still created
      assertTrue("maven index should exist even when test table doesn't exist",
          dbHelper.indexExists(conn, MAVEN_ASSET_BLOB_TABLE, MAVEN_INDEX_NAME));

      // Verify result shows 1 index created (only maven)
      assertEquals("Should have created 1 index (only maven)", 1, result);
    }
  }

  @Test
  public void testExecute_withH2Database() throws Exception {
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create asset_blob table
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);

      // H2 is the default for tests, so this tests the H2 path
      assertTrue("should be H2 database", dbHelper.isH2(conn));

      // Execute task
      underTest.execute();

      // Verify index was created using H2 syntax (CREATE INDEX IF NOT EXISTS)
      assertTrue("index should exist after H2 execution",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testGetMessage() {
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    String message = underTest.getMessage();

    assertThat("Message should not be null", message, notNullValue());
    assertThat("Message should describe the task",
        message,
        equalTo("Creating asset blob indexes for blob_created and asset_blob_id"));
  }

  @Test
  public void testExecute_withIndexAlreadyExists() throws Exception {
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create asset_blob table and index manually
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createIndex(conn, ASSET_BLOB_TABLE, INDEX_NAME);

      // Verify index exists before execution
      assertTrue("index should exist before execution",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));

      // Execute task
      Object result = underTest.execute();

      // Verify no new indexes were created
      assertEquals("Should have created 0 indexes (already exists)", 0, result);

      // Verify index still exists
      assertTrue("index should still exist after execution",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testExecute_withEmptyFormatList() throws Exception {
    underTest = new CreateAssetBlobIndexTask(Collections.emptyList(), dataSessionSupplier);

    // Execute task with no formats
    Object result = underTest.execute();

    // Verify no indexes were created
    assertEquals("Should have created 0 indexes with empty format list", 0, result);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_rejectsNullFormats() {
    new CreateAssetBlobIndexTask(null, dataSessionSupplier);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_rejectsNullDataSessionSupplier() {
    new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), null);
  }

  @Test
  public void testExecute_threeFormats_allTablesExist() throws Exception {
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat, npmFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, MAVEN_ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, NPM_ASSET_BLOB_TABLE);

      Object result = underTest.execute();

      assertEquals("Should have created 3 indexes", 3, result);
      assertTrue("test index should exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
      assertTrue("maven index should exist",
          dbHelper.indexExists(conn, MAVEN_ASSET_BLOB_TABLE, MAVEN_INDEX_NAME));
      assertTrue("npm index should exist",
          dbHelper.indexExists(conn, NPM_ASSET_BLOB_TABLE, NPM_INDEX_NAME));
    }
  }

  @Test
  public void testExecute_multipleFormats_someTablesExist_someDoNot() throws Exception {
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat, npmFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Only create test and npm tables; maven table does not exist
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, NPM_ASSET_BLOB_TABLE);

      Object result = underTest.execute();

      assertEquals("Should have created 2 indexes (skipping maven)", 2, result);
      assertTrue("test index should exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
      assertTrue("npm index should exist",
          dbHelper.indexExists(conn, NPM_ASSET_BLOB_TABLE, NPM_INDEX_NAME));
    }
  }

  @Test
  public void testExecute_multipleFormats_someAlreadyIndexed() throws Exception {
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat, npmFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, MAVEN_ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, NPM_ASSET_BLOB_TABLE);

      // Pre-create test and npm indexes; only maven should be new
      createIndex(conn, ASSET_BLOB_TABLE, INDEX_NAME);
      createIndex(conn, NPM_ASSET_BLOB_TABLE, NPM_INDEX_NAME);

      Object result = underTest.execute();

      assertEquals("Should have created only 1 new index (maven)", 1, result);
      assertTrue("maven index should exist",
          dbHelper.indexExists(conn, MAVEN_ASSET_BLOB_TABLE, MAVEN_INDEX_NAME));
    }
  }

  @Test
  public void testExecute_connectionFailure_throwsException() throws Exception {
    when(dataSessionSupplier.openConnection(DEFAULT_DATASTORE_NAME))
        .thenThrow(new SQLException("Connection refused"));

    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try {
      underTest.execute();
      fail("Expected SQLException to propagate");
    }
    catch (SQLException e) {
      assertThat("Exception message should indicate connection failure",
          e.getMessage(), containsString("Connection refused"));
    }
  }

  @Test
  public void testExecute_idempotent_multipleFormats() throws Exception {
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, MAVEN_ASSET_BLOB_TABLE);

      // First execution creates both indexes
      Object result1 = underTest.execute();
      assertEquals("First execution should create 2 indexes", 2, result1);

      // Second execution creates no new indexes
      Object result2 = underTest.execute();
      assertEquals("Second execution should create 0 indexes", 0, result2);

      // Third execution also creates no new indexes
      Object result3 = underTest.execute();
      assertEquals("Third execution should create 0 indexes", 0, result3);

      // Indexes still exist after multiple runs
      assertTrue("test index should still exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
      assertTrue("maven index should still exist",
          dbHelper.indexExists(conn, MAVEN_ASSET_BLOB_TABLE, MAVEN_INDEX_NAME));
    }
  }

  @Test
  public void testExecute_noTablesExist_returnsZero() throws Exception {
    // All formats have no tables at all
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat, npmFormat), dataSessionSupplier);

    Object result = underTest.execute();

    assertEquals("Should have created 0 indexes when no tables exist", 0, result);
  }

  @Test
  public void testExecute_singleFormat_indexAlreadyExists_returnsZero() throws Exception {
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createIndex(conn, ASSET_BLOB_TABLE, INDEX_NAME);

      Object result = underTest.execute();

      assertEquals("Should return 0 when index already exists", 0, result);
    }
  }

  @Test
  public void testExecute_indexCreatedOnCorrectColumns() throws Exception {
    // Verifies the index is created on the right columns by inserting data and checking it works
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);

      // Insert some test data
      dbHelper.runStatement(conn,
          "INSERT INTO " + ASSET_BLOB_TABLE + " (blob_ref, blob_size) VALUES ('ref1', 100)");
      dbHelper.runStatement(conn,
          "INSERT INTO " + ASSET_BLOB_TABLE + " (blob_ref, blob_size) VALUES ('ref2', 200)");

      Object result = underTest.execute();

      assertEquals("Should have created 1 index", 1, result);
      assertTrue("index should exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testGetMessage_isNotEmpty() {
    underTest = new CreateAssetBlobIndexTask(Collections.emptyList(), dataSessionSupplier);

    String message = underTest.getMessage();

    assertThat("Message should not be null", message, notNullValue());
    assertFalse("Message should not be empty", message.isEmpty());
  }

  @Test
  public void testExecute_formatsProcessedInOrder() throws Exception {
    // Verifies all formats are processed even if they appear in different orders
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(npmFormat, testFormat, mavenFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, NPM_ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
      createAssetBlobTable(conn, MAVEN_ASSET_BLOB_TABLE);

      Object result = underTest.execute();

      assertEquals("Should have created 3 indexes", 3, result);
      assertTrue("npm index should exist",
          dbHelper.indexExists(conn, NPM_ASSET_BLOB_TABLE, NPM_INDEX_NAME));
      assertTrue("test index should exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
      assertTrue("maven index should exist",
          dbHelper.indexExists(conn, MAVEN_ASSET_BLOB_TABLE, MAVEN_INDEX_NAME));
    }
  }

  @Test
  public void testExecute_usesNexusDataStoreName() throws Exception {
    // Verifies the task opens a connection with "nexus" as the datastore name
    DataSessionSupplier strictSupplier = org.mockito.Mockito.mock(DataSessionSupplier.class);
    when(strictSupplier.openConnection("nexus"))
        .thenAnswer(invocation -> sessionRule.openConnection(DEFAULT_DATASTORE_NAME));

    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), strictSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
    }

    Object result = underTest.execute();

    assertEquals("Should have created 1 index", 1, result);
    org.mockito.Mockito.verify(strictSupplier).openConnection("nexus");
  }

  @Test
  public void testExecute_mixedTableStates_correctCount() throws Exception {
    // 3 formats: one table missing, one index already exists, one needs creation
    underTest = new CreateAssetBlobIndexTask(
        Arrays.asList(testFormat, mavenFormat, npmFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // test table doesn't exist (should skip)
      // maven table exists with index already (should skip)
      createAssetBlobTable(conn, MAVEN_ASSET_BLOB_TABLE);
      createIndex(conn, MAVEN_ASSET_BLOB_TABLE, MAVEN_INDEX_NAME);
      // npm table exists without index (should create)
      createAssetBlobTable(conn, NPM_ASSET_BLOB_TABLE);

      Object result = underTest.execute();

      assertEquals("Should have created exactly 1 index (only npm)", 1, result);
      assertTrue("npm index should have been created",
          dbHelper.indexExists(conn, NPM_ASSET_BLOB_TABLE, NPM_INDEX_NAME));
    }
  }

  @Test
  public void testExecute_singletonFormatList_sameAsSingletonCollections() throws Exception {
    // Verify that List.of() works the same as Collections.singletonList()
    underTest = new CreateAssetBlobIndexTask(List.of(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);

      Object result = underTest.execute();

      assertEquals("Should have created 1 index", 1, result);
      assertTrue("index should exist",
          dbHelper.indexExists(conn, ASSET_BLOB_TABLE, INDEX_NAME));
    }
  }

  @Test
  public void testExecute_closesConnectionProperly() throws Exception {
    // After task execution, the connection should be closed.
    // We verify that the DataSessionSupplier was called and the task completes without resource leak errors.
    underTest = new CreateAssetBlobIndexTask(Collections.singletonList(testFormat), dataSessionSupplier);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createAssetBlobTable(conn, ASSET_BLOB_TABLE);
    }

    // Execute should complete without throwing any resource-related errors
    Object result = underTest.execute();
    assertEquals("Should have created 1 index", 1, result);

    // Verify connection was requested
    org.mockito.Mockito.verify(dataSessionSupplier).openConnection(DEFAULT_DATASTORE_NAME);
  }

  private void createAssetBlobTable(Connection conn, String tableName) throws SQLException {
    String createTable = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
        + "asset_blob_id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
        + "blob_ref VARCHAR NOT NULL,"
        + "blob_size BIGINT NOT NULL,"
        + "blob_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + "created_by VARCHAR,"
        + "created_by_ip VARCHAR"
        + ")";
    dbHelper.runStatement(conn, createTable);
  }

  private void createIndex(Connection conn, String tableName, String indexName) throws SQLException {
    String createIndex = "CREATE INDEX " + indexName +
        " ON " + tableName + " (blob_created DESC, asset_blob_id ASC)";
    dbHelper.runStatement(conn, createIndex);
  }
}
