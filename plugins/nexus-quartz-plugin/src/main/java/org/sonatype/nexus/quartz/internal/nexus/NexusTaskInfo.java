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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.schedule.Manual;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import com.google.common.base.Throwables;
import org.quartz.JobKey;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * {@link TaskInfo} support class backed by Quartz.
 *
 * Note about {@link NexusTaskFuture}: when this class has future (is not null), the task is meant to be started
 * (either by schedule or by "runNow"). When this class has no future, that means that task is in WAITING or DONE
 * states.
 *
 * @since 3.0
 */
public class NexusTaskInfo
    extends ComponentSupport
    implements TaskInfo
{
  /**
   * Key used in job execution context to stick task info in.
   */
  static final String TASK_INFO_KEY = NexusTaskInfo.class.getName();

  private final QuartzTaskExecutorSPI quartzSupport;

  private final JobKey jobKey;

  private volatile State state;

  private volatile NexusTaskState nexusTaskState;

  private volatile NexusTaskFuture nexusTaskFuture;

  private volatile boolean removed;

  public NexusTaskInfo(final QuartzTaskExecutorSPI quartzSupport,
                       final JobKey jobKey,
                       final NexusTaskState nexusTaskState,
                       final @Nullable NexusTaskFuture nexusTaskFuture)
  {
    this.quartzSupport = checkNotNull(quartzSupport);
    this.jobKey = checkNotNull(jobKey);
    this.removed = false;
    setNexusTaskState(nexusTaskFuture != null ? State.RUNNING : State.WAITING, nexusTaskState, nexusTaskFuture);
  }

  public synchronized boolean isRemovedOrDone() {
    return removed || State.DONE == state;
  }

  public synchronized void setNexusTaskState(final State state,
                                             final NexusTaskState nexusTaskState,
                                             final @Nullable NexusTaskFuture nexusTaskFuture)
  {
    checkNotNull(state);
    checkNotNull(nexusTaskState);
    checkState(State.RUNNING != state || nexusTaskFuture != null, "Running task must have future");
    if (this.state == null) {
      log.info("Task {} : state={}", nexusTaskState.getConfiguration().getTaskLogName(), state);
    }
    else {
      if (this.state != state) {
        // we have a transition
        String newState = state.name();
        if (state != State.RUNNING && nexusTaskState.getLastRunState() != null) {
          // we ended running and have lastRunState available, enhance log with it
          newState = newState + " (" + nexusTaskState.getLastRunState().getEndState().name() + ")";
        }
        if (log.isDebugEnabled()) {
          log.info("Task {} : {} state change {} -> {}",
              jobKey,
              nexusTaskState.getConfiguration().getTaskLogName(),
              this.state, newState);
        }
        else {
          log.info("Task {} state change {} -> {}",
              nexusTaskState.getConfiguration().getTaskLogName(),
              this.state, newState);
        }
      }
      else {
        // this is usually config change of waiting task
        log.debug("Task {} : {} : state={} nextRun={}",
            jobKey.getName(),
            nexusTaskState.getConfiguration().getTaskLogName(),
            state,
            nexusTaskState.getNextExecutionTime());
      }
    }
    this.state = state;
    this.nexusTaskState = nexusTaskState;
    this.nexusTaskFuture = nexusTaskFuture;

    // DONE tasks should be removed, if not removed already by #remove() method
    if (!removed && state == State.DONE) {
      quartzSupport.removeTask(jobKey);
      removed = true;
      log.debug("Task {} : {} is done and removed", jobKey.getName(),
          nexusTaskState.getConfiguration().getTaskLogName());
    }
  }

  /**
   * Sets task state only if it's in given state, otherwise does nothing.
   */
  public synchronized void setNexusTaskStateIfInState(final State state,
                                                      final NexusTaskState nexusTaskState,
                                                      final @Nullable NexusTaskFuture nexusTaskFuture)
  {
    if (this.state == state) {
      setNexusTaskState(state, nexusTaskState, nexusTaskFuture);
    }
  }

  public JobKey getJobKey() {
    return jobKey;
  }

  @Nullable
  public NexusTaskFuture getNexusTaskFuture() {
    return nexusTaskFuture;
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
  public String getMessage() {
    return getConfiguration().getMessage();
  }

  @Override
  public synchronized TaskConfiguration getConfiguration() {
    return nexusTaskState.getConfiguration();
  }

  @Override
  public synchronized Schedule getSchedule() {
    return nexusTaskState.getSchedule();
  }

  @Override
  public synchronized CurrentState getCurrentState() {
    // TODO: why was this check here?
    // checkState(state == State.DONE || !removed, "Task already removed/updated");
    if (nexusTaskState.getSchedule() instanceof Manual) {
      return new CS(state, null, nexusTaskFuture);
    }
    else {
      return new CS(state, nexusTaskState.getNextExecutionTime(), nexusTaskFuture);
    }
  }

  @Nullable
  @Override
  public synchronized LastRunState getLastRunState() {
    return nexusTaskState.getLastRunState();
  }

  @Override
  public synchronized boolean remove() {
    if (isRemovedOrDone()) {
      // already removed
      log.debug("Task {} : {} already removed", jobKey.getName(), nexusTaskState.getConfiguration().getTaskLogName());
      return true;
    }
    if (nexusTaskFuture != null && !nexusTaskFuture.cancel(false)) {
      // running and not cancelable
      log.debug("Task {} : {} is running as is not cancelable", jobKey.getName(),
          nexusTaskState.getConfiguration().getTaskLogName());
      return false;
    }
    if (!NexusTaskState.hasLastRunState(nexusTaskState.getConfiguration())) {
      // if no last state (removed even before 1st run), add one noting it got removed/canceled
      // if was running and is cancelable, the task will itself set a proper ending state
      NexusTaskState.setLastRunState(nexusTaskState.getConfiguration(), EndState.CANCELED, new Date(), 0L);
    }
    removed = true;
    boolean result = quartzSupport.removeTask(jobKey);
    if (result) {
      log.info("Task {} removed", nexusTaskState.getConfiguration().getTaskLogName());
    }
    else {
      // TODO: WTF?
      log.info("Task {} not found in QZ", nexusTaskState.getConfiguration().getTaskLogName());
    }
    return result;
  }

  @Override
  public synchronized TaskInfo runNow() throws TaskRemovedException {
    checkState(State.RUNNING != state, "Task already running");
    if (isRemovedOrDone()) {
      throw new TaskRemovedException("Task removed: " + jobKey);
    }
    log.info("Task {} runNow", nexusTaskState.getConfiguration().getTaskLogName());
    setNexusTaskState(
        State.RUNNING,
        nexusTaskState,
        new NexusTaskFuture(
            quartzSupport,
            jobKey,
            getConfiguration().getTaskLogName(),
            new Date(),
            new Now()
        )
    );
    try {
      // DONE jobs are removed, and here will fail
      quartzSupport.runNow(jobKey);
    }
    catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, TaskRemovedException.class);
      throw Throwables.propagate(e);
    }
    return this;
  }

  // ==

  static class CS
      implements CurrentState
  {
    private final State state;

    private final Date nextRun;

    private final NexusTaskFuture future;

    public CS(final State state, final Date nextRun, final NexusTaskFuture nexusTaskFuture)
    {
      this.state = state;
      this.nextRun = nextRun;
      this.future = nexusTaskFuture;
    }

    @Override
    public State getState() {
      return state;
    }

    @Override
    public Date getNextRun() {
      return nextRun;
    }

    @Override
    public Date getRunStarted() {
      return state == State.RUNNING ? future.getStartedAt() : null;
    }

    @Override
    public RunState getRunState() {
      return state == State.RUNNING ? future.getRunState() : null;
    }

    @Override
    public NexusTaskFuture getFuture() {
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