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
package org.sonatype.nexus.repository.negativecache;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Status;

/**
 * Negative cache management {@link Facet}.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface NegativeCacheFacet
    extends Facet
{
  /**
   * Retrieve an entry from negative cache.
   *
   * @param key cache key
   * @return cached {@link Status} or null if no cache entry found
   */
  @Nullable
  Status get(NegativeCacheKey key);

  /**
   * Add an entry to negative cache
   *
   * @param key    cache key
   * @param status (404) status to be cached
   */
  void put(NegativeCacheKey key, Status status);

  /**
   * Removes an entry from negative cache.
   *
   * @param key cache key
   */
  void invalidate(NegativeCacheKey key);

  /**
   * Removes entry for passed in parent key and all is children (using {@link NegativeCacheKey#isParentOf(NegativeCacheKey)}).
   *
   * @param key parent cache key
   */
  void invalidateSubset(NegativeCacheKey key);

  /**
   * Removes all entries from negative cache.
   */
  void invalidate();

  /**
   * Retrieves the cache key based on context.
   *
   * @param context view context
   * @return cache key
   */
  NegativeCacheKey getCacheKey(Context context);
}
