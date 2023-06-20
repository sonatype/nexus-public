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
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.29
 */
@Named
@Singleton
public class TaskUtils
    extends ComponentSupport
{
  private final Provider<TaskScheduler> taskSchedulerProvider;

  private final TaskResultStateStore taskResultStateStore;

  @Inject
  public TaskUtils(
      final Provider<TaskScheduler> taskSchedulerProvider,
      @Nullable final TaskResultStateStore taskResultStateStoreProvider)
  {
    this.taskSchedulerProvider = checkNotNull(taskSchedulerProvider);
    this.taskResultStateStore = taskResultStateStoreProvider;
  }

  public void checkForConflictingTasks(
      final String taskId,
      final String taskName,
      final List<String> conflictingTypeIds,
      final Map<String, List<String>> conflictingConfiguration)
  {
    Set<TaskInfo> incompatibleTasks = taskSchedulerProvider.get().listsTasks().stream()
        .filter(taskInfo -> isConflictingTask(taskId, taskInfo, conflictingTypeIds, conflictingConfiguration))
        .collect(Collectors.toSet());

    String names = incompatibleTasks.stream().map(TaskInfo::getName).collect(Collectors.joining(","));

    if (!incompatibleTasks.isEmpty()) {
      throw new IllegalStateException(
          "Cannot start task '" + taskName + "' there is at least one other task (" + names +
              ") running that is conflicting, please restart this task once the other(s) complete.");
    }
  }

  private boolean isConflictingTask(
      final String currentTaskId,
      final TaskInfo taskInfo,
      final List<String> conflictingTypeIds,
      final Map<String, List<String>> conflictingConfiguration)
  {
    //ignore tasks that aren't in the conflicting type set
    if (!conflictingTypeIds.contains(taskInfo.getTypeId())) {
      return false;
    }

    //ignore 'this' task
    if (currentTaskId.equals(taskInfo.getId())) {
      return false;
    }

    //ignore tasks that aren't running
    if (!isTaskRunning(taskInfo)) {
      return false;
    }

    //ignore tasks that aren't dealing with same config (i.e. don't conflict if 2 tasks dealing with diff blobstores)
    return conflictingConfiguration.entrySet().stream()
        .anyMatch(entry -> entry.getValue().contains(taskInfo.getConfiguration().getString(entry.getKey())));
  }

  private boolean isTaskRunning(final TaskInfo taskInfo) {
    if (taskResultStateStore != null) {
      log.debug("Checking state store for status of {}", taskInfo.getId());
      return taskResultStateStore.getState(taskInfo)
          .map(state -> state.getState().isRunning())
          .orElse(false);
    }
    return taskInfo.getCurrentState().getState().isRunning();
  }
}
