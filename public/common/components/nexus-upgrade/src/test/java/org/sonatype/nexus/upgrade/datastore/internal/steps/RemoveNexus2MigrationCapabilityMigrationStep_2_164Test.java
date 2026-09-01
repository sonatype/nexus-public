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
package org.sonatype.nexus.upgrade.datastore.internal.steps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class RemoveNexus2MigrationCapabilityMigrationStep_2_164Test
{
  @Rule
  public DataSessionRule dataSessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  private final RemoveNexus2MigrationCapabilityMigrationStep_2_164 underTest =
      new RemoveNexus2MigrationCapabilityMigrationStep_2_164();

  @Test
  public void shouldDeleteMigrationCapabilityRows() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create the capability_storage_item table
      createCapabilityStorageItemTable(connection);

      // Insert a migration capability row
      insertCapabilityRow(connection, UUID.randomUUID(), "migration");

      // Verify row exists before migration
      assertEquals(1, countRowsByType(connection, "migration"));

      // Run migration
      underTest.migrate(connection);

      // Verify migration capability rows are deleted
      assertEquals(0, countRowsByType(connection, "migration"));
    }
  }

  @Test
  public void shouldNotDeleteOtherCapabilityTypes() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create the capability_storage_item table
      createCapabilityStorageItemTable(connection);

      // Insert rows with different types
      insertCapabilityRow(connection, UUID.randomUUID(), "migration");
      insertCapabilityRow(connection, UUID.randomUUID(), "log4j-visualizer");
      insertCapabilityRow(connection, UUID.randomUUID(), "some-other-type");

      // Run migration
      underTest.migrate(connection);

      // Verify only migration rows are deleted
      assertEquals(0, countRowsByType(connection, "migration"));
      assertEquals(1, countRowsByType(connection, "log4j-visualizer"));
      assertEquals(1, countRowsByType(connection, "some-other-type"));
    }
  }

  @Test
  public void shouldBeIdempotent() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Create the capability_storage_item table
      createCapabilityStorageItemTable(connection);

      // Insert a migration capability row
      insertCapabilityRow(connection, UUID.randomUUID(), "migration");

      // Run migration twice
      underTest.migrate(connection);
      underTest.migrate(connection);

      // Verify migration capability rows are still deleted
      assertEquals(0, countRowsByType(connection, "migration"));
    }
  }

  @Test
  public void shouldHandleMissingTableGracefully() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Verify table doesn't exist
      assertFalse("capability_storage_item table should not exist",
          underTest.tableExists(connection, "capability_storage_item"));

      // Run migration - should not throw
      underTest.migrate(connection);

      // Verify table still doesn't exist
      assertFalse("capability_storage_item table should still not exist",
          underTest.tableExists(connection, "capability_storage_item"));
    }
  }

  private void createCapabilityStorageItemTable(final Connection connection) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
        CREATE TABLE IF NOT EXISTS capability_storage_item (
          id         UUID NOT NULL,
          version    INT NOT NULL,
          type       VARCHAR(100) NOT NULL,
          enabled    BOOLEAN NOT NULL,
          notes      VARCHAR(400),
          properties BLOB NOT NULL,
          CONSTRAINT pk_capability_storage_item_id PRIMARY KEY (id)
        )
        """)) {
      statement.execute();
    }
  }

  private void insertCapabilityRow(
      final Connection connection,
      final UUID id,
      final String type) throws Exception
  {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO capability_storage_item (id, version, type, enabled, notes, properties)
        VALUES (?, ?, ?, ?, ?, ?)
        """)) {
      statement.setObject(1, id);
      statement.setInt(2, 1);
      statement.setString(3, type);
      statement.setBoolean(4, true);
      statement.setString(5, "test notes");
      statement.setBytes(6, "{}".getBytes());
      statement.executeUpdate();
    }
  }

  private int countRowsByType(final Connection connection, final String type) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM capability_storage_item WHERE type = ?")) {
      statement.setString(1, type);
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }
}
