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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo.RunState;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.internal.Tasks.SleeperTask;
import org.sonatype.nexus.scheduling.spi.TaskExecutorSPI;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.util.Providers;
import org.eclipse.sisu.BeanEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

public class TaskSchedulerTest
    extends TestSupport
{
  private DefaultTaskScheduler nexusTaskScheduler;

  @Before
  public void prepare() {
    final BeanEntry<Named, Task> task = Tasks.beanEntry(SleeperTask.class);
    final DefaultTaskFactory nexusTaskFactory = new DefaultTaskFactory(
        ImmutableList.of(task), Lists.<TaskDescriptor<?>>newArrayList());
    nexusTaskScheduler = new DefaultTaskScheduler(nexusTaskFactory,
        Providers.<TaskExecutorSPI>of(new ThreadPoolTaskExecutorSPI(nexusTaskFactory)));
  }

  @Test
  public void lifecycle() throws Exception {
    // reset the latches
    SleeperTask.meWait = new CountDownLatch(1);

    final TaskConfiguration taskConfiguration = nexusTaskScheduler.createTaskConfigurationInstance(SleeperTask.class);
    final String RESULT = "This is the expected result";
    taskConfiguration.setString(SleeperTask.RESULT_KEY, RESULT);
    final TaskInfo taskInfo = nexusTaskScheduler.submit(taskConfiguration);

    assertThat(taskInfo, notNullValue());
    assertThat(taskInfo.getId(), equalTo(taskConfiguration.getId()));
    assertThat(taskInfo.getName(), equalTo(taskConfiguration.getName()));
    assertThat(taskInfo.getConfiguration().getTypeId(), equalTo(taskConfiguration.getTypeId()));
    assertThat(taskInfo.getConfiguration().getCreated(), notNullValue());
    assertThat(taskInfo.getConfiguration().getUpdated(), notNullValue());
    assertThat(nexusTaskScheduler.getRunningTaskCount(), equalTo(1));

    final CurrentState currentState = taskInfo.getCurrentState();
    assertThat(currentState, notNullValue());
    assertThat(currentState.getState(), equalTo(State.RUNNING));
    assertThat(currentState.getRunState(), equalTo(RunState.RUNNING));
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
    assertThat(nexusTaskScheduler.getRunningTaskCount(), equalTo(0));
    // taskInfo for DONE task is terminal
    assertThat(taskInfo.getCurrentState().getState(), equalTo(State.DONE));
  }
}
