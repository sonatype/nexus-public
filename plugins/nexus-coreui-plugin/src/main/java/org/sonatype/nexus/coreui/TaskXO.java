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
package org.sonatype.nexus.coreui;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.scheduling.TaskNotificationCondition;
import org.sonatype.nexus.scheduling.constraints.CronExpression;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

/**
 * Task exchange object.
 *
 * @since 3.0
 */
public class TaskXO
{
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTypeId() {
    return typeId;
  }

  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatusDescription() {
    return statusDescription;
  }

  public void setStatusDescription(String statusDescription) {
    this.statusDescription = statusDescription;
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

  public String getLastRunResult() {
    return lastRunResult;
  }

  public void setLastRunResult(String lastRunResult) {
    this.lastRunResult = lastRunResult;
  }

  public Boolean getRunnable() {
    return runnable;
  }

  public void setRunnable(Boolean runnable) {
    this.runnable = runnable;
  }

  public Boolean getStoppable() {
    return stoppable;
  }

  public void setStoppable(Boolean stoppable) {
    this.stoppable = stoppable;
  }

  public String getTimeZoneOffset() {
    return timeZoneOffset;
  }

  public void setTimeZoneOffset(String timeZoneOffset) {
    this.timeZoneOffset = timeZoneOffset;
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

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public String getSchedule() {
    return schedule;
  }

  public void setSchedule(String schedule) {
    this.schedule = schedule;
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

  public Boolean isReadOnlyUi() {
    return isReadOnlyUi;
  }

  public void setIsReadOnlyUi(Boolean isReadOnlyUi) {
    this.isReadOnlyUi = isReadOnlyUi;
  }

  public interface Schedule
  {
  }

  public interface AdvancedSchedule
  {
  }

  public interface OnceSchedule
  {
  }

  public interface OnceToMonthlySchedule
  {
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
