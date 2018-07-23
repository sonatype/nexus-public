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
package org.sonatype.nexus.repository.upgrade

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.OClassNameBuilder
import org.sonatype.nexus.orient.OIndexBuilder
import org.sonatype.nexus.orient.OIndexNameBuilder
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not
import static org.hamcrest.core.IsNull.nullValue
import static org.junit.Assert.assertThat

class ComponentDatabaseUpgrade_1_5_Test
    extends TestSupport
{
  static final String COMPONENT_CLASS = new OClassNameBuilder()
      .type('component')
      .build()

  static final String I_CI_NAME_CASE_INSENSITIVE = new OIndexNameBuilder()
      .type(COMPONENT_CLASS)
      .property(P_CI_NAME)
      .caseInsensitive()
      .build()

  static final String I_NAME_CASE_INSENSITIVE = new OIndexNameBuilder()
      .type(COMPONENT_CLASS)
      .property(P_NAME)
      .caseInsensitive()
      .build()

  static final String P_NAME = 'name'

  static final String P_FORMAT = 'format'

  static final String P_CI_NAME = 'ci_name'

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component")

  ComponentDatabaseUpgrade_1_5 underTest

  @Before
  void setUp() {
    underTest = new ComponentDatabaseUpgrade_1_5(componentDatabase.getInstanceProvider())
  }

  @Test
  void 'upgrade step creates and populates ci_name property'() {
    componentDatabase.instance.connect().withCloseable { db ->
      createComponentType(db)
      createComponentRecord(db)
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertComponentType(db)
      assertDocuments(db)
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertComponentType(db)
      assertDocuments(db)
    }
  }

  @Test
  void 'upgrade step creates ci_name case-insensitive index'() {
    componentDatabase.instance.connect().withCloseable { db ->
      createComponentType(db)
      assertThat(db.metadata.indexManager.getIndex(I_CI_NAME_CASE_INSENSITIVE), is(nullValue()))
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertThat(db.metadata.indexManager.getIndex(I_CI_NAME_CASE_INSENSITIVE), not(nullValue()))
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertThat(db.metadata.indexManager.getIndex(I_CI_NAME_CASE_INSENSITIVE), not(nullValue()))
    }
  }

  @Test
  void 'upgrade step drops existing case-insensitive index on name field'() {
    componentDatabase.instance.connect().withCloseable { db ->
      createComponentType(db)
      OSchema schema = db.metadata.schema
      OClass componentType = schema.getClass(COMPONENT_CLASS)
      new OIndexBuilder(componentType, I_NAME_CASE_INSENSITIVE, INDEX_TYPE.NOTUNIQUE)
          .property(P_NAME, OType.STRING)
          .caseInsensitive()
          .build(db)
    }

    componentDatabase.instance.connect().withCloseable { db ->
      assertThat(db.metadata.indexManager.getIndex(I_NAME_CASE_INSENSITIVE), not(nullValue()))
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertThat(db.metadata.indexManager.getIndex(I_NAME_CASE_INSENSITIVE), is(nullValue()))
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertThat(db.metadata.indexManager.getIndex(I_NAME_CASE_INSENSITIVE), is(nullValue()))
    }
  }

  private void createComponentRecord(ODatabaseDocumentTx db) {
    ODocument document = db.newInstance(COMPONENT_CLASS)
    document.field(P_NAME, 'TestName')
    document.field(P_FORMAT, 'format')
    document.save()
  }

  private void createComponentType(ODatabaseDocumentTx db) {
    OSchema schema = db.metadata.schema
    OClass componentType = schema.createClass(COMPONENT_CLASS)
    componentType.createProperty(P_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true)
    componentType.createProperty(P_FORMAT, OType.STRING)
        .setMandatory(true)
        .setNotNull(true)
  }

  private void assertDocuments(ODatabaseDocumentTx db) {
    db.browseClass(COMPONENT_CLASS).forEach { document ->
      assertThat(document.field(P_NAME), is('TestName'))
      assertThat(document.field(P_CI_NAME), is('testname')) // has to be lowercase for now by convention (workaround)
    }
  }

  private void assertComponentType(ODatabaseDocumentTx db) {
    OClass componentType = db.metadata.schema.getClass(COMPONENT_CLASS)
    assertThat(componentType.existsProperty(P_CI_NAME), is(true))
    assertThat(componentType.getProperty(P_CI_NAME).mandatory, is(true))
    assertThat(componentType.getProperty(P_CI_NAME).notNull, is(true))
    assertThat(componentType.getProperty(P_CI_NAME).collate.name, is('ci'))
  }
}
