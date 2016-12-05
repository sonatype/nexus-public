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

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Mutex;
import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.events.TaskBlockedEvent;
import org.sonatype.nexus.scheduling.events.TaskEventCanceled;
import org.sonatype.nexus.scheduling.events.TaskStartedRunningEvent;

import com.google.common.base.Throwables;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.UnableToInterruptJobException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.scheduling.TaskInfo.RunState.BLOCKED;
import static org.sonatype.nexus.scheduling.TaskInfo.RunState.RUNNING;

/**
 * Quartz {@link Job} wrapping a Nexus {@link Task}.
 *
 * Supports {@link InterruptableJob} but actual interrupt-ability depends on underlying tasks implementation.
 *
 * @since 3.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Named
public class QuartzTaskJob
    extends ComponentSupport
    implements InterruptableJob
{
  private static final Mutex MUTEX = new Mutex();

  private final EventManager eventManager;

  private final Provider<QuartzSchedulerSPI> scheduler;

  private final TaskFactory taskFactory;

  private final BaseUrlManager baseUrlManager;

  private JobExecutionContext context;

  private QuartzTaskInfo taskInfo;

  private QuartzTaskFuture taskFuture;

  private Task task;

  @Inject
  public QuartzTaskJob(final EventManager eventManager,
                       final Provider<QuartzSchedulerSPI> scheduler,
                       final TaskFactory taskFactory,
                       final BaseUrlManager baseUrlManager)
  {
    this.eventManager = checkNotNull(eventManager);
    this.scheduler = checkNotNull(scheduler);
    this.taskFactory = checkNotNull(taskFactory);
    this.baseUrlManager = checkNotNull(baseUrlManager);
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    this.context = checkNotNull(context);
    try {
      doExecute();
    }
    catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, JobExecutionException.class);
      throw new JobExecutionException(e);
    }
    finally {
      this.context = null;
    }
  }

  private void doExecute() throws Exception {
    Exception failure = null;
    try {
      // init all the needed members: context already set, taskInfo and future (to set thread)
      taskInfo = (QuartzTaskInfo) context.get(QuartzTaskInfo.TASK_INFO_KEY);
      checkState(taskInfo != null);

      taskFuture = (QuartzTaskFuture) context.get(QuartzTaskFuture.FUTURE_KEY);
      checkState(taskFuture != null);

      taskFuture.setJobExecutingThread(Thread.currentThread());

      // detect and set the application base-url
      baseUrlManager.detectAndHoldUrl();

      // create TaskConfiguration, and using that the Task
      final TaskConfiguration config = configurationOf(context.getJobDetail());
      task = taskFactory.create(config);
      // after this point, cancellation will be handled okay too

      try {
        if (!taskFuture.isCancelled()) {
          mayBlock();

          if (!taskFuture.isCancelled()) {
            taskFuture.setRunState(RUNNING);
            eventManager.post(new TaskStartedRunningEvent(taskInfo));
            try {
              context.setResult(task.call());
            }
            finally {
              // put back any state task modified to have it persisted
              context.getJobDetail().getJobDataMap().putAll(task.taskConfiguration().asMap());
            }
          }
        }
      }
      catch (TaskInterruptedException e) {
        log.debug("Task {} : {} canceled", config.getId(), config.getTaskLogName(), e);

        // cancel task if not already canceled when interrupted
        QuartzTaskFuture future = taskInfo.getTaskFuture();
        if (future != null && !future.isCancelled()) {
          future.doCancel();
          eventManager.post(new TaskEventCanceled(taskInfo));
        }
      }
      catch (InterruptedException e) {
        log.debug("Task {} : {} interrupted", config.getId(), config.getTaskLogName(), e);

        // non-cancelable task interrupted, treat as canceled to cleanup
        QuartzTaskFuture future = taskInfo.getTaskFuture();
        if (future != null) {
          future.doCancel();
          eventManager.post(new TaskEventCanceled(taskInfo));
        }
      }
      catch (Exception e) {
        log.warn("Task {} : {} execution failure", config.getId(), config.getTaskLogName(), e);
        failure = e;
      }
    }
    catch (Exception e) {
      log.warn("Task {} instantiation failure", context.getJobDetail().getKey(), e);
      failure = e;
    }

    // propagate any failure
    if (failure != null) {
      throw failure;
    }
  }

  /**
   * Waits for other tasks blocked by current task to finish.
   */
  private void mayBlock() {
    while (true) {
      final List<TaskInfo> blockedBy;

      // perform runState change exclusive to all other tasks
      synchronized (MUTEX) {
        // filter all tasks which are not the present task AND are RUNNING
        blockedBy = blockedBy();

        if (blockedBy.isEmpty()) {
          // no tasks are blocked
          log.trace("No blockers for task: {}", task);
          return;
        }
        TaskInfo.RunState previousRunState = taskFuture.getRunState();
        taskFuture.setRunState(BLOCKED);
        if (BLOCKED != previousRunState) {
          // the loop might need multiple iterations but we only want to send the event for an actual state transition
          eventManager.post(new TaskBlockedEvent(taskInfo));
        }
      }

      log.trace("Task: {} is blocked by: {}", task, blockedBy);
      // wait for all blocked tasks
      try {
        for (TaskInfo taskInfo : blockedBy) {
          try {
            Future<?> future = taskInfo.getCurrentState().getFuture();
            if (future != null) {
              future.get(1L, TimeUnit.MINUTES);
            }
            if (taskFuture.isCancelled()) {
              return;
            }
          }
          catch (TimeoutException e) {
            log.trace("Wait for unblock expired", e);
            throw new TaskInterruptedException("Blocked for too long, giving up", true);
          }
          catch (Exception e) {
            // we don't care if other task failed or not, it will report itself
            log.trace("Blocked task failed; ignoring", e);
          }
        }
      }
      catch (IllegalStateException e) {
        // FIXME: refine, not clear this is actually what happens due to wide code block
        // task got canceled: setRunState threw ISEx
        break;
      }
    }
  }

  /**
   * Returns a list of tasks that are considered as "blocking" the execution of this task.
   */
  private List<TaskInfo> blockedBy() {
    // filter all tasks which are not the present task
    // AND are same type
    // AND are RUNNING
    // AND are not blocked
    return scheduler.get().listsTasks().stream()
        .filter(t -> !task.getId().equals(t.getId())
            && task.taskConfiguration().getTypeId().equals(t.getConfiguration().getTypeId())
            && State.RUNNING == t.getCurrentState().getState()
            && BLOCKED != t.getCurrentState().getRunState()
        )
        .collect(Collectors.toList());
  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {
    if (task instanceof Cancelable && !((Cancelable) task).isCanceled()) {
      ((Cancelable) task).cancel();
      eventManager.post(new TaskEventCanceled(taskInfo));
    }
    else if (task != null) {
      log.info("Task not cancelable: {}", task.taskConfiguration().getTaskLogName());
      throw new UnableToInterruptJobException("Task not cancelable: " + task);
    }
    // else premature/too-late; ignore
  }

  /**
   * Extracts {@link TaskConfiguration} from given {@link JobDetail}.
   *
   * Only copies string values from job-data map.
   */
  public static TaskConfiguration configurationOf(final JobDetail jobDetail) {
    checkNotNull(jobDetail);
    TaskConfiguration config = new TaskConfiguration();
    for (Entry<String, Object> entry : jobDetail.getJobDataMap().entrySet()) {
      if (entry.getValue() instanceof String) {
        config.setString(entry.getKey(), (String) entry.getValue());
      }
    }
    return config;
  }
}
