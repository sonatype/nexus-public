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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.assertj.db.type.Table;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.db.api.Assertions.assertThat;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class DistributedAuthTicketMigrationStep_1_30Test
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  private DistributedAuthTicketMigrationStep_1_30 underTest = new DistributedAuthTicketMigrationStep_1_30();

  @Test
  public void testMigrate() throws Exception {
    stubTable();
    // Sanity check
    assertThat(table()).hasNumberOfRows(1);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(conn);
    }
    assertThat(table()).hasNumberOfRows(0);
  }

  @Test
  public void testMigrate_noTable() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(conn);
    }
    // The real check here is that the migration does not error.
    assertThat(table()).doesNotExist();
  }

  private void stubTable() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement statement = conn.prepareStatement(
            "CREATE TABLE distributed_auth_ticket_cache (user_name  VARCHAR(200) NOT NULL);")) {
      statement.execute();
    }
    // sanity check
    assertThat(table()).exists();

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement insert =
            conn.prepareStatement("INSERT INTO distributed_auth_ticket_cache (user_name) VALUES (?)")) {
      insert.setString(1, "a_user");
      insert.execute();
    }
  }

  private Table table() {
    DataStore<?> dataStore = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).orElseThrow(RuntimeException::new);
    return new Table(dataStore.getDataSource(), "distributed_auth_ticket_cache");
  }
}
