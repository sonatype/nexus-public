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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;

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
   * The "bare" executor (non-Shiro aware), needed to gather stats.
   */
  private final ThreadPoolExecutor threadPoolExecutor;

  /**
   * The shiro aware executor wrapper service.
   *
   * Using this wrapper are the Jobs scheduled and made Shiro Subject equipped.
   */
  private final NexusExecutorService nexusExecutorService;

  /**
   * Used to block execution if thread pool is full.
   */
  private final Semaphore semaphore;

  private String instanceId;

  private String instanceName;

  public QuartzThreadPool(final int poolSize) {
    checkArgument(poolSize > 0, "Pool size must be greater than zero");

    this.threadPoolExecutor = new ThreadPoolExecutor(
        poolSize, // core-size
        poolSize, // max-size
        0L, // keep-alive
        TimeUnit.MILLISECONDS,
        new SynchronousQueue<>(), // no queuing
        new NexusThreadFactory("quartz", "nx-tasks"),
        new AbortPolicy());

    // wrapper for Shiro integration
    this.nexusExecutorService = NexusExecutorService.forFixedSubject(
        threadPoolExecutor, FakeAlmightySubject.TASK_SUBJECT);

    semaphore = new Semaphore(poolSize);
  }

  @Override
  public void setInstanceId(final String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public void setInstanceName(final String instanceName) {
    this.instanceName = instanceName;
  }

  @Override
  public int getPoolSize() {
    return threadPoolExecutor.getPoolSize();
  }

  //
  // Lifecycle
  //

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
        throw new RuntimeException(e);
      }
    }
  }

  //
  // Operations
  //

  @Override
  public boolean runInThread(final Runnable runnable) {
    try {
      semaphore.acquire();
      // this below is true as we do not use queue on executor combined with abort policy.
      // Meaning, if no exception, the task is accepted for execution
      // Use a wrapper to decrement the semaphore after the runnable completes
      nexusExecutorService.submit(semaphoreReleasingRunnable(runnable));
      return true;
    }
    catch (RejectedExecutionException | InterruptedException e) {
      // must decrement semaphore if job failed to submit
      semaphore.release();
      return false;
    }
  }

  /**
   * Wrap a runnable to release the semaphore after completion.
   */
  private Runnable semaphoreReleasingRunnable(final Runnable runnable) {
    return () -> {
        try {
          runnable.run();
        }
        finally {
          semaphore.release();
        }
    };
  }

  @Override
  public int blockForAvailableThreads() {
    try {
      semaphore.acquire();
      try {
        return semaphore.availablePermits() + 1;
      }
      finally {
        semaphore.release();
      }
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
