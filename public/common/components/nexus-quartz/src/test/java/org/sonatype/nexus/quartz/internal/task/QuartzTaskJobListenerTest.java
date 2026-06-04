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
package org.sonatype.nexus.quartz.internal.task;

import java.util.Date;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
import org.sonatype.nexus.quartz.internal.QuartzTriggerConverter;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Manual;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener.INIT_ERROR_KEY;
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskUtils.configurationOf;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class QuartzTaskJobListenerTest
{
  @Mock
  private EventManager eventManager;

  @Mock
  private QuartzSchedulerSPI scheduler;

  @Mock
  private JobExecutionContext context;

  @Mock
  private JobDetail jobDetail;

  @Mock
  private Trigger trigger;

  @Mock
  private org.quartz.Scheduler quartzScheduler;

  @Mock
  private QuartzTriggerConverter triggerConverter;

  private JobKey jobKey;

  private TaskConfiguration taskConfiguration;

  private QuartzTaskState taskState;

  private QuartzTaskInfo taskInfo;

  @Before
  public void setUp() throws Exception {
    jobKey = new JobKey("test-job", "nexus");
    taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId("test-id");
    taskConfiguration.setTypeId("test-type");
    taskConfiguration.setName("Test Task");

    taskState = new QuartzTaskState(taskConfiguration, new Manual(), null);
    taskInfo = new QuartzTaskInfo(eventManager, scheduler, jobKey, taskState, null);

    when(context.getJobDetail()).thenReturn(jobDetail);
    when(context.getScheduler()).thenReturn(quartzScheduler);
    when(jobDetail.getKey()).thenReturn(jobKey);
    when(jobDetail.getJobDataMap()).thenReturn(new org.quartz.JobDataMap(taskConfiguration.asMap()));
    when(quartzScheduler.getTrigger(any())).thenReturn(trigger);
    when(trigger.getNextFireTime()).thenReturn(null);
    when(trigger.getJobDataMap()).thenReturn(new org.quartz.JobDataMap());
    when(scheduler.triggerConverter()).thenReturn(triggerConverter);
    when(triggerConverter.convert(any(Trigger.class))).thenReturn(new Manual());
  }

  @Test
  public void testJobWasExecuted_capturesScheduledAndActualStartTimes() throws InterruptedException {
    // Create a future with scheduled time
    Date scheduledTime = new Date(System.currentTimeMillis() - 5000L); // 5 seconds ago
    QuartzTaskFuture future = new QuartzTaskFuture(
        scheduler,
        jobKey,
        taskConfiguration.getTaskLogName(),
        scheduledTime,
        new Manual(),
        null);

    // Simulate task being blocked then starting
    Thread.sleep(100);
    future.setRunState(TaskState.RUNNING);
    Date actualStartTime = future.getStartedAt();

    // Put future in context
    when(context.get(QuartzTaskFuture.FUTURE_KEY)).thenReturn(future);
    when(context.get(QuartzTaskInfo.TASK_INFO_KEY)).thenReturn(taskInfo);

    // Create listener and call jobWasExecuted
    QuartzTaskJobListener listener = new QuartzTaskJobListener(
        "test-listener",
        eventManager,
        scheduler,
        taskInfo);

    // Simulate successful execution
    listener.jobWasExecuted(context, null);

    // Verify task configuration has both times
    TaskConfiguration updatedConfig = configurationOf(jobDetail);
    assertThat(updatedConfig.hasLastRunState(), equalTo(true));
    assertThat(updatedConfig.getLastRunState(), notNullValue());

    assertThat(updatedConfig.getLastRunState().getRunStarted(), equalTo(actualStartTime));
    assertThat(updatedConfig.getLastRunState().getRunScheduled(), equalTo(scheduledTime));

    // Verify they are different (task was delayed)
    assertThat(actualStartTime.getTime(), greaterThan(scheduledTime.getTime()));
  }

  @Test
  public void testJobWasExecuted_whenTaskNotBlocked_timesAreSame() {
    // Create a future and immediately start it (no blocking)
    Date scheduledTime = new Date();
    QuartzTaskFuture future = new QuartzTaskFuture(
        scheduler,
        jobKey,
        taskConfiguration.getTaskLogName(),
        scheduledTime,
        new Manual(),
        null);

    future.setRunState(TaskState.RUNNING);

    when(context.get(QuartzTaskFuture.FUTURE_KEY)).thenReturn(future);
    when(context.get(QuartzTaskInfo.TASK_INFO_KEY)).thenReturn(taskInfo);

    QuartzTaskJobListener listener = new QuartzTaskJobListener(
        "test-listener",
        eventManager,
        scheduler,
        taskInfo);

    listener.jobWasExecuted(context, null);

    TaskConfiguration updatedConfig = configurationOf(jobDetail);

    // When not blocked, times should be very close (within milliseconds)
    long timeDiff = Math.abs(
        updatedConfig.getLastRunState().getRunStarted().getTime() -
            updatedConfig.getLastRunState().getRunScheduled().getTime());

    // Allow small time difference due to execution time (generous bound to avoid flakiness on slow CI)
    assertThat(timeDiff, lessThan(100L));
  }

  @Test
  public void testJobToBeExecuted_capturesInitializationError() {
    // Simulate an exception during initialization by making triggerConverter throw
    RuntimeException expectedException = new RuntimeException("Simulated initialization failure");
    when(scheduler.triggerConverter()).thenThrow(expectedException);

    QuartzTaskJobListener listener = new QuartzTaskJobListener(
        "test-listener",
        eventManager,
        scheduler,
        taskInfo);

    // Call jobToBeExecuted - should not throw, but should capture the error
    listener.jobToBeExecuted(context);

    // Verify the error was stored in context
    verify(context).put(eq(INIT_ERROR_KEY), eq(expectedException));
  }

  @Test
  public void testJobToBeExecuted_storesContextOnSuccess() throws Exception {
    // Setup for successful initialization
    when(context.getFireTime()).thenReturn(new Date());
    when(context.getTrigger()).thenReturn(trigger);

    QuartzTaskJobListener listener = new QuartzTaskJobListener(
        "test-listener",
        eventManager,
        scheduler,
        taskInfo);

    listener.jobToBeExecuted(context);

    // Verify context was populated with future and task info
    verify(context).put(eq(QuartzTaskFuture.FUTURE_KEY), any(QuartzTaskFuture.class));
    verify(context).put(eq(QuartzTaskInfo.TASK_INFO_KEY), eq(taskInfo));
  }
}
