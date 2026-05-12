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
package org.sonatype.nexus.repository.search.sql.store.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.sonatype.nexus.repository.search.sql.store.SearchTableDAO;
import org.sonatype.nexus.testdb.DataSessionConfiguration;

import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.sonatype.nexus.testdb.DatabaseTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

/**
 * DB tests for {@link SearchTablePathsColumnJsonMigrationStep_2_102} class
 */
class SearchTablePathsColumnJsonMigrationStep_2_102Test
{
  @DataSessionConfiguration(daos = {SearchTableDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  SearchTablePathsColumnJsonMigrationStep_2_102 underTest = new SearchTablePathsColumnJsonMigrationStep_2_102();

  private static final String SEARCH_COMPONENTS_TABLE = "SEARCH_COMPONENTS";

  private static final String PATHS_COLUMN = "PATHS";

  private static final String VARCHAR = "CHARACTER VARYING";

  private static final String JSON = "JSON";

  private static final String ALTER_TO_VARCHAR =
      "ALTER TABLE search_components ALTER COLUMN paths SET DATA TYPE VARCHAR";

  @DatabaseTest
  void testMigration() throws Exception {
    try (Connection conn = dataSessionSupplier.openConnection()) {
      if (underTest.isH2(conn)) {

        // Change from JSON to VARCHAR to simulate pre-migration state
        underTest.runStatement(conn, ALTER_TO_VARCHAR);

        // Verify paths column is VARCHAR before migration
        assertThat("paths should be VARCHAR before migration",
            getColumnType(conn, SEARCH_COMPONENTS_TABLE, PATHS_COLUMN),
            equalTo(VARCHAR));

        // Run migration
        underTest.migrate(conn);

        // Verify paths column is JSON after migration
        assertThat("paths should be JSON after migration",
            getColumnType(conn, SEARCH_COMPONENTS_TABLE, PATHS_COLUMN),
            equalTo(JSON));
      }
      else {
        assertThat("paths should be VARCHAR for postgres DB",
            getColumnType(conn, SEARCH_COMPONENTS_TABLE.toLowerCase(), PATHS_COLUMN.toLowerCase()),
            equalToIgnoringCase(VARCHAR));
      }
    }
  }

  private String getColumnType(Connection conn, String tableName, String columnName) throws Exception {
    String sql = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, tableName);
      stmt.setString(2, columnName);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("DATA_TYPE");
        }
      }
    }
    throw new IllegalStateException("Column " + columnName + " not found in table " + tableName);
  }
}
