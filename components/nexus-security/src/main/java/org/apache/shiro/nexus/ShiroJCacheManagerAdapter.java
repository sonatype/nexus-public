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

import java.util.Objects;
import java.util.Optional;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.cache.CacheHelper;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.shiro.session.mgt.eis.CachingSessionDAO.ACTIVE_SESSION_CACHE_NAME;

/**
 * Shiro {@link javax.cache.CacheManager} to {@link CacheManager} adapter.
 *
 * @since 3.0
 */
public class ShiroJCacheManagerAdapter
  extends ComponentSupport
  implements CacheManager
{
  private final Provider<CacheHelper> cacheHelperProvider;

  private final Provider<Time> defaultTimeToLive;

  public ShiroJCacheManagerAdapter(final Provider<CacheHelper> cacheHelperProvider,
                                   final Provider<Time> defaultTimeToLive)
  {
    this.cacheHelperProvider = checkNotNull(cacheHelperProvider);
    this.defaultTimeToLive = checkNotNull(defaultTimeToLive);
  }

  @Override
  public <K, V> Cache<K, V> getCache(final String name) {
    log.debug("Getting cache: {}", name);
    return new ShiroJCacheAdapter<>(this.<K,V>maybeCreateCache(name));
  }

  @VisibleForTesting
  <K, V> javax.cache.Cache<K, V> maybeCreateCache(final String name) {
    if (Objects.equals(ACTIVE_SESSION_CACHE_NAME, name)) {
      // shiro's session cache needs to never expire:
      // http://shiro.apache.org/session-management.html#ehcache-session-cache-configuration
      return cacheHelperProvider.get().maybeCreateCache(name, EternalExpiryPolicy.factoryOf());
    }
    else {
      Time timeToLive = Optional.ofNullable(System.getProperty(name + ".timeToLive"))
          .map(Time::parse)
          .orElse(defaultTimeToLive.get());
      return cacheHelperProvider.get().maybeCreateCache(name,
          CreatedExpiryPolicy.factoryOf(new Duration(timeToLive.getUnit(), timeToLive.getValue())));
    }
  }
}
