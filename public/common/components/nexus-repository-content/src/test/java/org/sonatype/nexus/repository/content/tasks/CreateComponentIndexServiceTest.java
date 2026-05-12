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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationUtility;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateComponentIndexServiceTest
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  @Mock
  private Format fakeFormat1;

  @Mock
  private Format fakeFormat2;

  @Mock
  private Format fakeFormat3;

  @Mock
  private Format fakeFormat4;

  @Mock
  private Format fakeFormat5;

  private CreateComponentIndexService underTest;

  @Before
  public void setup() {
    when(fakeFormat1.getValue()).thenReturn("test1");
    when(fakeFormat2.getValue()).thenReturn("test2");
    when(fakeFormat3.getValue()).thenReturn("test3");
    when(fakeFormat4.getValue()).thenReturn("test4");
    when(fakeFormat5.getValue()).thenReturn("test5");
    underTest =
        new CreateComponentIndexService(
            sessionRule,
            new DatabaseMigrationUtility(),
            asList(fakeFormat1, fakeFormat2, fakeFormat3, fakeFormat4, fakeFormat5));
  }

  @Test
  public void testRecreateComponentIndexes() throws Exception {
    try (Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      createTableAndIndex(connection, "test1");
      createTableAndIndex(connection, "test2");
      createTable(connection, "test3");
      createTable(connection, "test4");
      createIndex(
          connection,
          "idx_test4_component_coordinates",
          "test4_component",
          "repository_id, namespace, name, version");
      createTableAndIndex(connection, "test5");
      createIndex(
          connection,
          "idx_test5_component_coordinates",
          "test5_component",
          "repository_id, namespace, name, version");
      assertThat(indexExists(connection, "idx_test1_component_set"), is(true));
      assertThat(indexExists(connection, "idx_test2_component_set"), is(true));
      assertThat(indexExists(connection, "idx_test3_component_set"), is(false));
      assertThat(indexExists(connection, "idx_test4_component_set"), is(false));
      assertThat(indexExists(connection, "idx_test5_component_set"), is(true));
      assertThat(indexExists(connection, "idx_test1_component_coordinates"), is(false));
      assertThat(indexExists(connection, "idx_test2_component_coordinates"), is(false));
      assertThat(indexExists(connection, "idx_test3_component_coordinates"), is(false));
      assertThat(indexExists(connection, "idx_test4_component_coordinates"), is(true));
      assertThat(indexExists(connection, "idx_test5_component_coordinates"), is(true));
    }

    underTest.recreateComponentIndexes();

    try (Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      assertThat(indexExists(connection, "idx_test1_component_set"), is(false));
      assertThat(indexExists(connection, "idx_test2_component_set"), is(false));
      assertThat(indexExists(connection, "idx_test3_component_set"), is(false));
      assertThat(indexExists(connection, "idx_test4_component_set"), is(false));
      assertThat(indexExists(connection, "idx_test5_component_set"), is(false));
      assertThat(indexExists(connection, "idx_test1_component_coordinates"), is(true));
      assertThat(indexExists(connection, "idx_test2_component_coordinates"), is(true));
      assertThat(indexExists(connection, "idx_test3_component_coordinates"), is(true));
      assertThat(indexExists(connection, "idx_test4_component_coordinates"), is(true));
      assertThat(indexExists(connection, "idx_test5_component_coordinates"), is(true));
    }
  }

  private void createTableAndIndex(final Connection connection, final String formatName) throws SQLException {
    createTable(connection, formatName);
    String tableName = formatName + "_component";
    String indexName = "idx_" + formatName + "_component_set";
    createIndex(connection, indexName, tableName, "repository_id, namespace, name");
  }

  private void createTable(final Connection connection, final String formatName) throws SQLException {
    String tableName = formatName + "_component";
    try (PreparedStatement statement = connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
            "component_id  INT GENERATED BY DEFAULT AS IDENTITY," +
            "repository_id INT NOT NULL," +
            "namespace     VARCHAR NOT NULL," +
            "name          VARCHAR NOT NULL," +
            "version       VARCHAR NOT NULL)")) {
      statement.execute();
    }
  }

  private void createIndex(
      final Connection connection,
      final String indexName,
      final String tableName,
      final String columns) throws SQLException
  {
    try (PreparedStatement statement = connection.prepareStatement(
        "CREATE INDEX " + indexName.toUpperCase(Locale.ENGLISH) + " ON " +
            tableName + " (" + columns + ")")) {
      statement.execute();
    }
  }

  private boolean indexExists(final Connection connection, final String indexName) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT * FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(INDEX_NAME) = ?")) {
      statement.setString(1, indexName.toUpperCase());
      try (ResultSet results = statement.executeQuery()) {
        if (!results.next()) {
          return false;
        }
      }
    }
    return true;
  }
}
