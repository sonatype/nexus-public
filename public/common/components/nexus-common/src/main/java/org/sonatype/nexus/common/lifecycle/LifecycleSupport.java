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
package org.sonatype.nexus.common.lifecycle;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

/**
 * Support for {@link Lifecycle} implementations.
 */
public class LifecycleSupport
    implements Lifecycle
{
  /**
   * Maximum time to wait when acquiring the lifecycle lock, to avoid potential deadlocks.
   */
  private static final long LOCK_TIMEOUT_SECONDS = 60;

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Lock lock = new ReentrantLock();

  @VisibleForTesting
  enum State
  {
    NEW,
    STARTED,
    STOPPED,
    FAILED
  }

  private volatile State current = State.NEW;

  /**
   * Log transition messages.
   */
  protected void logTransition(final String message) {
    log.debug(message);
  }

  /**
   * Log transition failure messages.
   */
  protected void logTransitionFailure(final String message, final Throwable cause) {
    log.error(message, cause);
  }

  /**
   * Check if current state is given state.
   */
  @VisibleForTesting
  boolean is(final State state) {
    return current == state;
  }

  /**
   * Ensure current state is one of allowed states.
   *
   * Must be called within scope of lock.
   */
  private void ensure(final State... allowed) {
    for (State allow : allowed) {
      if (current == allow) {
        return;
      }
    }

    throw new IllegalStateException("Invalid state: " + current + "; allowed: " + Arrays.toString(allowed));
  }

  /**
   * Acquire the lifecycle lock with a timeout to avoid potential deadlocks.
   */
  private void acquireLock() {
    try {
      if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new RuntimeException("Failed to obtain lock after " + LOCK_TIMEOUT_SECONDS + " seconds");
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  //
  // Start
  //

  @Override
  public final void start() throws Exception {
    ensure(State.NEW, State.STOPPED); // check state before taking lock
    acquireLock();
    try {
      ensure(State.NEW, State.STOPPED); // check again now we have lock
      try {
        logTransition("Starting");
        doStart();
        current = State.STARTED;
        logTransition("Started");
      }
      catch (Throwable failure) {
        doFailed("start", failure);
      }
    }
    finally {
      lock.unlock();
    }
  }

  protected void doStart() throws Exception {
    // empty
  }

  protected boolean isStarted() {
    return is(State.STARTED);
  }

  protected void ensureStarted() {
    checkState(isStarted(), "Not started");
  }

  //
  // Stop
  //

  @Override
  public final void stop() throws Exception {
    ensure(State.STARTED); // check state before taking lock
    acquireLock();
    try {
      ensure(State.STARTED); // check again now we have lock
      try {
        logTransition("Stopping");
        doStop();
        current = State.STOPPED;
        logTransition("Stopped");
      }
      catch (Throwable failure) {
        doFailed("stop", failure);
      }
    }
    finally {
      lock.unlock();
    }
  }

  protected void doStop() throws Exception {
    // empty
  }

  protected boolean isStopped() {
    return is(State.STOPPED);
  }

  protected void ensureStopped() {
    checkState(isStopped(), "Not stopped");
  }

  //
  // Failed
  //

  protected void doFailed(final String operation, final Throwable cause) throws Exception {
    logTransitionFailure("Lifecycle operation " + operation + " failed", cause);
    current = State.FAILED;
    Throwables.propagateIfPossible(cause, Exception.class);
    throw propagate(cause);
  }

  protected boolean isFailed() {
    return is(State.FAILED);
  }

  private static RuntimeException propagate(Throwable throwable) {
    Throwables.throwIfUnchecked(throwable);
    throw new RuntimeException(throwable);
  }
}
