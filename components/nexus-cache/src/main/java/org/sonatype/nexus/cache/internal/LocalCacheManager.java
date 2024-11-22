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

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.cache.CacheManager;
import org.sonatype.nexus.cache.NexusCache;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The cache manager which creates the {@link LocalCache}.
 * The lightweight version of the {@link javax.cache.Cache} which should be used in the single mode only.
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
@Named
@Singleton
public class LocalCacheManager<K, V>
    implements CacheManager<K, V>
{
  private final CacheHelper cacheHelper;

  @Inject
  public LocalCacheManager(final CacheHelper cacheHelper) {
    this.cacheHelper = checkNotNull(cacheHelper);
  }

  /**
   * Warning:
   * If the cache specified by cacheName already exists, then the existing cache is returned and
   * the expiryAfter is ignored.
   * The cache must be destroyed before a new expiryAfter can be specified.
   */
  @Override
  public NexusCache<K, V> getCache(
      final String cacheName,
      final Class<K> keyType,
      final Class<V> valueType,
      final Duration expiryAfter)
  {
    return new LocalCache<>(
        cacheHelper.maybeCreateCache(cacheName, keyType, valueType, CreatedExpiryPolicy.factoryOf(expiryAfter)));
  }

  @Override
  public void destroyCache(final String cacheName) {
    cacheHelper.maybeDestroyCache(cacheName);
  }
}
