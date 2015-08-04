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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The Class EhCacheCacheManager is a thin wrapper around EhCache, just to make things going.
 *
 * @author cstamas
 */
@Named
@Singleton
public class EhCacheCacheManager
    extends ComponentSupport
    implements CacheManager
{
  private final net.sf.ehcache.CacheManager cacheManager;

  public static final String SINGLE_PATH_CACHE_NAME = "nx-repository-path-cache";

  @Inject
  public EhCacheCacheManager(final EventBus eventBus, final net.sf.ehcache.CacheManager cacheManager) {
    eventBus.register(this);
    this.cacheManager = checkNotNull(cacheManager);
  }

  public synchronized PathCache getPathCache(String cache) {
    if (!cacheManager.cacheExists(SINGLE_PATH_CACHE_NAME)) {
      cacheManager.addCache(SINGLE_PATH_CACHE_NAME);
    }

    return new EhCachePathCache(cache, cacheManager.getEhcache(SINGLE_PATH_CACHE_NAME));
  }

  @Subscribe
  public void on(final NexusStoppedEvent event) {
    cacheManager.shutdown();
  }
}
