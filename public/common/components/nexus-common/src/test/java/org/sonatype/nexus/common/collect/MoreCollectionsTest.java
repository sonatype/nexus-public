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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoreCollectionsTest
{
  @Test
  void preservesEncounterOrderFromCollection() {
    assertThat(MoreCollections.toImmutableList(List.of("a", "b", "c")), contains("a", "b", "c"));
  }

  @Test
  void preservesEncounterOrderFromNonCollectionIterable() {
    Iterable<String> iterable = () -> List.of("x", "y", "z").iterator();
    assertThat(MoreCollections.toImmutableList(iterable), contains("x", "y", "z"));
  }

  @Test
  void resultIsImmutable() {
    List<String> result = MoreCollections.toImmutableList(List.of("a"));
    assertThrows(UnsupportedOperationException.class, () -> result.add("b"));
    assertThrows(UnsupportedOperationException.class, () -> result.set(0, "b"));
    assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
  }

  @Test
  void resultToleratesNullQuery() {
    // Guava ImmutableList tolerated contains(null); the JDK wrapper must too (List.of/copyOf would throw NPE).
    assertThat(MoreCollections.toImmutableList(List.of("a")).contains(null), is(false));
  }

  @Test
  void emptyIterableYieldsEmptyList() {
    Iterable<String> iterable = () -> List.<String>of().iterator();
    assertThat(MoreCollections.toImmutableList(iterable), empty());
  }

  @Test
  void resultIndexOfNullReturnsMinusOne() {
    assertThat(MoreCollections.toImmutableList(List.of("a")).indexOf(null), is(-1));
  }

  @Test
  void resultIsDetachedSnapshotOfCollectionSource() {
    List<String> source = new ArrayList<>(List.of("a", "b"));
    List<String> result = MoreCollections.toImmutableList(source);

    source.add("c");
    source.set(0, "changed");

    assertThat(result, contains("a", "b"));
    source.clear();
    assertThat(result, contains("a", "b"));
  }

  @Test
  void resultIsDetachedSnapshotOfNonCollectionIterableSource() {
    List<String> backing = new ArrayList<>(List.of("x", "y"));
    Iterable<String> iterable = backing::iterator;
    List<String> result = MoreCollections.toImmutableList(iterable);

    backing.add("z");
    backing.set(0, "changed");

    assertThat(result, contains("x", "y"));
    backing.clear();
    assertThat(result, contains("x", "y"));
  }

  @Test
  void nullIterableThrows() {
    assertThrows(NullPointerException.class, () -> MoreCollections.toImmutableList(null));
  }

  @Test
  void nullSourceElementsAreAcceptedAndContainsNullReturnsTrue() {
    // Unlike Guava ImmutableList.copyOf(), null elements in the source are accepted.
    // When a null element is present, contains(null) returns true (standard List contract).
    List<String> result = MoreCollections.toImmutableList(Arrays.asList("a", null, "b"));
    assertThat(result.get(1), is((String) null));
    assertThat(result.contains(null), is(true));
  }
}
