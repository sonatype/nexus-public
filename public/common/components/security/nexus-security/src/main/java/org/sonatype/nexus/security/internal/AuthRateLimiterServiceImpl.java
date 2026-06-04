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
package org.sonatype.nexus.security.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;

import org.sonatype.nexus.security.authc.AuthRateLimiterService;
import org.sonatype.nexus.security.authc.RateLimitResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.inject.Singleton;

import static org.sonatype.nexus.common.app.FeatureFlags.AUTH_RATE_LIMIT_ENABLED;

/**
 * Default implementation of {@link AuthRateLimiterService} using in-memory tracking with
 * exponential backoff. Tracks failed attempts per username; a request is blocked when
 * the username exceeds the configured failure threshold.
 *
 * <p>
 * <strong>Cluster limitation:</strong> State is held in-memory on each node and is not
 * shared across cluster members. In a multi-node deployment an attacker can distribute
 * attempts across nodes and receive up to {@code maxAttempts} free tries per node before
 * any single node begins blocking. This is a known limitation of this implementation.
 * </p>
 */
@Component
@Singleton
@ConditionalOnProperty(name = AUTH_RATE_LIMIT_ENABLED, havingValue = "true", matchIfMissing = true)
public class AuthRateLimiterServiceImpl
    implements AuthRateLimiterService
{
  private static final Logger log = LoggerFactory.getLogger(AuthRateLimiterServiceImpl.class);

  private static final String USER_PREFIX = "user::";

  private final int maxAttempts;

  private final long baseDelaySeconds;

  private final long maxDelaySeconds;

  private final int maxTrackedKeys;

  private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

  private final ScheduledExecutorService cleaner;

  public AuthRateLimiterServiceImpl(
      @Value("${nexus.auth.ratelimit.max-attempts:3}") final int maxAttempts,
      @Value("${nexus.auth.ratelimit.base-delay-seconds:30}") final long baseDelaySeconds,
      @Value("${nexus.auth.ratelimit.max-delay-seconds:900}") final long maxDelaySeconds,
      @Value("${nexus.auth.ratelimit.max-tracked-keys:10000}") final int maxTrackedKeys)
  {
    this.maxAttempts = maxAttempts;
    this.baseDelaySeconds = baseDelaySeconds;
    this.maxDelaySeconds = maxDelaySeconds;
    this.maxTrackedKeys = maxTrackedKeys;

    this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "auth-rate-limit-cleaner");
      t.setDaemon(true);
      return t;
    });
    this.cleaner.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
  }

  @Override
  @Nullable
  public RateLimitResult checkAndRecord(final String username) {
    return checkAndRecordForKey(USER_PREFIX + username);
  }

  @Override
  public void recordSuccess(final String username) {
    attempts.remove(USER_PREFIX + username);
    log.debug("Rate limit counter cleared on success for user '{}'", username);
  }

  @Override
  public void reset(final String username) {
    attempts.remove(USER_PREFIX + username);
    log.debug("Rate limit counter reset for user '{}'", username);
  }

  @PreDestroy
  public void stop() {
    cleaner.shutdownNow();
  }

  private RateLimitResult checkAndRecordForKey(final String key) {
    if (attempts.size() >= maxTrackedKeys && !attempts.containsKey(key)) {
      log.warn("Rate limit tracker full ({} entries); skipping tracking for '{}'", maxTrackedKeys, key);
      return null;
    }
    AttemptRecord record = attempts.computeIfAbsent(key, k -> new AttemptRecord());
    synchronized (record) {
      // Intentionally increment even when already rate-limited: each blocked attempt
      // advances the exponential backoff, discouraging repeated brute-force retries.
      record.failureCount++;
      record.lastFailureTimeMs = System.currentTimeMillis();

      if (record.failureCount <= maxAttempts) {
        return null;
      }

      long shift = record.failureCount - maxAttempts - 1;
      long retryAfter = (shift >= 63)
          ? maxDelaySeconds
          : Math.min(baseDelaySeconds * (1L << shift), maxDelaySeconds);
      log.debug("Rate limiting key '{}': attempt={}, retryAfter={}s", key, record.failureCount, retryAfter);
      return new RateLimitResult(retryAfter, record.failureCount);
    }
  }

  private void cleanup() {
    long cutoffMs = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(maxDelaySeconds);
    // Note: removeIf and computeIfAbsent (in checkAndRecordForKey) are not atomically coordinated.
    // In the narrow window between removeIf evicting an entry and the synchronized block in
    // checkAndRecordForKey acquiring it, a new AttemptRecord could be inserted and then immediately
    // evicted. The worst-case effect is that a counter is silently reset — an acceptable trade-off
    // given the rarity of the race and the self-correcting nature of the backoff algorithm.
    attempts.entrySet().removeIf(entry -> {
      synchronized (entry.getValue()) {
        return entry.getValue().lastFailureTimeMs < cutoffMs;
      }
    });
  }

  private static final class AttemptRecord
  {
    int failureCount;

    long lastFailureTimeMs;
  }
}
