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
package org.sonatype.nexus.scheduling.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.scheduling.NexusTask;
import org.sonatype.scheduling.NoSuchTaskException;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.Scheduler;
import org.sonatype.scheduling.schedules.Schedule;

/**
 * The Nexus scheduler.
 */
@Named
@Singleton
public class DefaultNexusScheduler
    implements NexusScheduler
{

  private final Scheduler scheduler;

  @Inject
  public DefaultNexusScheduler(final Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void initializeTasks() {
    scheduler.initializeTasks();
  }

  @Override
  public void shutdown() {
    scheduler.shutdown();
  }

  @Override
  public <T> ScheduledTask<T> submit(String name, NexusTask<T> nexusTask)
      throws RejectedExecutionException, NullPointerException
  {
    if (nexusTask.allowConcurrentSubmission(scheduler.getActiveTasks())) {
      return scheduler.submit(name, nexusTask);
    }
    else {
      throw new RejectedExecutionException("Task of this type is already submitted!");
    }
  }

  @Override
  public <T> ScheduledTask<T> schedule(String name, NexusTask<T> nexusTask, Schedule schedule)
      throws RejectedExecutionException, NullPointerException
  {
    if (nexusTask.allowConcurrentSubmission(scheduler.getActiveTasks())) {
      return scheduler.schedule(name, nexusTask, schedule);
    }
    else {
      throw new RejectedExecutionException("Task of this type is already scheduled!");
    }
  }

  @Override
  public <T> ScheduledTask<T> updateSchedule(ScheduledTask<T> task)
      throws RejectedExecutionException, NullPointerException
  {
    if (task != null) {
      scheduler.updateSchedule(task);
    }

    return task;
  }

  @Override
  public Map<String, List<ScheduledTask<?>>> getAllTasks() {
    return scheduler.getAllTasks();
  }

  @Override
  public Map<String, List<ScheduledTask<?>>> getActiveTasks() {
    return scheduler.getActiveTasks();
  }

  @Override
  public ScheduledTask<?> getTaskById(String id)
      throws NoSuchTaskException
  {
    return scheduler.getTaskById(id);
  }

  @Override
  @Deprecated
  @SuppressWarnings("unchecked")
  public NexusTask<?> createTaskInstance(String taskType)
      throws IllegalArgumentException
  {
    return (NexusTask) scheduler.createTaskInstance(taskType);
  }

  @Override
  public <T> T createTaskInstance(Class<T> taskType)
      throws IllegalArgumentException
  {
    return scheduler.createTaskInstance(taskType);
  }

}
