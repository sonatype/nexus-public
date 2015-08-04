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
package org.sonatype.nexus.plugins.tasks.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.TaskState;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.scheduling.schedules.RunNowSchedule;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class TasksWaitForPlexusResource
    extends AbstractPlexusResource
{

  private static final String RESOURCE_URI = "/tasks/waitFor";

  private final NexusScheduler nexusScheduler;

  @Inject
  public TasksWaitForPlexusResource(final NexusScheduler nexusScheduler) {
    this.nexusScheduler = checkNotNull(nexusScheduler);
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "anon");
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    final Form form = request.getResourceRef().getQueryAsForm();
    final String name = form.getFirstValue("name");
    final String taskType = form.getFirstValue("taskType");
    final long window = Long.parseLong(form.getFirstValue("window", "10000"));
    final long timeout = Long.parseLong(form.getFirstValue("timeout", "60000"));

    final ScheduledTask<?> namedTask = getTaskByName(nexusScheduler, name);

    if (name != null && namedTask == null) {
      // task wasn't found, so bounce on outta here
      response.setStatus(Status.SUCCESS_OK);
      return "OK";
    }

    long lastTimeTasksWereStillRunning = System.currentTimeMillis();
    final long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime <= timeout) {
      sleep();

      if (isTaskCompleted(nexusScheduler, taskType, namedTask)) {
        if (System.currentTimeMillis() - lastTimeTasksWereStillRunning >= window) {
          response.setStatus(Status.SUCCESS_OK);
          return "OK";
        }
      }
      else {
        lastTimeTasksWereStillRunning = System.currentTimeMillis();
      }
    }

    response.setStatus(Status.SUCCESS_ACCEPTED);
    return "Tasks Not Finished";
  }

  static boolean isTaskCompleted(final NexusScheduler nexusScheduler,
                                 final String taskType,
                                 final ScheduledTask<?> namedTask)
  {
    if (namedTask != null) {
      return isTaskCompleted(namedTask);
    }
    else {
      for (final ScheduledTask<?> task : getTasks(nexusScheduler, taskType)) {
        if (!isTaskCompleted(task)) {
          return false;
        }
      }
      return true;
    }
  }

  static ScheduledTask<?> getTaskByName(final NexusScheduler nexusScheduler, final String name) {
    if (name == null) {
      return null;
    }

    final Map<String, List<ScheduledTask<?>>> taskMap = nexusScheduler.getAllTasks();

    for (List<ScheduledTask<?>> taskList : taskMap.values()) {
      for (ScheduledTask<?> task : taskList) {
        if (task.getName().equals(name)) {
          return task;
        }
      }
    }

    return null;
  }

  static void sleep() {
    try {
      Thread.sleep(500);
    }
    catch (final InterruptedException e) {
      // ignore
    }
  }

  private static boolean isTaskCompleted(ScheduledTask<?> task) {
    if (task.getSchedule() instanceof RunNowSchedule) {
      // runNow scheduled tasks will _dissapear_ when done. So, the fact they are PRESENT simply
      // means they are not YET complete
      return false;
    }
    else {
      final TaskState state = task.getTaskState();

      if (task.getSchedule() instanceof ManualRunSchedule) {
        // MnuallRunSchedule stuff goes back to SUBMITTED state and sit there for next "kick"
        // but we _know_ it ran once at least if lastRun date != null AND is in some of the following
        // states
        // Note: I _think_ ManualRunScheduled task never go into WAITING state! (unverified claim)
        return task.getLastRun() != null
            && (TaskState.SUBMITTED.equals(state) || TaskState.WAITING.equals(state)
            || TaskState.FINISHED.equals(state) || TaskState.BROKEN.equals(state)
            || TaskState.CANCELLED.equals(state));
      }
      else {
        // the rest of tasks are completed if in any of these statuses
        return TaskState.WAITING.equals(state) || TaskState.FINISHED.equals(state)
            || TaskState.BROKEN.equals(state) || TaskState.CANCELLED.equals(state);
      }
    }
  }

  private static Set<ScheduledTask<?>> getTasks(final NexusScheduler nexusScheduler, final String taskType) {
    Set<ScheduledTask<?>> tasks = new HashSet<ScheduledTask<?>>();

    Map<String, List<ScheduledTask<?>>> taskMap = nexusScheduler.getAllTasks();

    for (List<ScheduledTask<?>> taskList : taskMap.values()) {
      for (ScheduledTask<?> task : taskList) {
        if (taskType == null || task.getType().equals(taskType)) {
          tasks.add(task);
        }
      }
    }

    return tasks;
  }

}
