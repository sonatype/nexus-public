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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.ehcache.jcache.JCacheCachingProvider;

import static com.google.common.base.Preconditions.checkNotNull;
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
  private static final String CONFIG_FILE = "ehcache.xml";

  private static final String DEFAULT_RESOURCE = "ehcache-default.xml";

  private volatile CacheManager cacheManager;

  @Inject
  public EhCacheManagerProvider(final ApplicationDirectories directories) {
    checkNotNull(directories);

    URI uri = null;
    File file = new File(directories.getConfigDirectory("fabric"), CONFIG_FILE);
    if (file.exists()) {
      uri = file.toURI();
    }
    else {
      log.warn("Missing configuration: {}", file.getAbsolutePath());
    }
    this.cacheManager = create(uri);
  }

  @VisibleForTesting
  public EhCacheManagerProvider(@Nullable final URI uri) {
    this.cacheManager = create(uri);
  }

  private CacheManager create(@Nullable final URI uri) {
    URI config = uri;
    if (config == null) {
      // load the configuration from defaults, this is mainly used for test environments
      log.warn("Using default configuration");
      URL url = getClass().getResource(DEFAULT_RESOURCE);
      checkState(url != null, "Missing default configuration resource: %s", DEFAULT_RESOURCE);
      try {
        config = url.toURI();
      }
      catch (URISyntaxException e) {
        throw Throwables.propagate(e);
      }
    }

    CachingProvider provider = Caching.getCachingProvider(
        JCacheCachingProvider.class.getName(),
        JCacheCachingProvider.class.getClassLoader()
    );

    log.info("Creating cache-manager with configuration: {}", config);
    // create manager using the EHCache bundle loader; it has a dynamic import to help unmarshal user classes
    CacheManager manager = provider.getCacheManager(config, net.sf.ehcache.CacheManager.class.getClassLoader());
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
