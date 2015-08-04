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
package org.sonatype.nexus.test.utils;

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.scheduling.NexusTask;

import org.hamcrest.Matcher;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Util class to talk with nexus tasks
 */
public class TaskScheduleUtil
{

  private static final TasksNexusRestClient nexusTasksRestClient;

  static {
    nexusTasksRestClient = new TasksNexusRestClient(RequestFacade.getNexusRestClient());
  }

  public static Status create(ScheduledServiceBaseResource task, Matcher<Response>... matchers)
      throws IOException
  {
    return nexusTasksRestClient.create(task, matchers);
  }

  public static ScheduledServiceListResource getTask(String name)
      throws Exception
  {
    return nexusTasksRestClient.getTask(name);
  }

  /**
   * @return only tasks visible from nexus UI
   */
  public static List<ScheduledServiceListResource> getTasks()
      throws IOException
  {
    return nexusTasksRestClient.getTasks();
  }

  /**
   * @return all tasks, even internal ones
   */
  public static List<ScheduledServiceListResource> getAllTasks()
      throws IOException
  {
    return nexusTasksRestClient.getAllTasks();
  }

  public static String getStatus(String name)
      throws Exception
  {
    return nexusTasksRestClient.getStatus(name);
  }

  public static void deleteAllTasks()
      throws Exception
  {
    nexusTasksRestClient.deleteAllTasks();
  }

  /**
   * Holds execution until all tasks stop running
   */
  public static void waitForAllTasksToStop()
      throws Exception
  {
    nexusTasksRestClient.waitForAllTasksToStop();
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param taskType task type
   */
  public static void waitForAllTasksToStop(String taskType)
      throws Exception
  {
    nexusTasksRestClient.waitForAllTasksToStop(taskType);
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param maxAttempts how many times check for tasks being stopped
   */
  public static void waitForAllTasksToStop(int maxAttempts)
      throws Exception
  {
    nexusTasksRestClient.waitForAllTasksToStop(maxAttempts);
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param taskClass task type
   */
  public static void waitForAllTasksToStop(Class<? extends NexusTask<?>> taskClass)
      throws Exception
  {
    nexusTasksRestClient.waitForAllTasksToStop(taskClass);
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param taskType    task type
   * @param maxAttempts how many times check for tasks being stopped
   */
  public static void waitForAllTasksToStop(int maxAttempts, String taskType)
      throws Exception
  {
    nexusTasksRestClient.waitForAllTasksToStop(maxAttempts, taskType);
  }

  /**
   * Blocks while waiting for a task to finish.
   */
  public static void waitForTask(String name, int maxAttempts)
      throws Exception
  {
    nexusTasksRestClient.waitForTask(name, maxAttempts);
  }

  public static void waitForTask(String name, int maxAttempts, boolean failIfNotFinished)
      throws Exception
  {
    nexusTasksRestClient.waitForTask(name, maxAttempts, failIfNotFinished);
  }

  public static Status update(ScheduledServiceBaseResource task, Matcher<Response>... matchers)
      throws IOException
  {
    return nexusTasksRestClient.update(task, matchers);
  }

  public static Status deleteTask(String id)
      throws IOException
  {
    return nexusTasksRestClient.deleteTask(id);
  }

  public static Status run(String taskId)
      throws IOException
  {
    return nexusTasksRestClient.run(taskId);
  }

  public static Status cancel(String taskId)
      throws IOException
  {
    return nexusTasksRestClient.cancel(taskId);
  }

  public static void runTask(String typeId, ScheduledServicePropertyResource... properties)
      throws Exception
  {
    nexusTasksRestClient.runTask(typeId, properties);
  }

  public static void runTask(String taskName, String typeId, ScheduledServicePropertyResource... properties)
      throws Exception
  {
    nexusTasksRestClient.runTask(taskName, typeId, properties);
  }

  public static void runTask(String taskName, String typeId, int maxAttempts,
                             ScheduledServicePropertyResource... properties)
      throws Exception
  {
    nexusTasksRestClient.runTask(taskName, typeId, maxAttempts, properties);
  }

  public static void runTask(String taskName, String typeId, int maxAttempts, boolean failIfNotFinished,
                             ScheduledServicePropertyResource... properties)
      throws Exception
  {
    nexusTasksRestClient.runTask(taskName, typeId, maxAttempts, failIfNotFinished, properties);
  }

  public static ScheduledServicePropertyResource newProperty(String name, String value) {
    return TasksNexusRestClient.newProperty(name, value);
  }
}
