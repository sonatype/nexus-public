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
package org.sonatype.nexus.tasklog;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Adds the {@link TaskLogCleanupTask} to the quartz cron definition in the database
 * 
 * @since 3.5
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class TaskLogCleanupQuartz
    extends StateGuardLifecycleSupport
{
  private final TaskScheduler taskScheduler;

  private final String taskLogCleanupCron;

  @Inject
  public TaskLogCleanupQuartz(
      final TaskScheduler taskScheduler,
      @Named("${nexus.tasks.log.cleanup.cron:-0 0 0 * * ?}") final String taskLogCleanupCron)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.taskLogCleanupCron = checkNotNull(taskLogCleanupCron);
  }

  @Override
  protected void doStart() throws Exception {
    if (!taskScheduler.listsTasks()
        .stream()
        .anyMatch((info) -> TaskLogCleanupTaskDescriptor.TYPE_ID.equals(info.getConfiguration().getTypeId()))) {
      TaskConfiguration configuration = taskScheduler.createTaskConfigurationInstance(
          TaskLogCleanupTaskDescriptor.TYPE_ID);
      Schedule schedule = taskScheduler.getScheduleFactory().cron(new Date(), taskLogCleanupCron);
      taskScheduler.scheduleTask(configuration, schedule);
    }
  }
}
