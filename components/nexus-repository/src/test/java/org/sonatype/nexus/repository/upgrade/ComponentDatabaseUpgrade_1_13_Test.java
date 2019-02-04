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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ComponentDatabaseUpgrade_1_13_Test
    extends TestSupport
{
  static final String DB_CLASS = new OClassNameBuilder().type("assetdownloadcount").build();

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component");

  private ComponentDatabaseUpgrade_1_13 underTest;

  @Before
  public void setUp() {
    underTest = new ComponentDatabaseUpgrade_1_13(componentDatabase.getInstanceProvider());
  }

  @Test
  public void testClassDropped() throws Exception {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      schema.createClass(DB_CLASS);
      db.browseClass(DB_CLASS);
    }

    underTest.apply();

    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      try {
        db.browseClass(DB_CLASS);
        fail("Expected exception thrown");
      }
      catch (IllegalArgumentException e) {
        assertThat(e.getMessage(), is("Class 'assetdownloadcount' not found in current database"));
      }
    }
  }

  @Test
  public void testWithoutExistingDb() throws Exception {
    underTest.apply();
  }

}
