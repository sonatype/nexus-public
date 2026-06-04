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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

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
    service.stop();
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
    // Exhaust initial free attempts
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
    try {
      // Attempt 1: allowed
      shortService.checkAndRecord("bob");
      // Attempt 2: blocked — 30 * 2^0 = 30s
      assertThat(shortService.checkAndRecord("bob").retryAfterSeconds(), is(30L));
      // Attempt 3: 30 * 2^1 = 60s (at cap)
      assertThat(shortService.checkAndRecord("bob").retryAfterSeconds(), is(60L));
      // Attempt 4: 30 * 2^2 = 120s → capped at 60s
      assertThat(shortService.checkAndRecord("bob").retryAfterSeconds(), is(60L));
    }
    finally {
      shortService.stop();
    }
  }

  @Test
  public void testRecordSuccess_resetsCounter() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }

    service.recordSuccess("alice");

    // Counter cleared — first attempt allowed again
    assertThat(service.checkAndRecord("alice"), is(nullValue()));
  }

  @Test
  public void testReset_resetsCounter() {
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }

    service.reset("alice");

    // Counter cleared — first attempt allowed again
    assertThat(service.checkAndRecord("alice"), is(nullValue()));
  }

  @Test
  public void testIsolation_differentUsersTrackedSeparately() {
    for (int i = 0; i <= 5; i++) {
      service.checkAndRecord("alice");
    }

    // alice is rate-limited, but bob has a clean slate
    assertThat(service.checkAndRecord("alice"), is(notNullValue()));
    assertThat(service.checkAndRecord("bob"), is(nullValue()));
  }

  @Test
  public void testCleanup_evictsStaleEntries() throws Exception {
    // Populate an entry for alice
    for (int i = 0; i < 5; i++) {
      service.checkAndRecord("alice");
    }
    assertThat(service.checkAndRecord("alice"), is(notNullValue())); // alice is rate-limited

    // Set lastFailureTimeMs to epoch so the entry is beyond maxDelaySeconds old
    Field attemptsField = AuthRateLimiterServiceImpl.class.getDeclaredField("attempts");
    attemptsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    ConcurrentHashMap<String, Object> attempts =
        (ConcurrentHashMap<String, Object>) attemptsField.get(service);
    Object record = attempts.get("user::alice");
    Field lastFailureField = record.getClass().getDeclaredField("lastFailureTimeMs");
    lastFailureField.setAccessible(true);
    lastFailureField.setLong(record, 0L); // epoch — older than maxDelaySeconds

    // Invoke the private cleanup() method
    Method cleanupMethod = AuthRateLimiterServiceImpl.class.getDeclaredMethod("cleanup");
    cleanupMethod.setAccessible(true);
    cleanupMethod.invoke(service);

    // Stale entry was evicted; alice's counter starts fresh
    assertThat(service.checkAndRecord("alice"), is(nullValue()));
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
    // With maxAttempts=5, at failureCount=70, shift=64 which wraps to 0 without the overflow fix,
    // collapsing retryAfter back to baseDelaySeconds instead of remaining capped at maxDelaySeconds.
    for (int i = 0; i < 5 + 65; i++) {
      service.checkAndRecord("overflow-user");
    }

    RateLimitResult result = service.checkAndRecord("overflow-user");

    assertThat(result, is(notNullValue()));
    assertThat(result.retryAfterSeconds(), is(3600L)); // must remain capped, not collapse to 30
  }
}
