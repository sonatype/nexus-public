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
package org.sonatype.nexus.common.cooperation2.datastore.internal;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.sonatype.nexus.common.cooperation2.Config;
import org.sonatype.nexus.common.cooperation2.CooperationException;
import org.sonatype.nexus.common.cooperation2.CooperationKey;
import org.sonatype.nexus.common.cooperation2.IOCall;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagateIfPossible;
import static java.lang.Boolean.TRUE;

/**
 * {@link CompletableFuture} that has various features added to help with cooperation.
 *
 * @since 3.14
 */
public class CooperatingFuture<T>
    extends CompletableFuture<T>
{
  protected static final Logger log = LoggerFactory.getLogger(CooperatingFuture.class);

  private static final ThreadLocal<Boolean> callInProgress = new ThreadLocal<>();

  private final AtomicLong staggerTimeMillis = new AtomicLong(System.currentTimeMillis());

  private final AtomicInteger threadCount = new AtomicInteger(1);

  private final CooperationKey requestKey;

  private final Config config;

  public CooperatingFuture(final CooperationKey requestKey, final Config config) {
    this.requestKey = checkNotNull(requestKey);
    this.config = checkNotNull(config);
  }

  /**
   * Performs the given I/O request and updates this future with any result or error.
   */
  public T call(final Function<Boolean, T> request) throws IOException {
    return performCall(request, false);
  }

  /**
   * Cooperates on the given I/O request by waiting for the lead thread to complete.
   */
  public T cooperate(final Function<Boolean, T> request) throws IOException {
    increaseCooperation();
    try {
      if (isNestedCall()) {
        // I/O dependency; use shorter timeout and be prepared to failover and repeat the request
        // (just in case the thread we're waiting for ends up waiting for our initial I/O request)
        return waitForCall(request, config.minorTimeout(), true);
      }
      else {
        // initial I/O request; use longer timeout and disallow repeated failover attempts
        return waitForCall(request, config.majorTimeout(), false);
      }
    }
    catch (ExecutionException e) { // NOSONAR unwrap and report download errors
      log.debug("Cooperative wait failed on {}", this, e.getCause());
      propagateIfPossible(e.getCause(), IOException.class);
      throw new IOException("Cooperative wait failed on " + this, e.getCause());
    }
    catch (CancellationException | InterruptedException e) {
      log.debug("Cooperative wait cancelled on {}", this, e);
      throw new CooperationException("Cooperative wait cancelled on " + this);
    }
    finally {
      decreaseCooperation();
    }
  }

  @VisibleForTesting
  public String getRequestKey() {
    return requestKey.getKey();
  }

  @VisibleForTesting
  public int getThreadCount() {
    return threadCount.get();
  }

  @Override
  public String toString() {
    return requestKey.getKey() + " (" + threadCount.get() + " threads cooperating)";
  }

  /**
   * Fluent method that performs I/O and stores the result in this future, before passing it back.
   */
  protected T performCall(final Function<Boolean, T> request, final boolean failover) throws IOException {
    boolean nested = isNestedCall();
    try {
      if (!nested) {
        callInProgress.set(TRUE);
      }
      log.debug("Requesting {}", this);
      T value = request.apply(failover);
      log.debug("Completing {}", this);
      complete(value);
      return value;
    }
    catch (Exception | Error e) { // NOSONAR report all errors to cooperating threads
      log.debug("Completing {} with exception", this, e);
      completeExceptionally(e);
      throw e;
    }
    finally {
      if (!nested) {
        callInProgress.remove();
      }
    }
  }

  /**
   * Cooperatively waits for the lead thread; may failover and repeat the request if allowed.
   */
  protected T waitForCall(
      final Function<Boolean, T> request,
      final Duration initialTimeout,
      final boolean failover) throws InterruptedException, ExecutionException, IOException
  {
    if (initialTimeout.isZero() || initialTimeout.isNegative()) {
      log.debug("Attempt cooperative wait on {}", this);
      return get(); // wait indefinitely
    }

    Duration timeout = initialTimeout;
    if (failover) {
      timeout = staggerTimeout(timeout); // preserve minimum gap between failover attempts
    }

    try {
      log.debug("Attempt cooperative wait on {} for {}", this, timeout);
      return get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (TimeoutException e) {
      log.debug("Cooperative wait timed out on {}", this, e);

      if (failover) {
        return performCall(request, true); // failover and repeat request in case lead thread is stuck
      }

      throw new CooperationException("Cooperative wait timed out on " + this);
    }
  }

  /**
   * @return {@code true} if the current thread is already inside a cooperative {@link IOCall}
   */
  private static boolean isNestedCall() {
    return TRUE.equals(callInProgress.get());
  }

  /**
   * Increases the cooperation count by one.
   *
   * @throws CooperationException if increasing the count would breach the given limit.
   */
  private void increaseCooperation() {
    int limit = config.threadsPerKey();
    // try to avoid depleting entire request pool with waiting threads
    threadCount.getAndUpdate(count -> {
      if (limit > 0 && count >= limit) {
        log.debug("Thread cooperation maxed for {}", this);
        throw new CooperationException("Thread cooperation maxed for " + this);
      }
      return count + 1;
    });
  }

  /**
   * Decreases the cooperation count by one.
   */
  private void decreaseCooperation() {
    threadCount.decrementAndGet();
  }

  /**
   * @return staggered timeout that makes sure waiting threads don't all wake-up at the same time
   */
  @VisibleForTesting
  Duration staggerTimeout(final Duration gap) {
    long currentTimeMillis = System.currentTimeMillis();

    // atomically progress the staggered time
    long prevTimeMillis, nextTimeMillis;
    do {
      prevTimeMillis = staggerTimeMillis.get();
      nextTimeMillis = Math.max(prevTimeMillis + gap.toMillis(), currentTimeMillis);
    }
    while (!staggerTimeMillis.compareAndSet(prevTimeMillis, nextTimeMillis));

    return Duration.ofMillis(nextTimeMillis - currentTimeMillis);
  }
}
