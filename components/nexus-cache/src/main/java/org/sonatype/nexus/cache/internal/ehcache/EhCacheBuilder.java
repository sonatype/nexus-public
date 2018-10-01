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
package org.sonatype.nexus.cache.internal.ehcache;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.expiry.ExpiryPolicy;
import javax.inject.Named;

import org.sonatype.nexus.cache.AbstractCacheBuilder;

import org.ehcache.ValueSupplier;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expiry;
import org.ehcache.jsr107.Eh107Configuration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * EhCache JCache {@link CacheBuilder}.
 *
 * @since 3.14
 */
@Named("ehcache")
public class EhCacheBuilder<K, V>
    extends AbstractCacheBuilder<K, V>
{
  @Override
  @SuppressWarnings("unchecked")
  public Cache<K, V> build(CacheManager manager) {
    checkNotNull(manager);
    checkNotNull(keyType);
    checkNotNull(valueType);
    checkNotNull(name);
    checkNotNull(expiryFactory);

    CacheConfigurationBuilder<K, V> builder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
        keyType,
        valueType,
        ResourcePoolsBuilder.heap(cacheSize));

    builder.withExpiry(mapToEhCacheExpiry(expiryFactory.create()));

    Cache<K, V> cache = manager.createCache(name,  Eh107Configuration.fromEhcacheCacheConfiguration(builder));

    manager.enableStatistics(name, statisticsEnabled);
    manager.enableManagement(name, managementEnabled);

    if (persister != null) {
      CacheEventListener listener = (final CacheEvent cacheEvent) ->
          persister.accept((K) cacheEvent.getKey(), (V) cacheEvent.getOldValue());

      Eh107Configuration<K, V> configuration = cache.getConfiguration(Eh107Configuration.class);
      configuration.unwrap(CacheRuntimeConfiguration.class)
          .registerCacheEventListener(listener, EventOrdering.UNORDERED, EventFiring.ASYNCHRONOUS,
              EventType.EVICTED, EventType.REMOVED, EventType.EXPIRED);
    }

    return cache;
  }

  private Expiry<K, V> mapToEhCacheExpiry(ExpiryPolicy policy) {
    return new Expiry<K, V>()
    {
      @Override
      public Duration getExpiryForCreation(K k, V v) {
        return toEhCacheDuration(policy.getExpiryForCreation());
      }

      @Override
      public Duration getExpiryForAccess(K k, ValueSupplier<? extends V> valueSupplier) {
        return toEhCacheDuration(policy.getExpiryForAccess());
      }

      @Override
      public Duration getExpiryForUpdate(K k, ValueSupplier<? extends V> valueSupplier, V v) {
        return toEhCacheDuration(policy.getExpiryForUpdate());
      }

      private Duration toEhCacheDuration(javax.cache.expiry.Duration duration) {
        return duration.isEternal() ?
            Duration.INFINITE :
            new Duration(duration.getDurationAmount(), duration.getTimeUnit());
      }
    };
  }
}
