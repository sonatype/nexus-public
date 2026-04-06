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
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Utility class for extracting cache-related attributes from asset metadata.
 *
 * @since 3.73
 */
public final class CacheAttributeUtils
{
  private CacheAttributeUtils() {
    // utility class
  }

  /**
   * Extracts the last_verified timestamp from cache attributes.
   * This is only present for proxy repository assets.
   * The value is stored as a Joda DateTime ISO string (e.g., "2024-02-27T10:30:00.000Z").
   *
   * @param attributes The attributes map containing cache data
   * @return Last verified date, or null if not available
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static Date extractLastVerified(final Map<String, Object> attributes) {
    if (attributes == null) {
      return null;
    }

    try {
      // First try to get cache.last_verified (for full attributes map)
      Map<String, Object> cacheAttributes = (Map<String, Object>) attributes.get("cache");
      Object lastVerifiedObj = null;

      if (cacheAttributes != null) {
        lastVerifiedObj = cacheAttributes.get("last_verified");
      }
      else {
        // Try direct access (for when attributes IS the cache map)
        lastVerifiedObj = attributes.get("last_verified");
      }

      if (lastVerifiedObj instanceof String) {
        // Parse Joda DateTime ISO string format
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(lastVerifiedObj);
        return dateTime.toDate();
      }
      else if (lastVerifiedObj instanceof Long) {
        return new Date((Long) lastVerifiedObj);
      }
    }
    catch (Exception e) {
      // Silently ignore - this attribute only exists for proxy repos
    }

    return null;
  }

  /**
   * Extracts the last_verified timestamp from cache attributes as an Optional.
   *
   * @param attributes The attributes map containing cache data
   * @return Optional containing last verified date, or empty if not available
   */
  public static Optional<Date> extractLastVerifiedAsOptional(final Map<String, Object> attributes) {
    return Optional.ofNullable(extractLastVerified(attributes));
  }
}
