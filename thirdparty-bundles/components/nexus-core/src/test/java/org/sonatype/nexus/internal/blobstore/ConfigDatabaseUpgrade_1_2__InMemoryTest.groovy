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
package org.sonatype.nexus.internal.blobstore

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.OClassNameBuilder
import org.sonatype.nexus.orient.OIndexNameBuilder
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.security.PasswordHelper

import com.google.common.annotations.VisibleForTesting
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock

/**
 * Tests {@link ConfigDatabaseUpgrade_1_2} with an in memory database to ensure adding case insensitive collation to the
 * name property doesn't cause errors.
 */
class ConfigDatabaseUpgrade_1_2__InMemoryTest
    extends TestSupport
{

  static final String DB_CLASS = new OClassNameBuilder()
      .prefix("repository")
      .type("blobstore")
      .build()

  private static final String P_NAME = "name"

  private static final String P_TYPE = "type"

  private static final String P_ATTRIBUTES = "attributes"

  @VisibleForTesting
  static final String I_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .build()

  private static final String QUARTZ_JOB_DETAIL_CLASS = new OClassNameBuilder()
      .prefix("quartz")
      .type("job_detail")
      .build();

  @Mock
  PasswordHelper passwordHelper;

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test")

  ConfigDatabaseUpgrade_1_2 underTest

  @Before
  public void setup() {
    //setup the schema as it was prior to the migration
    database.instance.connect().withCloseable { db ->
      OSchema schema = db.getMetadata().getSchema()
      def type = schema.createClass(DB_CLASS)

      type.createProperty(P_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      type.createProperty(P_TYPE, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      type.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
          .setMandatory(true)
          .setNotNull(true)
      type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME)

      schema.createClass(QUARTZ_JOB_DETAIL_CLASS)
      schema.createClass("repository")
    }
    underTest = new ConfigDatabaseUpgrade_1_2(database.getInstanceProvider(), database.getInstanceProvider())
  }

  @Test
  public void 'upgrade step can safely be re-run on already upgraded database'() {
    underTest.apply()

    //db shouldn't care if we make the same change twice
    underTest.apply()
  }
}
