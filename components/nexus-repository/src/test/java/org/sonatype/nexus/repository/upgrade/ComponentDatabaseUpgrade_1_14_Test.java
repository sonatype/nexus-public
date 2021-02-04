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
package org.sonatype.nexus.repository.upgrade;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ComponentDatabaseUpgrade_1_14_Test // NOSONAR
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("component");

  private ComponentDatabaseUpgrade_1_14 upgrade;

  private static final String BROWSE_NODE = new OClassNameBuilder().type("browse_node").build();

  @Before
  public void setUp() throws Exception {
    try (ODatabaseDocumentTx componentDb = componentDatabase.getInstance().connect()) {
      OSchema componentSchema = componentDb.getMetadata().getSchema();
      componentSchema.createClass(BROWSE_NODE);
    }

    upgrade = new ComponentDatabaseUpgrade_1_14(componentDatabase.getInstanceProvider());
  }

  @Test
  public void testApply() throws Exception {
    assertThat(browseNodeTableExists(), is(true));
    upgrade.apply();
    assertThat(browseNodeTableExists(), is(false));
  }

  @Test
  public void testApply_tableMissing() throws Exception {
    dropBrowseNodeTable();
    assertThat(browseNodeTableExists(), is(false));
    upgrade.apply();
    assertThat(browseNodeTableExists(), is(false));
  }

  private boolean browseNodeTableExists() {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      return db.getMetadata().getSchema().getClass(BROWSE_NODE) != null;
    }
  }

  private void dropBrowseNodeTable() {
    try (ODatabaseDocumentTx componentDb = componentDatabase.getInstance().connect()) {
      componentDb.getMetadata().getSchema().dropClass(BROWSE_NODE);
    }
  }
}
