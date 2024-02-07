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
package org.sonatype.nexus.scheduling.spi;

import java.util.Date;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.scheduling.LastRunState;
import org.sonatype.nexus.scheduling.TaskState;

import static com.google.common.base.Preconditions.checkNotNull;

public class TaskResultState
{
  private final String taskId;

  private final TaskState state;

  private final LastRunState lastRunState;

  private final Date nextFireTime;

  private final String progress;

  public TaskResultState(
      final String taskId,
      final TaskState state,
      @Nullable final Date nextFireTime,
      @Nullable final LastRunState lastRunState,
      @Nullable final String progress)
  {
    this.taskId = checkNotNull(taskId);
    this.state = checkNotNull(state);
    this.nextFireTime = nextFireTime;
    this.lastRunState = lastRunState;
    this.progress = progress;
  }

  public Date getNextFireTime() {
    return nextFireTime;
  }

  public Optional<LastRunState> getLastRunState() {
    return Optional.ofNullable(lastRunState);
  }

  public String getTaskId() {
    return taskId;
  }

  public TaskState getState() {
    return state;
  }

  @Nullable
  public TaskState getLastEndState() {
    return getLastRunState()
        .map(LastRunState::getEndState)
        .orElse(null);
  }

  @Nullable
  public Date getLastRunStarted() {
    return getLastRunState()
        .map(LastRunState::getRunStarted)
        .orElse(null);
  }

  @Nullable
  public Long getLastRunDuration() {
    return getLastRunState()
        .map(LastRunState::getRunDuration)
        .orElse(null);
  }

  @Nullable
  public String getProgress() {
    return progress;
  }
}
