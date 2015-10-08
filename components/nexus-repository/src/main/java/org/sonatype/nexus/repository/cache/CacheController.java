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

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;

import org.joda.time.DateTime;

/**
 * A support class which implements basic cache-control logic.
 *
 * @since 3.0
 */
public class CacheController
    extends ComponentSupport
{
  public static String newCacheToken() {
    return Long.toString(System.nanoTime());
  }

  private final int contentMaxAgeSeconds;

  private volatile String cacheToken;

  public CacheController(final int contentMaxAgeSeconds, @Nullable final String cacheToken) {
    this.contentMaxAgeSeconds = contentMaxAgeSeconds;
    this.cacheToken = cacheToken;
  }

  /**
   * After invoking this method, all {@link #isStale(CacheInfo)} checks will return true that has {@link CacheInfo} not
   * carrying same token as created in this method.
   */
  public void invalidateCache() {
    this.cacheToken = newCacheToken();
  }

  /**
   * Returns the currently effective {@link CacheInfo} with "now" timestamp.
   */
  public CacheInfo current() {
    return new CacheInfo(DateTime.now(), cacheToken);
  }

  /**
   * Returns {@code true} if passed in cache info carries stale information, detected either by cache token or
   * age of the info.
   */
  public boolean isStale(final CacheInfo cacheInfo) {
    if (cacheToken != null && !cacheToken.equals(cacheInfo.getCacheToken())) {
      log.debug("Content expired (cacheToken)");
      return true;
    }
    if (contentMaxAgeSeconds < 0) {
      log.trace("Content max age checking disabled");
      return false;
    }
    if (cacheInfo.getLastVerified().isBefore(new DateTime().minusSeconds(contentMaxAgeSeconds))) {
      log.debug("Content expired (age)");
      return true;
    }
    return false;
  }
}
