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
package org.sonatype.nexus.internal.jwt.datastore.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class JwtSessionAddTypeColumnMigrationStep_2_127_Test
{
  private static final String TABLE_NAME = "jwt_session";

  private static final String CREATE_TABLE_WITHOUT_TYPE = """
      CREATE TABLE IF NOT EXISTS jwt_session (
          user_session_id VARCHAR(36) NOT NULL,
          username        VARCHAR(200) NOT NULL,
          user_source     VARCHAR(200) NOT NULL,
          revoked_at      TIMESTAMP WITH TIME ZONE NOT NULL,
          expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
          CONSTRAINT pk_jwt_session_user_session_id PRIMARY KEY (user_session_id)
      )
      """;

  @Rule
  public DataSessionRule dataSessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  private final JwtSessionAddTypeColumnMigrationStep_2_127 underTest =
      new JwtSessionAddTypeColumnMigrationStep_2_127();

  @Test
  public void testVersion() {
    assertEquals(Optional.of("2.127"), underTest.version());
  }

  @Test
  public void testMigrate_addsTypeColumn() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(connection, CREATE_TABLE_WITHOUT_TYPE);
      assertFalse(underTest.columnExists(connection, TABLE_NAME, "type"));

      underTest.migrate(connection);

      assertTrue(underTest.columnExists(connection, TABLE_NAME, "type"));
    }
  }

  @Test
  public void testMigrate_isIdempotent() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(connection, CREATE_TABLE_WITHOUT_TYPE);

      underTest.migrate(connection);
      underTest.migrate(connection);

      assertTrue(underTest.columnExists(connection, TABLE_NAME, "type"));
    }
  }

  @Test
  public void testMigrate_noErrorWhenTableDoesNotExist() throws Exception {
    try (Connection connection = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      assertFalse(underTest.tableExists(connection, TABLE_NAME));

      underTest.migrate(connection);

      assertFalse(underTest.tableExists(connection, TABLE_NAME));
    }
  }
}
