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
package org.sonatype.nexus.audit.internal.store;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.AUDIT_EVENTS_CLEANUP_TASK_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.AUDIT_EVENTS_CLEANUP_TASK_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Auto-creates the {@link AuditEventCleanupTask} daily schedule at startup unless a task of the
 * same type already exists. Any custom schedule an operator has configured via the tasks UI is
 * preserved across restarts.
 */
@Component
@ManagedLifecycle(phase = TASKS)
public class AuditEventCleanupTaskManager
    extends StateGuardLifecycleSupport
{
  private final TaskScheduler taskScheduler;

  private final String cleanupCron;

  private final boolean enabled;

  private final Predicate<TaskInfo> isCleanupTask =
      info -> AuditEventCleanupTaskDescriptor.TYPE_ID.equals(info.getConfiguration().getTypeId());

  @Autowired
  public AuditEventCleanupTaskManager(
      final TaskScheduler taskScheduler,
      @Value("${nexus.audit.events.cleanup.cron:0 30 3 * * ?}") final String cleanupCron,
      @Value(AUDIT_EVENTS_CLEANUP_TASK_ENABLED_NAMED_VALUE) final boolean enabled)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.cleanupCron = checkNotNull(cleanupCron);
    this.enabled = enabled;
  }

  @Override
  protected void doStart() throws Exception {
    removeDuplicateTasks();

    if (!enabled) {
      log.info("Audit_events cleanup task disabled by feature flag ({}=false); skipping auto-registration",
          AUDIT_EVENTS_CLEANUP_TASK_ENABLED);
      return;
    }

    TaskInfo taskInfo = taskScheduler.getTaskByTypeId(AuditEventCleanupTaskDescriptor.TYPE_ID);
    if (taskInfo == null) {
      TaskConfiguration configuration =
          taskScheduler.createTaskConfigurationInstance(AuditEventCleanupTaskDescriptor.TYPE_ID);
      configuration.setName(AuditEventCleanupTaskDescriptor.TASK_NAME);
      Schedule schedule = taskScheduler.getScheduleFactory().cron(new Date(), this.cleanupCron);
      taskScheduler.scheduleTask(configuration, schedule);
      log.info("Created audit_events cleanup task with default schedule: {}", this.cleanupCron);
    }
    else {
      log.debug("Audit cleanup task already exists, preserving existing schedule");
    }
  }

  private void removeDuplicateTasks() {
    List<TaskInfo> tasks = taskScheduler.listsTasks()
        .stream()
        .filter(isCleanupTask)
        .sorted(Comparator.comparing(TaskInfo::getId))
        .skip(1)
        .toList();

    if (!tasks.isEmpty()) {
      log.warn("Found {} duplicate audit cleanup tasks, removing extras", tasks.size());
      tasks.forEach(TaskInfo::remove);
    }
  }
}
