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
package org.sonatype.nexus.quartz;

import java.util.Date;
import java.util.concurrent.Future;

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.LastRunState;
import org.sonatype.nexus.scheduling.TaskState;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for scheduled tasks and their TaskInfo.
 */
@Ignore("NEXUS-43375")
public class ScheduledTaskInfoLifecycleTest
    extends QuartzTestSupport
{

  @Before
  public void prepare() throws Exception {
    SleeperTask.reset();
  }

  /**
   * "one shot", aka "runNow", aka "bg jobs" tasks are getting into DONE state once run.
   */
  @Test
  public void taskLifecycleRunNow() throws Exception {

    // create the task
    final TaskConfiguration taskConfiguration = taskScheduler().createTaskConfigurationInstance(SleeperTaskDescriptor.TYPE_ID);
    final String RESULT = "This is the expected result";
    taskConfiguration.setString(SleeperTask.RESULT_KEY, RESULT);
    final TaskInfo taskInfo = taskScheduler().submit(taskConfiguration);

    // give it some time to start
    SleeperTask.youWait.await();

    assertThat(taskInfo, notNullValue());
    assertThat(taskInfo.getId(), equalTo(taskConfiguration.getId()));
    assertThat(taskInfo.getName(), equalTo(taskConfiguration.getName()));
    assertThat(taskInfo.getConfiguration().getTypeId(), equalTo(taskConfiguration.getTypeId()));
    assertThat(taskInfo.getConfiguration().getCreated(), notNullValue());
    assertThat(taskInfo.getConfiguration().getUpdated(), notNullValue());
    assertRunningTaskCount(1);

    final CurrentState currentState = taskInfo.getCurrentState();
    assertThat(currentState, notNullValue());
    assertThat(currentState.getState(), equalTo(TaskState.RUNNING));
    assertThat(currentState.getRunState(), equalTo(TaskState.RUNNING));
    assertThat(currentState.getRunStarted(), notNullValue());
    assertThat(currentState.getRunStarted().getTime(), lessThan(System.currentTimeMillis()));
    final Future<?> future = currentState.getFuture();
    assertThat(future, notNullValue());

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // and block for the result
    final String result = (String) future.get();
    assertThat(result, equalTo(RESULT));

    // done
    assertTaskState(taskInfo, TaskState.OK);
    assertRunningTaskCount(0);
  }

  /**
   * Repeatedly run tasks are bouncing between "running" and "waiting".
   */
  @Test
  public void taskLifecycleRunRepeatedly() throws Exception {
    // create the task
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);
    final TaskConfiguration taskConfiguration =    taskInfo.getConfiguration();

    // task message is available after task is done (as it comes from persisted dataMap)
    assertThat(taskInfo.getMessage(), nullValue());

    // give it some time to start
    SleeperTask.youWait.await();

    // task message is available after task is done (as it comes from persisted dataMap)
    assertThat(taskInfo.getMessage(), nullValue());

    assertThat(taskInfo, notNullValue());
    assertThat(taskInfo.getId(), equalTo(taskConfiguration.getId()));
    assertThat(taskInfo.getName(), equalTo(taskConfiguration.getName()));
    assertThat(taskInfo.getConfiguration().getTypeId(), equalTo(taskConfiguration.getTypeId()));
    assertThat(taskInfo.getConfiguration().getCreated(), notNullValue());
    assertThat(taskInfo.getConfiguration().getUpdated(), notNullValue());
    assertRunningTaskCount(1);

    Date runStarted;
    {
      final CurrentState currentState = taskInfo.getCurrentState();
      assertThat(currentState, notNullValue());
      assertThat(currentState.getState(), equalTo(TaskState.RUNNING));
      assertThat(currentState.getRunState(), equalTo(TaskState.RUNNING));
      runStarted = currentState.getRunStarted();
      assertThat(runStarted, notNullValue());
      // started in past
      assertThat(currentState.getRunStarted().getTime(), lessThan(System.currentTimeMillis()));
      // started after it was scheduled
      assertThat(currentState.getRunStarted().getTime(),
          greaterThan(taskInfo.getConfiguration().getCreated().getTime()));
      final Future<?> future = currentState.getFuture();
      assertThat(future, notNullValue());

      // make it be done
      SleeperTask.meWait.countDown();
      Thread.yield();

      // and block for the result
      final String result = (String) future.get();
      assertThat(result, equalTo(RESULT));
    }

    assertTaskState(taskInfo, TaskState.WAITING);
    assertRunningTaskCount(0);

    // repeating tasks when done are waiting, call for state is okay at any time
    {
      final TaskInfo ti = taskScheduler().getTaskById(taskInfo.getId());
      assertThat(ti, notNullValue());
      assertThat(ti.getId(), equalTo(taskConfiguration.getId()));
      assertThat(ti.getName(), equalTo(taskConfiguration.getName()));
      assertThat(ti.getConfiguration().getTypeId(), equalTo(taskConfiguration.getTypeId()));
      assertThat(ti.getConfiguration().getCreated(), notNullValue());
      assertThat(ti.getConfiguration().getUpdated(), notNullValue());

      final CurrentState currentState = ti.getCurrentState();
      assertThat(currentState, notNullValue());
      assertThat(currentState.getState(), equalTo(TaskState.WAITING));
      assertThat(currentState.getRunState(), nullValue());
      assertThat(currentState.getRunStarted(), nullValue());
      // task future is last future
       assertThat(currentState.getFuture(), nullValue());
    }

    // same thing from "old" handle
    {
      final CurrentState currentState = taskInfo.getCurrentState();
      assertThat(currentState, notNullValue());
      assertThat(currentState.getState(), equalTo(TaskState.WAITING));
      assertThat(currentState.getRunState(), nullValue());
      assertThat(currentState.getRunStarted(), nullValue());
      // task future is last future
       assertThat(currentState.getFuture(), nullValue());
    }

    // and post-execution it will have last rune state
    {
      final LastRunState lastRunState = taskInfo.getLastRunState();
      assertThat(lastRunState, notNullValue());
      assertThat(lastRunState.getEndState(), equalTo(TaskState.OK));
      assertThat(lastRunState.getRunStarted().getTime(), equalTo(runStarted.getTime()));
      assertThat(lastRunState.getRunDuration(), greaterThan(0L));
    }

    // task message is persisted, last run's message is here
    // TODO: the point in time when message becomes available is unclear. It depends WHEN Quartz serializes the done job map
    // and nothing signals it to caller when it's done. Make it part of CurrentState?
    assertThat(taskInfo.getMessage(), equalTo("Message is:" + RESULT));
  }
}
