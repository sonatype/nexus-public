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
package org.sonatype.nexus.internal.wonderland;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Mutex;
import org.sonatype.goodies.common.Time;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Manages cache (and expiration) of authentication tickets.
 *
 * @since 2.7
 */
@Named
public class AuthTicketCache
    extends ComponentSupport
{
  private static final String CPREFIX = "${wonderland.authTicketCache";

  private final Mutex lock = new Mutex();

  private final Map<String, Long> tokens = Maps.newHashMap();

  private final Time expireAfter;

  @Inject
  public AuthTicketCache(@Named(CPREFIX + ".expireAfter:-20s}") final Time expireAfter) {
    this.expireAfter = checkNotNull(expireAfter);
    log.debug("Expire after: {}", expireAfter);
  }

  @VisibleForTesting
  public AuthTicketCache() {
    this(Time.seconds(2));
  }

  private long now() {
    return System.currentTimeMillis();
  }

  /**
   * Expires any tokens older than {@link #expireAfter}.
   */
  private void expireTokens() {
    boolean trace = log.isTraceEnabled();
    long now = now();
    Iterator<Entry<String, Long>> iter = tokens.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, Long> entry = iter.next();
      if (isTokenExpired(now, entry)) {
        iter.remove();
        if (trace) {
          log.trace("Expired token: {}", entry.getKey());
        }
      }
    }

    if (trace && !tokens.isEmpty()) {
      log.trace("Valid tokens:");
      for (Entry<String, Long> entry : tokens.entrySet()) {
        log.trace("  {}", entry.getKey());
      }
    }
  }

  @VisibleForTesting
  protected boolean isTokenExpired(final long now, final Entry<String, Long> entry) {
    long diff = now - entry.getValue();
    return diff > expireAfter.toMillis();
  }

  /**
   * Add token to the cache.
   */
  public void add(final String token) {
    synchronized (lock) {
      expireTokens();
      // Sanity check we don't clobber tokens
      checkState(!tokens.containsKey(token), "Duplicate token"); //NON-NLS
      tokens.put(token, now());
    }
  }

  /**
   * Remove token from cache.
   *
   * @return True if the token existed (was added and not yet expired)
   */
  public boolean remove(final String token) {
    synchronized (lock) {
      expireTokens();
      Long tmp = tokens.remove(token);
      return tmp != null;
    }
  }
}
