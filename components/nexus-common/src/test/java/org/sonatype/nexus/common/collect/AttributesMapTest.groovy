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
package org.sonatype.nexus.common.collect

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.fail

/**
 * Tests for {@link org.sonatype.nexus.common.collect.AttributesMap}.
 */
class AttributesMapTest
  extends TestSupport
{
  private AttributesMap underTest

  @Before
  void setUp() {
    underTest = new AttributesMap()
  }

  @Test
  void 'set null removes'() {
    underTest.set('foo', 'bar')
    underTest.set('foo', null)
    assert !underTest.contains('foo')
  }

  @Test
  void 'required value'() {
    underTest.set('foo', 'bar')
    def value = underTest.require('foo')
    assert value == 'bar'

    try {
      underTest.require('baz')
      fail()
    }
    catch (Exception e) {
      // expected
    }
  }

  @Test
  void 'contains value'() {
    underTest.set('foo', 'bar')
    assert underTest.contains('foo')
    assert !underTest.contains('baz')
  }

  @Test
  void 'size empty and clear'() {
    assert underTest.isEmpty()
    assert underTest.size() == 0

    underTest.set('foo', 'bar')
    log underTest
    assert underTest.size() == 1

    underTest.set('baz', 'ick')
    log underTest
    assert underTest.size() == 2

    underTest.clear()
    log underTest
    assert underTest.isEmpty()
    assert underTest.size() == 0
  }

  @Test
  void 'typed accessors'() {
    def value

    underTest.set('foo', 1)
    value = underTest.get('foo', Integer.class)
    assert value.class == Integer.class
    assert value == 1

    underTest.set('foo', false)
    value = underTest.get('foo', Boolean.class)
    assert value.class == Boolean.class
    assert value == false
  }

  // private to test creation with accessible=true
  private static class GetOrCreateAttribute
  {
    // empty
  }

  @Test
  void 'get or create'() {
    assert !underTest.contains(GetOrCreateAttribute.class)
    def value = underTest.getOrCreate(GetOrCreateAttribute.class)
    assert value != null;
    assert underTest.contains(GetOrCreateAttribute.class)
  }

  @Test
  void 'handle Date as Date or Long'() {
    Date testDate = new Date()
    Long testDateLong = testDate.getTime()

    underTest.set('foo', testDate)
    underTest.set('bar', testDateLong)

    assert underTest.get('foo', Date.class) == testDate
    assert underTest.get('bar', Date.class) == testDate
  }
}
