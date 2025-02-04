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
package org.sonatype.nexus.scheduling.internal.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.api.TaskXO;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.scheduling.TaskState.OK;
import static org.sonatype.nexus.scheduling.TaskState.RUNNING;
import static org.sonatype.nexus.scheduling.TaskState.WAITING;

public class TasksApiResourceTest
    extends TestSupport
{
  private final TaskConfiguration visibleConfig = configuration(true);

  private final TaskConfiguration invisibleConfig = configuration(false);

  private TestTaskInfo[] testTasks = new TestTaskInfo[]{
      new TestTaskInfo("task0", "Invisible task", "aType", new TestCurrentState(WAITING), invisibleConfig),
      new TestTaskInfo("task1", "Task 1", "anotherType", new TestCurrentState(WAITING), visibleConfig),
      new TestTaskInfo("task2", "Task 2", "aType", new TestCurrentState(RUNNING, new Date(), new CompletableFuture<>()),
          visibleConfig),
      new TestTaskInfo("task3", "Task 3", "anotherType",
          new TestCurrentState(OK, new Date(), CompletableFuture.completedFuture(null)), visibleConfig)
  };

  @Mock
  private TaskScheduler taskScheduler;

  TasksApiResource tasksResource;

  @Before
  public void setup() {
    tasksResource = new TasksApiResource(taskScheduler);
    when(taskScheduler.getTaskById(any())).thenAnswer(this::findTask);
  }

  @Test
  public void testResourceURI() {
    assertThat(TasksApiResource.RESOURCE_URI, is("/v1/tasks"));
  }

  /*
   * getTasks gets list of scheduled tasks
   */
  @Test
  public void testGetTasks() {
    when( taskScheduler.listsTasks()).thenReturn(Arrays.asList(testTasks));
    when(taskScheduler.toExternalTaskState(any())).thenReturn(new ExternalTaskState(testTasks[1]),
        new ExternalTaskState(testTasks[2]), new ExternalTaskState(testTasks[3]));

    Page<TaskXO> page = tasksResource.getTasks(null);

    assertThat(page.getItems(), hasSize(3));
    assertThat(extract(page.getItems(), TaskXO::getId), contains("task1", "task2", "task3"));
    assertThat(extract(page.getItems(), TaskXO::getName), contains("Task 1", "Task 2", "Task 3"));
    assertThat(extract(page.getItems(), TaskXO::getType), contains("anotherType", "aType", "anotherType"));
    assertThat(extract(page.getItems(), TaskXO::getCurrentState), contains(WAITING.toString(), RUNNING.toString(), OK.toString()));
  }

  /*
   * getTasks filters on task type
   */
  @Test
  public void testGetTasks_taskTypeFilter() {
    when(taskScheduler.listsTasks()).thenReturn(Arrays.asList(testTasks));
    when(taskScheduler.toExternalTaskState(any())).thenReturn(new ExternalTaskState(testTasks[1]),
        new ExternalTaskState(testTasks[3]));
    Page<TaskXO> page = tasksResource.getTasks("anotherType");

    assertThat(page.getItems(), hasSize(2));
    assertThat(extract(page.getItems(), TaskXO::getId), contains("task1", "task3"));
  }

  /*
   * getTaskById gets tasks by id
   */
  @Test
  public void testGetTaskById() {
    when(taskScheduler.toExternalTaskState(any())).thenReturn(new ExternalTaskState(testTasks[1]));

    TaskXO validTaskXO = tasksResource.getTaskById("task1");

    assertThat(validTaskXO.getId(), is("task1"));
    assertThat(validTaskXO.getName(), is("Task 1"));
    assertThat(validTaskXO.getType(), is("anotherType"));
    assertThat(validTaskXO.getCurrentState(), is(WAITING.toString()));
  }

  @Test
  public void testGetTaskById_invalid() {
    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> tasksResource.getTaskById("nosuchtask"));

    assertThat(Status.fromStatusCode(exception.getResponse().getStatus()), is(NOT_FOUND));
  }

  /*
   * run invokes runNow on tasks
   */
  @Test
  public void testRun() {
    tasksResource.run("task1");

    assertThat(findTask("task1").runs, contains("REST API"));
  }

  @Test
  public void testRun_invalidId() {
    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> tasksResource.run("nosuchtask"));

    assertThat(Status.fromStatusCode(exception.getResponse().getStatus()), is(NOT_FOUND));
  }

  @Test
  public void testRun_error() {
    when(taskScheduler.getTaskById(any())).thenThrow(new RuntimeException("error"));

    WebApplicationException exception = assertThrows(WebApplicationException.class, () -> tasksResource.run("error"));

    assertThat(Status.fromStatusCode(exception.getResponse().getStatus()), is(INTERNAL_SERVER_ERROR));
  }

  /*
   * stop cancels running tasks
   */
  @Test
  public void testStop() {
    tasksResource.stop("task2");

    assertThat(findTask("task2").currentState.getFuture().isCancelled(), is(true));
  }

  /*
   * stop called with valid id for a non-running task
   */
  @Test
  public void testStop_notRunning() {
    WebApplicationException exception = assertThrows(WebApplicationException.class, () -> tasksResource.stop("task1"));

    assertThat(Status.fromStatusCode(exception.getResponse().getStatus()), is(CONFLICT));

  }

  @Test
  public void testStop_stopOnCompleted() {
    WebApplicationException exception = assertThrows(WebApplicationException.class, () -> tasksResource.stop("task3"));

    assertThat(Status.fromStatusCode(exception.getResponse().getStatus()), is(CONFLICT));
  }

  @Test
  public void testStop_invalidId() {
    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> tasksResource.stop("nosuchtask"));

    assertThat(Status.fromStatusCode(exception.getResponse().getStatus()), is(NOT_FOUND));
  }

  @Test
  public void testStop_error() {
    when(taskScheduler.getTaskById(any())).thenThrow(new RuntimeException("error"));
    WebApplicationException exception = assertThrows(WebApplicationException.class, () -> tasksResource.stop("error"));

    assertThat(Status.fromStatusCode(exception.getResponse().getStatus()), is(INTERNAL_SERVER_ERROR));
  }

  private TestTaskInfo findTask(final InvocationOnMock invocation) {
    return findTask(invocation.getArguments()[0]);
  }

  private TestTaskInfo findTask(final Object id) {
    return Arrays.asList(testTasks)
        .stream()
        .filter(candidate -> candidate.getId().equals(id))
        .findFirst()
        .orElse(null);
  }

  private static TaskConfiguration configuration(final boolean visible) {
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setVisible(visible);
    return configuration;
  }

  private static <A, R> Collection<R> extract(final Collection<A> collection, final Function<A, R> fn) {
    return collection.stream().map(fn).toList();
  }

  private static class TestTaskInfo
      implements TaskInfo
  {
    String id;

    String name;

    String typeId;

    String message;

    String triggerSource;

    Object lastResult;

    CurrentState currentState;

    TaskConfiguration configuration;

    Schedule schedule;

    List<String> runs = new ArrayList<>();

    public TestTaskInfo(
        final String id,
        final String name,
        final String typeId,
        final CurrentState currentState,
        final TaskConfiguration configuration)
    {
      this.id = id;
      this.name = name;
      this.typeId = typeId;
      this.currentState = currentState;
      this.configuration = configuration;
    }

    @Override
    public boolean remove() {
      return false;
    }

    @Override
    public TaskInfo runNow(final String s) {
      runs.add(s);
      return this;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getTypeId() {
      return typeId;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public String getTriggerSource() {
      return triggerSource;
    }

    @Override
    public Object getLastResult() {
      return lastResult;
    }

    @Override
    public CurrentState getCurrentState() {
      return currentState;
    }

    @Override
    public TaskConfiguration getConfiguration() {
      return configuration;
    }

    @Override
    public Schedule getSchedule() {
      return schedule;
    }

    public List<String> getRuns() {
      return runs;
    }
  }

  private static class TestCurrentState
      implements CurrentState
  {
    TaskState state;

    Date nextRun;

    Date runStarted;

    TaskState runState;

    Future<?> future;

    public TestCurrentState(final TaskState state, final Date runStarted, final Future<?> future) {
      this.state = state;
      this.runStarted = runStarted;
      this.future = future;
    }

    public TestCurrentState(final TaskState state) {
      this.state = state;
    }

    @Override
    public TaskState getState() {
      return state;
    }

    @Override
    public Date getNextRun() {
      return nextRun;
    }

    @Override
    public Date getRunStarted() {
      return runStarted;
    }

    @Override
    public TaskState getRunState() {
      return runState;
    }

    @Override
    public Future<?> getFuture() {
      return future;
    }
  }
}
