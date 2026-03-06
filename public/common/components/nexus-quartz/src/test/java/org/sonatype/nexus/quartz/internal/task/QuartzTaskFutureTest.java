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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Manual;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

public class QuartzTaskFutureTest
    extends TestSupport
{
  @Mock
  private QuartzSchedulerSPI scheduler;

  private JobKey jobKey;

  private String taskLogName;

  private Date scheduledAt;

  private Manual schedule;

  @Before
  public void setUp() {
    jobKey = new JobKey("test-job", "test-group");
    taskLogName = "Test Task";
    scheduledAt = new Date(System.currentTimeMillis() - 1000L); // 1 second ago
    schedule = new Manual();
  }

  @Test
  public void testScheduledAtReturnsScheduledTime() {
    QuartzTaskFuture future = new QuartzTaskFuture(scheduler, jobKey, taskLogName, scheduledAt, schedule, null);

    assertThat(future.getRunState(), equalTo(TaskState.RUNNING_STARTING));
    assertThat(future.getScheduledAt(), equalTo(scheduledAt));
    assertThat(future.getStartedAt(), equalTo(scheduledAt));
  }

  @Test
  public void testStartedAtCapturedWhenTransitioningToRunning() throws InterruptedException {
    QuartzTaskFuture future = new QuartzTaskFuture(scheduler, jobKey, taskLogName, scheduledAt, schedule, null);

    Thread.sleep(100);
    future.setRunState(TaskState.RUNNING);

    Date actualStartTime = future.getStartedAt();

    assertThat(actualStartTime, notNullValue());
    assertThat(actualStartTime.getTime(), greaterThan(scheduledAt.getTime()));

    long timeDiff = actualStartTime.getTime() - scheduledAt.getTime();
    assertThat(timeDiff >= 100, equalTo(true)); // At least 100ms difference
  }

  @Test
  public void testStartedAtOnlyCapturedOnceWhenTransitioningToRunning() throws InterruptedException {
    QuartzTaskFuture future = new QuartzTaskFuture(scheduler, jobKey, taskLogName, scheduledAt, schedule, null);

    // Initial state is RUNNING_STARTING, go through RUNNING_BLOCKED first
    future.setRunState(TaskState.RUNNING_BLOCKED);

    Thread.sleep(50);

    future.setRunState(TaskState.RUNNING);
    Date startTime = future.getStartedAt();

    assertThat(startTime, notNullValue());
    assertThat(startTime.getTime(), greaterThan(scheduledAt.getTime()));
  }

  @Test
  public void testBlockedTaskScenario() throws InterruptedException {
    Date scheduledTime = new Date(System.currentTimeMillis() - 5000L); // 5 seconds ago
    QuartzTaskFuture future = new QuartzTaskFuture(scheduler, jobKey, taskLogName, scheduledTime, schedule, null);

    future.setRunState(TaskState.RUNNING_BLOCKED);

    Thread.sleep(100);

    future.setRunState(TaskState.RUNNING);

    assertThat(future.getScheduledAt(), equalTo(scheduledTime));

    Date actualStartTime = future.getStartedAt();
    assertThat(actualStartTime.getTime(), greaterThan(scheduledTime.getTime()));

    long delay = actualStartTime.getTime() - scheduledTime.getTime();
    assertThat(delay >= 100, equalTo(true));
  }
}
