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
package org.sonatype.nexus.quartz.internal.nexus;

import java.util.Date;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo.EndState;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.events.TaskEventStarted;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedCanceled;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.quartz.TriggerKey.triggerKey;
import static org.sonatype.nexus.quartz.internal.nexus.NexusTaskJobSupport.toTaskConfiguration;

/**
 * A {#link JobListenerSupport} that provides NX Task integration by creating future when task starts, recording
 * execution results. Each NX Task wrapping job has one listener. Since NX Job wrapping tasks cannot concurrently
 * execute ("unique per jobKey", basically per NX Task "instance"), this listener may be stateful, and maintain
 * the task info in simple way.
 *
 * @since 3.0
 */
public class NexusTaskJobListener<T>
    extends JobListenerSupport
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final EventBus eventBus;

  private final QuartzTaskExecutorSPI quartzSupport;

  private final JobKey jobKey;

  private final NexusScheduleConverter nexusScheduleConverter;

  private final NexusTaskInfo nexusTaskInfo;

  public NexusTaskJobListener(final EventBus eventBus,
                              final QuartzTaskExecutorSPI quartzSupport,
                              final JobKey jobKey,
                              final NexusScheduleConverter nexusScheduleConverter,
                              final NexusTaskState initialState,
                              final @Nullable NexusTaskFuture nexusTaskFuture)
  {
    this.eventBus = checkNotNull(eventBus);
    this.quartzSupport = checkNotNull(quartzSupport);
    this.jobKey = checkNotNull(jobKey);
    this.nexusScheduleConverter = checkNotNull(nexusScheduleConverter);
    this.nexusTaskInfo = new NexusTaskInfo(
        quartzSupport,
        jobKey,
        initialState,
        nexusTaskFuture
    );
  }

  public NexusTaskInfo getNexusTaskInfo() {
    return nexusTaskInfo;
  }

  // == JobListener

  /**
   * Returns the trigger associated with NX Task wrapping job. The trigger executing this Job does NOT have to be
   * THAT trigger, think about "runNow"! So, this method returns the associated trigger, while the trigger in
   * context might be something completely different. If not found, returns {@code null}.
   */
  @Nullable
  private Trigger getJobTrigger(final JobExecutionContext context) {
    try {
      return context.getScheduler().getTrigger(triggerKey(jobKey.getName(), jobKey.getGroup()));
    }
    catch (SchedulerException e) {
      return null;
    }
  }

  /**
   * Returns the current trigger for currently executing NX Task. That is either its associated trigger loaded
   * up by key using {@link #getJobTrigger(JobExecutionContext)}, or if not found (can happen when invoked from
   * {@link #jobWasExecuted(JobExecutionContext, JobExecutionException)} method for a canceled/removed job, the
   * "current" trigger from context is returned. Never returns {@code null}, as QZ context always contains a
   * trigger.
   */
  private Trigger getCurrentTrigger(final JobExecutionContext context) {
    final Trigger jobTrigger = getJobTrigger(context);
    return jobTrigger != null ? jobTrigger : context.getTrigger();
  }

  @Override
  public void jobToBeExecuted(final JobExecutionContext context) {
    log.trace("Job {} : {} jobToBeExecuted", jobKey.getName(), nexusTaskInfo.getConfiguration().getTaskLogName());
    // get current trigger, which in this method SHOULD be job's trigger.
    // Still, in some circumstances (that I cannot imagine right now, except to have concurrency bug)
    // the NX Task's Trigger might be missing. Still, we don't want to throw in this listener
    // as that would make whole QZ instance inconsistent. Also, even if job removed (coz bug exists)
    // we do want to "follow" it's lifecycle here.
    final Trigger currentTrigger = getCurrentTrigger(context);

    NexusTaskFuture future = nexusTaskInfo.getNexusTaskFuture();
    if (future == null) {
      log.trace("Job {} : {} has no future, creating it", jobKey.getName(),
          nexusTaskInfo.getConfiguration().getTaskLogName());
      future = new NexusTaskFuture(quartzSupport, jobKey, nexusTaskInfo.getConfiguration().getTaskLogName(),
          context.getFireTime(),
          nexusScheduleConverter.toSchedule(context.getTrigger()));
      // set the future on taskinfo
      nexusTaskInfo.setNexusTaskState(
          State.RUNNING,
          new NexusTaskState(
              toTaskConfiguration(context.getJobDetail().getJobDataMap()),
              nexusScheduleConverter.toSchedule(currentTrigger),
              currentTrigger.getNextFireTime()
          ),
          future
      );
    }
    context.put(NexusTaskFuture.FUTURE_KEY, future);
    context.put(NexusTaskInfo.TASK_INFO_KEY, nexusTaskInfo);
    eventBus.post(new TaskEventStarted(nexusTaskInfo));
  }

  @Override
  public void jobWasExecuted(final JobExecutionContext context, final JobExecutionException jobException) {
    log.trace("Job {} : {} jobWasExecuted", jobKey.getName(), nexusTaskInfo.getConfiguration().getTaskLogName());
    final NexusTaskFuture future = (NexusTaskFuture) context.get(NexusTaskFuture.FUTURE_KEY);
    // on Executed, the taskInfo might be removed or even replaced, so use the one we started with
    // DO NOT TOUCH the listener's instance
    final NexusTaskInfo nexusTaskInfo = (NexusTaskInfo) context.get(NexusTaskInfo.TASK_INFO_KEY);
    final EndState endState;
    if (future.isCancelled()) {
      endState = EndState.CANCELED;
    }
    else if (jobException != null) {
      endState = EndState.FAILED;
    }
    else {
      endState = EndState.OK;
    }

    final TaskConfiguration taskConfiguration = toTaskConfiguration(context.getJobDetail().getJobDataMap());
    NexusTaskState.setLastRunState(
        taskConfiguration,
        endState,
        future.getStartedAt(),
        System.currentTimeMillis() - future.getStartedAt().getTime());
    log.trace("Job {} : {} lastRunState={}", jobKey.getName(), nexusTaskInfo.getConfiguration().getTaskLogName(),
        endState);

    Trigger currentTrigger = getCurrentTrigger(context);

    // the job trigger's next fire time (this is ok, as "now" trigger will return null too,
    // as this is the end of it's single one execution)
    final Date nextFireTime = currentTrigger != null ? currentTrigger.getNextFireTime() : null;

    // actual schedule
    final Schedule jobSchedule = nexusScheduleConverter.toSchedule(currentTrigger);
    // state: if not removed and will fire again: WAITING, otherwise DONE
    final State state = !nexusTaskInfo.isRemovedOrDone() && nextFireTime != null ? State.WAITING : State.DONE;
    // update task state, w/ respect to future: if DONE keep future, if WAITING drop it
    nexusTaskInfo.setNexusTaskState(
        state,
        new NexusTaskState(
            taskConfiguration,
            jobSchedule,
            nextFireTime
        ),
        State.DONE == state ? future : null
    );

    // unwrap the QZ wrapped exception and set future result
    final Exception failure =
        jobException != null && jobException.getCause() instanceof Exception ?
            (Exception) jobException.getCause() : jobException;
    future.setResult(
        (T) context.getResult(),
        failure
    );

    // fire events
    switch (endState) {
      case OK:
        eventBus.post(new TaskEventStoppedDone(nexusTaskInfo));
        break;
      case FAILED:
        eventBus.post(new TaskEventStoppedFailed(nexusTaskInfo, failure));
        break;
      case CANCELED:
        eventBus.post(new TaskEventStoppedCanceled(nexusTaskInfo));
        break;
    }
  }

  @Override
  public String getName() {
    return listenerName(jobKey);
  }

  // ==

  public static String listenerName(final JobKey jobKey) {
    return NexusTaskJobListener.class.getName() + ":" + jobKey.toString();
  }
}