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

import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.quartz.JobSupport;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.RunState;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.events.TaskEventCanceled;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.UnableToInterruptJobException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A {#link JobSupport} wrapping NX Task that is also {@link InterruptableJob} (but actual interruption ability depends
 * on underlying NX Task).
 *
 * @since 3.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Named
public class NexusTaskJobSupport
    extends JobSupport
    implements InterruptableJob
{
  private static class OtherRunningTasks
      implements Predicate<TaskInfo>
  {
    private final Task me;

    public OtherRunningTasks(final Task me) {
      this.me = me;
    }

    @Override
    public boolean apply(final TaskInfo taskInfo) {
      return !me.getId().equals(taskInfo.getId())
          && State.RUNNING == taskInfo.getCurrentState().getState();
    }
  }

  private final EventBus eventBus;

  private final Provider<QuartzTaskExecutorSPI> quartzNexusSchedulerSPIProvider;

  private final TaskFactory taskFactory;

  private final BaseUrlManager baseUrlManager;

  private NexusTaskInfo nexusTaskInfo;

  private NexusTaskFuture future;

  private Task nexusTask;

  @Inject
  public NexusTaskJobSupport(final EventBus eventBus,
                             final Provider<QuartzTaskExecutorSPI> quartzNexusSchedulerSPIProvider,
                             final TaskFactory taskFactory,
                             final BaseUrlManager baseUrlManager)
  {
    this.eventBus = checkNotNull(eventBus);
    this.quartzNexusSchedulerSPIProvider = checkNotNull(quartzNexusSchedulerSPIProvider);
    this.taskFactory = checkNotNull(taskFactory);
    this.baseUrlManager = checkNotNull(baseUrlManager);
  }

  @Override
  public void execute() throws Exception {
    Exception ex = null;
    try {
      // init all the needed members: context already set, taskInfo and future (to set thread)
      nexusTaskInfo = (NexusTaskInfo) context.get(NexusTaskInfo.TASK_INFO_KEY);
      checkState(nexusTaskInfo != null);
      future = (NexusTaskFuture) context.get(NexusTaskFuture.FUTURE_KEY);
      checkState(future != null);
      future.setJobExecutingThread(Thread.currentThread());

      // detect and set the application base-url
      baseUrlManager.detectAndHoldUrl();

      // create TaskConfiguration, and using that the Task
      final TaskConfiguration taskConfiguration = toTaskConfiguration(context.getJobDetail().getJobDataMap());
      nexusTask = taskFactory.createTaskInstance(taskConfiguration);
      // after this point, cancellation will be handled okay too

      try {
        if (!future.isCancelled()) {
          mayBlock();
          if (!future.isCancelled()) {
            future.setRunState(RunState.RUNNING);
            try {
              final Object result = nexusTask.call();
              context.setResult(result);
            }
            finally {
              // put back any state task modified to have it persisted
              context.getJobDetail().getJobDataMap().putAll(nexusTask.taskConfiguration().asMap());
            }
          }
        }
      }
      catch (TaskInterruptedException e) {
        log.debug("Task {} : {} canceled:", taskConfiguration.getId(), taskConfiguration.getTaskLogName(), e);
        if (!nexusTaskInfo.getNexusTaskFuture().isCancelled()) {
          nexusTaskInfo.getNexusTaskFuture().doCancel();
          eventBus.post(new TaskEventCanceled(nexusTaskInfo));
        }
      }
      catch (InterruptedException e) {
        log.debug("Task {} : {} interrupted:", taskConfiguration.getId(), taskConfiguration.getTaskLogName(), e);
        // this is non-cancelable task being interrupted, do the paperwork in this case
        // same as would be done for cancelable tasks in case of #interrupt()
        nexusTaskInfo.getNexusTaskFuture().doCancel();
        eventBus.post(new TaskEventCanceled(nexusTaskInfo));
      }
      catch (Exception e) {
        log.warn("Task {} : {} execution failure", taskConfiguration.getId(), taskConfiguration.getTaskLogName(), e);
        ex = e;
      }
    }
    catch (Exception e) {
      log.warn("Task {} instantiation failure", context.getJobDetail().getKey(), e);
      ex = e;
    }
    if (ex != null) {
      throw ex;
    }
  }

  /**
   * Busy waiting if this task declares itself blocking by other, already running tasks.
   */
  private void mayBlock() {
    // filter for running tasks, to be reused
    final OtherRunningTasks otherRunningTasks = new OtherRunningTasks(nexusTask);
    List<TaskInfo> blockedBy;
    do {
      blockedBy = nexusTask.isBlockedBy(Lists.newArrayList(Iterables.filter(
          quartzNexusSchedulerSPIProvider.get().listsTasks(), otherRunningTasks)));
      // wait for them all
      if (blockedBy != null && !blockedBy.isEmpty()) {
        try {
          // will ISEx if canceled!
          future.setRunState(RunState.BLOCKED);
          for (TaskInfo taskInfo : blockedBy) {
            try {
              taskInfo.getCurrentState().getFuture().get();
            }
            catch (Exception e) {
              // we don't care if other task failed or not, it will report itself
            }
          }
        }
        catch (IllegalStateException e) {
          // task got canceled: setRunState threw ISEx
          break;
        }
      }
    }
    while (!blockedBy.isEmpty());
  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {
    if (nexusTask instanceof Cancelable) {
      ((Cancelable) nexusTask).cancel();
      eventBus.post(new TaskEventCanceled(nexusTaskInfo));
    }
    else {
      if (nexusTask == null) {
        // premature or too late cancellation attempt, just do nothing
        return;
      }
      else {
        log.info("Task {} not cancelable", nexusTask.taskConfiguration().getTaskLogName());
        throw new UnableToInterruptJobException("Task " + nexusTask + " not Cancellable");
      }
    }
  }

  /**
   * Creates {@link TaskConfiguration} out of provided {@link JobDataMap}, by copying only those values that
   * are Strings (as task configuration is {@code Map<String, String>} while job data map values are Objects.
   */
  public static TaskConfiguration toTaskConfiguration(final JobDataMap jobDataMap) {
    final TaskConfiguration taskConfiguration = new TaskConfiguration();
    for (Entry<String, Object> entry : jobDataMap.entrySet()) {
      if (entry.getValue() instanceof String) {
        taskConfiguration.setString(entry.getKey(), (String) entry.getValue());
      }
    }
    return taskConfiguration;
  }
}