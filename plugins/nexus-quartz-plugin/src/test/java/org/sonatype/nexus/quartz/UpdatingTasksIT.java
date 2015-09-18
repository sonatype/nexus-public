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
import org.sonatype.nexus.scheduling.schedule.Manual;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.fail;

/**
 * IT for updating tasks.
 */
public class UpdatingTasksIT
    extends QuartzITSupport
{
  /**
   * Updating a non cancelable task that is not running.
   */
  @Test
  public void updateNonRunningNonCancelableTask() throws Exception {
    // reset the latch
    SleeperTask.reset();

    // create the task
    final TaskConfiguration taskConfiguration = taskScheduler
        .createTaskConfigurationInstance(SleeperTask.class);
    taskConfiguration.setString(SleeperTask.RESULT_KEY, "first");
    TaskInfo taskInfo = taskScheduler.scheduleTask(taskConfiguration, new Manual());

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
    taskInfo = taskScheduler.scheduleTask(taskConfiguration, new Manual());

    assertThat(taskInfo.getCurrentState().getState(), equalTo(State.WAITING));
    assertThat(taskInfo.getConfiguration().getString(SleeperTask.RESULT_KEY), equalTo("second"));

    // see what scheduler has
    TaskInfo ti2 = taskScheduler.getTaskById(taskInfo.getId());
    assertThat(ti2.getCurrentState().getState(), equalTo(State.WAITING));
    assertThat(ti2.getConfiguration().getString(SleeperTask.RESULT_KEY), equalTo("second"));
  }

  /**
   * Updating a non cancelable task that is running.
   */
  @Test
  public void updateRunningNonCancelableTask() throws Exception {
    // reset the latch
    SleeperTask.reset();

    // create the task
    final TaskConfiguration taskConfiguration = taskScheduler
        .createTaskConfigurationInstance(SleeperTask.class);
    taskConfiguration.setString(SleeperTask.RESULT_KEY, "first");
    TaskInfo taskInfo = taskScheduler.submit(taskConfiguration);

    // give it some time to start
    SleeperTask.youWait.await();

    assertThat(taskInfo, notNullValue());
    assertThat(taskInfo.getId(), equalTo(taskConfiguration.getId()));
    assertThat(taskInfo.getName(), equalTo(taskConfiguration.getName()));
    assertThat(taskInfo.getConfiguration().getTypeId(), equalTo(taskConfiguration.getTypeId()));
    assertThat(taskInfo.getConfiguration().getCreated(), notNullValue());
    assertThat(taskInfo.getConfiguration().getUpdated(), notNullValue());
    assertThat(taskScheduler.getRunningTaskCount(), equalTo(1));

    final CurrentState currentState = taskInfo.getCurrentState();
    assertThat(currentState, notNullValue());
    assertThat(currentState.getState(), equalTo(State.RUNNING));
    assertThat(currentState.getRunState(), equalTo(RunState.RUNNING));
    assertThat(currentState.getRunStarted(), notNullValue());
    assertThat(currentState.getRunStarted().getTime(), lessThan(System.currentTimeMillis()));
    final Future<?> future = currentState.getFuture();
    assertThat(future, notNullValue());

    // update it
    taskConfiguration.setString(SleeperTask.RESULT_KEY, "second");
    try {
      taskInfo = taskScheduler.submit(taskConfiguration);
      fail("Should fail");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("running and not cancelable"));
    }

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // and block for the result
    final String result = (String) future.get();
    assertThat(result, equalTo("first"));
    // taskInfo for DONE task is terminal
    assertThat(taskInfo.getCurrentState().getState(), equalTo(State.DONE));

    // the fact that future.get returned still does not mean that the pool is done
    // pool maintenance might not be done yet
    // so let's sleep for some
    Thread.sleep(500);

    // done
    assertThat(taskScheduler.getRunningTaskCount(), equalTo(0));
  }
}
