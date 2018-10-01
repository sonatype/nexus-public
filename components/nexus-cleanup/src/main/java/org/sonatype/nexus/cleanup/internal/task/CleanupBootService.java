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
package org.sonatype.nexus.cleanup.internal.task;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Cron;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Instantiates the cleanup task on system startup if it does not already exist
 *
 * @since 3.14
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class CleanupBootService
    extends LifecycleSupport
{
  @VisibleForTesting
  static final String TASK_NAME = "Cleanup service";

  private final TaskScheduler taskScheduler;

  @Inject
  public CleanupBootService(final TaskScheduler taskScheduler) {
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  protected void doStart() {
    createCleanupTask();
  }

  private void createCleanupTask() {
    if (!doesTaskExist()) {
      TaskConfiguration taskConfig = taskScheduler.createTaskConfigurationInstance(CleanupTaskDescriptor.TYPE_ID);
      taskConfig.setName(TASK_NAME);
      try {
        Cron run1amEveryDay = taskScheduler.getScheduleFactory().cron(new Date(), "0 0 1 * * ?");
        taskScheduler.scheduleTask(taskConfig, run1amEveryDay);
      }
      catch (RuntimeException e) {
        log.error("Problem scheduling cleanup task", e);
      }
    }
  }

  private boolean doesTaskExist() {
    return taskScheduler.listsTasks().stream()
        .anyMatch(info -> CleanupTaskDescriptor.TYPE_ID.equals(info.getConfiguration().getTypeId()));
  }
}
