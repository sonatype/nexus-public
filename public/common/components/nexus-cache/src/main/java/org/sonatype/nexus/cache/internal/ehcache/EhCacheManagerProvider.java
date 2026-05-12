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

import java.io.File;
import java.net.URI;

import javax.annotation.Nullable;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import jakarta.inject.Inject;

import org.sonatype.nexus.common.lifecycle.LifecycleSupport;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;

import com.google.common.annotations.VisibleForTesting;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;

/**
 * EhCache JCache {@link CacheManager} provider.
 *
 * Loads configuration from {@code etc/fabric/ehcache.xml} if missing will use {@code ehcache-default.xml} resource.
 *
 * @since 3.0
 */
@Component
@Qualifier("ehcache")
@ManagedLifecycle(phase = STORAGE)
// not a singleton because we want to provide a new manager when bouncing services
public class EhCacheManagerProvider
    extends LifecycleSupport
    implements FactoryBean<CacheManager>
{
  private static final String CONFIG_FILE = "ehcache.xml";

  private final URI configUri;

  // provide same manager instance until bounced
  private volatile CacheManager cacheManager;

  @Inject
  public EhCacheManagerProvider(final ApplicationDirectories directories) {
    checkNotNull(directories);
    File file = new File(directories.getConfigDirectory("fabric"), CONFIG_FILE);
    if (file.exists()) {
      configUri = file.toURI();
    }
    else {
      log.warn("Missing configuration: {}", file.getAbsolutePath());
      configUri = null;
    }
  }

  @VisibleForTesting
  public EhCacheManagerProvider(@Nullable final URI uri) {
    this.configUri = uri;
  }

  private CacheManager create(@Nullable final URI config) {
    CachingProvider provider = Caching.getCachingProvider(
        EhcacheCachingProvider.class.getName(),
        EhcacheCachingProvider.class.getClassLoader());

    log.info("Creating cache-manager with configuration: {}", config);
    CacheManager manager = provider.getCacheManager(config, getClass().getClassLoader());
    log.debug("Created cache-manager: {}", manager);
    return manager;
  }

  @Override
  public synchronized CacheManager getObject() {
    checkState(!isStopped(), "Cache-manager destroyed");
    if (cacheManager == null) {
      this.cacheManager = create(configUri);
    }
    return cacheManager;
  }

  @Override
  protected void doStop() {
    if (cacheManager != null) {
      cacheManager.close();
      log.info("Cache-manager closed");
      cacheManager = null;
    }
  }

  @Override
  public Class<?> getObjectType() {
    return CacheManager.class;
  }
}
