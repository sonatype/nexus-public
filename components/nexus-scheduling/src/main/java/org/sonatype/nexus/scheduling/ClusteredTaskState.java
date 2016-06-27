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

import org.sonatype.nexus.scheduling.TaskInfo.EndState;
import org.sonatype.nexus.scheduling.TaskInfo.RunState;
import org.sonatype.nexus.scheduling.TaskInfo.State;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes the basic state of a task on a node within a clustered environment.
 * 
 * @since 3.1
 */
public class ClusteredTaskState
{
  private final String nodeId;

  private final State state;

  private final RunState runState;

  private final EndState lastEndState;

  private final Date lastRunStarted;

  private final Long lastRunDuration;

  public ClusteredTaskState(String nodeId,
                            State state,
                            @Nullable RunState runState,
                            @Nullable EndState lastEndState,
                            @Nullable Date lastRunStarted,
                            @Nullable Long lastRunDuration)
  {
    this.nodeId = checkNotNull(nodeId);
    this.state = checkNotNull(state);
    this.runState = runState;
    this.lastEndState = lastEndState;
    this.lastRunStarted = lastRunStarted; // NOSONAR
    this.lastRunDuration = lastRunDuration;
  }

  public String getNodeId() {
    return nodeId;
  }

  public State getState() {
    return state;
  }

  @Nullable
  public RunState getRunState() {
    return runState;
  }

  @Nullable
  public EndState getLastEndState() {
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

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{nodeId=" + nodeId + ", state=" + state + ", runState=" + runState
        + ", lastEndState=" + lastEndState + ", lastRunStarted=" + lastRunStarted + ", lastRunDuration="
        + lastRunDuration + "}";
  }
}
