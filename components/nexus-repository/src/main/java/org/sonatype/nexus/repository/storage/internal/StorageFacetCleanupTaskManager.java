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
package org.sonatype.nexus.repository.storage.internal;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.storage.internal.StorageFacetCleanupTaskDescriptor.TYPE_ID;

/**
 * Manager which ensures the {@link StorageFacetCleanupTask} is scheduled during the startup of Nexus.
 *
 * @since 3.6
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class StorageFacetCleanupTaskManager
    extends StateGuardLifecycleSupport
{
  private final TaskScheduler taskScheduler;

  private final String storageCleanupCron;

  @Inject
  public StorageFacetCleanupTaskManager(final TaskScheduler taskScheduler,
      @Named("${nexus.storageCleanup.cron:-0 */10 * * * ?}") final String storageCleanupCron)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.storageCleanupCron = checkNotNull(storageCleanupCron);
  }

  @Override
  protected void doStart() throws Exception {
    // Remove any existing tasks
    taskScheduler.listsTasks().stream()
        .filter((info) -> TYPE_ID.equals(info.getConfiguration().getTypeId()))
        .forEach(TaskInfo::remove);

    // Create task
    TaskConfiguration configuration = taskScheduler.createTaskConfigurationInstance(TYPE_ID);
    Schedule schedule = taskScheduler.getScheduleFactory().cron(new Date(), storageCleanupCron);
    taskScheduler.scheduleTask(configuration, schedule);
  }
}
