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
package org.sonatype.nexus.cache;

import javax.cache.expiry.Duration;

/**
 * The cache manager to create the {@link NexusCache}.
 * This is the lightweight version of the {@link javax.cache.Cache} which may use persistent cache
 * and if that is not required one should use {@link CacheHelper} then.
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
public interface CacheManager<K, V>
{
  /**
   * Creates the {@link NexusCache}. It may be a distributed or a local cache depending on a Nexus mode.
   *
   * @param cacheName the name of the cache.
   * @param keyType the expected key type.
   * @param valueType the expected value type.
   * @param expiryAfter the expiration policy of the cache.
   * @return the newly created {@link NexusCache}
   */
  NexusCache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType, Duration expiryAfter);

  /**
   * Destroys the {@link NexusCache} specified by cacheName.
   */
  void destroyCache(String cacheName);
}
