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
package org.sonatype.nexus.common.collect;

import java.util.Date;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link org.sonatype.nexus.common.collect.AttributesMap}.
 */
public class AttributesMapTest
    extends TestSupport
{
  private AttributesMap underTest;

  @Before
  public void setUp() {
    underTest = new AttributesMap();
  }

  @Test
  public void testSetNullRemoves() {
    underTest.set("foo", "bar");
    underTest.set("foo", null);
    assertFalse(underTest.contains("foo"));
  }

  @Test
  public void testRequiredValue() {
    underTest.set("foo", "bar");
    assertEquals("bar", underTest.require("foo"));

    assertThrows(Exception.class, () -> {
      underTest.require("baz");
    });
  }

  @Test
  public void testContainsValue() {
    underTest.set("foo", "bar");
    assertTrue(underTest.contains("foo"));
    assertFalse(underTest.contains("baz"));
  }

  @Test
  public void testSizeEmptyAndClear() {
    assertTrue(underTest.isEmpty());
    assertEquals(0, underTest.size());

    underTest.set("foo", "bar");
    log(underTest);
    assertEquals(1, underTest.size());

    underTest.set("baz", "ick");
    log(underTest);
    assertEquals(2, underTest.size());

    underTest.clear();
    log(underTest);
    assertTrue(underTest.isEmpty());
    assertEquals(0, underTest.size());
  }

  @Test
  public void testTypedAccessors() {
    underTest.set("foo", 1);
    Object integerValue = underTest.get("foo", Integer.class);
    assertNotNull(integerValue);
    assertEquals(Integer.class, integerValue.getClass());
    assertEquals(1, integerValue);

    underTest.set("foo", false);
    Object booleanValue = underTest.get("foo", Boolean.class);
    assertNotNull(booleanValue);
    assertEquals(Boolean.class, booleanValue.getClass());
    assertEquals(false, booleanValue);
  }

  // private to test creation with accessible=true
  private static class GetOrCreateAttribute
  {
    // empty
  }

  @Test
  public void testGetOrCreate() {
    assertFalse(underTest.contains(GetOrCreateAttribute.class));
    Object value = underTest.getOrCreate(GetOrCreateAttribute.class);
    assertNotNull(value);
    assertTrue(underTest.contains(GetOrCreateAttribute.class));
  }

  @Test
  public void testHandleDateAsDateOrLong() {
    Date testDate = new Date();
    Long testDateLong = testDate.getTime();

    underTest.set("foo", testDate);
    underTest.set("bar", testDateLong);

    assertEquals(testDate, underTest.get("foo", Date.class));
    assertEquals(testDate, underTest.get("bar", Date.class));
  }

  @Test
  public void testHandleBooleanStrings() {
    underTest.set("test", "true");
    assertEquals(true, underTest.get("test", Boolean.class));

    underTest.set("test", "false");
    assertEquals(false, underTest.get("test", Boolean.class));

    underTest.set("test", "notABooleanString");
    assertEquals(false, underTest.get("test", Boolean.class));

    underTest.set("test", 123);
    assertNull(underTest.get("test", Boolean.class));
  }

  private Object testComputeFunction(Object test) {
    if (test instanceof Integer) {
      return (Integer) test + 1;
    }
    return 1;
  }

  @Test
  public void testCompute() {
    Object result = underTest.compute("foo", this::testComputeFunction);
    assertNull(result);
    assertEquals(1, underTest.get("foo"));
    result = underTest.compute("foo", this::testComputeFunction);
    assertEquals(1, result);
    assertEquals(2, underTest.get("foo"));
    result = underTest.compute("foo", this::testComputeFunction);
    assertEquals(2, result);
    assertEquals(3, underTest.get("foo"));
    result = underTest.compute("foo", this::testComputeFunction);
    assertEquals(3, result);
    assertEquals(4, underTest.get("foo"));
  }
}
