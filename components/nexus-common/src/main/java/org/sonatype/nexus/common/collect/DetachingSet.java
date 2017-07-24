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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.google.common.collect.ForwardingSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Set} wrapper that automatically detaches from the original in response to a mutating request
 * or when content escapes back to the caller, as we may need to wrap that content and store it locally.
 *
 * This provides a good balance between an eager copy and a completely lazy (but complex) wrapper.
 *
 * @since 3.5
 */
public class DetachingSet<V>
    extends ForwardingSet<V>
{
  private Set<V> backing;

  private final BooleanSupplier allowDetach;

  private final Function<V, V> detach;

  private boolean detached;

  /**
   * Wraps a set that detaches on-demand when allowed; its elements are detached using the given strategy.
   *
   * @param backing The original set
   * @param allowDetach Is detaching currently allowed?
   * @param detach The detach strategy
   */
  public DetachingSet(final Set<V> backing, final BooleanSupplier allowDetach, final Function<V, V> detach) {
    this.backing = checkNotNull(backing);
    this.allowDetach = checkNotNull(allowDetach);
    this.detach = checkNotNull(detach);
  }

  /**
   * Wraps a set that detaches on-demand; its elements are detached using the given strategy.
   *
   * @param backing The original set
   * @param detach The detach strategy
   */
  public DetachingSet(final Set<V> backing, final Function<V, V> detach) {
    this(backing, () -> true, detach);
  }

  /* Methods that don't mutate the set or allow any content to escape */

  @Override
  public boolean contains(final Object key) {
    return backing.contains(key);
  }

  @Override
  public boolean containsAll(final Collection<?> collection) {
    return backing.containsAll(collection);
  }

  @Override
  public boolean isEmpty() {
    return backing.isEmpty();
  }

  @Override
  public int size() {
    return backing.size();
  }

  @Override
  public boolean equals(final Object object) {
    return backing.equals(object);
  }

  @Override
  public int hashCode() {
    return backing.hashCode();
  }

  @Override
  public String toString() {
    return backing.toString();
  }

  /**
   * Incoming request where either the original content will escape back to the caller or the set will change.
   * If detaching is allowed we detach one level by copying the set and applying the strategy to any elements.
   */
  @Override
  protected Set<V> delegate() {
    if (!detached && allowDetach.getAsBoolean()) {
      Set<V> detaching = new HashSet<>(backing.size());
      backing.forEach(e -> detaching.add(detach.apply(e)));
      backing = detaching;
      detached = true;
    }
    return backing;
  }
}
