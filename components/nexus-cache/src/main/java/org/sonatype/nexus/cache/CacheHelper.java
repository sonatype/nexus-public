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

import javax.annotation.Nullable;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.ExpiryPolicy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.6
 */
@SuppressWarnings("rawtypes")
@Named
@Singleton
public class CacheHelper
    extends ComponentSupport
{
  private final Provider<CacheManager> cacheManagerProvider;

  private final Provider<CacheBuilder> cacheBuilderProvider;

  @Inject
  public CacheHelper(
      final Provider<CacheManager> cacheManagerProvider,
      final Provider<CacheBuilder> cacheBuilderProvider)
  {
    this.cacheManagerProvider = checkNotNull(cacheManagerProvider);
    this.cacheBuilderProvider = checkNotNull(cacheBuilderProvider);
  }

  private CacheManager manager() {
    return cacheManagerProvider.get();
  }

  @SuppressWarnings("unchecked")
  public <K, V> CacheBuilder<K, V> builder() {
    return cacheBuilderProvider.get();
  }

  public synchronized <K, V> Cache<K, V> maybeCreateCache(
      final String name,
      final MutableConfiguration<K, V> mutableConfiguration)
  {
    checkNotNull(name);
    checkNotNull(mutableConfiguration);

    Cache<K, V> cache = manager()
        .getCache(name, mutableConfiguration.getKeyType(), mutableConfiguration.getValueType());

    if (cache == null) {
      cache = manager().createCache(name, mutableConfiguration);
      log.debug("Created cache: {}", cache);
    }
    else {
      log.debug("Re-using existing cache: {}", cache);
    }

    return cache;
  }

  public synchronized <K, V> Cache<K, V> getOrCreate(final CacheBuilder<K, V> builder) {
    checkNotNull(builder);

    Cache<K, V> cache = manager().getCache(builder.getName(), builder.getKeyType(), builder.getValueType());

    if (cache == null) {
      cache = builder.build(manager());
      log.debug("Created cache: {}", cache);
    }
    else {
      log.debug("Re-using existing cache: {}", cache);
    }

    return cache;
  }

  public synchronized <K, V> Cache<K, V> maybeCreateCache(
      final String name,
      final Factory<? extends ExpiryPolicy> expiryPolicyFactory)
  {
    return maybeCreateCache(name, null, null, expiryPolicyFactory);
  }

  public synchronized <K, V> Cache<K, V> maybeCreateCache(
      final String name,
      @Nullable final Class<K> keyType,
      @Nullable final Class<V> valueType,
      final Factory<? extends ExpiryPolicy> expiryPolicyFactory)
  {
    return maybeCreateCache(name, createCacheConfig(keyType, valueType, expiryPolicyFactory));
  }

  public static <K, V> MutableConfiguration<K, V> createCacheConfig(
      @Nullable final Class<K> keyType,
      @Nullable final Class<V> valueType,
      final Factory<? extends ExpiryPolicy> expiryPolicyFactory)
  {
    MutableConfiguration<K, V> config = new MutableConfiguration<K, V>()
        .setStoreByValue(false)
        .setExpiryPolicyFactory(expiryPolicyFactory)
        .setManagementEnabled(true)
        .setStatisticsEnabled(true);

    if (keyType != null && valueType != null) {
      config.setTypes(keyType, valueType);
    }
    return config;
  }

  public synchronized void maybeDestroyCache(final String name) {
    manager().destroyCache(name);
    log.debug("Destroyed cache: {}", name);
  }
}
