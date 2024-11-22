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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.NotWritableException;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * An {@link ExecutorService} that tries to delay all submitted tasks until the database is writable.
 * It doesn't retry if the database goes read-only after a task has been started.
 * If the database doesn't become writable within the time limit, then the task is run anyway.
 *
 * @since 3.16
 */
@Named
@Singleton
@ManagedLifecycle(phase = STORAGE)
public class DatabaseStatusDelayedExecutor
    extends StateGuardLifecycleSupport
    implements ExecutorService
{
  private final FreezeService freezeService;

  private final int delayedExecutorThreadPoolSize;

  private final int sleepInterval;

  private final int maxRetries;

  private ExecutorService executor;

  @Inject
  public DatabaseStatusDelayedExecutor(
      final FreezeService freezeService,
      @Named("${nexus.delayedExecutor.threadPoolSize:-1}") final int delayedExecutorThreadPoolSize,
      @Named("${nexus.delayedExecutor.sleepIntervalMs:-5000}") final int sleepInterval,
      @Named("${nexus.delayedExecutor.maxRetries:-8640}") final int maxRetries)
  {
    this.freezeService = checkNotNull(freezeService);
    checkArgument(delayedExecutorThreadPoolSize > 0, delayedExecutorThreadPoolSize);
    this.delayedExecutorThreadPoolSize = delayedExecutorThreadPoolSize;
    checkArgument(sleepInterval > 0, sleepInterval);
    this.sleepInterval = sleepInterval;
    checkArgument(maxRetries > 0, maxRetries);
    this.maxRetries = maxRetries;
  }

  private Runnable wrap(final Runnable runnable) {
    Callable<?> callable = wrap(Executors.callable(runnable));
    return () -> {
      try {
        callable.call();
      }
      catch (Exception e) {
        log.warn("Unexpected exception running task.", e);
      }
    };
  }

  private <T> Callable<T> wrap(final Callable<T> callable) {
    return () -> {
      try {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
          try {
            freezeService.checkWritable("Task needs writable database");
            return callable.call();
          }
          catch (NotWritableException e) {
            log.debug("Waiting for database to become writable.", e);
            Thread.sleep(sleepInterval);
          }
        }
        log.warn("Hit retry limit waiting for a writable database.");
        return callable.call();
      }
      catch (InterruptedException e) {
        log.warn("Interrupted while waiting to call task.", e);
        return null;
      }
    };
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
