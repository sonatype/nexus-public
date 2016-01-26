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

import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo.EndState;
import org.sonatype.nexus.scheduling.TaskInfo.LastRunState;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import org.quartz.JobDataMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * State of Quartz executed {@link Task}.
 *
 * @since 3.0
 */
public class QuartzTaskState
{
  private static final String LAST_RUN_STATE_END_STATE = "lastRunState.endState";

  private static final String LAST_RUN_STATE_RUN_STARTED = "lastRunState.runStarted";

  private static final String LAST_RUN_STATE_RUN_DURATION = "lastRunState.runDuration";

  private final TaskConfiguration taskConfiguration;

  private final Schedule schedule;

  private final Date nextExecutionTime;

  public QuartzTaskState(final TaskConfiguration config,
                         final Schedule schedule,
                         @Nullable final Date nextExecutionTime)
  {
    this.taskConfiguration = checkNotNull(config);
    this.schedule = checkNotNull(schedule);
    this.nextExecutionTime = nextExecutionTime;
  }

  public TaskConfiguration getConfiguration() {
    return taskConfiguration;
  }

  public Schedule getSchedule() {
    return schedule;
  }

  public Date getNextExecutionTime() {
    return nextExecutionTime;
  }

  @Nullable
  public LastRunState getLastRunState() {
    return getLastRunState(taskConfiguration);
  }

  /**
   * Helper to set ending state on a map.
   *
   * The maps might be {@link JobDataMap} or {@link TaskConfiguration}.
   */
  public static void setLastRunState(final TaskConfiguration config,
                                     final EndState endState,
                                     final Date runStarted,
                                     final long runDuration)
  {
    checkNotNull(config);
    checkNotNull(endState);
    checkNotNull(runStarted);
    checkArgument(runDuration >= 0);

    config.setString(LAST_RUN_STATE_END_STATE, endState.name());
    config.setLong(LAST_RUN_STATE_RUN_STARTED, runStarted.getTime());
    config.setLong(LAST_RUN_STATE_RUN_DURATION, runDuration);
  }

  /**
   * Helper to get ending state from a map. Returns {@code null} if no ending state in task configuration.
   */
  @Nullable
  public static LastRunState getLastRunState(final TaskConfiguration config) {
    if (hasLastRunState(config)) {
      String endStateString = config.getString(LAST_RUN_STATE_END_STATE);
      long runStarted = config.getLong(LAST_RUN_STATE_RUN_STARTED, System.currentTimeMillis());
      long runDuration = config.getLong(LAST_RUN_STATE_RUN_DURATION, 0);
      return new LastRunStateImpl(EndState.valueOf(endStateString), new Date(runStarted), runDuration);
    }
    return null;
  }

  /**
   * Helper to check existence of ending state on a map.
   */
  public static boolean hasLastRunState(final TaskConfiguration config) {
    checkNotNull(config);
    return config.getString(LAST_RUN_STATE_END_STATE) != null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "taskConfiguration=" + taskConfiguration +
        ", schedule=" + schedule +
        ", nextExecutionTime=" + nextExecutionTime +
        '}';
  }

  /**
   * {@link LastRunState} implementation.
   */
  private static class LastRunStateImpl
      implements LastRunState
  {
    private final EndState endState;

    private final Date runStarted;

    private final long runDuration;

    public LastRunStateImpl(final EndState endState, final Date runStarted, final long runDuration) {
      this.endState = endState;
      this.runStarted = runStarted;
      this.runDuration = runDuration;
    }

    @Override
    public EndState getEndState() {
      return endState;
    }

    @Override
    public Date getRunStarted() {
      return runStarted;
    }

    @Override
    public long getRunDuration() {
      return runDuration;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "endState=" + endState +
          ", runStarted=" + runStarted +
          ", runDuration=" + runDuration +
          '}';
    }
  }
}