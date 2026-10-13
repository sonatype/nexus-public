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
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

import org.sonatype.nexus.security.authc.RateLimitResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AuthRateLimiterServiceImplTest
{
  private AuthRateLimiterServiceImpl service;

  @Before
  public void setUp() {
    service = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000);
  }

  @After
  public void tearDown() {
    // nothing to stop — Guava cache uses no background threads by default
  }

  @Test
  public void testCheckAndRecord_allowsAttemptsUpToMax() {
    for (int i = 0; i < 5; i++) {
      assertThat("attempt " + (i + 1) + " should be allowed",
          service.checkAndRecord("alice"), is(nullValue()));
    }
  }

  @Test
  public void testCheckAndRecord_blocksAfterMaxAttempts() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }

    RateLimitResult result = service.checkAndRecord("alice");

    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(30L));
    assertThat(result.attemptCount(), is(6));
  }

  @Test
  public void testCheckAndRecord_exponentialBackoff() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }

    // Attempt 6: baseDelay * 2^0 = 30s
    assertThat(service.checkAndRecord("alice").retryAfterSeconds(), is(30L));
    // Attempt 7: baseDelay * 2^1 = 60s
    assertThat(service.checkAndRecord("alice").retryAfterSeconds(), is(60L));
    // Attempt 8: baseDelay * 2^2 = 120s
    assertThat(service.checkAndRecord("alice").retryAfterSeconds(), is(120L));
  }

  @Test
  public void testCheckAndRecord_capsAtMaxDelay() {
    AuthRateLimiterServiceImpl shortService = new AuthRateLimiterServiceImpl(1, 30L, 60L, 10000);
    // Attempt 1: allowed
    shortService.checkAndRecord("bob");
    // Attempt 2: blocked — 30 * 2^0 = 30s
    assertThat(shortService.checkAndRecord("bob").retryAfterSeconds(), is(30L));
    // Attempt 3: 30 * 2^1 = 60s (at cap)
    assertThat(shortService.checkAndRecord("bob").retryAfterSeconds(), is(60L));
    // Attempt 4: 30 * 2^2 = 120s → capped at 60s
    assertThat(shortService.checkAndRecord("bob").retryAfterSeconds(), is(60L));
  }

  @Test
  public void testCheck_returnsNullWhenNotBlocked() {
    // No prior attempts — check should pass
    assertThat(service.check("alice"), is(nullValue()));
  }

  @Test
  public void testCheck_returnsNullWhenBelowThreshold() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }
    // Exactly at threshold — not yet blocked
    assertThat(service.check("alice"), is(nullValue()));
  }

  @Test
  public void testCheck_returnsResultWhenBlocked() {
    // check() reports the time actually remaining on the block, not the nominal backoff, so it
    // needs a frozen clock: under a real clock any millisecond elapsed between the blocking
    // checkAndRecord() and this check() truncates 30s down to 29s.
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);
    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice");
    }
    // alice is now blocked; check should reflect that without incrementing
    RateLimitResult result = clockedService.check("alice");
    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(30L));
  }

  @Test
  public void testCheck_doesNotIncrementCounter() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }
    // Drive alice to first block
    service.checkAndRecord("alice"); // attempt 6 → retryAfter=30s

    // check() several times — should not advance backoff
    service.check("alice");
    service.check("alice");
    service.check("alice");

    // Next record call should still see attempt 7 backoff (60s), not further advanced
    RateLimitResult result = service.checkAndRecord("alice");
    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(60L));
    assertThat(result.attemptCount(), is(7));
  }

  @Test
  public void testRecordSuccess_resetsCounter() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }

    service.recordSuccess("alice");

    assertThat(service.checkAndRecord("alice"), is(nullValue()));
  }

  @Test
  public void testReset_resetsCounter() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }

    service.reset("alice");

    assertThat(service.checkAndRecord("alice"), is(nullValue()));
  }

  @Test
  public void testIsolation_differentUsersTrackedSeparately() {
    for (int i = 0; i <= 5; i++) {
      service.checkAndRecord("alice");
    }

    assertThat(service.checkAndRecord("alice"), is(notNullValue()));
    assertThat(service.checkAndRecord("bob"), is(nullValue()));
  }

  @Test
  public void testClass_hasConditionalOnPropertyForEnabledFlag() {
    ConditionalOnProperty annotation = AuthRateLimiterServiceImpl.class.getAnnotation(ConditionalOnProperty.class);

    assertThat("@ConditionalOnProperty must be present", annotation, is(notNullValue()));
    assertThat(Arrays.asList(annotation.name()), hasItem("nexus.auth.ratelimit.enabled"));
    assertThat(annotation.havingValue(), is("true"));
    assertThat(annotation.matchIfMissing(), is(true));
  }

  @Test
  public void testCheckAndRecord_overflowSafeAtHighAttemptCount() {
    // With baseDelaySeconds=30, baseDelaySeconds*(1L<<shift) overflows a long at shift=59
    // (failureCount=65 for maxAttempts=5), producing a negative retryAfter that placed
    // blockedUntilMs in the past, transiently disabling blocking for shifts 59-62.
    // MutableClock stays at EPOCH so blockedUntilMs in the future is verifiable via check().
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    // Drive to failureCount=64 (shift=58, safely below the overflow window)
    for (int i = 0; i < 64; i++) {
      clockedService.checkAndRecord("overflow-user");
    }

    // Shifts 59-62 (failureCount 65-68): the range where the old guard overflowed
    for (int expectedShift = 59; expectedShift <= 62; expectedShift++) {
      RateLimitResult result = clockedService.checkAndRecord("overflow-user");
      assertThat("must be blocked at shift " + expectedShift, result, is(notNullValue()));
      assertThat("retryAfter must be capped at maxDelay (not a negative overflow) at shift " + expectedShift,
          result.retryAfterSeconds(), is(3600L));
      // Clock still at EPOCH so blockedUntilMs must be in the future — check() must stay non-null
      assertThat("check() must report blocked at shift " + expectedShift,
          clockedService.check("overflow-user"), is(notNullValue()));
    }

    // Shifts 63+ still return maxDelay via the explicit >= 63 guard
    for (int i = 0; i < 6; i++) {
      clockedService.checkAndRecord("overflow-user");
    }
    RateLimitResult highResult = clockedService.checkAndRecord("overflow-user");
    assertThat("must be blocked past the overflow window", highResult, is(notNullValue()));
    assertThat(highResult.retryAfterSeconds(), is(3600L));
  }

  // Token-based rate limiting tests

  @Test
  public void testCheckByToken_allowsAttemptsUpToMax() {
    String tokenHash = "abc123def456";
    for (int i = 0; i < 5; i++) {
      assertThat("attempt " + (i + 1) + " should be allowed",
          service.checkAndRecordByToken(tokenHash), is(nullValue()));
    }
  }

  @Test
  public void testCheckByToken_blocksAfterMaxAttempts() {
    String tokenHash = "abc123def456";
    for (int i = 0; i < 5; i++) {
      service.checkAndRecordByToken(tokenHash);
    }

    RateLimitResult result = service.checkAndRecordByToken(tokenHash);

    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(30L));
    assertThat(result.attemptCount(), is(6));
  }

  @Test
  public void testCheckAndRecordByToken_exponentialBackoff() {
    String tokenHash = "abc123def456";
    for (int i = 0; i < 5; i++) {
      service.checkAndRecordByToken(tokenHash);
    }

    // Attempt 6: baseDelay * 2^0 = 30s
    assertThat(service.checkAndRecordByToken(tokenHash).retryAfterSeconds(), is(30L));
    // Attempt 7: baseDelay * 2^1 = 60s
    assertThat(service.checkAndRecordByToken(tokenHash).retryAfterSeconds(), is(60L));
  }

  @Test
  public void testCheckByToken_returnsNullWhenNotBlocked() {
    String tokenHash = "abc123def456";
    // No prior attempts — check should pass
    assertThat(service.checkByToken(tokenHash), is(nullValue()));
  }

  @Test
  public void testCheckByToken_returnsResultWhenBlocked() {
    // Frozen clock for the same reason as testCheck_returnsResultWhenBlocked: checkByToken()
    // returns remaining time, which a real clock erodes below the nominal 30s.
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);
    String tokenHash = "abc123def456";
    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecordByToken(tokenHash);
    }

    RateLimitResult result = clockedService.checkByToken(tokenHash);
    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(30L));
  }

  @Test
  public void testRecordSuccessByToken_resetsCounter() {
    String tokenHash = "abc123def456";
    for (int i = 0; i < 5; i++) {
      service.checkAndRecordByToken(tokenHash);
    }

    service.recordSuccessByToken(tokenHash);

    assertThat(service.checkAndRecordByToken(tokenHash), is(nullValue()));
  }

  @Test
  public void testIsolation_differentTokensTrackedSeparately() {
    String tokenHash1 = "token-hash-1";
    String tokenHash2 = "token-hash-2";

    // Block token1
    for (int i = 0; i <= 5; i++) {
      service.checkAndRecordByToken(tokenHash1);
    }

    // token1 should be blocked
    assertThat(service.checkAndRecordByToken(tokenHash1), is(notNullValue()));
    // token2 should still be allowed
    assertThat(service.checkAndRecordByToken(tokenHash2), is(nullValue()));
  }

  @Test
  public void testIsolation_tokensAndUsersTrackedSeparately() {
    String username = "alice";
    String tokenHash = "abc123def456";

    // Block the username
    for (int i = 0; i <= 5; i++) {
      service.checkAndRecord(username);
    }

    // Username should be blocked
    assertThat(service.checkAndRecord(username), is(notNullValue()));
    // Token should still be allowed (tracked separately)
    assertThat(service.checkAndRecordByToken(tokenHash), is(nullValue()));

    // Now block the token
    for (int i = 0; i <= 5; i++) {
      service.checkAndRecordByToken(tokenHash);
    }

    // Token should be blocked
    assertThat(service.checkAndRecordByToken(tokenHash), is(notNullValue()));
    // A different username should still be allowed
    assertThat(service.checkAndRecord("bob"), is(nullValue()));
  }

  @Test
  public void testCheckByToken_doesNotIncrementCounter() {
    String tokenHash = "abc123def456";
    for (int i = 0; i < 5; i++) {
      service.checkAndRecordByToken(tokenHash);
    }
    // Drive to first block
    service.checkAndRecordByToken(tokenHash); // attempt 6 → retryAfter=30s

    // check() several times — should not advance backoff
    service.checkByToken(tokenHash);
    service.checkByToken(tokenHash);
    service.checkByToken(tokenHash);

    // Next record call should still see attempt 7 backoff (60s), not further advanced
    RateLimitResult result = service.checkAndRecordByToken(tokenHash);
    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(60L));
    assertThat(result.attemptCount(), is(7));
  }

  // =====================
  // Time-bounded block tests (TC-T01 through TC-T04, TC-TK01, TC-TK02)
  // =====================

  @Test
  public void testCheck_stillBlockedBeforeDeadline() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice");
    }
    clock.advanceSeconds(29); // still within the 30s window

    assertThat("block must remain before deadline elapses",
        clockedService.check("alice"), is(notNullValue()));
  }

  @Test
  public void testCheck_releasedAfterBlockDeadline() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice");
    }
    clock.advanceSeconds(31); // past the 30s deadline

    assertThat("block must release after the advertised Retry-After elapses",
        clockedService.check("alice"), is(nullValue()));
  }

  @Test
  public void testCheck_retryAfterReflectsRemainingTime() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice");
    }
    clock.advanceSeconds(15); // half of the 30s window

    RateLimitResult result = clockedService.check("alice");
    assertThat(result, is(notNullValue()));
    assertThat("Retry-After must reflect remaining time, not the original full window",
        result.retryAfterSeconds(), is(15L));
  }

  @Test
  public void testCheck_repeatedCallsDoNotExtendBlockDeadline() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice");
    }
    // Simulate a client honoring Retry-After: poll once per second for 29s
    for (int i = 0; i < 29; i++) {
      clock.advanceSeconds(1);
      clockedService.check("alice");
    }
    assertThat("still blocked at 29s", clockedService.check("alice"), is(notNullValue()));

    clock.advanceSeconds(2); // now 31s total — past the 30s deadline
    assertThat("polling must not extend the block deadline",
        clockedService.check("alice"), is(nullValue()));
  }

  @Test
  public void testCheckAndRecord_failureCountPreservedAfterDeadline() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice"); // 6 failures; attempt 6 → retryAfter=30s
    }
    clock.advanceSeconds(31); // deadline elapsed; check() now returns null

    // Wrong password again after block expires — counter must NOT reset, backoff must escalate
    RateLimitResult result = clockedService.checkAndRecord("alice");
    assertThat(result, is(notNullValue()));
    assertThat("backoff must escalate from prior count (attempt 7 → 60s), not restart at 30s",
        result.retryAfterSeconds(), is(60L));
    assertThat(result.attemptCount(), is(7));
  }

  @Test
  public void testRecordSuccess_afterDeadline_resetsToCleanState() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    // Drive to first block (6 failures → 30s deadline)
    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice");
    }
    clock.advanceSeconds(31); // deadline elapsed
    assertThat("block released after deadline", clockedService.check("alice"), is(nullValue()));

    // Successful authentication clears the record
    clockedService.recordSuccess("alice");

    // First new failure after success must be allowed
    assertThat("first attempt after success must be allowed",
        clockedService.checkAndRecord("alice"), is(nullValue()));

    // Drive to a new block — backoff must start fresh at base delay, not escalated
    for (int i = 0; i < 4; i++) {
      clockedService.checkAndRecord("alice"); // attempts 2-5
    }
    RateLimitResult result = clockedService.checkAndRecord("alice"); // attempt 6 → 30s
    assertThat(result, is(notNullValue()));
    assertThat("backoff must restart at base delay after success clears the record",
        result.retryAfterSeconds(), is(30L));
    assertThat(result.attemptCount(), is(6));
  }

  @Test
  public void testCheckByToken_releasedAfterBlockDeadline() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);
    String tokenHash = "abc123def456";

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecordByToken(tokenHash);
    }
    clock.advanceSeconds(31);

    assertThat("token block must release after deadline",
        clockedService.checkByToken(tokenHash), is(nullValue()));
  }

  @Test
  public void testCheckByToken_repeatedCallsDoNotExtendBlockDeadline() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);
    String tokenHash = "abc123def456";

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecordByToken(tokenHash);
    }
    for (int i = 0; i < 29; i++) {
      clock.advanceSeconds(1);
      clockedService.checkByToken(tokenHash);
    }
    clock.advanceSeconds(2); // 31s total

    assertThat("polling must not extend the token block deadline",
        clockedService.checkByToken(tokenHash), is(nullValue()));
  }

  @Test
  public void testCheck_releasedAtExactDeadlineBoundary() {
    // The release condition is (now >= blockedUntilMs), so at exactly t=30s the block must release.
    // MutableClock starts at Instant.EPOCH (millis=0), so blockedUntilMs = 30_000 after the first block.
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecord("alice");
    }
    clock.advanceSeconds(30); // exactly at the deadline

    assertThat("block must release at exactly the deadline (>= condition)",
        clockedService.check("alice"), is(nullValue()));
  }

  @Test
  public void testCheckByToken_releasedAtExactDeadlineBoundary() {
    MutableClock clock = new MutableClock();
    AuthRateLimiterServiceImpl clockedService = new AuthRateLimiterServiceImpl(5, 30L, 3600L, 10000, clock);
    String tokenHash = "abc123def456";

    for (int i = 0; i <= 5; i++) {
      clockedService.checkAndRecordByToken(tokenHash);
    }
    clock.advanceSeconds(30);

    assertThat("token block must release at exactly the deadline (>= condition)",
        clockedService.checkByToken(tokenHash), is(nullValue()));
  }

  // MutableClock starts at Instant.EPOCH so millis() == 0 at t=0; this makes deadline arithmetic
  // exact in tests (blockedUntilMs = retryAfterSeconds * 1000, no sub-second remainder).
  private static final class MutableClock
      extends Clock
  {
    private Instant now = Instant.EPOCH;

    void advanceSeconds(long seconds) {
      now = now.plusSeconds(seconds);
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(final ZoneId zone) {
      throw new UnsupportedOperationException("MutableClock does not support withZone");
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
