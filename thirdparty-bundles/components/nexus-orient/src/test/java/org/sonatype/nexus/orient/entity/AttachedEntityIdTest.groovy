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
package org.sonatype.nexus.orient.entity

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.orient.HexRecordIdObfuscator
import org.sonatype.nexus.orient.RecordIdObfuscator

import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OClass
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.junit.Assert.fail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Tests for {@link AttachedEntityId}
 */
class AttachedEntityIdTest
    extends TestSupport
{
  @Mock
  EntityAdapter entityAdapter

  RecordIdObfuscator recordIdObfuscator

  @Before
  void setUp() {
    when(entityAdapter.schemaType).thenReturn(mock(OClass.class, 'oclass'))
    recordIdObfuscator = new HexRecordIdObfuscator()
    when(entityAdapter.recordIdObfuscator).thenReturn(recordIdObfuscator)
  }

  @Test
  void 'human representation'() {
    AttachedEntityId a = new AttachedEntityId(entityAdapter, new ORecordId(1, 1))
    def str = a.toString()
    println str
    assert str != null
  }

  @Test
  void 'value externalization'() {
    AttachedEntityId a = new AttachedEntityId(entityAdapter, new ORecordId(1, 1))
    // > 1 to verify only 1 encoding
    println a.value
    assert a.value != null
    assert a.value != null
    verify(entityAdapter, times(1)).recordIdObfuscator
  }

  @Test
  void 'value externalization with temporary'() {
    AttachedEntityId a = new AttachedEntityId(entityAdapter, new ORecordId(1, -2))
    try {
      a.value
      fail()
    }
    catch (IllegalStateException e) {
      // ignore
    }
  }

  @Test
  void 'attached equality'() {
    AttachedEntityId a = new AttachedEntityId(entityAdapter, new ORecordId(1, 1))
    assert a.equals(a)
    assert a.equals(new AttachedEntityId(entityAdapter, new ORecordId(1, 1)))

    AttachedEntityId b = new AttachedEntityId(entityAdapter, new ORecordId(1, 2))
    assert !a.equals(b)
    assert !b.equals(a)

    // attached equality should not obfuscate
    verify(entityAdapter, times(0)).recordIdObfuscator
  }

  @SuppressWarnings("GrEqualsBetweenInconvertibleTypes")
  @Test
  void 'detached equality'() {
    ORID rid1 = new ORecordId(1, 1)
    EntityId a = new AttachedEntityId(entityAdapter, rid1)
    EntityId b = new DetachedEntityId(recordIdObfuscator.encode(mock(OClass.class), rid1))
    assert a.equals(b)
    assert b.equals(a)

    ORID rid2 = new ORecordId(1, 2)
    EntityId c = new DetachedEntityId(recordIdObfuscator.encode(mock(OClass.class), rid2))
    assert !a.equals(c)
    assert !c.equals(a)
  }
}
