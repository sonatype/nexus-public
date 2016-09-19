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
package org.sonatype.nexus.repository.cache.internal;

import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheKey;
import org.sonatype.nexus.repository.cache.PathNegativeCacheKey;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Status;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;

/**
 * Default {@link NegativeCacheFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class NegativeCacheFacetImpl
    extends FacetSupport
    implements NegativeCacheFacet
{
  private final CacheManager cacheManager;

  @VisibleForTesting
  static final String CONFIG_KEY = "negativeCache";

  @VisibleForTesting
  static class Config
  {
    @NotNull
    public Boolean enabled = Boolean.TRUE;

    /**
     * Time-to-live seconds.
     */
    @NotNull
    @Min(0)
    public Integer timeToLive = Time.hours(24).toSecondsI();

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "enabled=" + enabled +
          ", timeToLive=" + timeToLive +
          '}';
    }
  }

  private Config config;

  private Cache<NegativeCacheKey, Status> cache;

  @Inject
  public NegativeCacheFacetImpl(final CacheManager cacheManager) {
    this.cacheManager = checkNotNull(cacheManager);
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
    log.debug("Config: {}", config);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);

    // create cache if enabled
    if (config.enabled) {
      maybeCreateCache();
    }
  }

  @Override
  protected void doUpdate(final Configuration configuration) throws Exception {
    Config previous = config;
    super.doUpdate(configuration);

    // re-create cache if enabled or cache settings changed
    if (config.enabled) {
      if (!previous.enabled || !config.timeToLive.equals(previous.timeToLive)) {
        maybeDestroyCache();
        maybeCreateCache();
      }
    }
    else {
      // else destroy cache if disabled
      maybeDestroyCache();
    }
  }

  @Override
  protected void doDestroy() throws Exception {
    maybeDestroyCache();
    config = null;
  }

  private void maybeCreateCache() {
    if (cache == null) {
      log.debug("Creating negative-cache for: {}", getRepository());

      MutableConfiguration<NegativeCacheKey, Status> cacheConfig = new MutableConfiguration<>();
      cacheConfig.setTypes(NegativeCacheKey.class, Status.class);
      cacheConfig.setExpiryPolicyFactory(
          CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, config.timeToLive))
      );
      cacheConfig.setManagementEnabled(true);
      cacheConfig.setStatisticsEnabled(true);

      cache = cacheManager.createCache(getRepository().getName() + "#negative-cache", cacheConfig);
      log.debug("Created negative-cache: {}", cache);
    }
  }

  private void maybeDestroyCache() {
    if (cache != null && !cacheManager.isClosed()) {
      log.debug("Destroying negative-cache for: {}", getRepository());
      cacheManager.destroyCache(cache.getName());
      cache = null;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public Status get(final NegativeCacheKey key) {
    checkNotNull(key);
    if (cache != null) {
      return cache.get(key);
    }
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public void put(final NegativeCacheKey key, final Status status) {
    checkNotNull(key);
    checkNotNull(status);
    if (cache != null) {
      log.debug("Adding {}={} to negative-cache of {}", key, status, getRepository());
      cache.put(key, status);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void invalidate(final NegativeCacheKey key) {
    checkNotNull(key);
    if (cache != null) {
      log.debug("Removing {} from negative-cache of {}", key, getRepository());
      cache.remove(key);
    }
  }

  @Override
  public void invalidateSubset(final NegativeCacheKey key) {
    if (cache != null) {
      invalidate(key);
      for (final Entry<NegativeCacheKey, Status> entry : cache) {
        if (!key.equals(entry.getKey()) && key.isParentOf(entry.getKey())) {
          invalidate(entry.getKey());
        }
      }
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void invalidate() {
    if (cache != null) {
      log.debug("Removing all from negative-cache of {}", getRepository());
      cache.removeAll();
    }
  }

  @Override
  public NegativeCacheKey getCacheKey(final Context context) {
    return new PathNegativeCacheKey(context.getRequest().getPath());
  }
}
