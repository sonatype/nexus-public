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
package org.sonatype.nexus.proxy.cache;

import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class JCacheCacheManager
    extends ComponentSupport
    implements CacheManager
{
  private final javax.cache.CacheManager cacheManager;

  @Inject
  public JCacheCacheManager(final javax.cache.CacheManager cacheManager) {
    this.cacheManager = checkNotNull(cacheManager);
  }

  public synchronized PathCache getPathCache(final Repository repository) {
    final String cacheName = "nx-nfc#" + repository.getId();
    int ttl = repository.getNotFoundCacheTimeToLive();
    PathCache oldCache = repository.getNotFoundCache();
    if (oldCache == null || oldCache.ttl() != ttl) {
      if (oldCache != null) {
        oldCache.destroy();
      }
      Cache<String, Boolean> cache = cacheManager.getCache(cacheName, String.class, Boolean.class);
      if (cache == null) {
        MutableConfiguration<String, Boolean> cacheConfig = new MutableConfiguration<>();
        cacheConfig.setTypes(String.class, Boolean.class);
        cacheConfig.setStoreByValue(false);

        Factory<? extends ExpiryPolicy> expiryPolicyFactory;
        if (ttl < 0) {
          // -1: eternal
          expiryPolicyFactory = CreatedExpiryPolicy.factoryOf(Duration.ETERNAL);
        }
        else if (ttl == 0) {
          // 0: immediate expiration: zero
          expiryPolicyFactory = CreatedExpiryPolicy.factoryOf(Duration.ZERO);
        }
        else {
          expiryPolicyFactory = CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, ttl));
        }
        cacheConfig.setExpiryPolicyFactory(expiryPolicyFactory);
        cacheConfig.setManagementEnabled(true);
        cacheConfig.setStatisticsEnabled(true);
        cache = cacheManager.createCache(cacheName, cacheConfig);
        log.info("Created path cache {}, TTL {}", cacheName, ttl);
      }
      return new JCachePathCache(cache, ttl);
    }
    else {
      return oldCache;
    }
  }
}
