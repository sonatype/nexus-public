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
package org.sonatype.nexus.scheduling.internal.resources

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response.Status

import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Schedule

import spock.lang.Specification

import static TaskInfo.State.*
import static javax.ws.rs.core.Response.Status.CONFLICT
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR
import static javax.ws.rs.core.Response.Status.NOT_FOUND

public class TasksResourceTest
    extends Specification {
  def tasksResource

  def taskScheduler = Mock(TaskScheduler)

  def visibleConfig = new TaskConfiguration(visible: true)
  def invisibleConfig = new TaskConfiguration(visible: false)

  def testTasks = [
      new TestTaskInfo(id: 'task0', name: 'Invisible task', typeId: 'aType',
          currentState: new TestCurrentState(state: WAITING), configuration: invisibleConfig),
      new TestTaskInfo(id: 'task1', name: 'Task 1', typeId: 'anotherType',
          currentState: new TestCurrentState(state: WAITING), configuration: visibleConfig),
      new TestTaskInfo(id: 'task2', name: 'Task 2', typeId: 'aType', currentState: new TestCurrentState(state: RUNNING,
          runStarted: new Date(), future: new CompletableFuture()), configuration: visibleConfig),
      new TestTaskInfo(id: 'task3', name: 'Task 3', typeId: 'anotherType',
          currentState: new TestCurrentState(state: DONE, runStarted: new Date(),
              future: CompletableFuture.completedFuture(null)), configuration: visibleConfig)
  ]

  def setup() {
    tasksResource = new TasksResource(taskScheduler)
  }

  def 'resource path is the expected value'() {
    expect:
      TasksResource.RESOURCE_URI == '/v1/tasks'
  }

  def 'getTasks gets list of scheduled tasks'() {
    when:
      def page = tasksResource.getTasks()

    then:
      1 * taskScheduler.listsTasks() >> testTasks
      page.items.size() == 3
      page.items*.id == ['task1', 'task2', 'task3']
      page.items*.name == ['Task 1', 'Task 2', 'Task 3']
      page.items*.type == ['anotherType', 'aType', 'anotherType']
      page.items*.currentState == [WAITING.toString(), RUNNING.toString(), DONE.toString()]
  }

  def 'getTasks filters on task type'() {
    when:
      def page = tasksResource.getTasks('anotherType')

    then:
      1 * taskScheduler.listsTasks() >> testTasks
      page.items.size() == 2
      page.items*.id == ['task1', 'task3']
  }

  def 'getTaskById gets tasks by id'() {
    when: 'getTaskById called with valid id'
      def validTaskXO = tasksResource.getTaskById('task1')

    then: 'expected task is returned'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      validTaskXO.id == 'task1'
      validTaskXO.name == 'Task 1'
      validTaskXO.type == 'anotherType'
      validTaskXO.currentState == WAITING.toString()

    when: 'getTaskById called with invalid id'
      def invalidTaskXO = tasksResource.getTaskById('nosuchtask')

    then: 'a 404 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      invalidTaskXO == null
      WebApplicationException exception = thrown()
      Status.fromStatusCode(exception.response.status) == NOT_FOUND
  }

  def 'run invokes runNow on tasks'() {
    when: 'run called with valid id'
      tasksResource.run('task1')

    then: 'task found and invoked once with runNow()'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      testTasks.find { it.id == 'task1' }.runs == ['REST API']

    when: 'run called with invalid id'
      tasksResource.run('nosuchtask')

    then: 'a 404 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      WebApplicationException exception1 = thrown()
      Status.fromStatusCode(exception1.response.status) == NOT_FOUND

    when: 'an error occurs'
      tasksResource.run('error')

    then: 'a 500 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> throw new RuntimeException("error") }
      WebApplicationException exception2 = thrown()
      Status.fromStatusCode(exception2.response.status) == INTERNAL_SERVER_ERROR
  }

  def 'stop cancels running tasks'() {
    when: 'stop called with valid id for a running task'
      tasksResource.stop('task2')
      
    then: 'task found and cancelled'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      testTasks.find { it.id == 'task2' }.currentState.future.isCancelled()

    when: 'stop called with valid id for a non-running task'
      tasksResource.stop('task1')
      
    then: 'task found, a 409 is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      WebApplicationException exception1 = thrown()
      Status.fromStatusCode(exception1.response.status) == CONFLICT

    when: 'stop called with completed task'
      tasksResource.stop('task3')

    then: 'a 409 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      WebApplicationException exception2 = thrown()
      Status.fromStatusCode(exception2.response.status) == CONFLICT

    when: 'stop called with invalid id'
      tasksResource.stop('nosuchtask')
      
    then: 'a 404 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      WebApplicationException exception3 = thrown()
      Status.fromStatusCode(exception3.response.status) == NOT_FOUND

    when: 'an error occurs'
      tasksResource.stop('error')
      
    then: 'a 500 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> throw new RuntimeException("error") }
      WebApplicationException exception4 = thrown()
      Status.fromStatusCode(exception4.response.status) == INTERNAL_SERVER_ERROR
  }

  class TestTaskInfo implements TaskInfo {
    String id
    String name
    String typeId
    String message
    String triggerSource
    TaskInfo.CurrentState currentState
    TaskInfo.LastRunState lastRunState
    TaskConfiguration configuration
    Schedule schedule

    boolean remove() {}
    TaskInfo runNow(String s) {
      runs << s
      return this
    }

    def runs = []
  }

  class TestCurrentState implements TaskInfo.CurrentState {
    TaskInfo.State state
    Date nextRun
    Date runStarted
    TaskInfo.RunState runState
    Future future
  }
}
