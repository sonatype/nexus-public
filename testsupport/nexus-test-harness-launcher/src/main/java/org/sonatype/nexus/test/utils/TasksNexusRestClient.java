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
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.integrationtests.NexusRestClient;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceResponse;
import org.sonatype.nexus.scheduling.NexusTask;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.hamcrest.Matcher;
import org.restlet.data.MediaType;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.test.utils.ResponseMatchers.isSuccessful;

/**
 * Util class to talk with nexus tasks
 */
public class TasksNexusRestClient
{

  private static final Logger LOG = LoggerFactory.getLogger(TasksNexusRestClient.class);

  private static XStream xstream;

  private final NexusRestClient nexusRestClient;

  static {
    xstream = XStreamFactory.getXmlXStream();
  }

  public TasksNexusRestClient(final NexusRestClient nexusRestClient) {
    this.nexusRestClient = checkNotNull(nexusRestClient);
  }

  public Status create(ScheduledServiceBaseResource task, Matcher<Response>... matchers)
      throws IOException
  {
    ScheduledServiceResourceResponse request = new ScheduledServiceResourceResponse();
    request.setData(task);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", MediaType.APPLICATION_XML);
    representation.setPayload(request);

    String serviceURI = "service/local/schedules";
    Matcher<Response> responseMatcher = allOf(matchers);
    return nexusRestClient.doPostForStatus(serviceURI, representation, responseMatcher);
  }

  public ScheduledServiceListResource getTask(String name)
      throws Exception
  {
    List<ScheduledServiceListResource> list = getTasks();
    for (ScheduledServiceListResource task : list) {
      if (name.equals(task.getName())) {
        return task;
      }
    }

    return null;
  }

  /**
   * @return only tasks visible from nexus UI
   */
  public List<ScheduledServiceListResource> getTasks()
      throws IOException
  {
    return getTaskRequest("service/local/schedules");
  }

  /**
   * @return all tasks, even internal ones
   */
  public List<ScheduledServiceListResource> getAllTasks()
      throws IOException
  {
    return getTaskRequest("service/local/schedules?allTasks=true");
  }

  private List<ScheduledServiceListResource> getTaskRequest(String uri)
      throws IOException
  {
    try {
      String entityText = nexusRestClient.doGetForText(uri, isSuccessful());
      XStreamRepresentation representation =
          new XStreamRepresentation(xstream, entityText, MediaType.APPLICATION_XML);

      ScheduledServiceListResourceResponse scheduleResponse =
          (ScheduledServiceListResourceResponse) representation.getPayload(
              new ScheduledServiceListResourceResponse());

      return scheduleResponse.getData();
    }
    catch (AssertionError e) {
      // unsuccessful GET
      LOG.error(e.getMessage(), e);
      return Collections.emptyList();
    }

  }

  public String getStatus(String name)
      throws Exception
  {
    ScheduledServiceListResource task = getTask(name);
    return task.getLastRunResult();
  }

  public void deleteAllTasks()
      throws Exception
  {
    List<ScheduledServiceListResource> tasks = getAllTasks();

    for (ScheduledServiceListResource task : tasks) {
      deleteTask(task.getId());
    }
  }

