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
package org.sonatype.nexus.repository.maven.upgrade

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.OClassNameBuilder
import org.sonatype.nexus.orient.OIndexNameBuilder
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.index.OIndex
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static org.junit.Assert.assertThat

class MavenUpgrade_1_1_Test
    extends TestSupport
{
  static final String REPOSITORY_CLASS = new OClassNameBuilder()
      .type("repository")
      .build()

  static final String I_REPOSITORY_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(REPOSITORY_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  static final String BUCKET_CLASS = new OClassNameBuilder()
      .type("bucket")
      .build()

  static final String I_BUCKET_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(BUCKET_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  static final String ASSET_CLASS = new OClassNameBuilder()
      .type("asset")
      .build()

  static final String I_ASSET_NAME = new OIndexNameBuilder()
      .type(ASSET_CLASS)
      .property(P_NAME)
      .build()

  private static final String P_NAME = "name"

  private static final String P_FORMAT = "format"

  private static final String P_ATTRIBUTES = "attributes"

  private static final String P_BUCKET = "bucket"

  private static final String P_REPOSITORY_NAME = "repository_name"

  private static final String P_RECIPE_NAME = "recipe_name"

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config")

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component")

  MavenUpgrade_1_1 underTest

  @Before
  void setUp() {
    configDatabase.instance.connect().withCloseable { db ->
      OSchema schema = db.getMetadata().getSchema()

      // repository
      def repositoryType = schema.createClass(REPOSITORY_CLASS)
      repositoryType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setCollate(new OCaseInsensitiveCollate())
          .setMandatory(true)
          .setNotNull(true)
      repositoryType.createProperty(P_RECIPE_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      repositoryType.createIndex(I_REPOSITORY_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)

      repository('maven2Hosted', 'maven2-hosted')
    }

    componentDatabase.instance.connect().withCloseable { db ->
      OSchema schema = db.getMetadata().getSchema()

      // bucket
      def bucketType = schema.createClass(BUCKET_CLASS)
      bucketType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      bucketType.createIndex(I_BUCKET_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)

      bucket('maven2Hosted')

      // asset
      def assetType = schema.createClass(ASSET_CLASS)

      assetType.createProperty(P_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      assetType.createProperty(P_FORMAT, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      assetType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
      assetType.createIndex(I_ASSET_NAME, INDEX_TYPE.UNIQUE, P_NAME)

      // create some test data
      OIndex<?> bucketIdx = db.getMetadata().getIndexManager().getIndex(I_BUCKET_REPOSITORY_NAME)
      asset(bucketIdx, 'maven2Hosted', 'asm/asm/3.1/asm-3.1.jar', 'ARTIFACT')
      asset(bucketIdx, 'maven2Hosted', 'asm/asm/3.1/asm-3.1.pom', 'ARTIFACT')
      asset(bucketIdx, 'maven2Hosted', 'asm/asm/3.1/asm-3.1.jar.sha1', 'ARTIFACT_SUBORDINATE')
      asset(bucketIdx, 'maven2Hosted', 'asm/asm/3.1/asm-3.1.pom.sha1', 'ARTIFACT_SUBORDINATE')
      asset(bucketIdx, 'maven2Hosted', '.index/nexus-maven-repository-index.gz', 'OTHER')
      asset(bucketIdx, 'maven2Hosted', '.index/nexus-maven-repository-index.properties', 'OTHER')
    }

    underTest = new MavenUpgrade_1_1(configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider())
  }

  @Test
  void 'upgrade step updates asset_kind'() {
    assertAssetKinds('OTHER')

    underTest.apply()

    assertAssetKinds('REPOSITORY_INDEX')
  }

  @Test
  void 'upgrade step can be called multiple times'() {
    assertAssetKinds('OTHER')

    underTest.apply()
    underTest.apply()

    assertAssetKinds('REPOSITORY_INDEX')
  }

  private assertAssetKinds(String expectedIndexFileAssetKind) {
    componentDatabase.instance.connect().withCloseable { db ->
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_ASSET_NAME)
      assertAssetKind(idx, 'asm/asm/3.1/asm-3.1.jar', 'ARTIFACT')
      assertAssetKind(idx, 'asm/asm/3.1/asm-3.1.pom', 'ARTIFACT')
      assertAssetKind(idx, 'asm/asm/3.1/asm-3.1.jar.sha1', 'ARTIFACT_SUBORDINATE')
      assertAssetKind(idx, 'asm/asm/3.1/asm-3.1.pom.sha1', 'ARTIFACT_SUBORDINATE')
      assertAssetKind(idx, '.index/nexus-maven-repository-index.gz', expectedIndexFileAssetKind)
      assertAssetKind(idx, '.index/nexus-maven-repository-index.properties', expectedIndexFileAssetKind)
    }
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
    asset.field(P_FORMAT, 'maven2')
    asset.field(P_ATTRIBUTES, [maven2: [asset_kind: assetKind]])
    asset.save()
  }

  private static assertAssetKind(OIndex idx, String name, String assetKind) {
    OIdentifiable idf = idx.get(name)
    assertThat(idf, notNullValue())
    ODocument asset = idf.record
    assertThat(asset, notNullValue())
    assertThat(asset.field(P_ATTRIBUTES)['maven2']['asset_kind'], equalTo(assetKind))
  }
}
