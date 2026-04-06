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
package org.sonatype.nexus.quartz.internal.upgrades;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.node.upgrade.MigrationTaskSupport;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Unifies Task Configuration ID with Quartz Job Key for optimal task lookup performance.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class QuartzJobKeyUnificationTask
    extends MigrationTaskSupport
    implements Cancelable
{
  private static final String RUNNING_STATE = "running";

  private static final String NOT_RUNNING_STATE = "not-running";

  private final TaskScheduler taskScheduler;

  private final Duration waitTimeout;

  private final Duration checkInterval;

  @Autowired
  public QuartzJobKeyUnificationTask(
      final TaskScheduler taskScheduler,
      @Nullable final NodeHeartbeatManager heartbeatManager,
      @Value("${nexus.quartz.jobkey.unification.timeout:10m}") final Duration waitTimeout,
      @Value("${nexus.quartz.jobkey.unification.check.interval:30s}") final Duration checkInterval)
  {
    super(heartbeatManager);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.waitTimeout = checkNotNull(waitTimeout);
    this.checkInterval = checkNotNull(checkInterval);
  }

  @Override
  public String getMessage() {
    return "Unifies job keys with task configuration IDs";
  }

  @Override
  protected Object execute() throws Exception {
    Instant startTime = Instant.now();
    Instant deadline = startTime.plus(waitTimeout);

    int notMigrated;
    boolean oldNodesRunning = true;

    log.info("Starting job key unification with timeout of {}", formatDuration(waitTimeout));

    // Keep migrating while: (1) tasks need migration OR (2) old nodes running (might create new tasks)
    while ((notMigrated = migrateNonRunningTasks()) > 0 ||
        (oldNodesRunning = oldNodesRunning())) {
      CancelableHelper.checkCancellation();

      // Check timeout
      if (Instant.now().isAfter(deadline)) {
        throw new IllegalStateException(buildTimeoutMessage(notMigrated, oldNodesRunning));
      }

      Duration elapsed = Duration.between(startTime, Instant.now());
      Duration remaining = Duration.between(Instant.now(), deadline);

      if (notMigrated > 0) {
        log.info("{} tasks still need migration, waiting {} (elapsed: {}, remaining: {})",
            notMigrated, formatDuration(checkInterval), formatDuration(elapsed), formatDuration(remaining));
      }
      else {
        log.info("Old nodes still running (might create new tasks), waiting {} (elapsed: {}, remaining: {})",
            formatDuration(checkInterval), formatDuration(elapsed), formatDuration(remaining));
      }

      Thread.sleep(checkInterval.toMillis());
    }

    log.info("Job key unification completed successfully");
    return null;
  }

  private String buildTimeoutMessage(final int notMigrated, final boolean oldNodesRunning) {
    StringBuilder message = new StringBuilder("Timeout reached after " + formatDuration(waitTimeout) + ". ");

    if (notMigrated > 0) {
      message.append(notMigrated).append(" tasks still need migration (running or blocked). ");
    }

    if (oldNodesRunning) {
      message.append("Old nodes still running in cluster. ");
    }

    message.append("Task will retry on next node startup.");

    return message.toString();
  }

  /**
   * Migrates non-running tasks by deleting and recreating them with unified job keys.
   *
   * @return the number of tasks that still need migration (running tasks that were skipped)
   */
  private int migrateNonRunningTasks() {
    Map<String, List<QuartzTaskInfo>> allTasks = taskScheduler.listsTasks()
        .stream()
        .map(QuartzTaskInfo.class::cast)
        .collect(Collectors.groupingBy(this::byState));

    List<QuartzTaskInfo> runningTasks = allTasks.getOrDefault(RUNNING_STATE, List.of())
        .stream()
        .filter(this::notItself)
        .filter(this::notMigrated)
        .toList();

    List<QuartzTaskInfo> toBeMigrated = allTasks.getOrDefault(NOT_RUNNING_STATE, List.of())
        .stream()
        .filter(this::notMigrated)
        .toList();

    int failedToRemove = 0;

    for (QuartzTaskInfo taskInfo : toBeMigrated) {
      TaskConfiguration config = taskInfo.getConfiguration();

      log.debug("Migrating task '{}' - job key : {} - task configuration id : {}", taskInfo.getName(),
          taskInfo.getJobKey(),
          config.getId());

      // Check if the task descriptor still exists (NEXUS-50585)
      // Tasks with removed/deprecated type IDs (e.g., "db.backup" after OrientDB removal) should be skipped
      TaskDescriptor descriptor = taskScheduler.getTaskFactory().findDescriptor(config.getTypeId());
      if (descriptor == null) {
        log.warn("Skipping migration of task '{}' with type '{}' - descriptor not found (likely removed/deprecated). "
            + "Removing task from database.", taskInfo.getName(), config.getTypeId());
        if (!taskInfo.remove()) {
          log.warn("Unable to remove orphaned task '{}' with missing descriptor type '{}'",
              taskInfo.getName(), config.getTypeId());
          failedToRemove++;
        }
        continue;
      }

      // cleans up memory and DB references, also emits JobDeletedEvent so other nodes update their caches
      if (!taskInfo.remove()) {
        log.info("Unable to remove task '{}' for migration, it may still need migration on next attempt",
            taskInfo.getName());
        failedToRemove++;
        continue;
      }
      // recreate the task, which will create it with unified job key and emits JobCreatedEvent so other nodes update
      // their caches
      TaskInfo migrated = taskScheduler.scheduleTask(config, taskInfo.getSchedule());
      log.debug("Migrated task '{}' to unified id {}", taskInfo.getName(), migrated.getId());
    }

    return runningTasks.size() + failedToRemove;
  }

  private String byState(final TaskInfo taskInfo) {
    ExternalTaskState taskState = taskScheduler.toExternalTaskState(taskInfo);
    return taskState.getState() == TaskState.RUNNING ? RUNNING_STATE : NOT_RUNNING_STATE;
  }

  private boolean notItself(final TaskInfo taskInfo) {
    return !Objects.equals(taskInfo.getId(), getId());
  }

  private boolean notMigrated(final QuartzTaskInfo taskInfo) {
    return !Objects.equals(taskInfo.getJobKey().getName(), taskInfo.getConfiguration().getId());
  }

  /**
   * Formats a Duration into a human-readable string.
   *
   * @param duration the duration to format
   * @return the formatted string , like "5 min 30 sec" or "45 sec"
   */
  private static String formatDuration(Duration duration) {
    long minutes = duration.toMinutes();
    long seconds = duration.minusMinutes(minutes).getSeconds();
    if (minutes > 0 && seconds > 0) {
      return String.format("%d min %d sec", minutes, seconds);
    }
    else if (minutes > 0) {
      return String.format("%d min", minutes);
    }
    else {
      return String.format("%d sec", seconds);
    }
  }
}
