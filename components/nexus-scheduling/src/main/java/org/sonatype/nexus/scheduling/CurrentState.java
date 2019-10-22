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

/**
 * Encapsulates the current state of a task.
 * 
 * @since 3.19
 */
public interface CurrentState
{
  /**
   * Returns the state of task, never {@code null}.
   */
  TaskState getState();

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
  TaskState getRunState();

  /**
   * If task is in states {@link TaskState.Group#RUNNING} or {@link TaskState.Group#DONE}, returns it's future, otherwise {@code null}.
   * In case of {@link TaskState.Group#DONE} the future is done too.
   */
  @Nullable
  Future<?> getFuture();
}
