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
package org.sonatype.nexus.cache.internal;

import java.util.Optional;
import javax.cache.Cache;

import org.sonatype.nexus.cache.NexusCache;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The implementation of the {@link NexusCache}.
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
public class LocalCache<K, V>
    implements NexusCache<K, V>
{
  private final Cache<K, V> cache;

  public LocalCache(final Cache<K, V> cache) {
    this.cache = checkNotNull(cache);
  }

  @Override
  public Optional<V> get(final K key) {
    return Optional.ofNullable(cache.get(key));
  }

  @Override
  public void put(final K key, final V value) {
    cache.put(key, value);
  }

  @Override
  public void remove(final K key) {
    cache.remove(key);
  }

  @Override
  public void removeAll() {
    cache.removeAll();
  }
}
