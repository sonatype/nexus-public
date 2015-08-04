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
package org.sonatype.scheduling;


/**
 * Enum for describing task state. It is a state machine: starting state is SUBMITTED, finishing states are FINISHED
 * and
 * CANCELLED. Scheduled tasks are jumping between RUNNING and WAITING until finished, cancelled or error (broken).
 *
 * @author cstamas
 */
public enum TaskState
{
  /**
   * Submitted, not runned yet.
   */
  SUBMITTED, // -> RUNNING, CANCELLING

  /**
   * Is currently running.
   */
  RUNNING, // -> WAITING, FINISHED, BROKEN, CANCELLING, SLEEPING

  /**
   * Was cancelled but is currently running.
   */
  CANCELLING, // -> WAITING, BROKEN, CANCELLED

  /**
   * Should run but is blocked by another clashing task. Will try to run later.
   */
  SLEEPING, // -> RUNNING, CANCELLING

  /**
   * Was running and is finished. Waiting for next execution.
   */
  WAITING, // -> RUNNING, CANCELLING

  /**
   * Was running and is finished. No more execution scheduled.
   */
  FINISHED, // END

  /**
   * Was running and is broken.
   */
  BROKEN, // END

  /**
   * Was running and is cancelled.
   */
  CANCELLED,; // END

  public boolean isRunnable() {
    return this.equals(SUBMITTED) || this.equals(RUNNING) || this.equals(SLEEPING) || this.equals(WAITING) ||
        this.equals(BROKEN);
  }

  public boolean isActiveOrSubmitted() {
    return this.equals(SUBMITTED) || this.equals(RUNNING) || this.equals(SLEEPING) || this.equals(WAITING)
        || this.equals(CANCELLING);
  }

  public boolean isActive() {
    return this.equals(RUNNING) || this.equals(SLEEPING) || this.equals(WAITING) || this.equals(CANCELLING);
  }

  public boolean isExecuting() {
    return this.equals(RUNNING) || this.equals(CANCELLING);
  }

  public boolean isEndingState() {
        /* I don't think BROKEN should apply, broken simply means an exception was thrown.
         * So what?  let the user attempt to do it again, maybe an fs perm problem that they resolved */
    return this.equals(FINISHED) || this.equals(CANCELLED);
  }

}
