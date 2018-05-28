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
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.Matchers.containsInAnyOrder
import static org.junit.Assert.assertThat
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME
import static org.sonatype.nexus.repository.upgrade.ComponentDatabaseUpgrade_1_4.ASSET_CLASS
import static org.sonatype.nexus.repository.upgrade.ComponentDatabaseUpgrade_1_4.COMPONENT_CLASS
import static org.sonatype.nexus.repository.upgrade.ComponentDatabaseUpgrade_1_4.I_ASSET_NAME
import static org.sonatype.nexus.repository.upgrade.ComponentDatabaseUpgrade_1_4.I_COMPONENT
import static org.sonatype.nexus.repository.upgrade.ComponentDatabaseUpgrade_1_4.I_COMPONENT_GROUP_NAME_VERSION
import static org.sonatype.nexus.repository.upgrade.ComponentDatabaseUpgrade_1_4.I_NAME_CASE_INSENSITIVE

class ComponentDatabaseUpgrade_1_4_Test
    extends TestSupport
{

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component")

  ComponentDatabaseUpgrade_1_4 underTest

  @Before
  void setUp() {
    underTest = new ComponentDatabaseUpgrade_1_4(componentDatabase.getInstanceProvider())
  }

  @Test
  void 'upgrade step creates indices when absent'() {
    populateComponentDatabase()

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertThat(indicesFor(db, ASSET_CLASS), containsInAnyOrder(I_COMPONENT, I_ASSET_NAME))
      assertThat(indicesFor(db, COMPONENT_CLASS),
          containsInAnyOrder(I_NAME_CASE_INSENSITIVE, I_COMPONENT_GROUP_NAME_VERSION))
    }
  }

  @Test
  void 'upgrade step works even if indices are present'() {
    populateComponentDatabase()

    underTest.apply()

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { db ->
      assertThat(indicesFor(db, ASSET_CLASS), containsInAnyOrder(I_COMPONENT, I_ASSET_NAME))
      assertThat(indicesFor(db, COMPONENT_CLASS),
          containsInAnyOrder(I_NAME_CASE_INSENSITIVE, I_COMPONENT_GROUP_NAME_VERSION))
    }
  }

  @Test
  void 'upgrade step does not throw exceptions if asset class and component class are not found in schema'() {
    underTest.apply()
  }

  private static indicesFor(final ODatabaseDocumentTx db, final String type) {
    db.metadata.schema.getClass(type).indexes.collect { it.name }
  }

  private void populateComponentDatabase() {
    componentDatabase.instance.connect().withCloseable { db ->

      OSchema schema = db.getMetadata().getSchema()

      OClass componentType = schema.createClass(COMPONENT_CLASS)
      componentType.createProperty(P_NAME, OType.STRING)

      OClass assetType = schema.createClass(ASSET_CLASS)
      assetType.createProperty(P_COMPONENT, OType.LINK, componentType)
    }
  }
}
