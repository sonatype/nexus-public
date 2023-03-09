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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.events.TaskDeletedEvent;
import org.sonatype.nexus.scheduling.schedule.Manual;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.scheduling.TaskState.RUNNING;
import static org.sonatype.nexus.scheduling.TaskState.WAITING;

/**
 * Quartz {@link TaskInfo}.
 *
 * When this class has future (is not null), the task is meant to be started (either by schedule or by "runNow").
 *
 * When this class has no future, that means that task is in {@link TaskState.Group#WAITING} or {@link TaskState.Group#DONE} states.
 *
 * @since 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuartzTaskInfo
    extends ComponentSupport
    implements TaskInfo
{
  /**
   * Key used in job execution context to stick task info in.
   */
  static final String TASK_INFO_KEY = QuartzTaskInfo.class.getName();

  private final EventManager eventManager;

  private final QuartzSchedulerSPI scheduler;

  private final JobKey jobKey;

  private volatile TaskState state;

  private volatile QuartzTaskState taskState;

  private volatile QuartzTaskFuture taskFuture;

  private volatile boolean removed;

  private Object lastResult;

  private final Map<String,Object> context = new ConcurrentHashMap<>();

  public QuartzTaskInfo(final EventManager eventManager,
                        final QuartzSchedulerSPI scheduler,
                        final JobKey jobKey,
                        final QuartzTaskState taskState,
                        @Nullable final QuartzTaskFuture taskFuture)
  {
    this.eventManager = checkNotNull(eventManager);
    this.scheduler = checkNotNull(scheduler);
    this.jobKey = checkNotNull(jobKey);
    this.removed = false;
    setNexusTaskState(taskFuture != null ? RUNNING : WAITING, taskState, taskFuture);
  }

  public synchronized boolean isRemovedOrDone() {
    return removed || state.isDone();
  }

  public synchronized void setNexusTaskState(final TaskState newState,
                                             final QuartzTaskState taskState,
                                             @Nullable final QuartzTaskFuture taskFuture)
  {
    checkNotNull(newState);
    checkNotNull(taskState);
    checkState(!newState.isRunning() || taskFuture != null, "Running task must have future");

    final TaskConfiguration config = taskState.getConfiguration();

    if (this.state == null) {
      log.info("Task {} : state={}", config.getTaskLogName(), newState);
    }
    else {
      if (this.state != newState) {
        // we have a transition
        String newStateName = newState.name();
        if (newState.isWaiting() && taskState.getLastRunState() != null) {
          // we ended running and have lastRunState available, enhance log with it
          newStateName = newStateName + " (" + taskState.getLastRunState().getEndState().name() + ")";
        }
        //some tasks don't care to add noise to the logs for start/stop states, and manage logging themselves
        if (config.isLogTaskState()) {
          if (log.isDebugEnabled()) {
            log.info("Task {} : {} state change {} -> {}",
                jobKey,
                config.getTaskLogName(),
                this.state, newStateName);
          }
          else {
            log.info("Task {} state change {} -> {}",
                config.getTaskLogName(),
                this.state, newStateName);
          }
        }
      }
      //more info we only want to print if the task needs it
      else if (config.isLogTaskState()) {
        // this is usually config change of waiting task
        log.debug("Task {} : {} : state={} nextRun={}",
            jobKey.getName(),
            config.getTaskLogName(),
            newState,
            taskState.getNextExecutionTime()
        );
      }
    }

    this.state = newState;
    this.taskState = taskState;
    this.taskFuture = taskFuture;

    // DONE tasks should be removed, if not removed already by #remove() method
    if (!removed && newState.isDone()) {
      scheduler.removeTask(jobKey);
      removed = true;
      log.debug("Task {} : {} is done and removed", jobKey.getName(), config.getTaskLogName());
    }
  }

  /**
   * Sets task state only if it's {@link TaskState#WAITING}, otherwise does nothing.
   */
  public synchronized void setNexusTaskStateIfWaiting(final QuartzTaskState taskState,
                                                      @Nullable final QuartzTaskFuture taskFuture)
  {
    if (state.isWaiting()) {
      setNexusTaskState(state, taskState, taskFuture);
    }
  }

  public JobKey getJobKey() {
    return jobKey;
  }

  @Nullable
  public QuartzTaskFuture getTaskFuture() {
    return taskFuture;
  }

  @Override
  public String getId() {
    return getConfiguration().getId();
  }

  @Override
  public String getName() {
    return getConfiguration().getName();
  }

  @Override
  public String getTypeId() {
    return getConfiguration().getTypeId();
  }

  @Override
  public String getMessage() {
    return getConfiguration().getMessage();
  }

  @Override
  public synchronized TaskConfiguration getConfiguration() {
    return taskState.getConfiguration();
  }

  @Override
  public synchronized Schedule getSchedule() {
    return taskState.getSchedule();
  }

  @Override
  public synchronized CurrentState getCurrentState() {
    if (taskState.getSchedule() instanceof Manual) {
      return new CurrentStateImpl(state, null, taskFuture);
    }
    else {
      return new CurrentStateImpl(state, taskState.getNextExecutionTime(), taskFuture);
    }
  }

  @Nullable
  public Object getLastResult() {
    return lastResult;
  }

  public void setLastResult(final Object result) {
    this.lastResult = result;
  }

  @Override
  public synchronized boolean remove() {
    final TaskConfiguration config = taskState.getConfiguration();

    if (isRemovedOrDone()) {
      // already removed
      log.debug("Task {} : {} already removed", jobKey.getName(), config.getTaskLogName());
      return true;
    }

    if (taskFuture != null && !taskFuture.cancel(false)) {
      // running and not cancelable
      log.debug("Task {} : {} is running as is not cancelable", jobKey.getName(), config.getTaskLogName());
      return false;
    }

    if (!config.hasLastRunState()) {
      // if no last state (removed even before 1st run), add one noting it got removed/canceled
      // if was running and is cancelable, the task will itself set a proper ending state
      config.setLastRunState(TaskState.CANCELED, new Date(), 0L);
    }

    removed = true;
    boolean result = scheduler.removeTask(jobKey);
    if (result) {
      log.info("Task {} removed", config.getTaskLogName());

      // HACK: does not seem to be a better place to fire an event when a task (with context of TaskInfo) is deleted
      eventManager.post(new TaskDeletedEvent(this));
    }
    else {
      log.warn("Task {} vanished", config.getTaskLogName());
    }
    return result;
  }

  @Override
  public TaskInfo runNow(final String triggerSource) throws TaskRemovedException {

    synchronized (this) {
      checkState(!state.isRunning(), "Task %s already running", taskState.getConfiguration().getTaskLogName());

      if (!getConfiguration().isEnabled()) {
        log.warn("Task {} is disabled and will not be run", taskState.getConfiguration().getTaskLogName());
        return this;
      }

      if (isRemovedOrDone()) {
        throw new TaskRemovedException("Task removed: " + jobKey);
      }
    }
    try {
      log.info("Task {} runNow", taskState.getConfiguration().getTaskLogName());

      // DONE jobs are removed, and here will fail
      scheduler.runNow(triggerSource, jobKey, this, taskState);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  public String getTriggerSource() {
    QuartzTaskFuture currentTaskFuture = taskFuture;
    return currentTaskFuture != null ? currentTaskFuture.getTriggerSource() : null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "jobKey=" + jobKey +
        ", state=" + state +
        ", taskState=" + taskState +
        ", taskFuture=" + taskFuture +
        ", removed=" + removed +
        '}';
  }

  @Override
  public Map<String, Object> getContext() {
    return context;
  }

  /**
   * Implementation of {@link CurrentState}.
   */
  private static class CurrentStateImpl
      implements CurrentState
  {
    private final TaskState state;

    private final Date nextRun;

    private final QuartzTaskFuture future;

    public CurrentStateImpl(final TaskState state, final Date nextRun, final QuartzTaskFuture taskFuture) {
      this.state = state;
      this.nextRun = nextRun;
      this.future = taskFuture;
    }

    @Override
    public TaskState getState() {
      return state;
    }

    @Override
    @Nullable
    public Date getNextRun() {
      return nextRun;
    }

    @Override
    @Nullable
    public Date getRunStarted() {
      return state.isRunning() ? future.getStartedAt() : null;
    }

    @Override
    @Nullable
    public TaskState getRunState() {
      return state.isRunning() ? future.getRunState() : null;
    }

    @Override
    @Nullable
    public QuartzTaskFuture getFuture() {
      return future;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "state=" + state +
          ", nextRun=" + nextRun +
          ", future=" + future +
          '}';
    }
  }
}
