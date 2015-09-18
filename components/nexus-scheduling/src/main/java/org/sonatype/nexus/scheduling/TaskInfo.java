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
package org.sonatype.nexus.scheduling;

import java.util.Date;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;

/**
 * The class holding information about task at the moment the instance of task info was created.
 * This is the "handle" of a scheduled task. The handle might become "stale"
 * if the task this instance is handle of is removed from scheduler (ie. by some other thread). In that case,
 * some of the methods will throw {@link TaskRemovedException} on invocation to signal that state. For task entering
 * {@link State#DONE}, this class will behave a bit differently: they will never throw {@link TaskRemovedException},
 * and upon they are done, the task info will cache task configuration, schedule, current and last run state forever.
 *
 * @since 3.0
 */
public interface TaskInfo
{
  /**
   * Returns a unique ID of the task instance. Shorthand method for {@link #getConfiguration()#getId()}
   */
  String getId();

  /**
   * Returns a name of the task instance. Shorthand method for {@link #getConfiguration()#getName()}
   */
  String getName();

  /**
   * Returns a message of the task instance. Shorthand method for {@link #getConfiguration()#getMessage()}
   */
  String getMessage();

  /**
   * Returns a COPY of the task configuration map from the moment this instance was created or any state change on task
   * happened. Modifications to this configuration are possible, but does not affect currently executing task, nor is
   * being persisted. Generally, this configuration is only for inspection, or, to be used to re-schedule existing task
   * with changed configuration.
   */
  TaskConfiguration getConfiguration();

  /**
   * Returns the task's schedule from the moment this instance was created or any state change on task happened.
   */
  Schedule getSchedule();

  // ==

  /**
   * Task instance might be waiting (to be run, either by schedule or manually), or might be running, or might be
   * done (will never run again, is "done"). The "done" state is ending state for task, it will according to it's
   * {@link Schedule} not execute anymore.
   * Scheduler will never give out "fresh" task info instances with state "done" as done task is also removed.
   * These states might be get into only by having a "single shot" task ended. Instances in
   * this "ending" state, while still holding valid configuration and schedule, might be used to reschedule a
   * NEW task instance, but the reference to this instance should be dropped and let for GC to collect it, and
   * continue with the newly returned task info.
   *
   * Transitions:
   * WAITING -> RUNNING
   * RUNNING -> WAITING
   * RUNNING -> DONE
   */
  enum State
  {
    WAITING, RUNNING, DONE
  }

  /**
   * Running task instance might be running okay, being blocked (by other tasks), or might be canceled but the
   * cancellation was not yet detected or some cleanup is being done.
   *
   * Possible transitions: currentRunState.ordinal <= newRunState.ordinal
   * Ending states are RUNNING and CANCELED.
   */
  enum RunState
  {
    STARTING, BLOCKED, RUNNING, CANCELED
  }

  interface CurrentState
  {
    /**
     * Returns the state of task, never {@code null}.
     */
    State getState();

    /**
     * Returns the date of next run, if applicable, or {@code null}.
     */
    @Nullable
    Date getNextRun();

    /**
     * If task is running, returns it's run state, otherwise {@code null}.
     */
    @Nullable
    Date getRunStarted();

    /**
     * If task is running, returns it's run state, otherwise {@code null}.
     */
    @Nullable
    RunState getRunState();

    /**
     * If task is in states {@link State#RUNNING} or {@link State#DONE}, returns it's future, otherwise {@code null}.
     * In case of {@link State#DONE} the future is done too.
     */
    @Nullable
    Future<?> getFuture();
  }

  enum EndState
  {
    OK, FAILED, CANCELED
  }

  interface LastRunState
  {
    /**
     * Returns the last end state.
     */
    EndState getEndState();

    /**
     * Returns the date of last run start.
     */
    Date getRunStarted();

    /**
     * Returns the last run duration.
     */
    long getRunDuration();
  }

  // ==

  /**
   * Returns the task current state, never {@code null}. For tasks scheduled with {@link Now} schedule, or having
   * manually started with {@link #runNow()} method, the invocation of this method will block until underlying
   * scheduler actually starts the task, hence, caller might get the task result.
   */
  CurrentState getCurrentState();

  /**
   * Returns the task last run state, if there was any, otherwise {@code null}.
   */
  @Nullable
  LastRunState getLastRunState();

  // ==

  /**
   * Removes (with canceling if runs) the task. Returns {@code true} if it's guaranteed that the task is removed from
   * scheduler, and no future executions of this task will happen. Still, task might be executing in this very moment,
   * until detects cancellation. Returns {@code false} if task executes and is not cancelable.
   */
  boolean remove();

  /**
   * Executes the scheduled task now, unrelated to it's actual schedule. This also implies that this method will NOT
   * change the state of tasks "original" schedule! For example: hourly schedule, has 30 minutes to run, and
   * task is getting run now. When the task is done, there will be still 30 minutes for it's original schedule
   * to run (minus the task execution time).
   *
   * @throws TaskRemovedException  if task with this ID has been removed from scheduler.
   * @throws IllegalStateException if task is already running.
   */
  TaskInfo runNow() throws TaskRemovedException;
}
