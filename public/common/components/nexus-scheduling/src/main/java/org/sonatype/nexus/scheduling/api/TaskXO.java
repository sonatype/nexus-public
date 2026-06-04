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
package org.sonatype.nexus.scheduling.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskNotificationCondition;
import org.sonatype.nexus.scheduling.schedule.Cron;
import org.sonatype.nexus.scheduling.schedule.Daily;
import org.sonatype.nexus.scheduling.schedule.Hourly;
import org.sonatype.nexus.scheduling.schedule.Monthly;
import org.sonatype.nexus.scheduling.schedule.Once;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.Weekly;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.apache.commons.lang3.StringUtils;

/**
 * Task transfer object for REST APIs.
 */
@ApiModel(description = "Represents a task in Nexus Repository Manager")
public class TaskXO
{
  @ApiModelProperty(value = "The unique identifier of the task")
  private String id;

  @ApiModelProperty(value = "The name of the task", example = "Compact blob store")
  private String name;

  @ApiModelProperty(value = "The type identifier of the task", example = "blobstore.compact")
  private String type;

  @ApiModelProperty(value = "A human-readable message describing the task", example = "Admin - Compact blob store")
  private String message;

  @ApiModelProperty(value = "The current state of the task", example = "WAITING")
  private String currentState;

  @ApiModelProperty(value = "The result of the last run", example = "OK")
  private String lastRunResult;

  @ApiModelProperty(value = "The next scheduled run time (ISO 8601 format)")
  private Date nextRun;

  @ApiModelProperty(value = "The last run start time (ISO 8601 format)")
  private Date lastRun;

  @ApiModelProperty(value = "The schedule type", example = "manual")
  private String schedule;

  @ApiModelProperty(value = "Task-specific configuration properties")
  private Map<String, String> properties;

  @ApiModelProperty(value = "Whether the task is enabled", example = "true")
  private boolean enabled;

  @ApiModelProperty(value = "Email address to send notifications to", example = "admin@example.com")
  private String alertEmail;

  @ApiModelProperty(value = "Condition for sending notifications", example = "FAILURE")
  private TaskNotificationCondition notificationCondition;

  @ApiModelProperty(value = "The start date for scheduled tasks (ISO 8601 format)")
  private Date startDate;

  @ApiModelProperty(
      value = "Days of the week/month for recurring schedules (1=Sunday, 7=Saturday, 999=last day of month)",
      example = "[1, 4]")
  private Integer[] recurringDays;

  @ApiModelProperty(value = "Cron expression for advanced schedules", example = "0 0 12 * * ?")
  private String cronExpression;

  @ApiModelProperty(value = "Time zone offset for cron schedules", example = "+00:00")
  private String timeZoneOffset;

  public static TaskXO fromTaskInfo(final TaskInfo taskInfo, ExternalTaskState externalTaskState) {
    TaskXO taskXO = new TaskXO();
    TaskConfiguration configuration = taskInfo.getConfiguration();
    Schedule schedule = taskInfo.getSchedule();

    taskXO.setId(taskInfo.getId());
    taskXO.setName(taskInfo.getName());
    taskXO.setType(taskInfo.getTypeId());
    taskXO.setMessage(taskInfo.getMessage());
    if (externalTaskState.getState().isRunning() && StringUtils.isNotBlank(externalTaskState.getProgress())) {
      taskXO.setCurrentState(externalTaskState.getState().toString() + ": " + externalTaskState.getProgress());
    }
    else {
      taskXO.setCurrentState(externalTaskState.getState().toString());
    }

    taskXO.setNextRun(taskInfo.getCurrentState().getNextRun());
    if (externalTaskState.getLastEndState() != null) {
      taskXO.setLastRunResult(externalTaskState.getLastEndState().toString());
    }
    taskXO.setLastRun(externalTaskState.getLastRunStarted());
    taskXO.setSchedule(schedule != null ? getScheduleType(schedule) : null);

    taskXO.setEnabled(configuration.isEnabled());
    taskXO.setAlertEmail(configuration.getAlertEmail());
    taskXO.setNotificationCondition(configuration.getNotificationCondition());
    taskXO.setProperties(extractTaskProperties(configuration));

    if (schedule != null) {
      populateScheduleDetails(taskXO, schedule);
    }

    return taskXO;
  }

