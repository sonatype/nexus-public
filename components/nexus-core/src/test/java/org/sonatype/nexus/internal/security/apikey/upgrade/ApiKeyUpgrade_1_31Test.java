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
package org.sonatype.nexus.internal.security.apikey.upgrade;

import java.sql.Connection;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyDAO;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class ApiKeyUpgrade_1_31Test
    extends TestSupport
{
  private static final String NEW_INDEX_NAME = "pk_api_key_primaryprincipal_domain_principals";

  private static final String OLD_INDEX_NAME = "pk_api_key_primaryprincipal_domain";

  private static final String OLD_SCHEMA = "CREATE TABLE IF NOT EXISTS api_key (\n"
      + "      primary_principal VARCHAR(200)   NOT NULL,\n"
      + "      domain            VARCHAR(200)   NOT NULL,\n"
      + "      token             VARCHAR(200)   NOT NULL,\n"
      + "      principals        VARCHAR(200) NOT NULL,\n"
      + "      created           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n"
      + "      CONSTRAINT pk_api_key_primaryprincipal_domain PRIMARY KEY (primary_principal, domain),\n"
      + "      CONSTRAINT uk_api_key_domain_token UNIQUE (domain, token)\n"
      + "    );\n"
      + "";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME);

  private ApiKeyUpgrade_1_31 underTest = new ApiKeyUpgrade_1_31();

  @Test
  public void testUpgrade() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // create old schema
      underTest.runStatement(conn, OLD_SCHEMA);

      // Sanity checks
      assertTrue("Sanity check - table exists", underTest.tableExists(conn, "api_key"));
      assertTrue("Sanity check - old index exists", underTest.indexExists(conn, OLD_INDEX_NAME));

      underTest.migrate(conn);

      assertTrue("New index created", underTest.indexExists(conn, NEW_INDEX_NAME));
      assertFalse("Old index removed", underTest.indexExists(conn, OLD_INDEX_NAME));
    }
  }

  @Test
  public void testUpgrade_tableNotExist() throws Exception {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(conn);
      // The real check here is that the migration does not error.
      assertFalse("Sanity check - table exists", underTest.tableExists(conn, "api_key"));
    }
  }

  @Test
  public void testUpgrade_notRequired() throws Exception {
    sessionRule.getDataStore(DEFAULT_DATASTORE_NAME)
        .orElseThrow(() -> new IllegalStateException())
        .register(ApiKeyDAO.class);

    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      // Sanity checks
      assertTrue("Sanity check - table exists", underTest.tableExists(conn, "api_key"));
      assertTrue("Sanity check - new index exists", underTest.indexExists(conn, NEW_INDEX_NAME));

      underTest.migrate(conn);
      // The real check here is that the migration does not error.
    }
  }
}
