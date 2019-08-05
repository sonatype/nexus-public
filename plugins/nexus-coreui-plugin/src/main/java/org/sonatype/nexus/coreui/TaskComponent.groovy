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
package org.sonatype.nexus.coreui

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.Validator
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.scheduling.ClusteredTaskState
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.TaskState
import org.sonatype.nexus.scheduling.schedule.Cron
import org.sonatype.nexus.scheduling.schedule.Daily
import org.sonatype.nexus.scheduling.schedule.Hourly
import org.sonatype.nexus.scheduling.schedule.Manual
import org.sonatype.nexus.scheduling.schedule.Monthly
import org.sonatype.nexus.scheduling.schedule.Monthly.CalendarDay
import org.sonatype.nexus.scheduling.schedule.Now
import org.sonatype.nexus.scheduling.schedule.Once
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.scheduling.schedule.Weekly
import org.sonatype.nexus.scheduling.schedule.Weekly.Weekday
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static org.sonatype.nexus.repository.date.TimeZoneUtils.shiftWeekDay
import static org.sonatype.nexus.repository.date.TimeZoneUtils.shiftMonthDay
import static org.sonatype.nexus.scheduling.TaskState.CANCELED
import static org.sonatype.nexus.scheduling.TaskState.FAILED
import static org.sonatype.nexus.scheduling.TaskState.INTERRUPTED
import static org.sonatype.nexus.scheduling.TaskState.OK
import static org.sonatype.nexus.scheduling.TaskState.RUNNING
import static org.sonatype.nexus.scheduling.TaskState.RUNNING_BLOCKED
import static org.sonatype.nexus.scheduling.TaskState.RUNNING_CANCELED
import static org.sonatype.nexus.scheduling.TaskState.RUNNING_STARTING
import static org.sonatype.nexus.scheduling.TaskState.WAITING

