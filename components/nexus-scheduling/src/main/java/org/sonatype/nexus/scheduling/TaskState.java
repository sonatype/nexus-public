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

import org.sonatype.nexus.scheduling.schedule.Schedule;

/**
 * Task instance might be waiting (to be run, either by schedule or manually), or might be running, or might be
 * done (will never run again, is "done"). The "done" state is ending state for task, it will according to it's
 * {@link Schedule} not execute anymore.
 *
 * Scheduler will never give out "fresh" task info instances with state "done" as done task is also removed.
 * These states might be get into only by having a "single shot" task ended. Instances in
 * this "ending" state, while still holding valid configuration and schedule, might be used to reschedule a
 * NEW task instance, but the reference to this instance should be dropped and let for GC to collect it, and
 * continue with the newly returned task info.
 *
 * Transitions:
 * {@link #WAITING} -> {@link #RUNNING}
 * {@link #RUNNING} -> {@link #WAITING}
 * {@link #RUNNING} -> {@link #OK}
 *
 * Running task instance might be running okay, being blocked (by other tasks), or might be canceled but the
 * cancellation was not yet detected or some cleanup is being done.
 *
 * Possible transitions: currentRunState.ordinal <= newRunState.ordinal
 * Ending states are {@link #RUNNING} and {@link #RUNNING_CANCELED}.
 */
public enum TaskState {
  WAITING(Group.WAITING, "Waiting"),

  RUNNING_STARTING(Group.RUNNING, "Starting"),
  RUNNING_BLOCKED(Group.RUNNING, "Blocked"),
  RUNNING(Group.RUNNING, "Running"),
  RUNNING_CANCELED(Group.RUNNING, "Canceled"),

  OK(Group.DONE, "Done"),
  FAILED(Group.DONE, "Done - Failed"),
  CANCELED(Group.DONE, "Done - Canceled"),
  INTERRUPTED(Group.DONE, "Done - Interrupted");

  private final Group group;

  private final String description;

  TaskState(final Group group, final String description) {
    this.group = group;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public boolean isIn(final Group group) {
    return this.group == group;
  }

  public boolean isWaiting() {
    return isIn(Group.WAITING);
  }

  public boolean isRunning() {
    return isIn(Group.RUNNING);
  }

  public boolean isDone() {
    return isIn(Group.DONE);
  }

  public enum Group {
    WAITING, RUNNING, DONE
  }
}
