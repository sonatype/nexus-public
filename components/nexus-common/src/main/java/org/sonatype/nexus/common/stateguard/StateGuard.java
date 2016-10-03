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
package org.sonatype.nexus.common.stateguard;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.Locks;
import org.sonatype.goodies.common.Loggers;

import com.google.common.base.Throwables;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * State guard provides support to transition from state to state and execute an action, as well as guard
 * execution of an action if state is acceptable.
 *
 * @since 3.0
 */
public class StateGuard
{
  private final Logger log;

  private final ReadWriteLock readWriteLock;

  @Nullable
  private final String failure;

  private String current;

  StateGuard(final Logger log,
             final ReadWriteLock readWriteLock,
             final String initial,
             @Nullable final String failure)
  {
    this.log = checkNotNull(log);
    this.readWriteLock = checkNotNull(readWriteLock);
    this.current = checkNotNull(initial);
    this.failure = failure;
  }

  /**
   * Returns the current state.
   */
  public String getCurrent() {
    Lock lock = Locks.read(readWriteLock);
    try {
      return current;
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Check if the current state is given state.
   *
   * Code executed after may not have the same state, to execute and ensure state is allowed use {@link #guard} instead.
   */
  public boolean is(final String state) {
    Lock lock = Locks.read(readWriteLock);
    try {
      return current.equals(state);
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Ensure the current state is one of allowed.
   *
   * Code executed after may not have the same state, to execute and ensure state is allowed use {@link #guard} instead.
   */
  public void ensure(final String... allowed) {
    checkNotNull(allowed);
    checkArgument(allowed.length != 0);

    Lock lock = Locks.read(readWriteLock);
    try {
      _ensure(allowed);
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Ensure the current state is one of allowed, bypassing locking semantics.
   */
  private void _ensure(final String[] allowed) {
    for (String allow : allowed) {
      if (current.equals(allow)) {
        return;
      }
    }

    throw new IllegalStateException("Invalid state: " + current + "; allowed: " + Arrays.toString(allowed));
  }

  /**
   * Create a transition to given state.
   */
  public Transition transition(final String to) {
    return new TransitionImpl(to, false, new Class[0]);
  }

  /**
   * Create a transition to given state with custom exception-handling behaviour.
   */
  public Transition transition(final String to, final boolean silent, final Class<? extends Exception>[] ignore) {
    return new TransitionImpl(to, silent, ignore);
  }

  /**
   * Create a guard which allows execution in the given states.
   */
  public Guard guard(final String... allowed) {
    return new GuardImpl(allowed);
  }

  //
  // Transition
  //

  /**
   * Transition from current state to target state and execute an action.
   */
  private class TransitionImpl
    implements Transition
  {
    private final String to;

    private final boolean silent;

    private final Class<? extends Exception>[] ignore;

    @Nullable
    private String[] allowed;

    private TransitionImpl(final String to, final boolean silent, final Class<? extends Exception>[] ignore)    {
      this.to = checkNotNull(to);
      this.silent = silent;
      this.ignore = checkNotNull(ignore);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "to='" + to + '\'' +
          ", allowed=" + Arrays.toString(allowed) +
          '}';
    }

    public TransitionImpl from(final String... allowed) {
      checkNotNull(allowed);
      checkArgument(allowed.length != 0);
      this.allowed = allowed;
      return this;
    }

    @Override
    @Nullable
    public <V> V run(final Action<V> action) throws Exception {
      Lock lock = Locks.write(readWriteLock);
      try {
        if (allowed != null) {
          _ensure(allowed);
        }

        try {
          log.trace("Transitioning: {} -> {}", current, to);

          V result = action.run();
          current = to;

          log.trace("Transitioned: {}", to);

          return result;
        }
        catch (Throwable t) {
          if (ignore(t)) {
            current = to;

            log.trace("Transitioned: {} ignoring: {}", to, t.toString());
          }
          else {
            if (silent) {
              log.debug("Failed transition: {} -> {}", current, to, t);
            }
            else {
              log.error("Failed transition: {} -> {}", current, to, t);
            }
  
            // maybe set failure state
            if (failure != null) {
              current = failure;
            }
          }

          Throwables.propagateIfPossible(t, Exception.class, Error.class);
          throw Throwables.propagate(t);
        }
      }
      finally {
        lock.unlock();
      }
    }

    private boolean ignore(final Throwable t) {
      for (final Class<? extends Exception> type : ignore) {
        if (type.isInstance(t)) {
          return true;
        }
      }
      return false;
    }
  }

  //
  // Guard
  //

  /**
   * Execute an action or callable if current state is allowed.
   */
  private class GuardImpl
    implements Guard
  {
    private final String[] allowed;

    private GuardImpl(final String[] allowed) {
      checkNotNull(allowed);
      checkArgument(allowed.length != 0);
      this.allowed = allowed;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "allowed=" + Arrays.toString(allowed) +
          '}';
    }

    @Override
    @Nullable
    public <V> V run(final Action<V> action) throws Exception {
      checkNotNull(action);

      Lock lock = Locks.read(readWriteLock);
      try {
        _ensure(allowed);
        return action.run();
      }
      finally {
        lock.unlock();
      }
    }
  }

  //
  // Builder
  //

  /**
   * {@link StateGuard} builder.
   */
  public static class Builder
  {
    private static final Logger defaultLogger = Loggers.getLogger(StateGuard.class);

    private Logger logger;

    private ReadWriteLock lock;

    private String initial;

    private String failure;

    // final states?

    public Builder logger(final Logger logger) {
      this.logger = logger;
      return this;
    }

    public Builder lock(final ReadWriteLock lock) {
      this.lock = lock;
      return this;
    }

    public Builder initial(final String state) {
      this.initial = state;
      return this;
    }

    public Builder failure(final String state) {
      this.failure = state;
      return this;
    }

    public StateGuard create() {
      return new StateGuard(
          logger != null ? logger : defaultLogger,
          lock != null ? lock : new ReentrantReadWriteLock(),
          initial,
          failure
      );
    }
  }
}
