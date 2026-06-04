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
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

/**
 * @since 3.29
 */
@Component
public class TaskUtils
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Provider<TaskScheduler> taskSchedulerProvider;

  private final TaskResultStateStore taskResultStateStore;

  @Autowired
  public TaskUtils(
      final Provider<TaskScheduler> taskSchedulerProvider,
      final TaskResultStateStore taskResultStateStoreProvider)
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
    Set<TaskInfo> incompatibleTasks = taskSchedulerProvider.get()
        .listsTasks()
        .stream()
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
    // ignore tasks that aren't in the conflicting type set
    if (!conflictingTypeIds.contains(taskInfo.getTypeId())) {
      return false;
    }

    // ignore 'this' task
    if (currentTaskId.equals(taskInfo.getId())) {
      return false;
    }

    // ignore tasks that aren't running
    if (!isTaskRunning(taskInfo)) {
      return false;
    }

    // ignore tasks that aren't dealing with same config (i.e. don't conflict if 2 tasks dealing with diff blobstores)
    return conflictingConfiguration.entrySet()
        .stream()
        .anyMatch(entry -> entry.getValue().contains(taskInfo.getConfiguration().getString(entry.getKey())));
  }

  private boolean isTaskRunning(final TaskInfo taskInfo) {
    if (taskResultStateStore.isSupported()) {
      log.debug("Checking state store for status of {}", taskInfo.getId());
      return taskResultStateStore.getState(taskInfo)
          .map(state -> state.getState().isRunning())
          .orElse(false);
    }
    return taskInfo.getCurrentState().getState().isRunning();
  }

  public void validateTaskCreationForUI(
      String taskName,
      String taskType,
      Map<String, String> taskProperties,
      Schedule schedule)
  {
    if (isVisibleTaskNameDuplicate(taskName)) {
      throw new ValidationException(String.format("Task with name '%s' already exists", taskName));
    }

    if (isVisibleTaskConfigurationDuplicate(taskType, taskProperties, schedule)) {
      throw new ValidationException(
          String.format("A task of type '%s' with identical configuration and schedule already exists", taskType));
    }
  }

  public void validateTaskCreationForAPI(
      String taskName,
      String taskType,
      Map<String, String> taskProperties,
      Schedule schedule)
  {
    if (isVisibleTaskNameDuplicate(taskName)) {
      throw new WebApplicationMessageException(Response.Status.CONFLICT,
          String.format("Task with name '%s' already exists", taskName), APPLICATION_JSON);
    }

    if (isVisibleTaskConfigurationDuplicate(taskType, taskProperties, schedule)) {
      throw new WebApplicationMessageException(Response.Status.CONFLICT,
          String.format("A task of type '%s' with identical configuration and schedule already exists", taskType),
          APPLICATION_JSON);
    }
  }

  /**
   * Validates that no visible task exists with the given name.
   * Only checks user-visible tasks, not internal system tasks.
   */
  public boolean isVisibleTaskNameDuplicate(String taskName) {
    log.debug("Checking for duplicate task name: {}", taskName);

    return taskSchedulerProvider.get()
        .listsTasks()
        .stream()
        .filter(taskInfo -> taskInfo.getConfiguration().isVisible())
        .anyMatch(taskInfo -> taskName.equals(taskInfo.getConfiguration().getName()));
  }

  /**
   * Validates that no visible task exists with identical configuration (type + properties + schedule).
   * Only checks user-visible tasks, not internal system tasks.
   */
  public boolean isVisibleTaskConfigurationDuplicate(
      String taskType,
      Map<String, String> taskProperties,
      Schedule newSchedule)
  {
    log.debug("Checking for duplicate task configuration - type: {}, properties: {}, schedule: {}",
        taskType, taskProperties, newSchedule.getClass().getSimpleName());

    return taskSchedulerProvider.get()
        .listsTasks()
        .stream()
        .filter(taskInfo -> taskInfo.getConfiguration().isVisible())
        .filter(taskInfo -> taskType.equals(taskInfo.getConfiguration().getTypeId()))
        .filter(taskInfo -> haveSameProperties(taskProperties, taskInfo.getConfiguration()))
        .anyMatch(taskInfo -> haveSameSchedule(newSchedule, taskInfo.getSchedule()));
  }

  private boolean haveSameProperties(Map<String, String> newProperties, TaskConfiguration existingConfig) {

    // Filter out null/empty properties from NEW properties (same as existing behavior)
    Map<String, String> filteredNewProperties = new HashMap<>();
    for (Map.Entry<String, String> entry : newProperties.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (value != null && !value.trim().isEmpty()) {
        filteredNewProperties.put(key, value.trim());
      }
    }

    // Get only non-internal properties from existing config
    Map<String, String> existingProperties = new HashMap<>();
    for (Map.Entry<String, String> entry : existingConfig.asMap().entrySet()) {
      String key = entry.getKey();
      if (!key.startsWith(".")) { // Skip internal properties
        existingProperties.put(key, entry.getValue());
      }
    }

    // Check if both maps have the same size (number of properties)
    if (filteredNewProperties.size() != existingProperties.size()) {
      return false;
    }

    // Check if all properties match exactly
    for (Map.Entry<String, String> newEntry : filteredNewProperties.entrySet()) {
      String key = newEntry.getKey();
      String newValue = newEntry.getValue();
      String existingValue = existingProperties.get(key);

      if (!java.util.Objects.equals(newValue, existingValue)) {
        return false;
      }
    }

    return true;
  }

  private boolean haveSameSchedule(Schedule newSchedule, Schedule existingSchedule) {

    // Compare schedule types first
    if (!newSchedule.getClass().equals(existingSchedule.getClass())) {
      return false;
    }

    // For Manual schedules, they are always the same
    if (newSchedule instanceof org.sonatype.nexus.scheduling.schedule.Manual) {
      return true;
    }
    return newSchedule.toString().equals(existingSchedule.toString());
  }

}
