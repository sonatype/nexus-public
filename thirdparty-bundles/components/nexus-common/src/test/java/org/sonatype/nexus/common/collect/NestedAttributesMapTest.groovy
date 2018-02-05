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
import static org.sonatype.nexus.common.collect.NestedAttributesMap.SEPARATOR

/**
 * Tests for {@link org.sonatype.nexus.common.collect.NestedAttributesMap}.
 */
class NestedAttributesMapTest
  extends TestSupport
{
  private NestedAttributesMap underTest

  @Before
  void setUp() {
    underTest = new NestedAttributesMap('foo', [:])
  }

  @Test
  void 'parentKey null when no parent'() {
    assert underTest.key == 'foo'
    assert underTest.parentKey == null
  }

  @Test
  void 'parentKey includes grandparent'() {
    NestedAttributesMap parent = underTest.child('bar')
    NestedAttributesMap child = parent.child('baz')

    assert underTest.key == 'foo'
    assert parent.key == 'bar'
    assert child.key == 'baz'
    assert "foo${SEPARATOR}bar" == child.parentKey
  }

  @Test
  void 'qualifiedKey w/o parent returns key'() {
    assert "foo" == underTest.qualifiedKey
  }

  @Test
  void 'qualifiedKey includes parent'() {
    assert "foo${SEPARATOR}bar" == underTest.child('bar').qualifiedKey
    assert "foo${SEPARATOR}bar${SEPARATOR}baz" == underTest.child('bar').child('baz').qualifiedKey
  }

  @Test
  void 'set with map value fails'() {
    try {
      underTest.set('invalid', [:])
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  void 'child() with non-map value fails'() {
    underTest.set('value', false)

    try {
      underTest.child('value')
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  void 'child require includes parent key'() {
    try {
      underTest.child('bar').require('baz')
      fail()
    }
    catch (Exception e) {
      log e.message
      assert e.message.contains('baz')
      assert e.message.contains("foo${SEPARATOR}bar")
    }

    try {
      underTest.child('bar').child('qux').require('baz')
      fail()
    }
    catch (Exception e) {
      log e.message
      assert e.message.contains('baz')
      assert e.message.contains("foo${SEPARATOR}bar${SEPARATOR}qux")
    }
  }
}
