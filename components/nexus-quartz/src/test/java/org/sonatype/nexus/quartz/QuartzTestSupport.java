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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Hourly;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import org.junit.After;
import org.junit.Before;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;

/**
 * This beast brings up real SISU container and complete Quartz environment.
 */
public abstract class QuartzTestSupport
    extends TestSupport
{
  //max time to wait for task completion
  public static final int RUN_TIMEOUT = 2000;
  
  public static final String RESULT = "This is the expected result";

  //@Rule
  //public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private TaskSchedulerHelper taskSchedulerHelper;

  @Before
  public void before() throws Exception {
    //taskSchedulerHelper = new TaskSchedulerHelper(database.getInstance());
    taskSchedulerHelper.init(null, null);
    taskSchedulerHelper.start();
  }

  @After
  public void after() throws Exception {
    if (taskSchedulerHelper != null) {
      taskSchedulerHelper.stop();
      taskSchedulerHelper = null;
    }
  }

  protected TaskSchedulerHelper helper() {
    return taskSchedulerHelper;
  }

  protected TaskScheduler taskScheduler() {
    return helper().getTaskScheduler();
  }

  /**
   * Creates a {@link Hourly} schedule that is about to start in .5sec in future, as we now "step" triggers if
   * in past, instead executing them immediately.
   */
  protected Schedule hourly() {
    return taskScheduler().getScheduleFactory().hourly(new Date(System.currentTimeMillis() + 1000L));
  }

  public TaskInfo createTask(String typeId) {
    return createTask(typeId, hourly());
  }

  public TaskInfo createTask(String typeId, Schedule schedule) {
    final TaskConfiguration taskConfiguration = taskScheduler()
        .createTaskConfigurationInstance(typeId);
    taskConfiguration.setString(SleeperTask.RESULT_KEY, RESULT);
    return taskScheduler()
        .scheduleTask(taskConfiguration, schedule);
  }

  public void assertRunningTaskCount(int expectedCount) {
    // pool maintenance might not be done when a task's future returns so polling is in order to be safe
    await().atMost(RUN_TIMEOUT, MILLISECONDS).until(() -> taskScheduler().getRunningTaskCount(), is(expectedCount));
  }

  public void assertExecutedTaskCount(int expectedCount) {
    await().atMost(RUN_TIMEOUT, MILLISECONDS).until(() -> taskScheduler().getExecutedTaskCount(), is(expectedCount));
  }

  public void assertTaskState(final TaskInfo taskInfo, final TaskState expectedState) {
    // unfortunately, a task's Future.get() returns before the task state is updated so polling is in order to be safe
    await().atMost(RUN_TIMEOUT, MILLISECONDS).until(() -> taskInfo.getCurrentState().getState(), is(expectedState));
  }
}
