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
package org.sonatype.nexus.blobstore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Ticker;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RateLimitedWarn}.
 */
public class RateLimitedWarnTest
{
  private static final long INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);

  private final AtomicLong now = new AtomicLong(TimeUnit.HOURS.toNanos(5));

  private final Ticker ticker = new Ticker()
  {
    @Override
    public long read() {
      return now.get();
    }
  };

  private Logger log;

  private RateLimitedWarn limiter;

  // slf4j has fixed-arity overloads; the limiter always uses the varargs one
  private static Object[] args(final Object... args) {
    return args;
  }

  @Before
  public void setUp() {
    log = mock(Logger.class);
    limiter = new RateLimitedWarn(log, INTERVAL_NANOS, ticker);
  }

  @Test
  public void firstFailureIsLoggedAtWarn() {
    limiter.warn(new RuntimeException("boom"), "failed {}", "x");

    verify(log).warn("failed {}", args("x"));
    verify(log, never()).debug(anyString(), any(Object[].class));
  }

  @Test
  public void repeatedFailuresWithinIntervalAreDemotedToDebugWithTheException() {
    RuntimeException boom = new RuntimeException("boom");
    limiter.warn(boom, "failed {}", "x");
    now.addAndGet(TimeUnit.SECONDS.toNanos(59));
    limiter.warn(boom, "failed {}", "x");
    limiter.warn(boom, "failed {}", "x");

    verify(log, times(1)).warn(anyString(), any(Object[].class));
    // demoted lines keep the full exception (as the trailing argument)
    verify(log, times(2)).debug("failed {}", args("x", boom));
  }

  @Test
  public void warnsAgainAfterIntervalAndReportsHowManyWereSuppressed() {
    RuntimeException boom = new RuntimeException("boom");
    limiter.warn(boom, "failed {}", "x");
    limiter.warn(boom, "failed {}", "x");
    limiter.warn(boom, "failed {}", "x");
    now.addAndGet(INTERVAL_NANOS);
    limiter.warn(boom, "failed {}", "x");

    verify(log).warn("failed {}", args("x"));
    verify(log).warn("failed {} ({} similar failures suppressed in the last interval)", args("x", 2L));
  }

  @Test
  public void suppressedCountResetsAfterBeingReported() {
    RuntimeException boom = new RuntimeException("boom");
    limiter.warn(boom, "failed {}", "x");
    limiter.warn(boom, "failed {}", "x");
    now.addAndGet(INTERVAL_NANOS);
    limiter.warn(boom, "failed {}", "x");
    now.addAndGet(INTERVAL_NANOS);
    limiter.warn(boom, "failed {}", "x");

    // second interval had nothing suppressed, so the plain message is used again
    verify(log, times(2)).warn("failed {}", args("x"));
    verify(log, times(1)).warn("failed {} ({} similar failures suppressed in the last interval)", args("x", 1L));
  }

  @Test
  public void worksWhenTickerIsNegative() {
    now.set(-TimeUnit.HOURS.toNanos(1));
    limiter.warn(new RuntimeException("boom"), "failed {}", "x");

    verify(log).warn("failed {}", args("x"));
  }
}
