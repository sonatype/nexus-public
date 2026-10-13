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

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sonatype.nexus.security.authc.AuthRateLimiterService;
import org.sonatype.nexus.security.authc.RateLimitResult;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Uses a Guava {@link Cache} with last-accessed expiration and soft values. Soft references
 * allow the GC to reclaim entries under memory pressure, preventing an attacker from exhausting
 * heap by submitting logins for arbitrary bogus usernames. Idle entries are automatically evicted
 * after {@code maxDelaySeconds} of inactivity.
 * </p>
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

  private static final String TOKEN_PREFIX = "token::";

  private final int maxAttempts;

  private final long baseDelaySeconds;

  private final long maxDelaySeconds;

  private final Clock clock;

  private final LoadingCache<String, AttemptRecord> attempts;

  @Autowired
  public AuthRateLimiterServiceImpl(
      @Value("${nexus.auth.ratelimit.max-attempts:3}") final int maxAttempts,
      @Value("${nexus.auth.ratelimit.base-delay-seconds:30}") final long baseDelaySeconds,
      @Value("${nexus.auth.ratelimit.max-delay-seconds:900}") final long maxDelaySeconds,
      @Value("${nexus.auth.ratelimit.max-tracked-keys:10000}") final int maxTrackedKeys)
  {
    this(maxAttempts, baseDelaySeconds, maxDelaySeconds, maxTrackedKeys, Clock.systemUTC());
  }

  AuthRateLimiterServiceImpl(
      final int maxAttempts,
      final long baseDelaySeconds,
      final long maxDelaySeconds,
      final int maxTrackedKeys,
      final Clock clock)
  {
    this.maxAttempts = maxAttempts;
    this.baseDelaySeconds = baseDelaySeconds;
    this.maxDelaySeconds = maxDelaySeconds;
    this.clock = clock;

    this.attempts = CacheBuilder.newBuilder()
        .expireAfterAccess(maxDelaySeconds, TimeUnit.SECONDS)
        .softValues()
        .maximumSize(maxTrackedKeys)
        .build(CacheLoader.from(AttemptRecord::new));
  }

  @Override
  @Nullable
  public RateLimitResult check(final String username) {
    return checkForKey(USER_PREFIX + username);
  }

  @Override
  @Nullable
  public RateLimitResult checkAndRecord(final String username) {
    return checkAndRecordForKey(USER_PREFIX + username);
  }

  @Override
  public void recordSuccess(final String username) {
    attempts.invalidate(USER_PREFIX + username);
    log.debug("Rate limit counter cleared on success for user '{}'", username);
  }

  @Override
  public void reset(final String username) {
    attempts.invalidate(USER_PREFIX + username);
    log.debug("Rate limit counter reset for user '{}'", username);
  }

  @Override
  @Nullable
  public RateLimitResult checkByToken(final String tokenHash) {
    return checkForKey(TOKEN_PREFIX + tokenHash);
  }

  @Override
  @Nullable
  public RateLimitResult checkAndRecordByToken(final String tokenHash) {
    return checkAndRecordForKey(TOKEN_PREFIX + tokenHash);
  }

  @Override
  public void recordSuccessByToken(final String tokenHash) {
    attempts.invalidate(TOKEN_PREFIX + tokenHash);
    log.debug("Rate limit counter cleared on success for token hash prefix '{}'",
        tokenHash.substring(0, Math.min(8, tokenHash.length())));
  }

  @Nullable
  private RateLimitResult checkForKey(final String key) {
    AttemptRecord record = attempts.getIfPresent(key);
    if (record == null) {
      return null;
    }
    synchronized (record) {
      if (record.failureCount <= maxAttempts) {
        return null;
      }
      long now = clock.millis();
      if (now >= record.blockedUntilMs) {
        // Deadline has passed — allow this request through so valid credentials can be evaluated.
        // The failure counter is intentionally preserved: a subsequent wrong password keeps
        // escalating the backoff (resetting it here would grant maxAttempts fresh guesses every
        // base-delay-seconds, undermining the CWE-307 control from NEXUS-50643).
        //
        // Note: blockedUntilMs is NOT updated here; it remains in the past until the next call to
        // checkAndRecordForKey() sets a new deadline. This is intentional: check() is read-only
        // and is only called as a pre-auth guard immediately before checkAndRecord() on a failed
        // credential, so the stale value is always overwritten before the next block window begins.
        return null;
      }
      long remainingSeconds = Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(record.blockedUntilMs - now));
      return new RateLimitResult(remainingSeconds, record.failureCount);
    }
  }

  private RateLimitResult checkAndRecordForKey(final String key) {
    AttemptRecord record = attempts.getUnchecked(key);
    synchronized (record) {
      // Intentionally increment even when already rate-limited: each blocked attempt
      // advances the exponential backoff, discouraging repeated brute-force retries.
      record.failureCount++;

      if (record.failureCount <= maxAttempts) {
        return null;
      }

      long shift = record.failureCount - maxAttempts - 1;
      // Guard before the multiply: baseDelaySeconds*(1L<<shift) overflows a long when shift≥59
      // with baseDelaySeconds=30. The condition `baseDelaySeconds > (maxDelaySeconds >> shift)` is
      // equivalent to `baseDelaySeconds*(1L<<shift) > maxDelaySeconds` without risk of overflow.
      long retryAfter = (shift >= 63 || baseDelaySeconds > (maxDelaySeconds >> shift))
          ? maxDelaySeconds
          : baseDelaySeconds * (1L << shift);

      record.blockedUntilMs = clock.millis() + TimeUnit.SECONDS.toMillis(retryAfter);

      // Log first block at INFO to leave a clear signal in production logs without spamming.
      if (record.failureCount == maxAttempts + 1) {
        log.info("Rate limiting key '{}': attempt={}, retryAfter={}s", key, record.failureCount, retryAfter);
      }
      else {
        log.debug("Rate limiting key '{}': attempt={}, retryAfter={}s", key, record.failureCount, retryAfter);
      }
      return new RateLimitResult(retryAfter, record.failureCount);
    }
  }

  private static final class AttemptRecord
  {
    int failureCount;

    // Epoch-ms deadline set by checkAndRecordForKey() once failureCount exceeds maxAttempts.
    // A value of 0 is safe: checkForKey() only reads this field when failureCount > maxAttempts,
    // and checkAndRecordForKey() always writes it before returning at that threshold, so
    // blockedUntilMs == 0 is unreachable from checkForKey() in normal execution.
    long blockedUntilMs;
  }
}
