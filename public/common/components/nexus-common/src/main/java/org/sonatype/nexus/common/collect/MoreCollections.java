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
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * JDK-only collection helpers replacing Guava idioms.
 */
public final class MoreCollections
{
  private MoreCollections() {
    // static utility
  }

  /**
   * Materialises an {@link Iterable} into an ordered, effectively immutable {@link List}, using the
   * {@link Collection} fast path (single copy) when possible. Near-drop-in replacement for
   * {@code com.google.common.collect.ImmutableList.copyOf(Iterable)}, with two intentional
   * differences: (1) null elements in the source are accepted rather than rejected;
   * (2) null queries (contains/indexOf(null)) return false/-1 only when no null element is present.
   */
  public static <T> List<T> toImmutableList(final Iterable<? extends T> iterable) {
    requireNonNull(iterable);
    if (iterable instanceof Collection<? extends T> collection) {
      return unmodifiableList(new ArrayList<>(collection)); // single defensive copy (fast path)
    }
    ArrayList<T> copy = new ArrayList<>();
    iterable.forEach(copy::add);
    copy.trimToSize();
    return unmodifiableList(copy);
  }
}
