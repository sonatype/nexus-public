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
package org.sonatype.nexus.security.authz;

import java.time.Duration;

import org.sonatype.nexus.common.time.Time;

import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_CONCURRENCY_LEVEL_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_RECORD_STATS_NAMED_VALUE;

/**
 * Creates a {@link PrincipalPermissionsCache} per authorizing realm from the shared feature-flag configuration. The
 * cache logic and its tuning live here once, but each {@link PermissionCachingAuthorizingRealm} owns its own cache
 * instance so realms never share entries or invalidation (NEXUS-53719).
 *
 * <p>
 * The {@code maximumSize} budget is per realm and independently tunable: the default (1000) comes from
 * {@code nexus.authorizingrealm.permissionscache.maximumsize}, and a realm may override it by appending its name, e.g.
 * {@code nexus.authorizingrealm.permissionscache.maximumsize.LdapRealm=5000} (useful for high-cardinality realms such
 * as ServiceAccount tokens or large LDAP/SSO user populations). Note the aggregate peak footprint is roughly
 * {@code (number of authorizing realms) x maximumSize x per-entry cost}; once every realm adopts this base, size the
 * default (and any per-realm overrides) with the realm count in mind.
 *
 * <p>
 * The cache time-to-live reuses {@code nexus.shiro.cache.defaultTimeToLive} (the same value that bounds Shiro's
 * per-realm authorization/role cache), so a principal's expanded permissions never outlive the roles they were expanded
 * from - both caches refresh on the same interval and cannot drift.
 */
@Component
public class PrincipalPermissionsCacheFactory
{
  private final boolean enabled;

  private final int defaultMaximumSize;

  private final Provider<Time> defaultTimeToLive;

  private final boolean cacheRecordStats;

  private final int cacheConcurrencyLevel;

  // Field-injected so per-realm overrides can be resolved by realm name at create() time; absent in plain unit tests
  // (constructed without Spring), where create() falls back to the default maximum size.
  @Autowired(required = false)
  private Environment environment;

  @Autowired
  public PrincipalPermissionsCacheFactory(
      @Value(PRINCIPAL_PERMISSIONS_CACHE_ENABLED_NAMED_VALUE) final boolean enabled,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE_NAMED_VALUE) final int defaultMaximumSize,
      @Value("${nexus.shiro.cache.defaultTimeToLive:2m}") final Provider<Time> defaultTimeToLive,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_RECORD_STATS_NAMED_VALUE) final boolean cacheRecordStats,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_CONCURRENCY_LEVEL_NAMED_VALUE) final int cacheConcurrencyLevel)
  {
    this.enabled = enabled;
    this.defaultMaximumSize = defaultMaximumSize;
    this.defaultTimeToLive = checkNotNull(defaultTimeToLive);
    this.cacheRecordStats = cacheRecordStats;
    this.cacheConcurrencyLevel = cacheConcurrencyLevel;
  }

  /**
   * Creates a new, independent permission cache for {@code realmName}. Its {@code maximumSize} is the realm-specific
   * override if configured, else the shared default; its expiry tracks {@code nexus.shiro.cache.defaultTimeToLive}.
   */
  public PrincipalPermissionsCache create(final String realmName) {
    int maximumSize = defaultMaximumSize;
    if (environment != null) {
      maximumSize = environment.getProperty(
          PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE + "." + checkNotNull(realmName), Integer.class, defaultMaximumSize);
    }
    return new PrincipalPermissionsCache(
        enabled,
        maximumSize,
        Duration.ofMillis(defaultTimeToLive.get().toMillis()),
        cacheRecordStats,
        cacheConcurrencyLevel);
  }
}
