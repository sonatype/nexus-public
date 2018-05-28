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
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ComponentDatabaseUpgrade_1_6_Test
    extends TestSupport
{
  static final String DB_CLASS = new OClassNameBuilder()
      .type("assetdownloadcount")
      .build();

  public static final String P_ASSET_NAME = "asset_name";

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component");

  private ComponentDatabaseUpgrade_1_6 underTest;

  @Before
  public void setUp() {
    underTest = new ComponentDatabaseUpgrade_1_6(componentDatabase.getInstanceProvider());
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass componentType = schema.createClass(DB_CLASS);
      componentType.createProperty(P_ASSET_NAME, OType.STRING);

      for (int i = 0 ; i < 1000 ; i ++) {
        ODocument document = db.newInstance(DB_CLASS);
        document.save();
      }
    }
  }

  @Test
  public void testAssetNamesUpdated() throws Exception {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      Iterable<ODocument> documents = db.browseClass(DB_CLASS);
      for (ODocument document : documents) {
        assertThat(document.field(P_ASSET_NAME), nullValue());
      }
    }

    underTest.apply();

    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      Iterable<ODocument> documents = db.browseClass(DB_CLASS);
      for (ODocument document : documents) {
        assertThat(document.field(P_ASSET_NAME), is(""));
      }
    }
  }

  @Test
  public void testWithoutExistingDb() throws Exception {
    underTest.apply();
  }
}
