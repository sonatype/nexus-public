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

import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Schedule

import spock.lang.Specification

import static TaskInfo.State.*

public class TasksResourceTest
    extends Specification {
  def tasksResource

  def taskScheduler = Mock(TaskScheduler)

  def testTasks = [
    new TestTaskInfo(id: 'task1', name: 'Task 1', currentState: new TestCurrentState(state: WAITING)),
    new TestTaskInfo(id: 'task2', name: 'Task 2', currentState: new TestCurrentState(state: RUNNING,
        runStarted: new Date(), future: new CompletableFuture())),
    new TestTaskInfo(id: 'task3', name: 'Task 3', currentState: new TestCurrentState(state: DONE,
        runStarted: new Date(), future: CompletableFuture.completedFuture(null)))
  ]

  def setup() {
    tasksResource = new TasksResource(taskScheduler)
  }

  def 'resource path is the expected value'() {
    expect:
      TasksResource.RESOURCE_URI == '/rest/beta/tasks'
  }

  def 'getTasks gets list of scheduled tasks'() {
    when:
      def page = tasksResource.getTasks()

    then:
      1 * taskScheduler.listsTasks() >> testTasks
      page.items.size() == 3
      page.items*.name == ['Task 1', 'Task 2', 'Task 3']
  }

  def 'getTaskById gets tasks by id'() {
    when: 'getTaskById called with valid id'
      def validTaskXO = tasksResource.getTaskById('task1')

    then: 'expected task is returned'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      validTaskXO.id == 'task1'

    when: 'getTaskById called with invalid id'
      def invalidTaskXO = tasksResource.getTaskById('nosuchtask')

    then: 'a 404 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      invalidTaskXO == null
      WebApplicationException exception = thrown()
      exception.response.status == 404
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
      exception1.response.status == 404

    when: 'an error occurs'
      tasksResource.run('error')

    then: 'a 500 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> throw new RuntimeException("error") }
      WebApplicationException exception2 = thrown()
      exception2.response.status == 500
  }

  def 'stop invokes cancels running tasks'() {
    when: 'stop called with valid id for a running task'
      tasksResource.stop('task2')
      
    then: 'task found and cancelled'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      testTasks.find { it.id == 'task2' }.currentState.future.isCancelled()

    when: 'stop called with valid id for a non-running task'
      tasksResource.stop('task1')
      
    then: 'task found and invoked once with runNow()'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      WebApplicationException exception1 = thrown()
      exception1.response.status == 404

    when: 'stop called with completed task'
      tasksResource.stop('task3')
      
    then: 'a 409 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      WebApplicationException exception2 = thrown()
      exception2.response.status == 409

    when: 'stop called with invalid id'
      tasksResource.stop('nosuchtask')
      
    then: 'a 404 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> testTasks.find { it.id == id } }
      WebApplicationException exception3 = thrown()
      exception3.response.status == 404

    when: 'an error occurs'
      tasksResource.stop('error')
      
    then: 'a 500 response is generated'
      1 * taskScheduler.getTaskById(_) >> { String id -> throw new RuntimeException("error") }
      WebApplicationException exception4 = thrown()
      exception4.response.status == 500
  }

  class TestTaskInfo implements TaskInfo {
    String id
    String name
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
