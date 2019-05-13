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
package org.sonatype.nexus.onboarding.internal;

import java.util.List;

import org.sonatype.nexus.orient.OIndexNameBuilder;
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
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.onboarding.internal.SecurityDatabaseUpgrade_1_2.ADMIN_PASS;
import static org.sonatype.nexus.onboarding.internal.SecurityDatabaseUpgrade_1_2.DB_CLASS;

public class SecurityDatabaseUpgrade_1_2Test
{
  private static final String P_ID = "id";

  private static final String P_PASSWORD = "password";

  private static final String P_STATUS = "status";

  private static final String I_ID = new OIndexNameBuilder().type(DB_CLASS).property(P_ID).build();

  private static final String SELECT_BY_USERNAME = String.format("select * from %s where %s = ?", DB_CLASS, P_ID);

  private static final String USER_ACTIVE = "active";

  private static final String USER_CHANGEPASSWORD = "changepassword";

  private static final String ADMIN_USER = "admin";

  @Rule
  public DatabaseInstanceRule securityDatabase = DatabaseInstanceRule.inMemory("security");

  private SecurityDatabaseUpgrade_1_2 underTest;

  @Before
  public void setup() {
    underTest = new SecurityDatabaseUpgrade_1_2(securityDatabase.getInstanceProvider());

    try (ODatabaseDocumentTx securityDb = securityDatabase.getInstance().connect()) {
      OSchema securitySchema = securityDb.getMetadata().getSchema();
      OClass type = securitySchema.createClass(DB_CLASS);
      type.createProperty(P_ID, OType.STRING).setNotNull(true);
      type.createProperty(P_PASSWORD, OType.STRING).setNotNull(true);
      type.createProperty(P_STATUS, OType.STRING).setNotNull(true);
      type.createIndex(I_ID, OClass.INDEX_TYPE.UNIQUE, P_ID);
    }
  }

  @Test
  public void testApply() {
    try (ODatabaseDocumentTx securityDb = securityDatabase.getInstance().connect()) {
      createUser(ADMIN_USER, ADMIN_PASS);
    }

    underTest.apply();

    assertAdminUserStatus(USER_CHANGEPASSWORD);
  }

  @Test
  public void testApply_passwordNotDefault() {
    try (ODatabaseDocumentTx securityDb = securityDatabase.getInstance().connect()) {
      createUser(ADMIN_USER, "other");
    }

    underTest.apply();

    assertAdminUserStatus(USER_ACTIVE);
  }

  private void createUser(String username, String password) {
    ODocument user = new ODocument(DB_CLASS);
    user.field(P_ID, username);
    user.field(P_PASSWORD, password);
    user.field(P_STATUS, USER_ACTIVE);
    user.save();
  }

  private void assertAdminUserStatus(String status) {
    try (ODatabaseDocumentTx securityDb = securityDatabase.getInstance().connect()) {
      List<ODocument> result = securityDb.command(new OCommandSQL(SELECT_BY_USERNAME)).execute(ADMIN_USER);
      assertThat(result.get(0).field(P_STATUS), is(status));
    }
  }
}