  /**
   * Holds execution until all tasks stop running
   */
  public void waitForAllTasksToStop()
      throws Exception
  {
    waitForAllTasksToStop(300);
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param taskType task type
   */
  public void waitForAllTasksToStop(String taskType)
      throws Exception
  {
    waitForAllTasksToStop(300, taskType);
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param maxAttempts how many times check for tasks being stopped
   */
  public void waitForAllTasksToStop(int maxAttempts)
      throws Exception
  {
    waitForAllTasksToStop(maxAttempts, null);
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param taskClass task type
   */
  public void waitForAllTasksToStop(Class<? extends NexusTask<?>> taskClass)
      throws Exception
  {
    waitForAllTasksToStop(taskClass.getSimpleName());
  }

  /**
   * Holds execution until all tasks of a given type stop running
   *
   * @param taskType    task type
   * @param maxAttempts how many times check for tasks being stopped
   */
  public void waitForAllTasksToStop(int maxAttempts, String taskType)
      throws Exception
  {
    String uri = "service/local/taskhelper?attempts=" + maxAttempts;
    if (taskType != null) {
      uri += "&taskType=" + taskType;
    }

    final Status status = nexusRestClient.doGetForStatus(uri);

    if (!status.isSuccess()) {
      throw new IOException("The taskhelper REST resource reported an error (" + status.toString()
          + "), bailing out!");
    }
  }

  /**
   * Blocks while waiting for a task to finish.
   */
  public void waitForTask(String name, int maxAttempts)
      throws Exception
  {
    waitForTask(name, maxAttempts, false);
  }

  public void waitForTask(String name, int maxAttempts, boolean failIfNotFinished)
      throws Exception
  {
    if (maxAttempts == 0) {
      return;
    }

    String uri = "service/local/taskhelper?attempts=" + maxAttempts;

    if (name != null) {
      uri += "&name=" + name;
    }

    final Status status = nexusRestClient.doGetForStatus(uri);

    if (failIfNotFinished) {
      if (Status.SUCCESS_NO_CONTENT.equals(status)) {
        throw new IOException("The taskhelper REST resource reported that task named '" + name
            + "' still running after '" + maxAttempts + "' cycles! This may indicate a performance issue.");
      }
    }
    else {
      if (!status.isSuccess()) {
        throw new IOException("The taskhelper REST resource reported an error (" + status.toString()
            + "), bailing out!");
      }
    }
  }

  public Status update(ScheduledServiceBaseResource task, Matcher<Response>... matchers)
      throws IOException
  {
    ScheduledServiceResourceResponse request = new ScheduledServiceResourceResponse();
    request.setData(task);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", MediaType.APPLICATION_XML);
    representation.setPayload(request);

    String serviceURI = "service/local/schedules/" + task.getId();
    Matcher<Response> matcher = allOf(matchers);
    return nexusRestClient.doPutForStatus(serviceURI, representation, matcher);
  }

  public Status deleteTask(String id)
      throws IOException
  {
    String serviceURI = "service/local/schedules/" + id;
    return nexusRestClient.doDeleteForStatus(serviceURI, null);
  }

  public Status run(String taskId)
      throws IOException
  {
    String serviceURI = "service/local/schedule_run/" + taskId;
    return nexusRestClient.doGetForStatus(serviceURI);
  }

  public Status cancel(String taskId)
      throws IOException
  {
    String serviceURI = "service/local/schedule_run/" + taskId;
    return nexusRestClient.doDeleteForStatus(serviceURI, null);
  }

  public void runTask(String typeId, ScheduledServicePropertyResource... properties)
      throws Exception
  {
    runTask(typeId, typeId, properties);
  }

  public void runTask(String taskName, String typeId, ScheduledServicePropertyResource... properties)
      throws Exception
  {
    runTask(taskName, typeId, 300, properties);
  }

  public void runTask(String taskName, String typeId, int maxAttempts,
                      ScheduledServicePropertyResource... properties)
      throws Exception
  {
    runTask(taskName, typeId, maxAttempts, false, properties);
  }

  public void runTask(String taskName, String typeId, int maxAttempts, boolean failIfNotFinished,
                      ScheduledServicePropertyResource... properties)
      throws Exception
  {
    ScheduledServiceBaseResource scheduledTask = new ScheduledServiceBaseResource();
    scheduledTask.setEnabled(true);
    scheduledTask.setId(null);
    scheduledTask.setName(taskName);
    scheduledTask.setTypeId(typeId);
    scheduledTask.setSchedule("manual");

    for (ScheduledServicePropertyResource property : properties) {
      scheduledTask.addProperty(property);
    }

    Status status = create(scheduledTask);
    assertThat("Unable to create task:" + scheduledTask.getTypeId(), status.isSuccess(), is(true));

    String taskId = getTask(scheduledTask.getName()).getId();
    status = run(taskId);
    assertThat("Unable to run task:" + scheduledTask.getTypeId(), status.isSuccess(), is(true));

    waitForTask(taskName, maxAttempts, failIfNotFinished);
  }

  public static ScheduledServicePropertyResource newProperty(String name, String value) {
    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey(name);
    prop.setValue(value);
    return prop;
  }
}
