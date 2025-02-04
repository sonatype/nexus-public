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

import com.google.common.collect.Maps;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for the {@link ImmutableNestedAttributesMap}
 */
public class ImmutableNestedAttributesMapTest
{
  private ImmutableNestedAttributesMap map = new ImmutableNestedAttributesMap(null, "key", Maps.newHashMap());

  @Test(expected = UnsupportedOperationException.class)
  public void classKeysUnsettable() {
    map.set(Integer.class, 15);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void stringKeysUnsettable() {
    map.set("key", "value");
  }

  @Test
  public void nonExistentChildrenAreNavigable() {
    final NestedAttributesMap nonexistent = map.child("nonexistent");
    assertThat(nonexistent, is(notNullValue()));
    assertThat(map.backing().isEmpty(), is(true));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void navigableChildrenAreUnmodifiable() {
    map.child("nonexistent").set("key", "value");
  }
}
