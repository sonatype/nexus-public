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

public interface ProgressListener
{
  /**
   * Marks that the amount of work (work-units) are not known in advance.
   */
  int UNKNOWN_WORKUNITS = -1;

  /**
   * Starts a new (sub)task with {@link #UNKNOWN_WORKUNITS} to be done.
   */
  public void beginTask(String name);

  /**
   * Starts a new (sub)task with {@code toDo} work-units to be done.
   */
  public void beginTask(String name, int toDo);

  /**
   * Marks work is underway without a message. It is left to {@link ProgressListener} implementor what will happen
   * with this information (like update a progress bar for example). This is NOT a setter! Work unit count sent in
   * here are accumulated (summed up).
   */
  public void working(int workDone);

  /**
   * Marks work is underway with a message. It is left to {@link ProgressListener} implementor what will happen with
   * this message, will it be shown in log, in UI or whatever.
   */
  public void working(String message);

  /**
   * Marks work is underway and {@code workDone} work-units as done. This is NOT a setter! Work unit count sent in
   * here are accumulated (summed up). It is left to {@link ProgressListener} implementor what will happen with this
   * message, will it be shown in log, in UI or whatever.
   */
  public void working(String message, int workDone);

  /**
   * Ends a (sub)task with a message.
   */
  public void endTask(String message);

  /**
   * Returns true if the task-run to which this progress monitor belongs to should be canceled.
   */
  boolean isCanceled();

  /**
   * Cancels the task-run to which this progress monitor belongs to. This call will return immediately (will not
   * block
   * to wait actual task cancellation).
   */
  void cancel();
}
