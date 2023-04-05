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
package org.sonatype.nexus.internal.atlas;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.atlas.SystemInformationManager;
import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.atlas.SystemInformationGenerator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * Default {@link SystemInformationManager}
 */
@ManagedLifecycle(phase = SERVICES)
@Named
@Singleton
public class SystemInformationManagerImpl
    extends LifecycleSupport
    implements SystemInformationManager
{
  private static final String CACHE_NAME = "SYSTEM_INFORMATION";

  private static final String SYSTEM_INFO_KEY = "systemInfo";

  private final SystemInformationGenerator systemInformationGenerator;

  private final CacheHelper cacheHelper;

  private final int cacheDurationSec;

  private Cache<String, Map<String, Object>> localCache;

  @Inject
  public SystemInformationManagerImpl(
      final SystemInformationGenerator systemInformationGenerator,
      final CacheHelper cacheHelper,
      @Named("${nexus.system.info.cache.duration:-3600}") int cacheDurationSec)
  {
    this.systemInformationGenerator = checkNotNull(systemInformationGenerator);
    this.cacheHelper = checkNotNull(cacheHelper);

    checkState(cacheDurationSec >= 0, "Initial cache duration should be positive");
    this.cacheDurationSec = cacheDurationSec;
  }

  @Override
  protected void doStart() {
    maybeCreateCache();
  }

  @Override
  public Map<String, Object> getSystemInfo() {
    Map<String, Object> systemInfo = localCache.get(SYSTEM_INFO_KEY);
    if (Objects.isNull(systemInfo)) {
      Map<String, Object> report = generateReport();
      log.debug("Caching system information report into {} cache.", CACHE_NAME);
      localCache.put(SYSTEM_INFO_KEY, report);
      return Collections.unmodifiableMap(report);
    }
    log.debug("Getting system information report from {} cache.", CACHE_NAME);
    return Collections.unmodifiableMap(systemInfo);
  }

  private Map<String, Object> generateReport() {
    Map<String, Object> report = systemInformationGenerator.report();
    return report.entrySet().stream().filter(e -> !"nexus-bundles".equals(e.getKey()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
  @Override
  protected void doStop() {
    maybeDestroyCache();
  }

  private void maybeCreateCache() {
    if (Objects.isNull(localCache)) {
      log.debug("Creating {} for system information.", CACHE_NAME);
      Duration duration = new Duration(TimeUnit.SECONDS, cacheDurationSec);
      Factory<ExpiryPolicy> expiryPolicyFactory = CreatedExpiryPolicy.factoryOf(duration);
      MutableConfiguration<String, Map<String, Object>> config =
          new MutableConfiguration<String, Map<String, Object>>()
              .setStoreByValue(false)
              .setExpiryPolicyFactory(expiryPolicyFactory)
              .setManagementEnabled(true)
              .setStatisticsEnabled(true);
      localCache = cacheHelper.maybeCreateCache(CACHE_NAME, config);
      log.debug("Created {} cache for system information", CACHE_NAME);
    }
  }

  private void maybeDestroyCache() {
    cacheHelper.maybeDestroyCache(CACHE_NAME);
  }
}
