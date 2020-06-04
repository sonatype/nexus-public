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
package org.sonatype.nexus.repository.cache;

import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maintains cache details a cached resource. {@link CacheController} relies on information provided by this
 * class to implement "aging" and cache invalidation.
 *
 * @since 3.0
 */
public class CacheInfo
{
  /**
   * Key of asset's nested map of cache related properties.
   *
   * @see CacheInfo
   */
  @VisibleForTesting
  public static final String CACHE = "cache";

  /**
   * Cache token {@link String}.
   */
  @VisibleForTesting
  public static final String CACHE_TOKEN = "cache_token";

  /**
   * Last verified {@link Date}.
   */
  @VisibleForTesting
  public static final String LAST_VERIFIED = "last_verified";

  /**
   * Cache token used to mark individually invalidated assets.
   */
  @VisibleForTesting
  public static final String INVALIDATED = "invalidated";

  private final DateTime lastVerified;

  @Nullable
  private final String cacheToken;

  public CacheInfo(final DateTime lastVerified, @Nullable final String cacheToken) {
    this.lastVerified = checkNotNull(lastVerified);
    this.cacheToken = cacheToken;
  }

  /**
   * Returns the {@link DateTime} when this item was last verified and detected as "fresh".
   */
  public DateTime getLastVerified() {
    return lastVerified;
  }

  /**
   * Returns the "cache token" that was in effect when this item was last verified.
   */
  @Nullable
  public String getCacheToken() {
    return cacheToken;
  }

  /**
   * Returns whether this item has been individually invalidated from the cache.
   *
   * @since 3.12
   */
  public boolean isInvalidated() {
    return INVALIDATED.equals(cacheToken);
  }

  /**
   * @since 3.24
   */
  public Map<String, String> toMap() {
    ImmutableMap.Builder<String, String> cacheHeaders = ImmutableMap.builder();
    cacheHeaders.put(LAST_VERIFIED, lastVerified.toString());
    if (cacheToken != null) {
      cacheHeaders.put(CACHE_TOKEN, cacheToken);
    }
    return cacheHeaders.build();
  }

  /**
   * @since 3.24
   */
  @Nullable
  public static CacheInfo fromMap(final Map<String, String> map) {
    if (map != null) {
      return new CacheInfo(new DateTime(map.get(LAST_VERIFIED)), map.get(CACHE_TOKEN));
    }
    return null;
  }

  /**
   * @since 3.24
   */
  @Nullable
  public static CacheInfo fromMap(final AttributesMap map) {
    if (map != null) {
      return new CacheInfo(new DateTime(map.get(LAST_VERIFIED)), map.get(CACHE_TOKEN, String.class));
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "lastVerified=" + lastVerified +
        ", cacheToken='" + cacheToken + '\'' +
        '}';
  }
}
