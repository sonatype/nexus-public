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
package org.sonatype.nexus.scheduling;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.sonatype.scheduling.NoSuchTaskException;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.schedules.Schedule;

/**
 * Scheduler component of Nexus, responsible for task management (their schedule, running, and configuration
 * persistence).
 *
 * @author cstamas
 */
public interface NexusScheduler
{
  /**
   * Initializes Nexus scheduler, but loading up tasks from persisted configuration and instantiating them.
   */
  void initializeTasks();

  /**
   * Performs a clean shutdown of the Nexus Scheduler.
   */
  void shutdown();

  /**
   * Issue a NexusTask for immediate execution, but have a control over it.
   */
  <T> ScheduledTask<T> submit(String name, NexusTask<T> nexusTask)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Issue a NexusTask for scheduled execution.
   */
  <T> ScheduledTask<T> schedule(String name, NexusTask<T> nexusTask, Schedule schedule)
      throws RejectedExecutionException, NullPointerException;

  /**
   * Update parameters of a scheduled task
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

  /**
   * A factory for tasks.
   *
   * @deprecated prefer the createTaskInstance(Class<T> type) method instead.
   */
  @Deprecated
  NexusTask<?> createTaskInstance(String taskType)
      throws IllegalArgumentException;

  /**
   * A factory for tasks.
   */
  <T> T createTaskInstance(Class<T> taskType)
      throws IllegalArgumentException;
}
