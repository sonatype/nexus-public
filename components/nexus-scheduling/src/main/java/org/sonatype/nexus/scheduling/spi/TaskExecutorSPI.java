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

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.schedule.Schedule;

/**
 * The underlying scheduler that provides scheduling.
 *
 * @since 3.0
 */
public interface TaskExecutorSPI
{
  /**
   * Returns the info of a NX task by it's ID, if present. If no task present with ID then {@code null}.
   */
  @Nullable
  TaskInfo getTaskById(String id);

  /**
   * Returns the list of defined NX tasks.
   */
  List<TaskInfo> listsTasks();

  /**
   * Schedules a NX task with given schedule. If given task configuration existed, it will be updated, if not,
   * added. Task must not be running.
   */
  TaskInfo scheduleTask(TaskConfiguration taskConfiguration, Schedule schedule);

  /**
   * Reschedules a NX task with given schedule. Task might be running. If no task found, {@code null} is returned.
   */
  TaskInfo rescheduleTask(String id, Schedule schedule);

  /**
   * Returns the count of currently running tasks.
   *
   * TODO: this is used in tests only, figure out do we really need it
   */
  int getRunningTaskCount();
}
