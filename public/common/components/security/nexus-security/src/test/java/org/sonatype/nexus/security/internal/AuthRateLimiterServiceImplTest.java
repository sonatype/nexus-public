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
    for (int i = 0; i <= 5; i++) {
      service.checkAndRecord("alice");
    }
    // alice is now blocked; check should reflect that without incrementing
    RateLimitResult result = service.check("alice");
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
    for (int i = 0; i < 5 + 65; i++) {
      service.checkAndRecord("overflow-user");
    }

    RateLimitResult result = service.checkAndRecord("overflow-user");

    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(3600L));
  }

  // Token-based rate limiting tests

  @Test
  public void testCheckByToken_allowsAttemptsUpToMax() {
    String tokenHash = "abc123def456";
    for (int i = 0; i < 5; i++) {
      assertThat("attempt " + (i + 1) + " should be allowed",
          service.checkByToken(tokenHash), is(nullValue()));
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
    String tokenHash = "abc123def456";
    for (int i = 0; i <= 5; i++) {
      service.checkAndRecordByToken(tokenHash);
    }

    RateLimitResult result = service.checkByToken(tokenHash);
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
}
