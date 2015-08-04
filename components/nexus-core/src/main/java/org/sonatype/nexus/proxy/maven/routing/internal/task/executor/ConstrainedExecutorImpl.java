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
package org.sonatype.nexus.proxy.maven.routing.internal.task.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

import org.sonatype.nexus.proxy.maven.routing.internal.task.CancelableRunnable;
import org.sonatype.nexus.proxy.maven.routing.internal.task.CancelableSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of Executor that adds a thin layer around {@link java.util.concurrent.Executor} that is
 * passed
 * in from constructor.
 *
 * @author cstamas
 * @since 2.4
 */
public class ConstrainedExecutorImpl
    implements ConstrainedExecutor
{
  /**
   * Plain executor for background batch-updates. This executor runs 1 periodic thread (see constructor) that
   * performs
   * periodic remote prexif list update, but also executes background "force" updates (initiated by user over REST or
   * when repository is added). But, as background threads are bounded by presence of proxy repositories, and
   * introduce hard limit of possible max executions, it protects this instance that is basically unbounded.
   */
  private final java.util.concurrent.Executor executor;

  /**
   * Plain map holding the repository IDs of repositories being batch-updated in background as keys. All read-write
   * access to this set must happen within synchronized block, using this instance as monitor object.
   */
  private final HashMap<String, CancelableRunnableWrapper> currentlyRunningCancelableRunnables;

  /**
   * Plain map holding the key semaphores for all keys that has scheduled jobs.
   */
  private final HashMap<String, Semaphore> currentlyRunningSemaphores;

  /**
   * Constructor.
   */
  public ConstrainedExecutorImpl(final java.util.concurrent.Executor executor) {
    this.executor = checkNotNull(executor);
    this.currentlyRunningCancelableRunnables = new HashMap<String, CancelableRunnableWrapper>();
    this.currentlyRunningSemaphores = new HashMap<String, Semaphore>();
  }

  @Override
  public synchronized Statistics getStatistics() {
    return new Statistics(new HashSet<String>(currentlyRunningCancelableRunnables.keySet()));
  }

  @Override
  public synchronized void cancelAllJobs() {
    for (CancelableRunnable command : currentlyRunningCancelableRunnables.values()) {
      command.cancel();
    }
    currentlyRunningCancelableRunnables.clear();
    currentlyRunningSemaphores.clear();
  }

  @Override
  public synchronized boolean hasRunningWithKey(String key) {
    checkNotNull(key);
    return currentlyRunningCancelableRunnables.containsKey(key);
  }

  @Override
  public synchronized boolean cancelRunningWithKey(String key) {
    checkNotNull(key);
    final CancelableRunnableWrapper oldCommand = currentlyRunningCancelableRunnables.get(key);
    if (oldCommand != null) {
      oldCommand.cancel();
    }
    return oldCommand != null;
  }

  @Override
  public synchronized boolean mayExecute(final String key, final CancelableRunnable command) {
    checkNotNull(key);
    checkNotNull(command);
    final CancelableRunnableWrapper oldCommand = currentlyRunningCancelableRunnables.get(key);
    if (oldCommand != null) {
      return false;
    }
    final CancelableRunnableWrapper wrappedCommand =
        new CancelableRunnableWrapper(this, key, getSemaphore(key), command);
    currentlyRunningCancelableRunnables.put(key, wrappedCommand);
    executor.execute(wrappedCommand);
    return true;
  }

  @Override
  public synchronized boolean mustExecute(final String key, final CancelableRunnable command) {
    checkNotNull(key);
    checkNotNull(command);
    final boolean canceledOldJob = cancelRunningWithKey(key);
    final CancelableRunnableWrapper wrappedCommand =
        new CancelableRunnableWrapper(this, key, getSemaphore(key), command);
    currentlyRunningCancelableRunnables.put(key, wrappedCommand);
    executor.execute(wrappedCommand);
    return canceledOldJob;
  }

  // ==

  protected Semaphore getSemaphore(final String key) {
    if (!currentlyRunningSemaphores.containsKey(key)) {
      currentlyRunningSemaphores.put(key, new Semaphore(1));
    }
    return currentlyRunningSemaphores.get(key);
  }

  protected synchronized void cancelableStopping(final CancelableRunnableWrapper wrappedCommand) {
    if (!wrappedCommand.isCanceled()) {
      currentlyRunningCancelableRunnables.remove(wrappedCommand.getKey());
      currentlyRunningSemaphores.remove(wrappedCommand.getKey());
    }
  }

  // ==

  protected static class CancelableRunnableWrapper
      implements CancelableRunnable
  {
    private final ConstrainedExecutorImpl host;

    private final String key;

    private final Semaphore semaphore;

    private final CancelableRunnable runnable;

    private final CancelableSupport cancelableSupport;

    public CancelableRunnableWrapper(final ConstrainedExecutorImpl host, final String key,
                                     final Semaphore semaphore, final CancelableRunnable runnable)
    {
      this.host = checkNotNull(host);
      this.key = checkNotNull(key);
      this.semaphore = checkNotNull(semaphore);
      this.runnable = checkNotNull(runnable);
      this.cancelableSupport = new CancelableSupport();
    }

    public String getKey() {
      return key;
    }

    @Override
    public void run() {
      try {
        semaphore.acquire();
        if (isCanceled()) {
          return;
        }
        try {
          runnable.run();
        }
        finally {
          host.cancelableStopping(this);
        }
      }
      catch (InterruptedException e) {
        //
      }
      finally {
        semaphore.release();
      }
    }

    @Override
    public boolean isCanceled() {
      return cancelableSupport.isCanceled();
    }

    @Override
    public void cancel() {
      runnable.cancel();
      cancelableSupport.cancel();
    }
  }
}
