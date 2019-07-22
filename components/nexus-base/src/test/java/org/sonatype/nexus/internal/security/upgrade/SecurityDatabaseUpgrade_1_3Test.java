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
package org.sonatype.nexus.internal.security.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.internal.security.upgrade.SecurityDatabaseUpgrade_1_3.DB_CLASS;
import static org.sonatype.nexus.internal.security.upgrade.SecurityDatabaseUpgrade_1_3.P_ACTIONS;
import static org.sonatype.nexus.internal.security.upgrade.SecurityDatabaseUpgrade_1_3.P_NAME;
import static org.sonatype.nexus.internal.security.upgrade.SecurityDatabaseUpgrade_1_3.P_PROPERTIES;
import static org.sonatype.nexus.internal.security.upgrade.SecurityDatabaseUpgrade_1_3.P_TYPE;

public class SecurityDatabaseUpgrade_1_3Test
{
  @Rule
  public DatabaseInstanceRule securityDatabase = DatabaseInstanceRule.inMemory("security");

  private SecurityDatabaseUpgrade_1_3 underTest;

  @Before
  public void setup() {
    underTest = new SecurityDatabaseUpgrade_1_3(securityDatabase.getInstanceProvider());

    try (ODatabaseDocumentTx securityDb = securityDatabase.getInstance().connect()) {
      OSchema securitySchema = securityDb.getMetadata().getSchema();
      OClass type = securitySchema.createClass(DB_CLASS);
      type.createProperty(P_NAME, OType.STRING).setNotNull(true);
      type.createProperty(P_TYPE, OType.STRING).setNotNull(true);
      type.createProperty(P_PROPERTIES, OType.EMBEDDEDMAP).setNotNull(true);
    }
  }

  @Test
  public void testApply() {
    try (ODatabaseDocumentTx securityDb = securityDatabase.getInstance().connect()) {
      createPrivilege("a_test_priv", "read,edit,delete,add");
      createPrivilege("b_test_priv", "mark,read,edit,delete");
      createPrivilege("c_test_priv", "create,read,edit,delete,add");
      createPrivilege("d_test_priv", "mark,read,edit,delete,create");
    }

    underTest.apply();

    try (ODatabaseDocumentTx securityDb = securityDatabase.getInstance().connect()) {
      List<ODocument> result = securityDb.command(new OCommandSQL("select from privilege")).execute();
      assertThat(result.size(), is(4));
      assertThat(result.get(0).field(P_NAME), is("a_test_priv"));
      assertThat(result.get(0).field(P_PROPERTIES), hasEntry(is("actions"), is("read,edit,delete,create")));
      assertThat(result.get(1).field(P_NAME), is("b_test_priv"));
      assertThat(result.get(1).field(P_PROPERTIES), hasEntry(is("actions"), is("read,edit,delete,create")));
      assertThat(result.get(2).field(P_NAME), is("c_test_priv"));
      assertThat(result.get(2).field(P_PROPERTIES), hasEntry(is("actions"), is("create,read,edit,delete")));
      assertThat(result.get(3).field(P_NAME), is("d_test_priv"));
      assertThat(result.get(3).field(P_PROPERTIES), hasEntry(is("actions"), is("read,edit,delete,create")));
    }
  }

  private void createPrivilege(String name, String actions) {
    ODocument document = new ODocument(DB_CLASS);
    document.field(P_NAME, name);
    document.field(P_TYPE, "application");
    Map<String,String> properties = new HashMap<>();
    properties.put(P_ACTIONS, actions);
    document.field(P_PROPERTIES, properties);
    document.save();
  }
}
