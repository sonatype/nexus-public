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
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

/**
 * Application task scheduling facade.
 *
 * Provides a high-level API around {@link SchedulerSPI}.
 */
public interface TaskScheduler
{
  /**
   * Returns the factory to create tasks.
   */
  TaskFactory getTaskFactory();

  /**
   * Returns the factory to create schedules.
   */
  ScheduleFactory getScheduleFactory();

  /**
   * Create a configuration for the given type-id.
   */
  TaskConfiguration createTaskConfigurationInstance(String typeId);

  /**
   * Issues a task for immediate execution, giving control over it with returned {@link Future} instance.
   *
   * Tasks executed via this method are executed as soon as possible, and are not persisted.
   */
  TaskInfo submit(TaskConfiguration configuration);

  /**
   * Returns the {@link TaskInfo} of a task by it's ID, if present, otherwise {@code null}.
   */
  @Nullable
  TaskInfo getTaskById(String id);

  /**
   * List existing tasks.
   */
  List<TaskInfo> listsTasks();

  /**
   * Schedules a task for execution based on given schedule.
   *
   * If existing task with ID exists, it will be replaced.
   *
   * Task must not be running.
   */
  TaskInfo scheduleTask(TaskConfiguration configuration, Schedule schedule);

  /**
   * Returns the count of currently running tasks.
   */
  int getRunningTaskCount();

  /**
   * Returns the count of tasks executed so far.
   *
   * @since 3.7
   */
  int getExecutedTaskCount();

  /**
   * Attempts to cancel execution of the task ({@code id}).  This attempt will
   * fail if the task has already completed, has already been cancelled,
   *  or could not be cancelled for some other reason.
   *
   * @return {@code false} if the task could not be cancelled,
   * typically because it has already completed normally;
   * {@code true} otherwise
   *
   * @since 3.19
   */
  boolean cancel(String id, boolean mayInterruptIfRunning);

  /**
   * Returns the {@link TaskInfo} of the first task with type ID matching {@code typeId}, otherwise {@code null}.
   */
  @Nullable
  TaskInfo getTaskByTypeId(String typeId);

  /**
   * Returns the {@link TaskInfo} of the first task with type ID matching {@code typeId}
   * and {@link TaskConfiguration} matching {@code config}, otherwise {@code null}.
   * <p/>
   * All entries in {@code config} must match entries in the task's {@link TaskConfiguration} to be
   * considered a match. Any entries of {@code config} with either a null key or null value will be ignored.
   */
  @Nullable
  TaskInfo getTaskByTypeId(String typeId, Map<String, String> config);

  /**
   * Find the first task with type ID matching {@code typeId}.
   * <p/>
   * If found, submit the task for execution if it is not already running.
   *
   * @param typeId task type ID
   * @return {@code true} if a task is found, {@code false} otherwise
   */
  boolean findAndSubmit(String typeId);

  /**
   * Find the first task with type ID matching {@code typeId} and {@link TaskConfiguration} matching {@code config}.
   * <p/>
   * All entries in {@code config} must match entries in the task's {@link TaskConfiguration} to be
   * considered a match. Any entries of {@code config} with either a null key or null value will be ignored.
   * <p/>
   * If found, don't submit the task for execution just confirm the waiting/running state with a boolean value
   *
   * @param typeId task type ID
   * @return {@code true} if a task is found waiting or already running, {@code false} otherwise
   */
  boolean findWaitingTask(String typeId, Map<String, String> config);

  /**
   * Find the first task with type ID matching {@code typeId} and {@link TaskConfiguration} matching {@code config}.
   * <p/>
   * All entries in {@code config} must match entries in the task's {@link TaskConfiguration} to be
   * considered a match. Any entries of {@code config} with either a null key or null value will be ignored.
   * <p/>
   * If found, submit the task for execution if it is not already running.
   *
   * @param typeId task type ID
   * @return {@code true} if a task is found, {@code false} otherwise
   */
  boolean findAndSubmit(String typeId, Map<String, String> config);

  /**
   * Returns the {@link ExternalTaskState} appropriate for the corresponding {@link TaskInfo}.
   *
   * @since 3.20
   */
  ExternalTaskState toExternalTaskState(TaskInfo taskInfo);
}
