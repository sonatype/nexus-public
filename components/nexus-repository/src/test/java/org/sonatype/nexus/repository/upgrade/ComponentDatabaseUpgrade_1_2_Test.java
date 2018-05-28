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
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.upgrade.ComponentDatabaseUpgrade_1_2.I_BUCKET_NAME_VERSION;

public class ComponentDatabaseUpgrade_1_2_Test
    extends TestSupport
{
  static final String COMPONENT_CLASS = new OClassNameBuilder()
      .type("component")
      .build();

  static final String BUCKET_CLASS = new OClassNameBuilder()
      .type("bucket")
      .build();

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component");

  ComponentDatabaseUpgrade_1_2 underTest;

  @Before
  public void setUp() {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();

      OClass bucketType = schema.createClass(BUCKET_CLASS);

      OClass componentType = schema.createClass(COMPONENT_CLASS);

      componentType.createProperty(P_GROUP, OType.STRING);
      componentType.createProperty(P_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true);
      componentType.createProperty(P_VERSION, OType.STRING);
      componentType.createProperty(P_BUCKET, OType.LINK, bucketType).setMandatory(true).setNotNull(true);
    }

    underTest = new ComponentDatabaseUpgrade_1_2(componentDatabase.getInstanceProvider());
  }

  @Test
  public void upgradeStep_CreatesNewIndex() throws Exception {
    //validate the index does not exist before
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      assertThat(db.getMetadata().getIndexManager().getIndex(I_BUCKET_NAME_VERSION), nullValue());
    }

    underTest.apply();

    //validate the index is available
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      assertThat(db.getMetadata().getIndexManager().getIndex(I_BUCKET_NAME_VERSION), not(nullValue()));
    }
  }

  @Test
  public void upgradeStep_IndexAlreadyExists() throws Exception {
    //create the index so upgrade step wont do anything
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      db.getMetadata().getSchema().getClass(COMPONENT_CLASS)
          .createIndex(I_BUCKET_NAME_VERSION, INDEX_TYPE.NOTUNIQUE, new String[]{P_BUCKET, P_NAME, P_VERSION});
    }

    underTest.apply();

    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      assertThat(db.getMetadata().getIndexManager().getIndex(I_BUCKET_NAME_VERSION), not(nullValue()));
    }
  }
}
