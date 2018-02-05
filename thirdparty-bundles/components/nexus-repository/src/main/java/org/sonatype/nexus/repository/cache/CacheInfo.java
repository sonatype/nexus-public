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

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.time.DateHelper.toDate;
import static org.sonatype.nexus.common.time.DateHelper.toDateTime;

/**
 * Maintains cache details a cached resource. {@link CacheController} relies on information provided by this
 * class to implement "aging" and cache invalidation.
 *
 * @since 3.0
 */
public class CacheInfo
{
  /**
   * Key of {@link Asset} nested map of cache related properties.
   *
   * @see CacheInfo
   */
  @VisibleForTesting
  static final String CACHE = "cache";

  /**
   * Cache token {@link String}.
   */
  @VisibleForTesting
  static final String CACHE_TOKEN = "cache_token";

  /**
   * Last verified {@link Date}.
   */
  @VisibleForTesting
  static final String LAST_VERIFIED = "last_verified";

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

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "lastVerified=" + lastVerified +
        ", cacheToken='" + cacheToken + '\'' +
        '}';
  }

  /**
   * Extracts non-format specific cache info from passed in {@link Asset}.
   */
  @Nullable
  public static CacheInfo extractFromAsset(final Asset asset) {
    checkNotNull(asset);
    final NestedAttributesMap proxyCache = asset.attributes().child(CACHE);
    final DateTime lastVerified = toDateTime(proxyCache.get(LAST_VERIFIED, Date.class));
    if (lastVerified == null) {
      return null;
    }
    final String cacheToken = proxyCache.get(CACHE_TOKEN, String.class);
    return new CacheInfo(lastVerified, cacheToken);
  }

  /**
   * Applies non-format specific cache info attributes onto passed in {@link Asset}.
   */
  public static void applyToAsset(final Asset asset, final CacheInfo cacheInfo) {
    checkNotNull(asset);
    checkNotNull(cacheInfo);
    final NestedAttributesMap proxyCache = asset.attributes().child(CACHE);
    proxyCache.set(LAST_VERIFIED, toDate(cacheInfo.getLastVerified()));
    proxyCache.set(CACHE_TOKEN, cacheInfo.getCacheToken());
  }
}
