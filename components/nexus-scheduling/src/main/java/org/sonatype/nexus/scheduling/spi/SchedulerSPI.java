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
package org.sonatype.nexus.scheduling.spi;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;

/**
 * The underlying scheduler that provides scheduling.
 *
 * @since 3.0
 */
public interface SchedulerSPI
    extends Lifecycle
{
  /**
   * Returns the SPI specific {@link ScheduleFactory}.
   */
  ScheduleFactory scheduleFactory();

  /**
   * Returns status message.
   */
  String renderStatusMessage();

  /**
   * Returns verbose detail message.
   */
  String renderDetailMessage();

  /**
   * Pause the scheduler.
   */
  void pause();

  /**
   * Resume the scheduler.
   */
  void resume();

  /**
   * Returns the task for the given identifier; or null if missing.
   */
  @Nullable
  TaskInfo getTaskById(String id);

  /**
   * Returns a list of all tasks which have been scheduled.
   */
  List<TaskInfo> listsTasks();

  /**
   * Returns description of triggers that were recovered after an error caused them to be lost
   *
   * @since 3.17
   */
  List<String> getMissingTriggerDescriptions();

  /**
   * Schedule a task with the given scheduler.
   *
   * If a task already exists with the same task identifier, the task will be updated.
   *
   * Task must not be running.
   */
  TaskInfo scheduleTask(TaskConfiguration config, Schedule schedule);

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
}
