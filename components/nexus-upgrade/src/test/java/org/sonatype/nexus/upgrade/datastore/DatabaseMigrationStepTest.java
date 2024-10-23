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
package org.sonatype.nexus.upgrade.datastore;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.PostgresTestGroup;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(PostgresTestGroup.class)
public class DatabaseMigrationStepTest
    extends TestSupport
{

  private static final String CUSTOM_SQL = "CREATE TABLE IF NOT EXISTS custom.test (\n"
      + "      domain            VARCHAR(200)   NOT NULL,\n"
      + "      token             VARCHAR(200)   NOT NULL,\n"
      + "      CONSTRAINT pk_domain PRIMARY KEY (domain)\n"
      + "    );\n"
      + "";

  @Rule
  public DataSessionRule customSessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  @Test
  public void testCustomSchemaIndexExists() throws Exception {
    try (Connection conn = customSessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.runStatement(conn, "drop schema if exists custom");
      underTest.runStatement(conn, "create schema custom authorization test");

      // create a table + index in custom schema
      underTest.runStatement(conn, CUSTOM_SQL);
      assertFalse("index does not exist in public schema", underTest.indexExists(conn, "pk_domain"));

      underTest.runStatement(conn, "SET search_path TO custom");
      assertTrue("index exists in custom schema", underTest.indexExists(conn, "pk_domain"));
    }
  }

  private final DatabaseMigrationStep underTest = new DatabaseMigrationStep(){
    public Optional<String> version() {
      return Optional.of("0.0");
    }
    @Override
    public void migrate(final Connection connection) {
    }
  };
}
