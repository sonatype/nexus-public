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
package org.sonatype.nexus.scheduling.events;

import java.util.Date;

import javax.annotation.Nullable;

import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo.LastRunState;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract super class for task related events.
 *
 * @since 2.0
 */
public abstract class TaskEvent
{
  private final Date eventDate;

  private final TaskInfo taskInfo;

  private final CurrentState currentState;

  private final LastRunState lastRunState;

  public TaskEvent(final TaskInfo taskInfo) {
    this.eventDate = new Date();
    this.taskInfo = checkNotNull(taskInfo);
    this.currentState = taskInfo.getCurrentState();
    this.lastRunState = taskInfo.getLastRunState();
  }

  /**
   * Returns the timestamp of event creation.
   */
  public Date getEventDate() {
    return eventDate;
  }

  /**
   * Returns the "handle" of the task. Please note that this is "live" object, so states returned by this
   * instance may change as task progresses or even finishes it's work while the event handler gets this event.
   */
  public TaskInfo getTaskInfo() {
    return taskInfo;
  }

  /**
   * Gets the "current state" of the task in the moment this event was fired, never returns {@code null}.
   */
  public CurrentState getCurrentState() {
    return currentState;
  }

  /**
   * Gets the "last run state" in the moment this event was fired, may return {@code null}.
   */
  @Nullable
  public LastRunState getLastRunState() {
    return lastRunState;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "nexusTask=" + getTaskInfo().getConfiguration() +
        ", currentState=" + currentState +
        ", lastRunState=" + lastRunState +
        '}';
  }
}