  private static String getScheduleType(final Schedule schedule) {
    String type = schedule.getType();
    return "cron".equals(type) ? "advanced" : type;
  }

  private static Map<String, String> extractTaskProperties(final TaskConfiguration configuration) {
    return configuration.asMap()
        .entrySet()
        .stream()
        .filter(e -> !e.getKey().startsWith(".") && !e.getKey().contains("lastRunState"))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
  }

  private static void populateScheduleDetails(final TaskXO taskXO, final Schedule schedule) {
    if (schedule instanceof Once) {
      taskXO.setStartDate(((Once) schedule).getStartAt());
    }
    else if (schedule instanceof Hourly) {
      taskXO.setStartDate(((Hourly) schedule).getStartAt());
    }
    else if (schedule instanceof Daily) {
      taskXO.setStartDate(((Daily) schedule).getStartAt());
    }
    else if (schedule instanceof Weekly) {
      Weekly weekly = (Weekly) schedule;
      taskXO.setStartDate(weekly.getStartAt());
      taskXO.setRecurringDays(weekly.getDaysToRun()
          .stream()
          .map(day -> day.ordinal() + 1)
          .toArray(Integer[]::new));
    }
    else if (schedule instanceof Monthly) {
      Monthly monthly = (Monthly) schedule;
      taskXO.setStartDate(monthly.getStartAt());
      taskXO.setRecurringDays(monthly.getDaysToRun()
          .stream()
          .map(day -> day.isLastDayOfMonth() ? 999 : day.getDay())
          .toArray(Integer[]::new));
    }
    else if (schedule instanceof Cron) {
      Cron cron = (Cron) schedule;
      taskXO.setStartDate(cron.getStartAt());
      taskXO.setCronExpression(cron.getCronExpression());
      taskXO.setTimeZoneOffset(cron.getTimeZone()
          .toZoneId()
          .getRules()
          .getOffset(java.time.Instant.now())
          .getId());
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getCurrentState() {
    return currentState;
  }

  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }

  public String getLastRunResult() {
    return lastRunResult;
  }

  public void setLastRunResult(String lastRunResult) {
    this.lastRunResult = lastRunResult;
  }

  public Date getNextRun() {
    return nextRun;
  }

  public void setNextRun(Date nextRun) {
    this.nextRun = nextRun;
  }

  public Date getLastRun() {
    return lastRun;
  }

  public void setLastRun(Date lastRun) {
    this.lastRun = lastRun;
  }

  public String getSchedule() {
    return schedule;
  }

  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getAlertEmail() {
    return alertEmail;
  }

  public void setAlertEmail(String alertEmail) {
    this.alertEmail = alertEmail;
  }

  public TaskNotificationCondition getNotificationCondition() {
    return notificationCondition;
  }

  public void setNotificationCondition(TaskNotificationCondition notificationCondition) {
    this.notificationCondition = notificationCondition;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Integer[] getRecurringDays() {
    return recurringDays;
  }

  public void setRecurringDays(Integer[] recurringDays) {
    this.recurringDays = recurringDays;
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  public String getTimeZoneOffset() {
    return timeZoneOffset;
  }

  public void setTimeZoneOffset(String timeZoneOffset) {
    this.timeZoneOffset = timeZoneOffset;
  }

  @Override
  public String toString() {
    return "TaskXO(" +
        "id:'" + id + '\'' +
        ", name:'" + name + '\'' +
        ", type:'" + type + '\'' +
        ", message:'" + message + '\'' +
        ", currentState:'" + currentState + '\'' +
        ", lastRunResult:'" + lastRunResult + '\'' +
        ", nextRun:" + nextRun +
        ", lastRun:" + lastRun +
        ", schedule:'" + schedule + '\'' +
        ", properties:" + properties +
        ", enabled:" + enabled +
        ", alertEmail:'" + alertEmail + '\'' +
        ", notificationCondition:" + notificationCondition +
        ", startDate:" + startDate +
        ", recurringDays:" + java.util.Arrays.toString(recurringDays) +
        ", cronExpression:'" + cronExpression + '\'' +
        ", timeZoneOffset:'" + timeZoneOffset + '\'' +
        ')';
  }
}
