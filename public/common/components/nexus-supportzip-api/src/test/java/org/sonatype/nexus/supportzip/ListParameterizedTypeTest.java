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
package org.sonatype.nexus.supportzip;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ListParameterizedType}
 */
public class ListParameterizedTypeTest
{
  @Test
  public void testGetActualTypeArguments() {
    ListParameterizedType underTest = new ListParameterizedType(String.class);

    Type[] actualTypeArguments = underTest.getActualTypeArguments();

    assertThat(actualTypeArguments.length, is(1));
    assertThat(actualTypeArguments[0], is((Type) String.class));
  }

  @Test
  public void testGetRawType() {
    ListParameterizedType underTest = new ListParameterizedType(String.class);

    assertThat(underTest.getRawType(), is((Type) ArrayList.class));
  }

  @Test
  public void testGetOwnerType() {
    ListParameterizedType underTest = new ListParameterizedType(String.class);

    assertThat(underTest.getOwnerType(), is(nullValue()));
  }

  @Test
  public void testHashCodeMatchesObjectsHash() {
    ListParameterizedType underTest = new ListParameterizedType(String.class);

    assertThat(underTest.hashCode(), is(Objects.hash(String.class)));
  }

  @Test
  public void testHashCodeEqualForSameType() {
    ListParameterizedType first = new ListParameterizedType(String.class);
    ListParameterizedType second = new ListParameterizedType(String.class);

    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void testEqualsSameInstance() {
    ListParameterizedType underTest = new ListParameterizedType(String.class);

    assertThat(underTest, sameInstance(underTest));
    assertTrue(underTest.equals(underTest));
  }

  @Test
  public void testEqualsNullIsFalse() {
    ListParameterizedType underTest = new ListParameterizedType(String.class);

    assertFalse(underTest.equals(null));
  }

  @Test
  public void testEqualsDifferentClassIsFalse() {
    ListParameterizedType underTest = new ListParameterizedType(String.class);

    assertFalse(underTest.equals("not a ListParameterizedType"));
  }

  @Test
  public void testEqualsSameClassEqualTypeIsTrue() {
    ListParameterizedType first = new ListParameterizedType(String.class);
    ListParameterizedType second = new ListParameterizedType(String.class);

    assertTrue(first.equals(second));
    assertEquals(first, second);
  }

  @Test
  public void testEqualsSameClassDifferentTypeIsFalse() {
    ListParameterizedType first = new ListParameterizedType(String.class);
    ListParameterizedType second = new ListParameterizedType(Integer.class);

    assertFalse(first.equals(second));
  }

  @Test
  public void testGetActualTypeArgumentsReflectsConstructorArgument() {
    ListParameterizedType underTest = new ListParameterizedType(Integer.class);

    Type[] actualTypeArguments = underTest.getActualTypeArguments();

    assertThat(actualTypeArguments.length, is(1));
    assertThat(actualTypeArguments[0], is((Type) Integer.class));
  }

  @Test
  public void testHashCodeDiffersForDifferentType() {
    ListParameterizedType first = new ListParameterizedType(String.class);
    ListParameterizedType second = new ListParameterizedType(Integer.class);

    assertThat(second.hashCode(), is(Objects.hash(Integer.class)));
    assertThat(first.hashCode(), is(not(second.hashCode())));
  }

  @Test
  public void testNullTypeEqualityAndArguments() {
    ListParameterizedType first = new ListParameterizedType(null);
    ListParameterizedType second = new ListParameterizedType(null);

    assertThat(first.getActualTypeArguments().length, is(1));
    assertThat(first.getActualTypeArguments()[0], is(nullValue()));
    assertTrue(first.equals(second));
    assertThat(first.hashCode(), is(Objects.hash((Object) null)));
  }
}
