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
package org.sonatype.nexus.quartz.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.base.Throwables;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Nexus specific implementation of {@link ThreadPool} that is integrated with Shiro.
 *
 * @since 3.0
 */
public class QuartzThreadPool
    implements ThreadPool
{
  /**
   * The "bare" executor (non-Shiro aware), needed to implement blocking logic and gather some stats.
   */
  private final NexusThreadPoolExecutor threadPoolExecutor;

  /**
   * The shiro aware executor wrapper service. Using this wrapper are the Jobs scheduled and made Shiro
   * Subject equipped.
   */
  private final NexusExecutorService nexusExecutorService;

  public QuartzThreadPool(final int poolSize) {
    checkArgument(poolSize > 0, "Pool size must be greater than zero");
    this.threadPoolExecutor = new NexusThreadPoolExecutor(poolSize, poolSize,
        0L, TimeUnit.MILLISECONDS,
        new SynchronousQueue<Runnable>(), // no queuing
        new NexusThreadFactory("qz", "nx-tasks"),
        new AbortPolicy());
    // wrapper for Shiro integration
    this.nexusExecutorService = NexusExecutorService
        .forFixedSubject(threadPoolExecutor, FakeAlmightySubject.TASK_SUBJECT);
  }

  @Override
  public boolean runInThread(final Runnable runnable) {
    try {
      // this below is true as we do not use queue on executor
      // combined with abort policy. Meaning, if no exception,
      // the task is accepted for execution
      nexusExecutorService.submit(runnable);
      return true;
    }
    catch (RejectedExecutionException e) {
      return false;
    }
  }

  @Override
  public int blockForAvailableThreads() {
    try {
      threadPoolExecutor.getSemaphore().acquire();
      try {
        return threadPoolExecutor.getSemaphore().availablePermits() + 1;
      }
      finally {
        threadPoolExecutor.getSemaphore().release();
      }
    }
    catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void initialize() throws SchedulerConfigException {
    // nop
  }

  @Override
  public void shutdown(final boolean waitForJobsToComplete) {
    nexusExecutorService.shutdown();
    if (waitForJobsToComplete) {
      try {
        nexusExecutorService.awaitTermination(5L, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  @Override
  public int getPoolSize() {
    return threadPoolExecutor.getPoolSize();
  }

  @Override
  public void setInstanceId(final String schedInstId) {
    // ?
  }

  @Override
  public void setInstanceName(final String schedName) {
    // ?
  }

  // ==

  /**
   * Nexus specific thread pool executor that helps implementing the blocking logic using a Semaphore and using
   * the "hooks" on {@link ThreadPoolExecutor} class.
   */
  public static class NexusThreadPoolExecutor
      extends ThreadPoolExecutor
  {

    private final Semaphore semaphore;

    public NexusThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime,
                                   final TimeUnit unit,
                                   final BlockingQueue<Runnable> workQueue,
                                   final ThreadFactory threadFactory,
                                   final RejectedExecutionHandler handler)
    {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
      this.semaphore = new Semaphore(maximumPoolSize);
    }

    public Semaphore getSemaphore() {
      return semaphore;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
      try {
        semaphore.tryAcquire();
      }
      finally {
        super.beforeExecute(t, r);
      }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      try {
        semaphore.release();
      }
      finally {
        super.afterExecute(r, t);
      }
    }
  }
}
