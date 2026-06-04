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
package org.sonatype.nexus.thread;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import org.springframework.stereotype.Component;

/**
 * An {@link ExecutorService} wrapper that executes tasks using a fixed thread pool.
 * This previously delayed tasks when the database was frozen, but that functionality
 * has been removed. Now it simply delegates to the underlying executor.
 *
 * @since 3.16
 */
@Component
@ManagedLifecycle(phase = STORAGE)
public class DatabaseStatusDelayedExecutor
    extends StateGuardLifecycleSupport
    implements ExecutorService
{
  private final int delayedExecutorThreadPoolSize;

  private ExecutorService executor;

  @Autowired
  public DatabaseStatusDelayedExecutor(
      @Value("${nexus.delayedExecutor.threadPoolSize:1}") final int delayedExecutorThreadPoolSize)
  {
    checkArgument(delayedExecutorThreadPoolSize > 0, delayedExecutorThreadPoolSize);
    this.delayedExecutorThreadPoolSize = delayedExecutorThreadPoolSize;
  }

  private Runnable wrap(final Runnable runnable) {
    // No longer need to check freeze status - just return the runnable as-is
    return runnable;
  }

  private <T> Callable<T> wrap(final Callable<T> callable) {
    // No longer need to check freeze status - just return the callable as-is
    return callable;
  }

  @Override
  @Guarded(by = NEW)
  protected void doStart() {
    executor = NexusExecutorService.forFixedSubject(
        newFixedThreadPool(delayedExecutorThreadPoolSize,
            new NexusThreadFactory("status-delayed-tasks", "status-delayed-tasks")),
        FakeAlmightySubject.TASK_SUBJECT);
  }

  @Override
  @Guarded(by = STARTED)
  protected void doStop() {
    executor.shutdownNow();
    executor = null;
  }

  @Override
  @Guarded(by = STARTED)
  public void shutdown() {
    executor.shutdown();
  }

  @Override
  @Guarded(by = STARTED)
  public List<Runnable> shutdownNow() {
    return executor.shutdownNow();
  }

  @Override
  @Guarded(by = STARTED)
  public boolean isShutdown() {
    return executor.isShutdown();
  }

  @Override
  @Guarded(by = STARTED)
  public boolean isTerminated() {
    return executor.isTerminated();
  }

  @Override
  @Guarded(by = STARTED)
  public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }

  @Override
  @Guarded(by = STARTED)
  public <T> Future<T> submit(final Callable<T> task) {
    return executor.submit(wrap(task));
  }

  @Override
  @Guarded(by = STARTED)
  public <T> Future<T> submit(final Runnable task, final T result) {
    return executor.submit(wrap(task), result);
  }

  @Override
  @Guarded(by = STARTED)
  public Future<?> submit(final Runnable task) {
    return executor.submit(wrap(task));
  }

  @Override
  @Guarded(by = STARTED)
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return executor.invokeAll(wrapAll(tasks));
  }

  @Override
  @Guarded(by = STARTED)
  public <T> List<Future<T>> invokeAll(
      final Collection<? extends Callable<T>> tasks,
      final long timeout,
      final TimeUnit unit) throws InterruptedException
  {
    return executor.invokeAll(wrapAll(tasks), timeout, unit);
  }

  @Override
  @Guarded(by = STARTED)
  public <T> T invokeAny(
      final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
  {
    return executor.invokeAny(wrapAll(tasks));
  }

  @Override
  @Guarded(by = STARTED)
  public <T> T invokeAny(
      final Collection<? extends Callable<T>> tasks,
      final long timeout,
      final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
  {
    return executor.invokeAny(wrapAll(tasks), timeout, unit);
  }

  @Override
  @Guarded(by = STARTED)
  public void execute(final Runnable command) {
    executor.execute(wrap(command));
  }

  @VisibleForTesting
  void setExecutor(final ExecutorService testExecutor) {
    this.executor = checkNotNull(testExecutor);
  }

  private <T> Collection<? extends Callable<T>> wrapAll(final Collection<? extends Callable<T>> tasks) {
    return tasks.stream().map(this::wrap).collect(toList());
  }
}
