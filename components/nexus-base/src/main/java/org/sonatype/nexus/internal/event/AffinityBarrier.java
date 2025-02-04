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
package org.sonatype.nexus.internal.event;

import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Waits for any overdue event deliveries before starting the next event posting.
 * Overdue deliveries do not block the posting thread forever, but are bumped to
 * the next cycle when the timeout expires.
 *
 * @since 3.11
 */
class AffinityBarrier
    extends Phaser
{
  private static final Logger log = LoggerFactory.getLogger(AffinityBarrier.class);

  private static final ThreadLocal<AffinityBarrier> CURRENT_BARRIER = new ThreadLocal<>();

  private final Executor coordinator;

  private final Executor executor;

  private final Time timeout;

  private final AtomicInteger cycleCounter = new AtomicInteger(-1);

  public AffinityBarrier(final Executor coordinator, final Executor executor, final Time timeout) {
    super(1); // initialize parties to 1 to represent the posting thread

    this.coordinator = checkNotNull(coordinator);
    this.executor = checkNotNull(executor);
    this.timeout = timeout;
  }

  /**
   * Returns the {@link AffinityBarrier} assigned to the current thread, if there is one.
   */
  @Nullable
  public static AffinityBarrier current() {
    return CURRENT_BARRIER.get();
  }

  /**
   * Coordinates the asynchronous event delivery, by waiting for previous parties to complete.
   */
  public void coordinate(final Runnable command) {
    coordinator.execute(() -> {
      await(); // hold things back if the previous round of execution isn't finished yet
      CURRENT_BARRIER.set(this);
      try {
        command.run(); // new round of execution - eventually calls back into execute (below)
      }
      finally {
        CURRENT_BARRIER.remove();
      }
    });
  }

  /**
   * Executes the asynchronous event delivery, registering a new party to track when it's done.
   */
  public void execute(final Runnable command) {
    register();
    executor.execute(() -> {
      try {
        command.run();
      }
      finally {
        arriveAndDeregister();
      }
    });
  }

  /**
   * Waits for any previous event deliveries to complete before proceeding.
   */
  public void await() {
    int cycle = cycleCounter.getAndIncrement();
    while (cycle >= getPhase()) {
      try {
        if (cycle == getPhase()) {
          arrive(); // our turn, declare posting thread has arrived
        }
        // wait for all overdue parties to finish their deliveries
        awaitAdvanceInterruptibly(cycle, timeout.value(), timeout.unit());
      }
      catch (TimeoutException e) { // NOSONAR: don't bother logging unless we end up bumping

        // if we get here then some deliveries are still overdue and we need to bump them to the next cycle
        // - they might arrive while we're bumping them, but that's ok because they're technically arriving
        // during the next cycle and registrations vs arrivals will still line up - the thing we have to be
        // careful about is accidentally bumping things too much that we end up skipping a cycle, so we use
        // a temporary party to stop that from happening

        if (cycle == getPhase()) { // is it still our turn?
          register(); // temporary party to stop phaser running ahead
          int overdueParties = 0;
          while (cycle == getPhase()) { // is it still our turn? (cf. double-checked lock)
            overdueParties++;
            arrive(); // try to bump overdue deliveries one-by-one
          }
          if (overdueParties > 0) {
            log.debug("Bumping affinity barrier: {} parties overdue", overdueParties);
          }
          arriveAndDeregister(); // finally remove our temporary party
        }
      }
      catch (IllegalStateException | InterruptedException e) { // NOSONAR: no need to log full stack
        log.warn("Bypassing affinity barrier: {}", e.toString());
        break;
      }
    }
  }

  @Override
  protected boolean onAdvance(final int phase, final int registeredParties) {
    return false; // never terminate this phaser, always let it continue
  }
}
