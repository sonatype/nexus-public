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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.google.common.collect.ForwardingMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Map} wrapper that automatically detaches from the original in response to a mutating request
 * or when content escapes back to the caller, as we may need to wrap that content and store it locally.
 *
 * This provides a good balance between an eager copy and a completely lazy (but complex) wrapper.
 *
 * @since 3.5
 */
public class DetachingMap<K, V>
    extends ForwardingMap<K, V>
{
  private Map<K, V> backing;

  private final BooleanSupplier allowDetach;

  private final Function<V, V> detach;

  private boolean detached;

  /**
   * Wraps a map that detaches on-demand when allowed; its mapped values are detached using the given strategy.
   *
   * @param backing The original map
   * @param allowDetach Is detaching currently allowed?
   * @param detach The detach strategy
   */
  public DetachingMap(final Map<K, V> backing, final BooleanSupplier allowDetach, final Function<V, V> detach) {
    this.backing = checkNotNull(backing);
    this.allowDetach = checkNotNull(allowDetach);
    this.detach = checkNotNull(detach);
  }

  /**
   * Wraps a map that detaches on-demand; its mapped values are detached using the given strategy.
   *
   * @param backing The original map
   * @param detach The detach strategy
   */
  public DetachingMap(final Map<K, V> backing, final Function<V, V> detach) {
    this(backing, () -> true, detach);
  }

  /* Methods that don't mutate the map or allow any content to escape */

  @Override
  public boolean containsKey(final Object key) {
    return backing.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    return backing.containsValue(value);
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
   * Incoming request where either the original content will escape back to the caller or the map will change.
   * If detaching is allowed we detach one level by copying the map and applying the strategy to mapped values.
   */
  @Override
  protected Map<K, V> delegate() {
    if (!detached && allowDetach.getAsBoolean()) {
      Map<K, V> detaching = new HashMap<>(backing.size());
      backing.entrySet().forEach(e -> detaching.put(e.getKey(), detach.apply(e.getValue())));
      backing = detaching;
      detached = true;
    }
    return backing;
  }
}
