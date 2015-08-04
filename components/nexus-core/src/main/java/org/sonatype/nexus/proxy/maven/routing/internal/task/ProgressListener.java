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
package org.sonatype.nexus.proxy.maven.routing.internal.task;

/**
 * Progress listener for tasks. Actual behavior is left to implementors. One possible behavior is to work like a
 * {@code java.util.Stack}, every {@link #beginTask(String, int)} pushes and {@link #endTask(String)} pops. How this is
 * represented, or what (log?) output it makes is left to implementor.
 *
 * @author cstamas
 * @since 2.4
 */
public interface ProgressListener
{
  /**
   * Starts a new (sub)task with {@code workAmount} work-units to be done.
   *
   * @param name       task name.
   * @param workAmount amount of work needed to finish this task.
   */
  public void beginTask(String name, int workAmount);

  /**
   * Marks work is in progress and {@code workAmountDelta} work-units as done since last invocation (or if first
   * invocation, since {@link #beginTask(String, int)}). This is NOT a setter! Work unit count sent in here are
   * accumulated (summed up).
   *
   * @param message the message for in-progress.
   */
  public void working(String message, int workAmountDelta);

  /**
   * Ends a (sub)task with a message.
   */
  public void endTask(String message);
}
