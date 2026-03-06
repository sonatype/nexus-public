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
package org.sonatype.nexus.repository.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/**
 * Request-scoped cache for content selector evaluation results.
 * <p>
 * This cache eliminates redundant selector evaluations within a single request,
 * particularly for Docker digest pulls from group repositories which can cause
 * N selectors × M member repos evaluations (NEXUS-50181).
 * </p>
 * <p>
 * The cache is created per-request and should be passed through the security
 * check call chain. It is automatically cleared when the request completes.
 * </p>
 * <p>
 * Cache key format: userId:repositoryName:selectorName:path
 * </p>
 *
 * @since 3.x
 */
public class SelectorEvaluationCache
{
  private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

  /**
   * Retrieves a cached selector evaluation result.
   *
   * @param key the cache key
   * @return the cached result, or null if not cached
   */
  @Nullable
  public Boolean get(final String key) {
    return cache.get(key);
  }

  /**
   * Stores a selector evaluation result in the cache.
   *
   * @param key the cache key
   * @param value the evaluation result
   */
  public void put(final String key, final Boolean value) {
    cache.put(key, value);
  }

  /**
   * Clears all cached entries.
   * Should be called when the request completes.
   */
  public void clear() {
    cache.clear();
  }

  /**
   * Returns the current number of cached entries.
   * Useful for testing and monitoring.
   *
   * @return cache size
   */
  public int size() {
    return cache.size();
  }
}
