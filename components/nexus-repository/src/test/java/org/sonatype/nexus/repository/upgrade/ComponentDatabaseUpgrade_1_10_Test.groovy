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
import org.sonatype.nexus.orient.OIndexNameBuilder
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static java.util.Collections.emptyMap
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat
import static org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule.inMemory

class ComponentDatabaseUpgrade_1_10_Test
    extends TestSupport
{
  static final String YUM_PROXY_REPOSITORY = 'yumProxy'

  static final String YUM_PROXY_REPOSITORY_2 = 'yumProxy2'

  static final String MAVEN_PROXY_REPOSITORY = 'mavenProxy'

  static final String FORMAT = 'yum'

  static final String COMPONENT_NAME = 'GeoIP'

  static final String RPM_VERSION = '1.5.0'

  static final String RPM_RELEASE = '11.el7'

  static final String COMPONENT_FULL_VERSION = RPM_VERSION + '-' + RPM_RELEASE

  static final String ASSET_1_NAME = 'GeoIP-1.5.0-11.el7.x86_64'

  static final String ASSET_2_NAME = 'GeoIP-1.5.0-11.el7.i686'

  static final String P_REPOSITORY_NAME = 'repository_name'

  static final String P_RECIPE_NAME = 'recipe_name'

  static final String P_ATTRIBUTES = 'attributes'

  static final String P_FORMAT = 'format'

  static final String P_BUCKET = 'bucket'

  static final String P_NAME = 'name'

  static final String P_VERSION = 'version'

  static final String P_COMPONENT = 'component'

  static final String REPOSITORY_CLASS = new OClassNameBuilder()
      .type('repository')
      .build()

  static final String BUCKET_CLASS = new OClassNameBuilder()
      .type('bucket')
      .build()

  static final String COMPONENT_CLASS = new OClassNameBuilder()
      .type('component')
      .build()

  static final String ASSET_CLASS = new OClassNameBuilder()
      .type('asset')
      .build()

  static final String I_REPOSITORY_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(REPOSITORY_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  static final String I_BUCKET_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(BUCKET_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  @Rule
  public DatabaseInstanceRule configDatabase = inMemory('test_config')

  @Rule
  public DatabaseInstanceRule componentDatabase = inMemory('test_component')

  ComponentDatabaseUpgrade_1_10 underTest

  @Before
  void setUp() {
    createAndPopulateConfigDatabase()

    createComponentDatabase()
    populateComponentDatabase(YUM_PROXY_REPOSITORY)

    underTest = new ComponentDatabaseUpgrade_1_10(configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider())
  }

  @Test
  void 'yum component is updated with new name and version'() {
    underTest.apply()

    componentDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      List<ODocument> components = db.query(new OSQLSynchQuery<ODocument>('select * from component'))

      assertThat(components.size(), is(equalTo(1)))

      verifyComponent(db, components.get(0))
    }
  }

  @Test
  void 'process multiple repositories'() {
    configDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      createRepository(YUM_PROXY_REPOSITORY_2, 'yum-proxy')
    }

    populateComponentDatabase(YUM_PROXY_REPOSITORY_2)

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      List<ODocument> components = db.query(new OSQLSynchQuery<ODocument>('select * from component'))

      assertThat(components.size(), is(equalTo(2)))

      verifyComponent(db, components.get(0))
      verifyComponent(db, components.get(1))
    }
  }

  static void verifyComponent(final ODatabaseDocumentTx db, final ODocument component) {
    List<ODocument> assets = findAssetsForComponent(db, component)

    assertThat(assets.size(), is(equalTo(2)))

    String name = component.field(P_NAME)
    String version = component.field(P_VERSION)

    assertThat(name, is(equalTo(COMPONENT_NAME)))
    assertThat(version, is(equalTo(COMPONENT_FULL_VERSION)))
    assertThat(assets.get(0).field(P_NAME), is(equalTo(ASSET_1_NAME)))
    assertThat(assets.get(1).field(P_NAME), is(equalTo(ASSET_2_NAME)))
  }

  void createComponentDatabase() {
    componentDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      OSchema schema = db.getMetadata().getSchema()

      OClass bucketType = schema.createClass(BUCKET_CLASS)

      bucketType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)

      bucketType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
          .setNotNull(true)

      bucketType.createIndex(I_BUCKET_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)

      createBucket(MAVEN_PROXY_REPOSITORY)
    }
  }

  void populateComponentDatabase(final String repository) {
    componentDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      def yumBucket = createBucket(repository)

      def component1 = createComponent(db, ASSET_1_NAME, RPM_VERSION, yumBucket)
      createAsset(db, ASSET_1_NAME, component1, COMPONENT_NAME, RPM_VERSION, RPM_RELEASE)

      def component2 = createComponent(db, ASSET_2_NAME, RPM_VERSION, yumBucket)
      createAsset(db, ASSET_2_NAME, component2, COMPONENT_NAME, RPM_VERSION, RPM_RELEASE)
    }
  }

  void createAndPopulateConfigDatabase() {
    configDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      OSchema schema = db.getMetadata().getSchema()

      OClass repositoryType = schema.createClass(REPOSITORY_CLASS)

      repositoryType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setCollate(new OCaseInsensitiveCollate())
          .setMandatory(true)
          .setNotNull(true)

      repositoryType.createProperty(P_RECIPE_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)

      repositoryType.createIndex(I_REPOSITORY_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)

      createRepository(YUM_PROXY_REPOSITORY, 'yum-proxy')
      createRepository(MAVEN_PROXY_REPOSITORY, 'maven-proxy')
    }
  }

  static void createRepository(final String name, final String recipe) {
    ODocument repository = new ODocument(REPOSITORY_CLASS)

    repository.field(P_REPOSITORY_NAME, name)
    repository.field(P_RECIPE_NAME, recipe)

    repository.save()
  }

  static ODocument createBucket(final String name) {
    ODocument bucket = new ODocument(BUCKET_CLASS)

    bucket.field(P_REPOSITORY_NAME, name)
    bucket.field(P_ATTRIBUTES, emptyMap())

    return bucket.save()
  }

  static ODocument createComponent(final ODatabaseDocumentTx db,
                                   final String name,
                                   final String version,
                                   final ODocument bucket)
  {
    ODocument document = db.newInstance(COMPONENT_CLASS)

    document.field(P_NAME, name)
    document.field(P_VERSION, version)
    document.field(P_FORMAT, FORMAT)
    document.field(P_BUCKET, bucket.identity)

    return document.save()
  }

  static void createAsset(final ODatabaseDocumentTx db,
                          final String name,
                          final ODocument component,
                          final String rpmName,
                          final String rpmVersion,
                          final String rpmRelease)
  {
    ODocument document = db.newInstance(ASSET_CLASS)

    document.field(P_NAME, name)
    document.field(P_COMPONENT, component.identity)
    document.field(P_ATTRIBUTES, ['yum': ['name': rpmName, 'version': rpmVersion, 'release': rpmRelease]])

    document.save()
  }

  static List<ODocument> findAssetsForComponent(final ODatabaseDocumentTx db, final ODocument component) {
    return db.query(new OSQLSynchQuery<ODocument>('select from asset where component = ?'), component.identity)
  }
}
