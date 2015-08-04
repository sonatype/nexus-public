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

import org.sonatype.nexus.proxy.maven.routing.internal.task.CancelableRunnable;

/**
 * Simple {@link java.util.concurrent.Executor} like service, that offers a bit extra functionality in a way it can
 * guarantee no two concurrent commands are running under same key.
 * <p>
 * The main point of ConstrainedExecutor is to workaround limitation of RepositoryItemUidLock that does not provide a
 * mechanism to lock-with-timeout and to identify and cancel long-running lock holders. Together with
 * CancelableRunnable, ConstrainedExecutor provides ability to avoid multiple concurrent executions of autorouting
 * metadata update for the same repository.
 *
 * @author cstamas
 * @since 2.4
 */
public interface ConstrainedExecutor
{
  /**
   * Returns statistics about this instance.
   *
   * @return statistics.
   */
  Statistics getStatistics();

  /**
   * Cancels all executing jobs. This method call does not wait for all jobs to terminate, it will return
   * immediately.
   */
  void cancelAllJobs();

  /**
   * Returns {@code true} if there is a {@link CancelableRunnable} running with given key.
   *
   * @return {@code true} if there is active task running with given key.
   */
  boolean hasRunningWithKey(String key);

  /**
   * Returns {@code true} if there was a {@link CancelableRunnable} running with given key and was cancelled.
   *
   * @return {@code true} if there is active task running with given key.
   */
  boolean cancelRunningWithKey(String key);

  /**
   * Schedules a command for execution, or, if a command with given key already runs, will simply "forget" (do
   * nothing) with passed in command instance.
   *
   * @return {@code true} if command was scheduled, or {@code false} if dropped.
   */
  boolean mayExecute(String key, CancelableRunnable command);

  /**
   * Schedules a command for execution. If command with given key already runs, it will cancel it, and replace it
   * with
   * new instance passed in parameter.
   *
   * @return {@code true} if this command caused a cancelation of other already scheduled command, or {@code false}
   *         if
   *         not.
   */
  boolean mustExecute(String key, CancelableRunnable command);
}
