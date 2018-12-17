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

import java.util.concurrent.Future;

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo.RunState;
import org.sonatype.nexus.scheduling.TaskInfo.State;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for updating tasks.
 */
public class UpdatingTasksTest
    extends QuartzTestSupport
{
  @Before
  public void prepare() throws Exception {
    SleeperTask.reset();
  }

  /**
   * Updating a non cancelable task that is not running.
   */
  @Test
  public void updateNonRunningNonCancelableTask() throws Exception {
    TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID,taskScheduler().getScheduleFactory().manual());
    TaskConfiguration taskConfiguration = taskInfo.getConfiguration();

    assertThat(taskInfo, notNullValue());
    assertThat(taskInfo.getId(), equalTo(taskConfiguration.getId()));
    assertThat(taskInfo.getName(), equalTo(taskConfiguration.getName()));
    assertThat(taskInfo.getConfiguration().getTypeId(), equalTo(taskConfiguration.getTypeId()));
    assertThat(taskInfo.getConfiguration().getCreated(), notNullValue());
    assertThat(taskInfo.getConfiguration().getUpdated(), notNullValue());

    final CurrentState currentState = taskInfo.getCurrentState();
    assertThat(currentState, notNullValue());
    assertThat(currentState.getState(), equalTo(State.WAITING));
    assertThat(currentState.getRunState(), nullValue());
    assertThat(currentState.getRunStarted(), nullValue());
    assertThat(currentState.getFuture(), nullValue());

    // update it
    taskConfiguration.setString(SleeperTask.RESULT_KEY, "second");
    taskInfo = taskScheduler().scheduleTask(taskConfiguration, taskScheduler().getScheduleFactory().manual());

    assertThat(taskInfo.getCurrentState().getState(), equalTo(State.WAITING));
    assertThat(taskInfo.getConfiguration().getString(SleeperTask.RESULT_KEY), equalTo("second"));

    // see what scheduler has
    TaskInfo ti2 = taskScheduler().getTaskById(taskInfo.getId());
    assertThat(ti2.getCurrentState().getState(), equalTo(State.WAITING));
    assertThat(ti2.getConfiguration().getString(SleeperTask.RESULT_KEY), equalTo("second"));
  }

  /**
   * Updating a non cancelable task that is running.
   */
  @Test
  public void updateRunningNonCancelableTask() throws Exception {
    TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID,taskScheduler().getScheduleFactory().manual());
    TaskConfiguration taskConfiguration = taskInfo.getConfiguration();
    taskInfo.runNow();

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
    assertThat(currentState.getState(), equalTo(State.RUNNING));
    assertThat(currentState.getRunState(), equalTo(RunState.RUNNING));
    assertThat(currentState.getRunStarted(), notNullValue());
    assertThat(currentState.getRunStarted().getTime(), lessThan(System.currentTimeMillis()));
    final Future<?> future = currentState.getFuture();
    assertThat(future, notNullValue());

    // TODO: behavior change: due to HealthCheckTask, currently this is NOT enforced (to throw if updating non-cancellable running task)
    // As this is still okay thing to do, as once task is done, it will "pick up" new changes anyway
    // update it
    taskConfiguration.setString(SleeperTask.RESULT_KEY, "second");
    // try {
      taskInfo = taskScheduler().scheduleTask(taskConfiguration, taskScheduler().getScheduleFactory().manual());
    //  fail("Should fail");
    // }
    // catch (IllegalStateException e) {
    //   assertThat(e.getMessage(), containsString("running and not cancelable"));
    // }

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // and block for the result
    final String result = (String) future.get();
    assertThat(result, equalTo(RESULT));

    // done
    assertTaskState(taskInfo, State.WAITING);
    assertRunningTaskCount(0);
  }

  @Test
  public void taskDisableEnableResumesTask() throws Exception {
    // create the task
    final TaskConfiguration taskConfiguration = taskScheduler().createTaskConfigurationInstance(SleeperTaskDescriptor.TYPE_ID);
    taskConfiguration.setLong(SleeperTask.SLEEP_MILLIS_KEY, 0); // do not sleep
    taskConfiguration.setString(SleeperTask.RESULT_KEY, "result");

    TaskInfo taskInfo = taskScheduler().scheduleTask(taskConfiguration, taskScheduler().getScheduleFactory().manual());
    Future future = taskInfo.runNow().getCurrentState().getFuture();
    assertThat(future, notNullValue());

    // give it some time to start and make it immediately done
    SleeperTask.youWait.await();
    SleeperTask.meWait.countDown();
    Thread.yield();
    assertThat(future.get(), notNullValue());
    assertTaskState(taskInfo, State.WAITING);

    SleeperTask.reset();

    // disable-enable it
    taskConfiguration.setEnabled(false);
    taskInfo = taskScheduler().scheduleTask(taskConfiguration, taskScheduler().getScheduleFactory().manual());

    assertThat("Disabled task should not produce a future result", taskInfo.runNow().getCurrentState().getFuture(),
        nullValue());

    taskConfiguration.setEnabled(true);
    taskInfo = taskScheduler().scheduleTask(taskConfiguration, taskScheduler().getScheduleFactory().manual());

    future = taskInfo.runNow().getCurrentState().getFuture();
    assertThat(future, notNullValue());

    // give it some time to start and make it immediately done
    SleeperTask.youWait.await();
    SleeperTask.meWait.countDown();
    Thread.yield();
    assertThat(future.get(), notNullValue());
  }
}
