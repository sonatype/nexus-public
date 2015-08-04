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
package org.sonatype.scheduling;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import org.sonatype.scheduling.schedules.Schedule;

public interface Scheduler
{
  /**
   * Loads up persisted tasks from TaskConfigManager and initializes all of them (to call on startup).
   */
  void initializeTasks();

  /**
   * Shuts down the scheduler cleanly.
   */
  void shutdown();

  /**
   * Initialize a task on bootup.
   */
  <T> ScheduledTask<T> initialize(String id, String name, String type, Callable<T> callable, Schedule schedule,
                                  boolean enabled)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Issue a Runnable for immediate execution, but have a control over it.
   */
  ScheduledTask<Object> submit(String name, Runnable runnable)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Issue a Runnable for scheduled execution.
   */
  ScheduledTask<Object> schedule(String name, Runnable runnable, Schedule schedule)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Issue a Callable for immediate execution, but have a control over it.
   */
  <T> ScheduledTask<T> submit(String name, Callable<T> callable)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Issue a Runnable for scheduled execution.
   */
  <T> ScheduledTask<T> schedule(String name, Callable<T> callable, Schedule schedule)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Issue a Runnable for scheduled execution.
   */
  <T> ScheduledTask<T> updateSchedule(ScheduledTask<T> task)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Returns the map of currently active tasks. The resturned collection is an unmodifiable snapshot. It may differ
   * from current one (if some thread finishes for example during processing of the returned list).
   */
  Map<String, List<ScheduledTask<?>>> getActiveTasks();

  /**
   * Returns the map of all tasks. The resturned collection is an unmodifiable snapshot. It may differ from current
   * one (if some thread finishes for example during processing of the returned list).
   */
  Map<String, List<ScheduledTask<?>>> getAllTasks();

  /**
   * Returns an active task by it's ID.
   */
  ScheduledTask<?> getTaskById(String id)
      throws NoSuchTaskException;

  @Deprecated
  SchedulerTask<?> createTaskInstance(String taskType)
      throws IllegalArgumentException;

  /**
   * A factory for tasks.
   */
  <T> T createTaskInstance(Class<T> taskType)
      throws IllegalArgumentException;
}
