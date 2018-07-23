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
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

class ComponentDatabaseUpgrade_1_3_Test
    extends TestSupport
{
  static final String ASSET_CLASS = new OClassNameBuilder()
      .type('asset')
      .build()

  static final String P_NAME = 'name'

  static final String P_FORMAT = 'format'

  static final String P_ATTRIBUTES = 'attributes'

  static final String P_LAST_ACCESSED = 'last_accessed'

  static final String P_LAST_DOWNLOADED = 'last_downloaded'

  static final String P_BLOB_CREATED = 'blob_created'

  static final String P_BLOB_UPDATED = 'blob_updated'

  static final Date LAST_ACCESSED_TIMESTAMP = new Date(0)

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component")

  ComponentDatabaseUpgrade_1_3 underTest

  @Before
  void setUp() {
    underTest = new ComponentDatabaseUpgrade_1_3(componentDatabase.getInstanceProvider())
  }

  @Test
  void 'upgrade step creates blob_created and blob_updated properties when absent'() {
    populateComponentDatabase()

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      OClass assetType = db.metadata.schema.getClass(ASSET_CLASS)
      assertThat(assetType.existsProperty(P_BLOB_CREATED), is(true))
      assertThat(assetType.existsProperty(P_BLOB_UPDATED), is(true))
      assertThat(assetType.getProperty(P_BLOB_CREATED).type, is(OType.DATETIME))
      assertThat(assetType.getProperty(P_BLOB_UPDATED).type, is(OType.DATETIME))
    }
  }

  @Test
  void 'upgrade step works even if blob_created and blob_updated properties are present'() {
    populateComponentDatabase()

    componentDatabase.instance.connect().withCloseable { db ->
      OClass assetType = db.metadata.schema.getClass(ASSET_CLASS)
      assetType.createProperty(P_BLOB_CREATED, OType.DATETIME)
      assetType.createProperty(P_BLOB_UPDATED, OType.DATETIME)
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      OClass assetType = db.metadata.schema.getClass(ASSET_CLASS)
      assertThat(assetType.existsProperty(P_BLOB_CREATED), is(true))
      assertThat(assetType.existsProperty(P_BLOB_UPDATED), is(true))
      assertThat(assetType.getProperty(P_BLOB_CREATED).type, is(OType.DATETIME))
      assertThat(assetType.getProperty(P_BLOB_UPDATED).type, is(OType.DATETIME))
    }
  }

  @Test
  void 'upgrade step replaces last_accessed with last_downloaded'() {
    populateComponentDatabase()

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      OClass assetType = db.metadata.schema.getClass(ASSET_CLASS)
      assertThat(assetType.existsProperty(P_LAST_ACCESSED), is(false))
      assertThat(assetType.existsProperty(P_LAST_DOWNLOADED), is(true))
      assertThat(assetType.getProperty(P_LAST_DOWNLOADED).type, is(OType.DATETIME))
      db.browseClass(ASSET_CLASS).each { asset ->
        assertThat(asset.field(P_LAST_DOWNLOADED, OType.DATETIME), is(LAST_ACCESSED_TIMESTAMP))
      }
    }
  }

  @Test
  void 'upgrade step does not throw exceptions if asset class is not found in schema'() {
    underTest.apply()
  }

  @Test
  void 'upgrade step does not throw exceptions if last_accessed property is not found on asset'() {
    populateComponentDatabase(false)
    underTest.apply()
  }

  private void populateComponentDatabase(boolean includeLastAccessedField = true) {
    componentDatabase.instance.connect().withCloseable { db ->

      OSchema schema = db.getMetadata().getSchema()
      OClass assetType = schema.createClass(ASSET_CLASS)

      assetType.createProperty(P_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      assetType.createProperty(P_FORMAT, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      if (includeLastAccessedField) {
        assetType.createProperty(P_LAST_ACCESSED, OType.DATETIME)
      }
      assetType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)

      ODocument document = db.newInstance(ASSET_CLASS)
      document.field(P_NAME, 'name')
      document.field(P_FORMAT, 'format')
      document.field(P_LAST_ACCESSED, LAST_ACCESSED_TIMESTAMP)
      document.save()
    }
  }
}
