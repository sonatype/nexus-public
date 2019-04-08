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
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ComponentDatabaseUpgrade_1_14_Test
    extends TestSupport
{
  static final String DB_CLASS = new OClassNameBuilder()
      .type("repository")
      .build();

  private static final String P_REPOSITORY_NAME = "repositoryName";

  private static final String P_ROUTING_RULE_ID = "routingRuleId";

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component");

  private ComponentDatabaseUpgrade_1_14 underTest;

  @Before
  public void setUp() {
    underTest = new ComponentDatabaseUpgrade_1_14(componentDatabase.getInstanceProvider());
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass dbClass = schema.createClass(DB_CLASS);
      dbClass.createProperty(P_REPOSITORY_NAME, OType.STRING);

      ODocument document = db.newInstance(DB_CLASS);
      document.save();
    }
  }

  @Test
  public void upgradeAddsRoutingRuleLinkField() throws Exception {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OClass table = db.getMetadata().getSchema().getClass(DB_CLASS);
      assertThat(table.getProperty(P_ROUTING_RULE_ID), is(nullValue()));
    }

    underTest.apply();

    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OClass table = db.getMetadata().getSchema().getClass(DB_CLASS);
      assertThat(table.getProperty(P_ROUTING_RULE_ID), is(notNullValue()));

      db.browseClass(DB_CLASS).forEach(record -> {
        EntityId routingRuleId = record.field(P_ROUTING_RULE_ID, OType.LINK);
        assertThat(routingRuleId, is(nullValue()));
      });
    }
  }

  @Test
  public void testWithoutExistingDb() throws Exception {
    underTest.apply();
  }
}
