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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.ehcache.jsr107.EhcacheCachingProvider;

import static com.google.common.base.Preconditions.checkState;

/**
 * EhCache JCache {@link CacheManager} provider.
 *
 * Loads configuration from {@code etc/fabric/ehcache.xml} if missing will use {@code ehcache-default.xml} resource.
 *
 * @since 3.0
 */
@Named("ehcache")
@Singleton
public class EhCacheManagerProvider
    extends ComponentSupport
    implements Provider<CacheManager>
{
  private static final String CONFIG_FILE = "/ehcache.xml";

  private static final String DEFAULT_CONFIG_FILE = "/org/sonatype/nexus/cache/internal/ehcache-default.xml";

  private final ClassLoader classLoader;

  private volatile CacheManager cacheManager;

  @Inject
  public EhCacheManagerProvider(@Nullable @Named("nexus-uber") final ClassLoader classLoader) {
    this(classLoader, null);
  }

  @VisibleForTesting
  public EhCacheManagerProvider(@Nullable @Named("nexus-uber") final ClassLoader classLoader, @Nullable URI uri) {
    this.classLoader = classLoader == null ? EhCacheManagerProvider.class.getClassLoader() : classLoader;
    this.cacheManager = create(uri);
  }

  private CacheManager create(@Nullable final URI uri) {
    URI config = uri;
    if (config == null) {
      // load the configuration from defaults, this is mainly used for test environments
      log.debug("Using default configuration");
      URL url = getClass().getResource(CONFIG_FILE);
      if (url == null) {
        url = getClass().getResource(DEFAULT_CONFIG_FILE);
      }
      if (url == null) {
        log.warn("Using default configuration");
      }
      else {
        try {
          config = url.toURI();
        }
        catch (URISyntaxException e) {
          throw Throwables.propagate(e);
        }
      }
    }

    CachingProvider provider = Caching.getCachingProvider(
        EhcacheCachingProvider.class.getName(),
        EhcacheCachingProvider.class.getClassLoader()
    );

    log.info("Creating cache-manager with configuration: {}", config);
    CacheManager manager = provider.getCacheManager(config, classLoader);
    log.debug("Created cache-manager: {}", manager);
    return manager;
  }

  @Override
  public CacheManager get() {
    checkState(cacheManager != null, "Cache-manager destroyed");
    return cacheManager;
  }

  @PreDestroy
  public void destroy() {
    if (cacheManager != null) {
      cacheManager.close();
      log.info("Cache-manager closed");
      cacheManager = null;
    }
  }
}