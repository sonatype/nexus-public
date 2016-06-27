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
   * Returns the state of a given task across the nodes in a clustered environment or {@code null} if clustering isn't
   * enabled.
   * 
   * @since 3.1
   */
  @Nullable
  List<ClusteredTaskState> getClusteredTaskStateById(String taskId);
}
