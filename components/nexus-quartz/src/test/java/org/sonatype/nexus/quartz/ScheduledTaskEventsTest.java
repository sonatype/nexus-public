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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.events.TaskBlockedEvent;
import org.sonatype.nexus.scheduling.events.TaskEvent;
import org.sonatype.nexus.scheduling.events.TaskEventCanceled;
import org.sonatype.nexus.scheduling.events.TaskEventStarted;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedCanceled;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;
import org.sonatype.nexus.scheduling.events.TaskScheduledEvent;
import org.sonatype.nexus.scheduling.events.TaskStartedRunningEvent;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests for task eventing.
 */
@Ignore("NEXUS-43375")
public class ScheduledTaskEventsTest
    extends QuartzTestSupport
{

  protected EventManager eventManager;

  protected Listener listener;

  @Before
  public void prepare() throws Exception {
    eventManager = helper().getEventManager();
    listener = new Listener();
    eventManager.register(listener);
    // reset the latch
    SleeperTask.reset();

    assertExecutedTaskCount(0);
  }

  @Test
  public void goodRun() throws Exception {
    // create the task
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);

    // give it some time to start
    SleeperTask.youWait.await();

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // done
    assertRunningTaskCount(0);
    assertExecutedTaskCount(1);
    assertThat(taskInfo.getCurrentState().getState(), equalTo(TaskState.WAITING));
    assertThat(taskInfo.getLastRunState().getEndState(), equalTo(TaskState.OK));

    // started, stoppedDone
    assertThat(listener.arrivedEvents, hasSize(4));
    assertThat(listener.arrivedEvents.get(0), instanceOf(TaskScheduledEvent.class));
    assertThat(listener.arrivedEvents.get(1), instanceOf(TaskEventStarted.class));
    assertThat(listener.arrivedEvents.get(2), instanceOf(TaskStartedRunningEvent.class));
    assertThat(listener.arrivedEvents.get(3), instanceOf(TaskEventStoppedDone.class));
  }

  @Test
  public void goodRunAfterBlocking() throws Exception {
    // create task to block the next one
    createTask(SleeperTaskDescriptor.TYPE_ID);

    // give it some time to start
    SleeperTask.youWait.await();

    // create the task of interest
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);

    // allow scheduler to start task and find it blocked
    await().atMost(RUN_TIMEOUT, MILLISECONDS).until(() -> TaskState.RUNNING_BLOCKED.equals(taskInfo.getCurrentState().getRunState()));

    // signal tasks to complete
    SleeperTask.meWait.countDown();
    Thread.yield();

    // done
    assertRunningTaskCount(0);
    assertExecutedTaskCount(2);
    assertThat(taskInfo.getCurrentState().getState(), equalTo(TaskState.WAITING));
    assertThat(taskInfo.getLastRunState().getEndState(), equalTo(TaskState.OK));

    // started, stoppedDone
    List<Class<?>> arrivedEvents = listener.arrivedEvents.stream()
        .filter(event -> event.getTaskInfo().getId().equals(taskInfo.getId()))
        .map(event -> event.getClass())
        .collect(Collectors.toList());
    assertThat(arrivedEvents, contains(TaskScheduledEvent.class, TaskEventStarted.class, TaskBlockedEvent.class,
        TaskStartedRunningEvent.class, TaskEventStoppedDone.class));
  }

  @Test
  public void failedRunCheckedException() throws Exception {
    SleeperTask.exception = new IOException("foo");

    // create the task
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);

    // give it some time to start
    SleeperTask.youWait.await();

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // done
    assertRunningTaskCount(0);
    assertExecutedTaskCount(1);
    assertThat(taskInfo.getCurrentState().getState(), equalTo(TaskState.WAITING));
    assertThat(taskInfo.getLastRunState().getEndState(), equalTo(TaskState.FAILED));

    // started, stoppedDone
    assertThat(listener.arrivedEvents, hasSize(4));
    assertThat(listener.arrivedEvents.get(0), instanceOf(TaskScheduledEvent.class));
    assertThat(listener.arrivedEvents.get(1), instanceOf(TaskEventStarted.class));
    assertThat(listener.arrivedEvents.get(2), instanceOf(TaskStartedRunningEvent.class));
    assertThat(listener.arrivedEvents.get(3), instanceOf(TaskEventStoppedFailed.class));
    assertThat(((TaskEventStoppedFailed) listener.arrivedEvents.get(3)).getFailureCause(), instanceOf(IOException.class));
  }

  @Test
  public void failedRunRuntimeException() throws Exception {
    SleeperTask.exception = new IllegalArgumentException("foo");

    // create the task
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);

    // give it some time to start
    SleeperTask.youWait.await();

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // done
    assertRunningTaskCount(0);
    assertExecutedTaskCount(1);
    assertThat(taskInfo.getCurrentState().getState(), equalTo(TaskState.WAITING));
    assertThat(taskInfo.getLastRunState().getEndState(), equalTo(TaskState.FAILED));

    // started, stoppedFailed
    assertThat(listener.arrivedEvents, hasSize(4));
    assertThat(listener.arrivedEvents.get(0), instanceOf(TaskScheduledEvent.class));
    assertThat(listener.arrivedEvents.get(1), instanceOf(TaskEventStarted.class));
    assertThat(listener.arrivedEvents.get(2), instanceOf(TaskStartedRunningEvent.class));
    assertThat(listener.arrivedEvents.get(3), instanceOf(TaskEventStoppedFailed.class));
    assertThat(((TaskEventStoppedFailed) listener.arrivedEvents.get(3)).getFailureCause(), instanceOf(IllegalArgumentException.class));
  }

  @Test
  public void canceledRunWithNonCancelableTaskWithoutInterruption() throws Exception {
    // create the task
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);

    // give it some time to start
    SleeperTask.youWait.await();

    // attempt to cancel it 3 times (w/o interruption)
    taskInfo.getCurrentState().getFuture().cancel(false);
    taskInfo.getCurrentState().getFuture().cancel(false);
    taskInfo.getCurrentState().getFuture().cancel(false);

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // done
    assertRunningTaskCount(0);
    assertExecutedTaskCount(1);
    assertThat(taskInfo.getCurrentState().getState(), equalTo(TaskState.WAITING));
    assertThat(taskInfo.getLastRunState().getEndState(), equalTo(TaskState.OK));

    // started, stoppedDone: task is not cancelable, hence, is "unaware" it was
    // attempted to be canceled at all
    assertThat(listener.arrivedEvents, hasSize(4));
    assertThat(listener.arrivedEvents.get(0), instanceOf(TaskScheduledEvent.class));
    assertThat(listener.arrivedEvents.get(1), instanceOf(TaskEventStarted.class));
    assertThat(listener.arrivedEvents.get(2), instanceOf(TaskStartedRunningEvent.class));
    assertThat(listener.arrivedEvents.get(3), instanceOf(TaskEventStoppedDone.class));
  }

  @Test
  public void prematureCanceledRunWithNonCancelableTask() throws Exception {
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID, taskScheduler().getScheduleFactory().now());
    taskInfo.getCurrentState().getFuture().cancel(false);
    // do not use latches, as this task will not even start!

    // done
    assertRunningTaskCount(0);
    assertExecutedTaskCount(1);
    assertThat(taskInfo.getCurrentState().getState(), equalTo(TaskState.OK));
    assertThat(taskInfo.getLastRunState().getEndState(), equalTo(TaskState.CANCELED));

    // started, stoppedDone: task is not cancelable, but it was canceled by framework
    // even before it was started
    assertThat(listener.arrivedEvents, hasSize(3));
    assertThat(listener.arrivedEvents.get(0), instanceOf(TaskScheduledEvent.class));
    assertThat(listener.arrivedEvents.get(1), instanceOf(TaskEventStarted.class));
    assertThat(listener.arrivedEvents.get(2), instanceOf(TaskEventStoppedCanceled.class));
  }
  
  @Test
  public void canceledRunWithNonCancelableTaskWithInterruption() throws Exception {
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);
    cancelledRun(taskInfo, true);
  }

  @Test
  public void canceledRunWithCancelableTask() throws Exception {
    final TaskInfo taskInfo = createTask(SleeperCancelableTaskDescriptor.TYPE_ID);
    cancelledRun(taskInfo, true);
  }

  @Test
  public void canceledRunByThrowingTaskInterruptedEx() throws Exception {
    SleeperTask.exception = new TaskInterruptedException("foo", true);
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);
    cancelledRun(taskInfo, false);
  }

  @Test
  public void canceledRunByThrowingInterruptedEx() throws Exception {
    SleeperTask.exception = new InterruptedException("foo");
    final TaskInfo taskInfo = createTask(SleeperTaskDescriptor.TYPE_ID);
    cancelledRun(taskInfo, false);
  }

  private void cancelledRun(final TaskInfo taskInfo, final boolean interrupt) throws InterruptedException {
    // give it some time to start
    SleeperTask.youWait.await();

    if (interrupt) {
      // cancel it w/ interruption
      taskInfo.getCurrentState().getFuture().cancel(true);
    }

    // make it be done
    SleeperTask.meWait.countDown();
    Thread.yield();

    // done
    assertRunningTaskCount(0);
    assertExecutedTaskCount(1);
    assertThat(taskInfo.getCurrentState().getState(), equalTo(TaskState.WAITING));
    assertThat(taskInfo.getLastRunState().getEndState(), equalTo(TaskState.CANCELED));

    // started, stoppedDone: task is not cancelable, hence, is "unaware" it was
    // attempted to be canceled at all (no canceled events), still, end state is canceled
    // as thread was interrupted
    assertThat(listener.arrivedEvents, hasSize(5));
    assertThat(listener.arrivedEvents.get(0), instanceOf(TaskScheduledEvent.class));
    assertThat(listener.arrivedEvents.get(1), instanceOf(TaskEventStarted.class));
    assertThat(listener.arrivedEvents.get(2), instanceOf(TaskStartedRunningEvent.class));
    assertThat(listener.arrivedEvents.get(3), instanceOf(TaskEventCanceled.class));
    assertThat(listener.arrivedEvents.get(4), instanceOf(TaskEventStoppedCanceled.class));
  }

  class Listener
  {
    final List<TaskEvent> arrivedEvents = Lists.newArrayList();

    @Subscribe
    public void on(final TaskEvent e) {
      log("Observing task event {}", e);
      arrivedEvents.add(e);
    }
  }
}
