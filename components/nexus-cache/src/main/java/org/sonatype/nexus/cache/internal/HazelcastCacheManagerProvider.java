package org.sonatype.nexus.cache.internal;

import javax.annotation.PreDestroy;
import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.core.HazelcastInstance;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Singleton
@Named("hazelcast")
public class HazelcastCacheManagerProvider
    extends ComponentSupport
    implements Provider<CacheManager>
{
  private volatile CacheManager cacheManager;

  @Inject
  public HazelcastCacheManagerProvider(final HazelcastInstance hazelcastInstance) {
    checkNotNull(hazelcastInstance);
    this.cacheManager = HazelcastServerCachingProvider.createCachingProvider(hazelcastInstance).getCacheManager();
  }

  @Override
  public CacheManager get() {
    checkState(cacheManager != null, "Cache-manager destroyed");
    return cacheManager;
  }

  @PreDestroy
  public void destroy() {
    if (cacheManager != null) {
      cacheManager.close(); // does not shuts down provided HZ instance
      log.info("Cache-manager closed");
      cacheManager = null;
    }
  }
}
