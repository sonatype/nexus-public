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
   * @since 3.next
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
}
