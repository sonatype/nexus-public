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
package org.sonatype.nexus.quartz.internal.task;

import java.util.Date;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
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
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.quartz.TriggerKey.triggerKey;

/**
 * A {#link JobListenerSupport} that provides NX Task integration by creating future when task starts, recording
 * execution results.
 *
 * Each NX Task wrapping job has one listener. Since NX Job wrapping tasks cannot concurrently execute
 * ("unique per jobKey", basically per NX Task "instance"), this listener may be stateful, and maintain
 * the task info in simple way.
 *
 * @since 3.0
 */
public class QuartzTaskJobListener
    extends ComponentSupport
    implements JobListener
{
  private final String name;

  private final EventManager eventManager;

  private final QuartzSchedulerSPI scheduler;

  private final QuartzTaskInfo taskInfo;

  public QuartzTaskJobListener(final String name,
                               final EventManager eventManager,
                               final QuartzSchedulerSPI scheduler,
                               final QuartzTaskInfo taskInfo)
  {
    this.name = checkNotNull(name);
    this.eventManager = checkNotNull(eventManager);
    this.scheduler = checkNotNull(scheduler);
    this.taskInfo = checkNotNull(taskInfo);
  }

  public QuartzTaskInfo getTaskInfo() {
    return taskInfo;
  }

  /**
   * Returns the trigger associated with NX Task wrapping job.
   *
   * The trigger executing this Job does NOT have to be THAT trigger, think about "runNow"!
   * So, this method returns the associated trigger, while the trigger in context might be something
   * completely different.
   *
   * If not found, returns {@code null}.
   */
  @Nullable
  private Trigger getJobTrigger(final JobExecutionContext context) {
    try {
      final JobKey jobKey = context.getJobDetail().getKey();
      return context.getScheduler().getTrigger(triggerKey(jobKey.getName(), jobKey.getGroup()));
    }
    catch (SchedulerException e) {
      return null;
    }
  }

  /**
   * Returns the current trigger for currently executing NX Task.
   *
   * That is either its associated trigger loaded up by key using {@link #getJobTrigger(JobExecutionContext)},
   * or if not found (can happen when invoked from {@link #jobWasExecuted(JobExecutionContext, JobExecutionException)}
   * method for a canceled/removed job, the "current" trigger from context is returned.
   *
   * Never returns {@code null}, as Quartz context always contains a trigger.
   */
  private Trigger getCurrentTrigger(final JobExecutionContext context) {
    final Trigger jobTrigger = getJobTrigger(context);
    return jobTrigger != null ? jobTrigger : context.getTrigger();
  }

  @Override
  public void jobToBeExecuted(final JobExecutionContext context) {
    final JobKey jobKey = context.getJobDetail().getKey();
    log.trace("Job {} : {} jobToBeExecuted", jobKey.getName(), taskInfo.getConfiguration().getTaskLogName());
    // get current trigger, which in this method SHOULD be job's trigger.
    // Still, in some circumstances (that I cannot imagine right now, except to have concurrency bug)
    // the NX Task's Trigger might be missing. Still, we don't want to throw in this listener
    // as that would make whole Quartz instance inconsistent. Also, even if job removed (coz bug exists)
    // we do want to "follow" it's lifecycle here.
    final Trigger currentTrigger = getCurrentTrigger(context);

    QuartzTaskFuture future = taskInfo.getTaskFuture();
    if (future == null) {
      log.trace("Job {} : {} has no future, creating it", jobKey.getName(),
          taskInfo.getConfiguration().getTaskLogName());

      future = new QuartzTaskFuture(scheduler,
          jobKey,
          taskInfo.getConfiguration().getTaskLogName(),
          context.getFireTime(),
          scheduler.triggerConverter().convert(context.getTrigger()),
          null
      );

      // set the future on taskinfo
      taskInfo.setNexusTaskState(
          State.RUNNING,
          new QuartzTaskState(
              QuartzTaskJob.configurationOf(context.getJobDetail()),
              scheduler.triggerConverter().convert(currentTrigger),
              currentTrigger.getNextFireTime()
          ),
          future
      );
    }

    context.put(QuartzTaskFuture.FUTURE_KEY, future);
    context.put(QuartzTaskInfo.TASK_INFO_KEY, taskInfo);

    eventManager.post(new TaskEventStarted(taskInfo));
  }

  @Override
  public void jobWasExecuted(final JobExecutionContext context, final JobExecutionException jobException) {
    final JobKey jobKey = context.getJobDetail().getKey();
    log.trace("Job {} : {} jobWasExecuted", jobKey.getName(), taskInfo.getConfiguration().getTaskLogName());
    final QuartzTaskFuture future = (QuartzTaskFuture) context.get(QuartzTaskFuture.FUTURE_KEY);

    // on Executed, the taskInfo might be removed or even replaced, so use the one we started with
    // DO NOT TOUCH the listener's instance
    final QuartzTaskInfo taskInfo = (QuartzTaskInfo) context.get(QuartzTaskInfo.TASK_INFO_KEY);
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

    final TaskConfiguration taskConfiguration = QuartzTaskJob.configurationOf(context.getJobDetail());
    QuartzTaskState.setLastRunState(
        taskConfiguration,
        endState,
        future.getStartedAt(),
        System.currentTimeMillis() - future.getStartedAt().getTime());

    log.trace("Job {} : {} lastRunState={}",
        jobKey.getName(), taskInfo.getConfiguration().getTaskLogName(), endState);

    Trigger currentTrigger = getCurrentTrigger(context);

    // the job trigger's next fire time (this is ok, as "now" trigger will return null too,
    // as this is the end of it's single one execution)
    final Date nextFireTime = currentTrigger != null ? currentTrigger.getNextFireTime() : null;

    // actual schedule
    final Schedule jobSchedule = scheduler.triggerConverter().convert(currentTrigger);

    // state: if not removed and will fire again: WAITING, otherwise DONE
    final State state = !taskInfo.isRemovedOrDone() && nextFireTime != null ? State.WAITING : State.DONE;

    // unwrap the Quartz wrapped exception and set future result
    final Exception failure = jobException != null && jobException.getCause() instanceof Exception ?
        (Exception) jobException.getCause() : jobException;
    future.setResult(context.getResult(), failure);

    // update task state, w/ respect to future: if DONE keep future, if WAITING drop it
    // This might result in task removal, so we MUST set result as above BEFORE this, otherwise task will be CANCELLED
    taskInfo.setNexusTaskState(
        state,
        new QuartzTaskState(taskConfiguration, jobSchedule, nextFireTime),
        State.DONE == state ? future : null
    );

    // fire events
    switch (endState) {
      case OK:
        eventManager.post(new TaskEventStoppedDone(taskInfo));
        break;

      case FAILED:
        eventManager.post(new TaskEventStoppedFailed(taskInfo, failure));
        break;

      case CANCELED:
        eventManager.post(new TaskEventStoppedCanceled(taskInfo));
        break;
    }
  }

  @Override
  public void jobExecutionVetoed(final JobExecutionContext context) {
    // ignore
  }

  @Override
  public String getName() {
    return name;
  }

  public static String listenerName(final JobKey jobKey) {
    return QuartzTaskJobListener.class.getName() + ":" + jobKey;
  }
}
