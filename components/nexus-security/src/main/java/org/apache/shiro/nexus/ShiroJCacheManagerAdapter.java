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
package org.apache.shiro.nexus;

import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Shiro {@link javax.cache.CacheManager} to {@link CacheManager} adapter.
 *
 * @since 3.0
 */
public class ShiroJCacheManagerAdapter
  extends ComponentSupport
  implements CacheManager
{
  private final Provider<javax.cache.CacheManager> cacheManagerProvider;

  public ShiroJCacheManagerAdapter(final Provider<javax.cache.CacheManager> cacheManagerProvider) {
    this.cacheManagerProvider = checkNotNull(cacheManagerProvider);
  }

  private javax.cache.CacheManager manager() {
    javax.cache.CacheManager result = cacheManagerProvider.get();
    checkState(result != null, "Cache-manager not bound");
    return result;
  }

  @Override
  public <K, V> Cache<K, V> getCache(final String name) {
    log.debug("Getting cache: {}", name);
    return new ShiroJCacheAdapter<>(this.<K,V>maybeCreateCache(name));
  }

  private <K, V> javax.cache.Cache<K, V> maybeCreateCache(final String name) {
    javax.cache.Cache<K, V> cache = manager().getCache(name);
    if (cache == null) {
      log.debug("Creating cache: {}", name);
      MutableConfiguration<K, V> cacheConfig = new MutableConfiguration<K, V>()
          .setStoreByValue(false)
          .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
          .setManagementEnabled(true)
          .setStatisticsEnabled(true);

      cache = manager().createCache(name, cacheConfig);
      log.debug("Created cache: {}", cache);
    }
    else {
      log.debug("Re-using existing cache: {}", cache);
    }
    return cache;
  }
}
