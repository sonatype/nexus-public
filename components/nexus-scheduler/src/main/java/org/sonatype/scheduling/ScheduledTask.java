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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.sonatype.scheduling.iterators.SchedulerIterator;
import org.sonatype.scheduling.schedules.Schedule;

public interface ScheduledTask<T>
{
  /**
   * Returns the task if it is an instance of SchedulerTask<?> or null if that's not the case (there is just a
   * Callable<?>).
   */
  SchedulerTask<T> getSchedulerTask();

  /**
   * Returns the progress listener of this run, if the task runs, otherwise null.
   */
  ProgressListener getProgressListener();

  /**
   * Returns the task (callable being run).
   */
  Callable<T> getTask();

  /**
   * Returns a unique ID of the task.
   */
  String getId();

  /**
   * Returns a name of the task.
   */
  String getName();

  /**
   * Sets the name of the ScheduledTask.
   */
  void setName(String name);

  /**
   * Returns the "type" of the task.
   */
  String getType();

  /**
   * Returns the task state.
   */
  TaskState getTaskState();

  /**
   * Returns the date when the task is scheduled.
   */
  Date getScheduledAt();

  /**
   * Runs the task right now, putting schedule on hold until complete
   */
  void runNow();

  /**
   * Cancels the task and does not removes it from queue (if it has schedule in future).
   */
  void cancelOnly();

  /**
   * Cancels the task and removes it from queue.
   */
  void cancel();

  /**
   * Cancels the task and removes it from queue (as {@link #cancel()} does), but if the passed in flag is {@code
   * true}
   * it will interrupt the thread too.
   */
  void cancel(boolean interrupt);

  /**
   * Resets the task state and reschedules if needed.
   */
  void reset();

  /**
   * Returns an exception is TaskState is BROKEN, null in any other case.
   *
   * @return null, if task in not in BROKEN status, otherwise the exception that broke it.
   */
  Throwable getBrokenCause();

  /**
   * Gets the result of Callable, or null if it is "converted" from Runnable. It behaves just like Future.get(), if
   * the task is not finished, it will block.
   */
  T get()
      throws ExecutionException, InterruptedException;

  /**
   * Gets the result of Callable, or null if it is "converted" from Runnable.
   */
  T getIfDone();

  /**
   * Returns the last run date of task, if any. Null otherwise.
   */
  Date getLastRun();

  /**
   * Returns the last run date of task, if any. Null otherwise.
   */
  TaskState getLastStatus();

  /**
   * How much time last execution took in miliseconds
   */
  Long getDuration();

  /**
   * Returns the next run date of task.
   */
  Date getNextRun();

  /**
   * Is the task enabled? If the task is enabled, it is executing when it needs to execute. If the task is disabled,
   * it will still "consume" it's schedules, but will do nothing (NOP).
   */
  boolean isEnabled();

  /**
   * Sets enabled.
   */
  void setEnabled(boolean enabled);

  /**
   * Returns the list of accumulated results.
   */
  List<T> getResults();

  /**
   * Returns the iterator that is being used to repeat the task
   */
  SchedulerIterator getScheduleIterator();

  /**
   * Returns the Schedule that is being used
   */
  Schedule getSchedule();

  /**
   * Sets the Schedule that is being used
   */
  void setSchedule(Schedule schedule);

  Map<String, String> getTaskParams();
}
