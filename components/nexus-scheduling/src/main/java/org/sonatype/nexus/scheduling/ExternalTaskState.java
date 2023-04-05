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

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * A simplified representation of a task's state suitable for external consumption.
 *
 * @since 3.20
 */
public class ExternalTaskState {
  private final TaskState state;

  private final TaskState lastEndState;

  private final Date lastRunStarted;

  private final Long lastRunDuration;

  private final Date nextFireTime;

  public ExternalTaskState(
      final TaskState state,
      final Date nextFireTime,
      @Nullable final TaskState lastEndState,
      @Nullable final Date lastRunStarted,
      @Nullable final Long lastRunDuration)
  {
    this.state = checkNotNull(state);
    this.nextFireTime = nextFireTime;
    this.lastEndState = lastEndState;
    this.lastRunStarted = lastRunStarted; // NOSONAR
    this.lastRunDuration = lastRunDuration;
  }

  public ExternalTaskState(final TaskInfo taskInfo) {
    this(
        taskInfo.getCurrentState().getState(),
        taskInfo.getCurrentState().getNextRun(),
        ofNullable(taskInfo.getLastRunState()).map(LastRunState::getEndState).orElse(null),
        ofNullable(taskInfo.getLastRunState()).map(LastRunState::getRunStarted).orElse(null),
        ofNullable(taskInfo.getLastRunState()).map(LastRunState::getRunDuration).orElse(null)
    );
  }

  public Date getNextFireTime() {
    return nextFireTime;
  }

  public TaskState getState() {
    return state;
  }

  @Nullable
  public TaskState getLastEndState() {
    return lastEndState;
  }

  @Nullable
  public Date getLastRunStarted() {
    return lastRunStarted; // NOSONAR
  }

  @Nullable
  public Long getLastRunDuration() {
    return lastRunDuration;
  }
}
