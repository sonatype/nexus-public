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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.NotFoundException;

import org.sonatype.nexus.coreui.TaskXO.AdvancedSchedule;
import org.sonatype.nexus.coreui.TaskXO.OnceSchedule;
import org.sonatype.nexus.coreui.TaskXO.OnceToMonthlySchedule;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Cron;
import org.sonatype.nexus.scheduling.schedule.Daily;
import org.sonatype.nexus.scheduling.schedule.Hourly;
import org.sonatype.nexus.scheduling.schedule.Manual;
import org.sonatype.nexus.scheduling.schedule.Monthly;
import org.sonatype.nexus.scheduling.schedule.Monthly.CalendarDay;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Once;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.Weekly;
import org.sonatype.nexus.scheduling.schedule.Weekly.Weekday;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.date.TimeZoneUtils.shiftMonthDay;
import static org.sonatype.nexus.repository.date.TimeZoneUtils.shiftWeekDay;
import static org.sonatype.nexus.scheduling.TaskState.CANCELED;
import static org.sonatype.nexus.scheduling.TaskState.FAILED;
import static org.sonatype.nexus.scheduling.TaskState.INTERRUPTED;
import static org.sonatype.nexus.scheduling.TaskState.OK;

@Named
@Singleton
@DirectAction(action = "coreui_Task")
public class TaskComponent
    extends DirectComponentSupport
    implements StateContributor
{
  private static final String TASK_RESULT_OK = "Ok";

  private static final String TASK_RESULT_CANCELED = "Canceled";

  private static final String TASK_RESULT_ERROR = "Error";

  private static final String TASK_RESULT_INTERRUPTED = "Interrupted";

  public static final String PLAN_RECONCILIATION_TASK_ID = "blobstore.planReconciliation";

  public static final String PLAN_RECONCILIATION_TASK_OK_TEXT = " - Plan(s) is ready to run";

  private final TaskScheduler taskScheduler;

  private final Provider<Validator> validatorProvider;

  private final boolean allowCreation;

  @Inject
  public TaskComponent(
      final TaskScheduler taskScheduler,
      final Provider<Validator> validatorProvider,
      @Named("${nexus.scripts.allowCreation:-false}") final boolean allowCreation)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.validatorProvider = checkNotNull(validatorProvider);
    this.allowCreation = allowCreation;
  }

  @Nullable
  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of("allowScriptCreation", allowCreation);
  }

  /**
   * Retrieve a list of scheduled tasks.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:tasks:read")
  public List<TaskXO> read() {
    return taskScheduler.listsTasks()
        .stream()
        .filter(taskInfo -> taskInfo.getConfiguration().isVisible())
        .map(this::asTaskXO)
        .collect(toList());
  }

  /**
   * Retrieve available task types.
   *
   * @return a list of task types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:tasks:read")
  public List<TaskTypeXO> readTypes() {
    return taskScheduler.getTaskFactory().getDescriptors().stream().map(TaskComponent::asTaskTypeXO).collect(toList());
  }

  /**
   * Creates a task.
   *
   * @param taskXO to be created
   * @return created task
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:create")
  @Validate(groups = {Create.class, Default.class})
  public TaskXO create(final @NotNull @Valid TaskXO taskXO) throws Exception {
    Schedule schedule = asSchedule(taskXO);

    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(taskXO.getTypeId());
    checkState(taskConfiguration.isExposed(), "This task is not allowed to be created");

    taskXO.getProperties().forEach(taskConfiguration::setString);
    taskConfiguration.setAlertEmail(taskXO.getAlertEmail());
    taskConfiguration.setNotificationCondition(taskXO.getNotificationCondition());
    taskConfiguration.setName(taskXO.getName());
    taskConfiguration.setEnabled(taskXO.getEnabled());

    TaskInfo task = scheduleTask(() -> taskScheduler.scheduleTask(taskConfiguration, schedule));
    log.debug("Created task with type '{}': {} {}", taskConfiguration.getClass(), taskConfiguration.getName(),
        taskConfiguration.getId());
    return asTaskXO(task);
  }

  /**
   * Updates a task.
   *
   * @param taskXO to be updated
   * @return updated task
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:update")
  @Validate(groups = {Update.class, Default.class})
  public TaskXO update(final @NotNull @Valid TaskXO taskXO) throws Exception {
    TaskInfo task = taskScheduler.getTaskById(taskXO.getId());
    validateState(taskXO.getId(), task);
    if ("script".equals(task.getTypeId())) {
      validateScriptUpdate(task, taskXO);
    }
    Schedule schedule = asSchedule(taskXO);
    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(taskXO.getTypeId());
    taskConfiguration.apply(task.getConfiguration());
    taskConfiguration.setEnabled(taskXO.getEnabled());
    taskConfiguration.setName(taskXO.getName());
    taskConfiguration.setAlertEmail(taskXO.getAlertEmail());
    taskConfiguration.setNotificationCondition(taskXO.getNotificationCondition());
    taskXO.getProperties().forEach(taskConfiguration::setString);

    task = scheduleTask(() -> taskScheduler.scheduleTask(taskConfiguration, schedule));

    return asTaskXO(task);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:delete")
  @Validate
  public void remove(final @NotEmpty String id) {
    TaskInfo taskInfo = taskScheduler.getTaskById(id);
    if (taskInfo != null) {
      taskInfo.remove();
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:start")
  @Validate
  public void run(final @NotEmpty String id) throws Exception {
    TaskInfo taskInfo = taskScheduler.getTaskById(id);
    if (taskInfo != null) {
      taskInfo.runNow();
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:stop")
  @Validate
  public void stop(final @NotEmpty String id) {
    taskScheduler.cancel(id, false);
  }

  private TaskXO asTaskXO(final TaskInfo taskInfo) {
    ExternalTaskState externalTaskState = taskScheduler.toExternalTaskState(taskInfo);
    TaskState taskState = externalTaskState.getState();
    TaskState endTaskState = externalTaskState.getLastEndState();
    Date lastRun = externalTaskState.getLastRunStarted();
    Long runDuration = externalTaskState.getLastRunDuration();

    TaskConfiguration configuration = taskInfo.getConfiguration();
    TaskXO result = new TaskXO();
    result.setId(taskInfo.getId());
    result.setEnabled(configuration.isEnabled());
    result.setName(taskInfo.getName());
    result.setTypeId(configuration.getTypeId());
    result.setTypeName(configuration.getTypeName());
    result.setStatus(taskState.name());
    String statusDescription = configuration.isEnabled() ? taskState.getDescription() : "Disabled";
    if (taskInfo.getCurrentState().getState().isRunning() && configuration.getProgress() != null) {
      statusDescription += ": " + configuration.getProgress();
    }
    result.setStatusDescription(statusDescription);
    result.setSchedule(getSchedule(taskInfo.getSchedule()));
    result.setLastRun(lastRun);
    result.setLastRunResult(getLastRunResult(taskInfo, endTaskState, runDuration));
    result.setNextRun(externalTaskState.getNextFireTime());
    result.setRunnable(taskState.isWaiting());
    result.setStoppable(taskState.isRunning());
    result.setAlertEmail(configuration.getAlertEmail());
    result.setNotificationCondition(configuration.getNotificationCondition());
    result.setProperties(configuration.asMap());

    Schedule schedule = taskInfo.getSchedule();
    if (schedule instanceof Once) {
      result.setStartDate(((Once) schedule).getStartAt());
    }
    else if (schedule instanceof Hourly) {
      result.setStartDate(((Hourly) schedule).getStartAt());
    }
    else if (schedule instanceof Daily) {
      result.setStartDate(((Daily) schedule).getStartAt());
    }
    else if (schedule instanceof Weekly) {
      result.setStartDate(((Weekly) schedule).getStartAt());
      // expects integers with 1=SUN, 2=MON, etc...
      result.setRecurringDays(
          ((Weekly) schedule).getDaysToRun()
              .stream()
              .map(dayToRun -> dayToRun.ordinal() + 1)
              .collect(toList())
              .toArray(new Integer[]{}));
    }
    else if (schedule instanceof Monthly) {
      result.setStartDate(((Monthly) schedule).getStartAt());
      // expects ints, with 999 being the lastDayOfMonth
      result.setRecurringDays(((Monthly) schedule).getDaysToRun()
          .stream()
          .map(dayToRun -> dayToRun.isLastDayOfMonth() ? 999 : dayToRun.getDay())
          .collect(toList())
          .toArray(new Integer[]{}));
    }
    else if (schedule instanceof Cron) {
      result.setStartDate(((Cron) schedule).getStartAt());
      result.setCronExpression(((Cron) schedule).getCronExpression());
    }
    result.setIsReadOnlyUi(configuration.getBoolean(".readOnlyUi", false));
    return result;
  }

  private Schedule asSchedule(final TaskXO taskXO) {
    if ("advanced".equals(taskXO.getSchedule())) {
      ZoneOffset clientZoneOffset = ZoneOffset.of(taskXO.getTimeZoneOffset());
      validatorProvider.get().validate(taskXO, AdvancedSchedule.class);
      return taskScheduler.getScheduleFactory().cron(new Date(), taskXO.getCronExpression(), clientZoneOffset.getId());
    }
    if (!"manual".equals(taskXO.getSchedule())) {
      if (taskXO.getStartDate() == null) {
        validatorProvider.get().validate(taskXO, OnceToMonthlySchedule.class);
      }
      ZoneOffset clientZoneOffset = ZoneOffset.of(taskXO.getTimeZoneOffset());
      LocalDateTime startDateClient =
          LocalDateTime.ofInstant(taskXO.getStartDate().toInstant(), ZoneId.of(clientZoneOffset.getId()));
      LocalDateTime startDateServer =
          LocalDateTime.ofInstant(taskXO.getStartDate().toInstant(), ZoneId.systemDefault());
      Calendar date = Calendar.getInstance();
      date.setTimeInMillis(taskXO.getStartDate().getTime());
      date.set(Calendar.SECOND, 0);
      date.set(Calendar.MILLISECOND, 0);
      switch (taskXO.getSchedule()) {
        case "once":
          validatorProvider.get().validate(taskXO, OnceSchedule.class);
          return taskScheduler.getScheduleFactory().once(date.getTime());

        case "hourly":
          return taskScheduler.getScheduleFactory().hourly(date.getTime());

        case "daily":
          return taskScheduler.getScheduleFactory().daily(date.getTime());

        case "weekly":
          return taskScheduler.getScheduleFactory()
              .weekly(date.getTime(), Arrays.stream(taskXO.getRecurringDays())
                  .map(recurringDay -> Weekday.values()[shiftWeekDay(recurringDay - 1, startDateClient,
                      startDateServer)])
                  .collect(Collectors.toSet()));

        case "monthly":
          return taskScheduler.getScheduleFactory()
              .monthly(date.getTime(), Arrays.stream(taskXO.getRecurringDays())
                  .map(recurringDay -> recurringDay == 999
                      ? CalendarDay.lastDay()
                      : CalendarDay.day(
                          shiftMonthDay(recurringDay, startDateClient, startDateServer)))
                  .collect(Collectors.toSet()));
      }
    }
    return taskScheduler.getScheduleFactory().manual();
  }

  @VisibleForTesting
  void validateState(final String taskId, final TaskInfo taskInfo) {
    if (taskInfo == null) {
      throw new NotFoundException(String.format("Task with id '%s' not found", taskId));
    }
    ExternalTaskState externalTaskState = taskScheduler.toExternalTaskState(taskInfo);
    if (externalTaskState.getState().isRunning()) {
      throw new IllegalStateException(
          "Task can not be edited while it is being executed or it is in line to be executed");
    }
  }

  @VisibleForTesting
  void validateScriptUpdate(final TaskInfo task, final TaskXO update) {
    String originalSource = task.getConfiguration().getString("source");
    String updateSource = update.getProperties().get("source");

    if (!allowCreation && originalSource != null && !originalSource.equals(updateSource)) {
      throw new IllegalStateException("Script source updates are not allowed");
    }
  }

  /**
   * Handle parsing errors at the quartz level, which include logically incorrect settings in addition to the purely
   * syntactic validations (regex) we already apply.
   */
  private TaskInfo scheduleTask(Callable<TaskInfo> callable) throws Exception {
    try {
      return callable.call();
    }
    catch (Exception e) {
      log.error("Failed to schedule task", e);
      throw e;
    }
  }

  private static String getSchedule(final Schedule schedule) {
    if (schedule instanceof Manual) {
      return "manual";
    }
    else if (schedule instanceof Now) {
      return "internal";
    }
    else if (schedule instanceof Once) {
      return "once";
    }
    else if (schedule instanceof Hourly) {
      return "hourly";
    }
    else if (schedule instanceof Daily) {
      return "daily";
    }
    else if (schedule instanceof Weekly) {
      return "weekly";
    }
    else if (schedule instanceof Monthly) {
      return "monthly";
    }
    else if (schedule instanceof Cron) {
      return "advanced";
    }
    else {
      // FIXME: Is this valid? There should be no other Schedule types other than handled above
      return schedule.getClass().getName();
    }
  }

  private static String getLastRunResult(final TaskInfo taskInfo, final TaskState endState, final Long runDuration) {
    StringBuilder lastRunResult = new StringBuilder();

    if (endState != null) {
      if (OK.equals(endState)) {
        lastRunResult.append(TASK_RESULT_OK);
      }
      else if (CANCELED.equals(endState)) {
        lastRunResult.append(TASK_RESULT_CANCELED);
      }
      else if (FAILED.equals(endState)) {
        lastRunResult.append(TASK_RESULT_ERROR);
      }
      else if (INTERRUPTED.equals(endState)) {
        lastRunResult.append(TASK_RESULT_INTERRUPTED);
      }
      else {
        lastRunResult.append(endState.name());
      }

      if (runDuration != null) {
        long milliseconds = runDuration;

        int hours = (int) ((milliseconds / 1000) / 3600);
        int minutes = (int) ((milliseconds / 1000) / 60 - hours * 60);
        int seconds = (int) ((milliseconds / 1000) % 60);

        lastRunResult.append(" [");
        if (hours != 0) {
          lastRunResult.append(hours).append("h");
        }
        if (minutes != 0 || hours != 0) {
          lastRunResult.append(minutes).append("m");
        }
        lastRunResult.append(seconds).append("s]");
      }

      appendPlanReconciliationText(lastRunResult, endState, taskInfo);
    }
    return lastRunResult.toString();
  }

  private static void appendPlanReconciliationText(StringBuilder lastRunResult, TaskState endState, TaskInfo taskInfo) {
    if (OK.equals(endState) && taskInfo.getTypeId().equals(PLAN_RECONCILIATION_TASK_ID)) {
      lastRunResult.append(PLAN_RECONCILIATION_TASK_OK_TEXT);
    }
  }

  private static TaskTypeXO asTaskTypeXO(final TaskDescriptor taskDescriptor) {
    TaskTypeXO taskTypeXO = new TaskTypeXO();

    taskTypeXO.setId(taskDescriptor.getId());
    taskTypeXO.setName(taskDescriptor.getName());
    taskTypeXO.setExposed(taskDescriptor.isExposed());
    taskTypeXO.setConcurrentRun(taskDescriptor.allowConcurrentRun());
    if (taskDescriptor.getFormFields() != null) {
      taskTypeXO.setFormFields(taskDescriptor.getFormFields().stream().map(FormFieldXO::create).collect(toList()));
    }
    return taskTypeXO;
  }
}
