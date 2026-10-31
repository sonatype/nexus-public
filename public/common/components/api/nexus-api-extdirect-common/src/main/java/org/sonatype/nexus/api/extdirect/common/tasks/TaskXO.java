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
package org.sonatype.nexus.api.extdirect.common.tasks;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.sonatype.nexus.scheduling.TaskNotificationCondition;
import org.sonatype.nexus.scheduling.constraints.CronExpression;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Task exchange object.
 *
 * @since 3.0
 */
public class TaskXO
{
  public interface AdvancedSchedule
  {
    // empty
  }

  public interface OnceSchedule
  {
    // empty
  }

  public interface OnceToMonthlySchedule
  {
    // empty
  }

  public interface Schedule
  {
    // empty
  }

  @NotBlank(groups = {Update.class, Schedule.class})
  private String id;

  @NotNull
  private Boolean enabled;

  @NotBlank(groups = {Create.class, Update.class})
  private String name;

  @NotBlank(groups = Create.class)
  private String typeId;

  private String typeName;

  private String status;

  private String statusDescription;

  private Date nextRun;

  private Date lastRun;

  private String lastRunResult;

  private Boolean runnable;

  private Boolean stoppable;

  private String timeZoneOffset;

  private String alertEmail;

  private boolean runPreviousPlan;

  private TaskNotificationCondition notificationCondition;

  private Map<String, String> properties;

  @NotBlank(groups = Create.class)
  private String schedule;

  @NotNull(groups = OnceToMonthlySchedule.class)
  @Future(groups = OnceSchedule.class)
  private Date startDate;

  private Integer[] recurringDays;

  @NotBlank(groups = AdvancedSchedule.class)
  @CronExpression(groups = AdvancedSchedule.class)
  private String cronExpression;

  private Boolean isReadOnlyUi;

  public String getAlertEmail() {
    return alertEmail;
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public String getId() {
    return id;
  }

  public Date getLastRun() {
    return lastRun;
  }

  public String getLastRunResult() {
    return lastRunResult;
  }

  public String getName() {
    return name;
  }

  public Date getNextRun() {
    return nextRun;
  }

  public TaskNotificationCondition getNotificationCondition() {
    return notificationCondition;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Integer[] getRecurringDays() {
    return recurringDays;
  }

  public Boolean getRunnable() {
    return runnable;
  }

  public String getSchedule() {
    return schedule;
  }

  public Date getStartDate() {
    return startDate;
  }

  public String getStatus() {
    return status;
  }

  public String getStatusDescription() {
    return statusDescription;
  }

  public Boolean getStoppable() {
    return stoppable;
  }

  public String getTimeZoneOffset() {
    return timeZoneOffset;
  }

  public String getTypeId() {
    return typeId;
  }

  public String getTypeName() {
    return typeName;
  }

  public Boolean isReadOnlyUi() {
    return isReadOnlyUi;
  }

  public boolean isRunPreviousPlan() {
    return runPreviousPlan;
  }

  public void setAlertEmail(final String alertEmail) {
    this.alertEmail = alertEmail;
  }

  public void setCronExpression(final String cronExpression) {
    this.cronExpression = cronExpression;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setIsReadOnlyUi(final Boolean isReadOnlyUi) {
    this.isReadOnlyUi = isReadOnlyUi;
  }

  public void setIsRunPreviousPlan(final boolean runPreviousPlan) {
    this.runPreviousPlan = runPreviousPlan;
  }

  public void setLastRun(final Date lastRun) {
    this.lastRun = lastRun;
  }

  public void setLastRunResult(final String lastRunResult) {
    this.lastRunResult = lastRunResult;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setNextRun(final Date nextRun) {
    this.nextRun = nextRun;
  }

  public void setNotificationCondition(final TaskNotificationCondition notificationCondition) {
    this.notificationCondition = notificationCondition;
  }

  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

  public void setRecurringDays(final Integer[] recurringDays) {
    this.recurringDays = recurringDays;
  }

  public void setRunnable(final Boolean runnable) {
    this.runnable = runnable;
  }

  public void setSchedule(final String schedule) {
    this.schedule = schedule;
  }

  public void setStartDate(final Date startDate) {
    this.startDate = startDate;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public void setStatusDescription(final String statusDescription) {
    this.statusDescription = statusDescription;
  }

  public void setStoppable(final Boolean stoppable) {
    this.stoppable = stoppable;
  }

  public void setTimeZoneOffset(final String timeZoneOffset) {
    this.timeZoneOffset = timeZoneOffset;
  }

  public void setTypeId(final String typeId) {
    this.typeId = typeId;
  }

  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

  @Override
  public String toString() {
    return "TaskXO{" +
        "id='" + id + '\'' +
        ", enabled=" + enabled +
        ", name='" + name + '\'' +
        ", typeId='" + typeId + '\'' +
        ", typeName='" + typeName + '\'' +
        ", status='" + status + '\'' +
        ", statusDescription='" + statusDescription + '\'' +
        ", nextRun=" + nextRun +
        ", lastRun=" + lastRun +
        ", lastRunResult='" + lastRunResult + '\'' +
        ", runnable=" + runnable +
        ", stoppable=" + stoppable +
        ", timeZoneOffset='" + timeZoneOffset + '\'' +
        ", alertEmail='" + alertEmail + '\'' +
        ", notificationCondition=" + notificationCondition +
        ", properties=" + properties +
        ", schedule='" + schedule + '\'' +
        ", startDate=" + startDate +
        ", recurringDays=" + Arrays.toString(recurringDays) +
        ", cronExpression='" + cronExpression + '\'' +
        ", isReadOnlyUi=" + isReadOnlyUi +
        '}';
  }
}
