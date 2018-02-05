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
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.MoreExecutors;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.event.EventHelper.asReplicating;
import static org.sonatype.nexus.common.event.EventHelper.isReplicating;
import static org.sonatype.nexus.internal.event.EventManagerImpl.HOST_THREAD_POOL_SIZE;

/**
 * Custom {@link Executor} used to dispatch events to {@link Asynchronous} subscribers.
 *
 * As Nexus starts, subscribers are called directly by the originating thread. Once the
 * TASKS phase is reached subscribers will be called asynchronously using a thread pool.
 *
 * Conversely as Nexus stops, the thread pool is shutdown after leaving the TASKS phase
 * and subscribers will again be called directly by the originating thread. This avoids
 * asynchronous subscribers from having services disappear beneath them.
 *
 * @since 3.2
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
class EventExecutor
    extends LifecycleSupport
    implements Executor
{
  private volatile Executor delegate;

  public EventExecutor() {
    // single-threaded until we reach TASKS phase
    this.delegate = MoreExecutors.directExecutor();
  }

  /**
   * Move from direct to asynchronous subscriber processing.
   */
  @Override
  protected void doStart() throws Exception {
    // direct hand-off used! Host pool will use caller thread to execute async subscribers when pool full!
    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        0,
        HOST_THREAD_POOL_SIZE,
        60L,
        TimeUnit.SECONDS,
        new SynchronousQueue<>(),
        new NexusThreadFactory("event", "event-manager"),
        new CallerRunsPolicy()
    );

    // begin distributing events in truly asynchronous fashion
    delegate = NexusExecutorService.forCurrentSubject(threadPool);
  }

  /**
   * Move from asynchronous to direct subscriber processing.
   */
  @Override
  protected void doStop() throws Exception {
    Executor currentExecutor = delegate;

    if (currentExecutor instanceof NexusExecutorService) {
      // go back to single-threaded for rest of shutdown
      delegate = MoreExecutors.directExecutor();

      // wait for all background event subscribers to finish to have consistent state
      ((NexusExecutorService) currentExecutor).shutdown();
      try {
        ((NexusExecutorService) currentExecutor).awaitTermination(5L, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        log.debug("Interrupted while waiting for termination", e);
      }
    }
  }

  /**
   * Used by UTs and ITs only, to "wait for calm period", when all the async event subscribers finished.
   */
  @VisibleForTesting
  boolean isCalmPeriod() {
    Executor currentExecutor = delegate;

    if (currentExecutor instanceof NexusExecutorService) {
      ThreadPoolExecutor threadPool = (ThreadPoolExecutor)
          ((NexusExecutorService) currentExecutor).getTargetExecutorService();

      // "calm period" is when we have no queued nor active threads
      return threadPool.getQueue().isEmpty() && threadPool.getActiveCount() == 0;
    }

    return true; // single-threaded mode
  }

  @Override
  public void execute(final Runnable command) {
    delegate.execute(inheritIsReplicating(command));
  }

  /**
   * Binds current "isReplicating" context to the {@link Runnable} regardless which thread executes it.
   */
  private static Runnable inheritIsReplicating(final Runnable command) {
    if (!isReplicating()) {
      return command; // no need to inherit flag
    }
    return () -> {
      // check state from context of thread doing the running
      if (!isReplicating()) {
        asReplicating(command); // set flag for duration of command
      }
      else {
        command.run(); // flag already set, maintain it
      }
    };
  }
}
