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
package org.sonatype.nexus.quartz.internal.orient

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.hamcrest.Matchers.startsWith

/**
 * Tests for {@link MarshalledEntityAdapter}.
 */
class MarshalledEntityAdapterTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  @Rule
  public ExpectedException expected = ExpectedException.none()

  private MarshalledEntityAdapter underTest

  @Before
  void setUp() {
    underTest = new TestMarshalledEntityAdapter()

    database.instance.connect().withCloseable {db ->
      underTest.register(db)

      // disable validation of these fields so we corrupt them later on
      def schema = db.metadata.schema.getClass('test_marshalled_entity')
      schema.getProperty('value_type').notNull = false
      schema.getProperty('value_data').notNull = false
    }
  }

  @Test
  void 'missing value_type throws clear exception'() {
    def entity = new TestMarshalledEntity()
    entity.setValue(new TestMarshalledValue())

    // persist and corrupt data
    database.instance.connect().withCloseable {db ->
      underTest.addEntity(db, entity).field('value_type', null).save()
    }

    expected.expect(IllegalStateException)
    expected.expectMessage(startsWith('Marshalled document missing value_type: '))

    // attempt to restore corrupted data
    database.instance.connect().withCloseable {db ->
      underTest.read(db, EntityHelper.id(entity))
    }
  }


  @Test
  void 'missing value_data throws clear exception'() {
    def entity = new TestMarshalledEntity()
    entity.setValue(new TestMarshalledValue())

    // persist and corrupt data
    database.instance.connect().withCloseable {db ->
      underTest.addEntity(db, entity).field('value_data', null).save()
    }

    expected.expect(IllegalStateException)
    expected.expectMessage(startsWith('Marshalled document missing value_data: '))

    // attempt to restore corrupted data
    database.instance.connect().withCloseable {db ->
      underTest.read(db, EntityHelper.id(entity))
    }
  }
}
