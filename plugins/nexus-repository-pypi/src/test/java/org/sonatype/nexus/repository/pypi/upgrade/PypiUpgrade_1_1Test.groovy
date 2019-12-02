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
package org.sonatype.nexus.repository.pypi.upgrade

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.OClassNameBuilder
import org.sonatype.nexus.orient.OIndexNameBuilder
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.index.OIndex
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertEquals

/**
 * @since 3.20
 */
class PypiUpgrade_1_1Test
    extends TestSupport
{
  static final String REPOSITORY_CLASS = new OClassNameBuilder().type("repository").build()

  static final String I_REPOSITORY_REPOSITORY_NAME = new OIndexNameBuilder().type(REPOSITORY_CLASS).
      property(P_REPOSITORY_NAME).build()

  static final String BUCKET_CLASS = new OClassNameBuilder().type("bucket").build()

  static final String I_BUCKET_REPOSITORY_NAME = new OIndexNameBuilder().type(BUCKET_CLASS).property(P_REPOSITORY_NAME).
      build()

  static final String ASSET_CLASS = new OClassNameBuilder().type("asset").build()

  static final String I_ASSET_NAME = new OIndexNameBuilder().type(ASSET_CLASS).property(P_NAME).build()

  private static final String P_NAME = "name"

  private static final String P_FORMAT = "format"

  private static final String P_ATTRIBUTES = "attributes"

  private static final String P_BUCKET = "bucket"

  private static final String P_REPOSITORY_NAME = "repository_name"

  private static final String P_RECIPE_NAME = "recipe_name"

  private static final String BROWSE_NODE_CLASS = new OClassNameBuilder().type("browse_node").build();

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config")

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component")

  PypiUpgrade_1_1 underTest

  @Before
  void setUp() {
    configDatabase.instance.connect().withCloseable { db ->
      OSchema schema = db.getMetadata().getSchema()
      createRepositoryType(schema)
      repository('pypiproxy', 'pypi-proxy')
      repository('pypihosted', 'pypi-hosted')
    }

    componentDatabase.instance.connect().withCloseable { db ->
      OSchema schema = db.getMetadata().getSchema()
      createBucketType(schema)
      bucket('pypiproxy')
      bucket('pypihosted')
      browseNode('pypiproxy');
      browseNode('pypihosted');
      createAssetType(schema)
      createTestAssets(db)
    }

    underTest = new PypiUpgrade_1_1(configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider())
  }

  @Test
  void 'upgrade step deletes indexes and browse nodes'() {
    assertIndexNumber(7L)
    assertBrowseNodeCountForRepository('pypiproxy', 1L)
    assertBrowseNodeCountForRepository('pypihosted', 1L)
    underTest.apply()
    assertIndexNumber(2L)
    assertAssetExists("anartifact");
    assertAssetExists("otherassetking");
    assertBrowseNodeCountForRepository('pypiproxy', 0L)
    assertBrowseNodeCountForRepository('pypihosted', 0L)
  }

  private assertBrowseNodeCountForRepository(final String repositoryName, Long expectedCount) {
    componentDatabase.instance.connect().withCloseable { db ->
      List<ODocument> result = db.command(new OCommandSQL(
          'select count(*) from browse_node where repository_name = ?')).execute(repositoryName);
      Long browseNodesCount = result.get(0).field("count");
      assertThat(expectedCount, is(browseNodesCount))
    }
  }

  private assertIndexNumber(long expectedIndexNumber) {
    componentDatabase.instance.connect().withCloseable { db ->
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_ASSET_NAME)
      assertThat(idx.getKeySize(), is(expectedIndexNumber))
    }
  }

  private assertAssetExists(String name) {
    componentDatabase.instance.connect().withCloseable { db ->
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_ASSET_NAME)
      OIdentifiable idf = idx.get(name)
      assertThat(idf, notNullValue())
      ODocument asset = idf.record
      assertThat(asset, notNullValue())
    }
  }

  private static void browseNode(final String repositoryName) {
    ODocument document = new ODocument(BROWSE_NODE_CLASS);
    document.field(P_REPOSITORY_NAME, repositoryName);
    document.save();
  }

  private static repository(String name, String recipe) {
    ODocument repository = new ODocument(REPOSITORY_CLASS)
    repository.field(P_REPOSITORY_NAME, name)
    repository.field(P_RECIPE_NAME, recipe)
    repository.save()
  }

  private static bucket(String name) {
    ODocument bucket = new ODocument(BUCKET_CLASS)
    bucket.field(P_REPOSITORY_NAME, name)
    bucket.save()
  }

  private static asset(OIndex bucketIdx, String repositoryName, String name, String assetKind) {
    OIdentifiable idf = bucketIdx.get(repositoryName)
    ODocument asset = new ODocument(ASSET_CLASS)
    asset.field(P_BUCKET, idf)
    asset.field(P_NAME, name)
    asset.field(P_FORMAT, 'pypi')
    asset.field(P_ATTRIBUTES, [pypi: [asset_kind: assetKind]])
    asset.save()
  }

  private void createTestAssets(ODatabaseDocumentTx db) {
    OIndex<?> bucketIdx = db.getMetadata().getIndexManager().getIndex(I_BUCKET_REPOSITORY_NAME)
    asset(bucketIdx, 'pypiproxy', 'asset1', 'ROOT_INDEX')
    asset(bucketIdx, 'pypiproxy', 'anartifact', 'ARTIFACT')
    asset(bucketIdx, 'pypiproxy', 'asset2', 'INDEX')
    asset(bucketIdx, 'pypiproxy', 'asset3', 'INDEX')
    asset(bucketIdx, 'pypihosted', 'asset4', 'ROOT_INDEX')
    asset(bucketIdx, 'pypihosted', 'asset5', 'INDEX')
    asset(bucketIdx, 'pypihosted', 'otherassetking', 'OTHER')
  }

  private void createAssetType(OSchemaProxy schema) {
    def assetType = schema.createClass(ASSET_CLASS)
    assetType.createProperty(P_NAME, OType.STRING).setMandatory(true).setNotNull(true)
    assetType.createProperty(P_FORMAT, OType.STRING).setMandatory(true).setNotNull(true)
    assetType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
    assetType.createIndex(I_ASSET_NAME, INDEX_TYPE.UNIQUE, P_NAME)
  }

  private void createBucketType(OSchemaProxy schema) {
    def bucketType = schema.createClass(BUCKET_CLASS)
    bucketType.createProperty(P_REPOSITORY_NAME, OType.STRING).setMandatory(true).setNotNull(true)
    bucketType.createIndex(I_BUCKET_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)
  }

  private void createRepositoryType(OSchemaProxy schema) {
    def repositoryType = schema.createClass(REPOSITORY_CLASS)
    repositoryType.createProperty(P_REPOSITORY_NAME, OType.STRING).setCollate(new OCaseInsensitiveCollate()).
        setMandatory(true).setNotNull(true)
    repositoryType.createProperty(P_RECIPE_NAME, OType.STRING).setMandatory(true).setNotNull(true)
    repositoryType.createIndex(I_REPOSITORY_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)
  }
}
