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
package org.sonatype.nexus.repository.content.fluent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.sonatype.nexus.common.entity.ContinuationAware;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

/**
 * Tests for {@link FluentContinuation}.
 */
public class FluentContinuationTest
{
  private static class Item
      implements ContinuationAware
  {
    private final String name;

    Item(final String name) {
      this.name = name;
    }

    @Override
    public String nextContinuationToken() {
      return "after-" + name;
    }
  }

  private final List<String> converted = new ArrayList<>();

  private final Function<Item, String> toName = item -> {
    converted.add(item.name);
    return item.name.toUpperCase();
  };

  private static ContinuationArrayList<Item> itemsNamed(final String... names) {
    ContinuationArrayList<Item> items = new ContinuationArrayList<>();
    for (String name : names) {
      items.add(new Item(name));
    }
    return items;
  }

  @Test
  public void testElementsAreConvertedWithTheFunction() {
    FluentContinuation<String, Item> fluent = new FluentContinuation<>(itemsNamed("a", "b", "c"), toName);

    assertThat(fluent, contains("A", "B", "C"));
    assertThat(fluent, hasSize(3));
  }

  @Test
  public void testConversionIsLazy() {
    FluentContinuation<String, Item> fluent = new FluentContinuation<>(itemsNamed("a", "b", "c"), toName);

    assertThat(converted, is(empty()));
    assertThat(fluent.size(), is(3));
    assertThat(fluent.isEmpty(), is(false));
    assertThat(converted, is(empty()));

    Iterator<String> iterator = fluent.iterator();
    assertThat(iterator.next(), is("A"));
    assertThat(converted, contains("a"));
  }

  @Test
  public void testViewReflectsChangesToTheContinuation() {
    ContinuationArrayList<Item> items = itemsNamed("a");
    FluentContinuation<String, Item> fluent = new FluentContinuation<>(items, toName);

    items.add(new Item("b"));

    assertThat(fluent, contains("A", "B"));
  }

  @Test
  public void testRemovalThroughTheViewRemovesFromTheContinuation() {
    ContinuationArrayList<Item> items = itemsNamed("a", "b");
    FluentContinuation<String, Item> fluent = new FluentContinuation<>(items, toName);

    Iterator<String> iterator = fluent.iterator();
    iterator.next();
    iterator.remove();

    assertThat(items, hasSize(1));
    assertThat(fluent, contains("B"));
  }

  @Test
  public void testNextContinuationTokenComesFromTheContinuation() {
    FluentContinuation<String, Item> fluent = new FluentContinuation<>(itemsNamed("a", "b"), toName);

    assertThat(fluent.nextContinuationToken(), is("after-b"));
  }

  @Test
  public void testNullArgumentsAreRejected() {
    assertThrows(NullPointerException.class, () -> new FluentContinuation<String, Item>(null, toName));
    assertThrows(NullPointerException.class, () -> new FluentContinuation<String, Item>(itemsNamed("a"), null));
  }
}
