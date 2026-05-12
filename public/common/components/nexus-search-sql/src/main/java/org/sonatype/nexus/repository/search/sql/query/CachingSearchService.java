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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A caching decorator for {@link SearchService} that provides an L1 in-memory cache
 * for search results using Google Guava's Cache.
 *
 * <p>
 * The cache is automatically invalidated when component events occur (create, update, delete, purge)
 * to ensure consistency between the cache and the underlying data.
 * </p>
 *
 * <p>
 * <strong>Security Note:</strong> Caching is intentionally bypassed when
 * {@link SearchRequest#isCheckAuthorization()} returns true. This is because search results
 * are user-specific when authorization is enabled - they depend on the current user's
 * repository permissions and content selector restrictions. Caching authorized searches
 * would create an RBAC bypass where one user's results could be served to another user
 * with different (more restrictive) permissions.
 * </p>
 *
 * <p>
 * Cache configuration:
 * <ul>
 * <li>Maximum size: 10,000 entries</li>
 * <li>TTL: 30 seconds after write</li>
 * <li>Statistics recording enabled for monitoring</li>
 * <li>Only caches searches with authorization disabled (anonymous/system searches)</li>
 * </ul>
 * </p>
 */
@Component
@Named("caching")
@Primary
@Singleton
public class CachingSearchService
    implements SearchService, EventAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final long DEFAULT_MAX_SIZE = 10_000L;

  private static final long DEFAULT_TTL_SECONDS = 30L;

  private final SearchService delegate;

  private final Cache<String, SearchResponse> cache;

  private final boolean enabled;

  @Inject
  public CachingSearchService(
      @Named("sql") final SearchService delegate,
      @Value("${nexus.search.cache.enabled:true}") final boolean enabled,
      @Value("${nexus.search.cache.maxSize:10000}") final long maxSize,
      @Value("${nexus.search.cache.ttlSeconds:30}") final long ttlSeconds)
  {
    this.delegate = checkNotNull(delegate);
    this.enabled = enabled;
    this.cache = CacheBuilder.newBuilder()
        .maximumSize(maxSize > 0 ? maxSize : DEFAULT_MAX_SIZE)
        .expireAfterWrite(ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS, TimeUnit.SECONDS)
        .recordStats()
        .build();

    log.info("Search cache initialized: enabled={}, maxSize={}, ttlSeconds={}",
        enabled, maxSize > 0 ? maxSize : DEFAULT_MAX_SIZE, ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    if (!enabled) {
      return delegate.search(searchRequest);
    }

    // SECURITY: Do not cache searches when authorization is enabled.
    // Search results are user-specific when authorization is checked - they depend on
    // the current user's repository permissions and content selector restrictions.
    // Caching these results would allow one user's results to be served to another
    // user with different (potentially more restrictive) permissions, bypassing RBAC.
    if (searchRequest.isCheckAuthorization()) {
      log.trace("Bypassing cache for authorized search (user-specific results)");
      return delegate.search(searchRequest);
    }

    String cacheKey = computeCacheKey(searchRequest);
    SearchResponse cachedResponse = cache.getIfPresent(cacheKey);

    if (cachedResponse != null) {
      log.trace("Cache hit for key: {}", cacheKey);
      return cachedResponse;
    }

    log.trace("Cache miss for key: {}", cacheKey);
    SearchResponse response = delegate.search(searchRequest);
    cache.put(cacheKey, response);
    return response;
  }

  @Override
  public Iterable<ComponentSearchResult> browse(final SearchRequest searchRequest) {
    // Don't cache browse - it's for iteration and could return large result sets
    return delegate.browse(searchRequest);
  }

  @Override
  public long count(final SearchRequest searchRequest) {
    // Count queries are typically expensive; for now, delegate without caching
    // A future enhancement could add a separate cache for count results
    return delegate.count(searchRequest);
  }

  @Override
  public void waitForCalm() {
    delegate.waitForCalm();
  }

  @Override
  public void waitForReady() {
    delegate.waitForReady();
  }

  /**
   * Invalidates all cached search results.
   * This is called when component data changes to ensure cache consistency.
   */
  public void invalidateAll() {
    long size = cache.size();
    cache.invalidateAll();
    log.debug("Invalidated {} cached search results", size);
  }

  /**
   * Returns the current cache statistics for monitoring purposes.
   *
   * @return the cache statistics
   */
  public CacheStats getStats() {
    return cache.stats();
  }

  /**
   * Returns the current number of entries in the cache.
   *
   * @return the cache size
   */
  public long getCacheSize() {
    return cache.size();
  }

  /**
   * Returns whether the cache is enabled.
   *
   * @return true if the cache is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Computes a deterministic cache key from the search request.
   * Uses SHA-256 hash of the request parameters to ensure consistent key generation.
   *
   * @param request the search request
   * @return a unique cache key for the request
   */
  private String computeCacheKey(final SearchRequest request) {
    StringBuilder keyBuilder = new StringBuilder();

    // Include search filters
    if (request.getSearchFilters() != null) {
      request.getSearchFilters()
          .forEach(filter -> keyBuilder.append(filter.getProperty())
              .append("=")
              .append(filter.getValue())
              .append(";"));
    }

    // Include repositories
    if (request.getRepositories() != null) {
      keyBuilder.append("repos=");
      request.getRepositories().forEach(repo -> keyBuilder.append(repo).append(","));
      keyBuilder.append(";");
    }

    // Include sort parameters
    keyBuilder.append("sort=").append(request.getSortField()).append(";");
    keyBuilder.append("dir=").append(request.getSortDirection()).append(";");

    // Include pagination parameters
    keyBuilder.append("token=").append(request.getContinuationToken()).append(";");
    keyBuilder.append("limit=").append(request.getLimit()).append(";");
    keyBuilder.append("offset=").append(request.getOffset()).append(";");

    // Include other flags
    keyBuilder.append("auth=").append(request.isCheckAuthorization()).append(";");
    keyBuilder.append("conj=").append(request.isConjunction()).append(";");
    keyBuilder.append("assets=").append(request.isIncludeAssets()).append(";");

    return DigestUtils.sha256Hex(keyBuilder.toString());
  }

  // Event handlers for cache invalidation

  @AllowConcurrentEvents
  @Subscribe
  public void onComponentCreated(final ComponentCreatedEvent event) {
    log.trace("Component created event received, invalidating cache");
    invalidateAll();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void onComponentUpdated(final ComponentUpdatedEvent event) {
    log.trace("Component updated event received, invalidating cache");
    invalidateAll();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void onComponentDeleted(final ComponentDeletedEvent event) {
    log.trace("Component deleted event received, invalidating cache");
    invalidateAll();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void onComponentPurged(final ComponentPurgedEvent event) {
    log.trace("Component purged event received, invalidating cache");
    invalidateAll();
  }
}