/**
 * Task {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Task')
class TaskComponent
    extends DirectComponentSupport
{
  @Inject
  TaskScheduler scheduler

  @Inject
  Provider<Validator> validatorProvider

  /**
   * Retrieve a list of scheduled tasks.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:tasks:read')
  List<TaskXO> read() {
    return scheduler.listsTasks().findAll { it.configuration.visible }.collect { TaskInfo task -> asTaskXO(task) }
  }

  /**
   * Retrieve available task types.
   * @return a list of task types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:tasks:read')
  List<TaskTypeXO> readTypes() {
    return scheduler.taskFactory.descriptors.collect { descriptor ->
      def result = new TaskTypeXO(
          id: descriptor.id,
          name: descriptor.name,
          exposed: descriptor.exposed,
          formFields: descriptor.formFields?.collect { FormFieldXO.create(it) }
      )
      return result
    }
  }

  /**
   * Creates a task.
   * @param taskXO to be created
   * @return created task
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:tasks:create')
  @Validate(groups = [Create.class, Default.class])
  TaskXO create(final @NotNull @Valid TaskXO taskXO) {
    Schedule schedule = asSchedule(taskXO)

    TaskConfiguration config = scheduler.createTaskConfigurationInstance(taskXO.typeId)
    taskXO.properties.each { key, value ->
      config.setString(key, value)
    }
    config.setAlertEmail(taskXO.alertEmail)
    config.setName(taskXO.name)
    config.setEnabled(taskXO.enabled)

    TaskInfo task = scheduleTask { scheduler.scheduleTask(config, schedule) }
    log.debug "Created task with type '${config.class}': ${config.name} (${config.id})"
    return asTaskXO(task)
  }

  /**
   * Updates a task.
   * @param taskXO to be updated
   * @return updated task
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:tasks:update')
  @Validate(groups = [Update.class, Default.class])
  TaskXO update(final @NotNull @Valid TaskXO taskXO) {
    TaskInfo task = scheduler.getTaskById(taskXO.id)
    validateState(task)
    Schedule schedule = asSchedule(taskXO)
    task.configuration.enabled = taskXO.enabled
    task.configuration.name = taskXO.name
    taskXO.properties.each { key, value ->
      task.configuration.setString(key, value)
    }
    task.configuration.setAlertEmail(taskXO.alertEmail)
    task.configuration.setName(taskXO.name)

    task = scheduleTask { scheduler.scheduleTask(task.configuration, schedule) }

    return asTaskXO(task)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:tasks:delete')
  @Validate
  void remove(final @NotEmpty String id) {
    scheduler.getTaskById(id)?.remove()
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:tasks:start')
  @Validate
  void run(final @NotEmpty String id) {
    scheduler.getTaskById(id)?.runNow()
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:tasks:stop')
  @Validate
  void stop(final @NotEmpty String id) {
    scheduler.cancel(id, false)
  }

  @PackageScope
  String getSchedule(final Schedule schedule) {
    if (schedule instanceof Manual) {
      return 'manual'
    }
    else if (schedule instanceof Now) {
      return 'internal'
    }
    else if (schedule instanceof Once) {
      return 'once'
    }
    else if (schedule instanceof Hourly) {
      return 'hourly'
    }
    else if (schedule instanceof Daily) {
      return 'daily'
    }
    else if (schedule instanceof Weekly) {
      return 'weekly'
    }
    else if (schedule instanceof Monthly) {
      return 'monthly'
    }
    else if (schedule instanceof Cron) {
      return 'advanced'
    }
    else {
      // FIXME: Is this valid?  There should be no other Schedule types other than handled above
      return schedule.getClass().getName()
    }
  }

  @PackageScope
  Date getNextRun(final TaskInfo task) {
    return task.currentState.nextRun
  }

  @PackageScope
  String getLastRunResult(final TaskState endState, final Long runDuration) {
    String lastRunResult = null

    if (endState) {
      switch (endState) {
        case OK:
          lastRunResult = 'Ok'
          break

        case CANCELED:
          lastRunResult = 'Canceled'
          break

        case FAILED:
          lastRunResult = 'Error'
          break

        case INTERRUPTED:
          lastRunResult = 'Interrupted'
          break

        default:
          lastRunResult = endState.name()
      }
      if (runDuration) {
        long milliseconds = runDuration

        int hours = (int) ((milliseconds / 1000) / 3600)
        int minutes = (int) ((milliseconds / 1000) / 60 - hours * 60)
        int seconds = (int) (((long) (milliseconds / 1000)) % 60)

        lastRunResult += ' ['
        if (hours != 0) {
          lastRunResult += hours
          lastRunResult += 'h'
        }
        if (minutes != 0 || hours != 0) {
          lastRunResult += minutes
          lastRunResult += 'm'
        }
        lastRunResult += seconds
        lastRunResult += 's'
        lastRunResult += ']'
      }
    }
    return lastRunResult
  }

  @PackageScope
  TaskXO asTaskXO(final TaskInfo task) {
    List<ClusteredTaskState> clusteredTaskStates = scheduler.getClusteredTaskStateById(task.id)
    TaskState state = task.currentState.state
    TaskState endState = task.lastRunState?.endState
    Date lastRun = task.lastRunState?.runStarted
    Long runDuration = task.lastRunState?.runDuration
    if (clusteredTaskStates) {
      state = getAggregateState(clusteredTaskStates)
      endState = getAggregateEndState(clusteredTaskStates)
      lastRun = getAggregateLastRun(clusteredTaskStates)
      runDuration = getAggregateRunDuration(clusteredTaskStates)
    }

    def result = new TaskXO(
        id: task.id,
        enabled: task.configuration.enabled,
        name: task.name,
        typeId: task.configuration.typeId,
        typeName: task.configuration.typeName,
        status: state,
        statusDescription: task.configuration.enabled ? state.description : 'Disabled',
        clusteredTaskStates: asTaskStates(clusteredTaskStates),
        schedule: getSchedule(task.schedule),
        lastRun: lastRun,
        lastRunResult: getLastRunResult(endState, runDuration),
        nextRun: getNextRun(task),
        runnable: state.waiting,
        stoppable: state.running,
        alertEmail: task.configuration.alertEmail,
        properties: task.configuration.asMap()
    )
    def schedule = task.schedule
    if (schedule instanceof Once) {
      result.startDate = schedule.startAt
    }
    if (schedule instanceof Hourly) {
      result.startDate = schedule.startAt
    }
    if (schedule instanceof Daily) {
      result.startDate = schedule.startAt
    }
    if (schedule instanceof Weekly) {
      result.startDate = schedule.startAt
      // expects integers with 1=SUN, 2=MON, etc...
      result.recurringDays = schedule.daysToRun.collect { it.ordinal() + 1 }
    }
    if (schedule instanceof Monthly) {
      result.startDate = schedule.startAt
      // expects ints, with 999 being the lastDayOfMonth
      result.recurringDays = schedule.daysToRun.collect { it.isLastDayOfMonth() ? 999 : it.day }
    }
    if (schedule instanceof Cron) {
      result.startDate = schedule.startAt
      result.cronExpression = schedule.cronExpression
    }
    result
  }

  @PackageScope
  TaskState getAggregateState(final List<ClusteredTaskState> clusteredTaskStates) {
    return findTopPriorityState(clusteredTaskStates*.state as Set, [RUNNING, WAITING, OK])
  }

  @PackageScope
  TaskState getAggregateRunState(final List<ClusteredTaskState> clusteredTaskStates) {
    return findTopPriorityState(clusteredTaskStates*.runState as Set,
        [RUNNING_CANCELED, RUNNING,
         RUNNING_BLOCKED, RUNNING_STARTING])
  }

  @PackageScope
  TaskState getAggregateEndState(final List<ClusteredTaskState> clusteredTaskStates) {
    return findTopPriorityState(clusteredTaskStates*.lastEndState as Set, [FAILED, CANCELED,
                                                                           INTERRUPTED, OK])
  }

  @PackageScope
  Date getAggregateLastRun(final List<ClusteredTaskState> clusteredTaskStates) {
    return clusteredTaskStates*.lastRunStarted.max()
  }
  
  @PackageScope
  Long getAggregateRunDuration(final List<ClusteredTaskState> clusteredTaskStates) {
    return clusteredTaskStates*.lastRunDuration.max()
  }

  private <E> E findTopPriorityState(Set<E> states, List<E> priorities) {
    for (E priority : priorities) {
      if (states.contains(priority)) {
        return priority
      }
    }
  }

  @PackageScope
  List<TaskStateXO> asTaskStates(final List<ClusteredTaskState> clusteredTaskStates) {
    List<TaskStateXO> xos = null
    if (hasAnyNotCompletedSuccessfully(clusteredTaskStates)) {
      xos = clusteredTaskStates.collect {
        new TaskStateXO(
          nodeId: it.nodeId,
          status: it.state,
          statusDescription: it.state.description,
          lastRunResult: getLastRunResult(it.lastEndState, it.lastRunDuration)
        )
      }
    }
    return xos
  }

  private boolean hasAnyNotCompletedSuccessfully(final List<ClusteredTaskState> clusteredTaskStates) {
    return clusteredTaskStates.any {
      it.state.running || it.lastEndState in [INTERRUPTED, FAILED, CANCELED]
    }
  }

  @PackageScope
  Schedule asSchedule(final TaskXO taskXO) {
    if (taskXO.schedule == 'advanced') {
      ZoneOffset clientZoneOffset = ZoneOffset.of(taskXO.timeZoneOffset)
      validatorProvider.get().validate(taskXO, TaskXO.AdvancedSchedule)
      return scheduler.getScheduleFactory().cron(new Date(), taskXO.cronExpression, clientZoneOffset.id)
    }
    if (taskXO.schedule != 'manual') {
      if (!taskXO.startDate) {
        validatorProvider.get().validate(taskXO, TaskXO.OnceToMonthlySchedule)
      }
      ZoneOffset clientZoneOffset = ZoneOffset.of(taskXO.timeZoneOffset)
      LocalDateTime startDateClient = LocalDateTime.ofInstant(taskXO.startDate.toInstant(), ZoneId.of(clientZoneOffset.id))
      LocalDateTime startDateServer = LocalDateTime.ofInstant(taskXO.startDate.toInstant(), ZoneId.systemDefault())
      def date = Calendar.instance
      date.setTimeInMillis(taskXO.startDate.time)
      date.set(Calendar.SECOND, 0)
      date.set(Calendar.MILLISECOND, 0)
      switch (taskXO.schedule) {
        case 'once':
          validatorProvider.get().validate(taskXO, TaskXO.OnceSchedule)
          return scheduler.getScheduleFactory().once(date.time)

        case 'hourly':
          return scheduler.getScheduleFactory().hourly(date.time)

        case 'daily':
          return scheduler.getScheduleFactory().daily(date.time)

        case 'weekly':
          return scheduler.getScheduleFactory().weekly(date.time, taskXO.recurringDays.
              collect { Weekday.values()[shiftWeekDay(it - 1, startDateClient, startDateServer)] } as Set<Weekday>)

        case 'monthly':
          return scheduler.getScheduleFactory().monthly(date.time, taskXO.recurringDays.
              collect { it == 999 ? CalendarDay.lastDay() : CalendarDay.
                  day(shiftMonthDay(it, startDateClient, startDateServer)) } as Set<CalendarDay>)
      }
    }
    return scheduler.getScheduleFactory().manual()
  }

  @PackageScope
  void validateState(final TaskInfo task) {
    TaskState localState = task.currentState.state
    List<ClusteredTaskState> clusteredTaskStates = scheduler.getClusteredTaskStateById(task.id)
    boolean running = localState.running ||
        (clusteredTaskStates != null &&
         clusteredTaskStates.any { clusteredTaskState -> clusteredTaskState.state.running })
    if (running) {
      throw new IllegalStateException(
          'Task can not be edited while it is being executed or it is in line to be executed')
    }
  }

  /**
   * Handle parsing errors at the quartz level, which include logically incorrect settings in addition to the purely
   * syntactic validations (regex) we already apply.
   */
  @PackageScope
  TaskInfo scheduleTask(Closure taskScheduler) {
    try {
      taskScheduler.call()
    }
    catch (Exception e) {
      log.error('Failed to schedule task', e)
      throw e
    }
  }
}
