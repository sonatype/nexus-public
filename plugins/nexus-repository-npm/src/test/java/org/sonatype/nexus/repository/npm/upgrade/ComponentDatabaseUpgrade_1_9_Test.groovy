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
package org.sonatype.nexus.repository.npm.upgrade

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
import static org.hamcrest.Matchers.anyOf
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue
import static org.junit.Assert.assertThat

class ComponentDatabaseUpgrade_1_9_Test
    extends TestSupport
{
  static final String REPOSITORY_CLASS = new OClassNameBuilder()
      .type('repository')
      .build()

  static final String I_REPOSITORY_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(REPOSITORY_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  static final String BUCKET_CLASS = new OClassNameBuilder()
      .type('bucket')
      .build()

  static final String I_BUCKET_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(BUCKET_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  static final String P_REPOSITORY_NAME = 'repository_name'

  static final String P_RECIPE_NAME = 'recipe_name'

  static final String P_ATTRIBUTES = 'attributes'

  static final String NPM_PROXY_REPOSITORY = 'npmProxy'

  static final String NPM_HOSTED_REPOSITORY = 'npmHosted'

  static final String NPM_GROUP_REPOSITORY = 'npmGroup'

  static final String MAVEN_PROXY_REPOSITORY = 'mavenProxy'

  static final String NPM_V1_SEARCH_UNSUPPORTED = 'npm_v1_search_unsupported'

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config")

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component")

  ComponentDatabaseUpgrade_1_9 underTest

  @Before
  void setUp() {
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

      repository(NPM_PROXY_REPOSITORY, 'npm-proxy')
      repository(NPM_HOSTED_REPOSITORY, 'npm-hosted')
      repository(NPM_GROUP_REPOSITORY, 'npm-group')
      repository(MAVEN_PROXY_REPOSITORY, 'maven-proxy')
    }

    componentDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      OSchema schema = db.getMetadata().getSchema()

      OClass bucketType = schema.createClass(BUCKET_CLASS)
      bucketType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      bucketType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
          .setNotNull(true)
      bucketType.createIndex(I_BUCKET_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)

      bucket(NPM_PROXY_REPOSITORY)
      bucket(NPM_HOSTED_REPOSITORY)
      bucket(NPM_GROUP_REPOSITORY)
      bucket(MAVEN_PROXY_REPOSITORY)
    }

    underTest = new ComponentDatabaseUpgrade_1_9(configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider())
  }

  @Test
  void 'existing npm proxy and hosted repositories are flagged regarding V1 search compatibility'() {
    componentDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      assertRepositoryFlagged(db, NPM_PROXY_REPOSITORY, false)
      assertRepositoryFlagged(db, NPM_HOSTED_REPOSITORY, false)
      assertRepositoryFlagged(db, NPM_GROUP_REPOSITORY, false)
      assertRepositoryFlagged(db, MAVEN_PROXY_REPOSITORY, false)
    }

    underTest.apply()

    componentDatabase.instance.connect().withCloseable { ODatabaseDocumentTx db ->
      assertRepositoryFlagged(db, NPM_PROXY_REPOSITORY, true)
      assertRepositoryFlagged(db, NPM_HOSTED_REPOSITORY, true)
      assertRepositoryFlagged(db, NPM_GROUP_REPOSITORY, false)
      assertRepositoryFlagged(db, MAVEN_PROXY_REPOSITORY, false)
    }
  }

  private static void repository(final String name, final String recipe) {
    ODocument repository = new ODocument(REPOSITORY_CLASS)
    repository.field(P_REPOSITORY_NAME, name)
    repository.field(P_RECIPE_NAME, recipe)
    repository.save()
  }

  private static void bucket(final String name) {
    ODocument bucket = new ODocument(BUCKET_CLASS)
    bucket.field(P_REPOSITORY_NAME, name)
    bucket.field(P_ATTRIBUTES, emptyMap())
    bucket.save()
  }

  private static void assertRepositoryFlagged(final ODatabaseDocumentTx tx, final String repositoryName,
                                              final boolean flagged)
  {
    List<ODocument> documents = tx.
        query(new OSQLSynchQuery<ODocument>('select * from bucket where repository_name = ?'), repositoryName)
    assertThat(documents, hasSize(1))
    documents.each { ODocument document ->
      Map<String, Object> map = document.field(P_ATTRIBUTES, Map)
      Boolean flag = (Boolean) map.get(NPM_V1_SEARCH_UNSUPPORTED)
      if (flagged) {
        assertThat(flag, is(flagged))
      }
      else {
        assertThat(flag, anyOf(is(nullValue()), is(flagged)))
      }
    }
  }
}
