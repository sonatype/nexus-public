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
package org.sonatype.nexus.scheduling.internal.upgrade.datastore;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.Cooperation2Selector;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeFailedEvent;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedCanceled;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;
import org.sonatype.nexus.testdb.DataSessionRule;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Tests for {@link QueuingUpgradeTaskScheduler}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class QueuingUpgradeTaskSchedulerTest

{
  private static final Duration ONE_SECOND = Duration.ofSeconds(1);

  private static final String TASK_ID = "my-upgrade-task";

  private static final String TASK_ID_2 = "my-upgrade-task-2";

  private static final Map<String, String> TASK_CONFIG = ImmutableMap.of("a", "b");

  private static final Map<String, String> NON_BLOCKING_TASK_CONFIG =
      ImmutableMap.of("a", "b", UpgradeTaskData.BLOCK_QUEUE_KEY, "false");

  @Mock
  private PeriodicJobService periodicJobService;

  @Mock
  private EventManager eventManager;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private TaskConfiguration taskConfiguration;

  @Mock
  private TaskInfo taskInfo;

  @Mock
  private Cooperation2Factory cooperationFactory;

  @Mock
  private Cooperation2Selector localCooperationSelector;

  @Mock
  private Cooperation2Selector distributedCooperationSelector;

  private QueuingUpgradeTaskScheduler underTest;

  private UpgradeTaskStore upgradeTaskStore;

  private ListAppender<ILoggingEvent> logCaptor;

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME).access(UpgradeTaskDAO.class);

  @Before
  public void start() {
    mockCooperation();

    when(taskScheduler.createTaskConfigurationInstance(any())).thenReturn(taskConfiguration);
    when(taskConfiguration.asMap()).thenReturn(TASK_CONFIG);
    when(taskConfiguration.getId()).thenReturn(TASK_ID);

    when(taskInfo.getId()).thenReturn(TASK_ID);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    when(taskScheduler.submit(any())).thenReturn(taskInfo);

    upgradeTaskStore = new UpgradeTaskStore(sessionRule);
    upgradeTaskStore.setDependencies(eventManager);

    underTest = new QueuingUpgradeTaskScheduler(periodicJobService, taskScheduler, upgradeTaskStore, true,
        Duration.ofSeconds(0), localCooperationSelector, distributedCooperationSelector);

    logCaptor = new ListAppender<>();
    logCaptor.start();
    ((Logger) LoggerFactory.getLogger(QueuingUpgradeTaskScheduler.class)).addAppender(logCaptor);
  }

  @After
  public void stop() {
    ((Logger) LoggerFactory.getLogger(QueuingUpgradeTaskScheduler.class)).detachAppender(logCaptor);
    // the default behavior is to use the distributed impl, so we should have selected it and see nothing be done with
    // the local impl
    verifyNoInteractions(localCooperationSelector);
  }

  /*
   * Verify that tasks requested to be scheduled before startup are scheduled after startup
   */
  @Test
  public void testSchedule_beforeStart() throws Exception {
    underTest.schedule(taskConfiguration);

    // the scheduler isn't invoked, delay due to async
    await().during(ONE_SECOND)
        .untilAsserted(() -> verify(taskScheduler, never()).submit(any()));

    // but we have a record in the DB in case something happens
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    startQueuingUpgradeTaskScheduler();

    await().atMost(ONE_SECOND).untilAsserted(() -> verify(taskScheduler).submit(any()));
  }

  /*
   * Verify that tasks requested to be scheduled after startup are scheduled immediately
   */
  @Test
  public void testSchedule_afterStart() throws Exception {
    startQueuingUpgradeTaskScheduler();
    underTest.schedule(taskConfiguration);

    // we have a DB record
    assertThat(upgradeTaskStore.browse().count(), is(1L));
    // and the task was scheduled

    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void testOn_upgradeCompleted() throws Exception {
    startQueuingUpgradeTaskScheduler();
    underTest.schedule(taskConfiguration);

    EventHelper.asReplicating(
        () -> underTest.on(new UpgradeCompletedEvent("jsmith", "123", Collections.emptyList(), "Migration_1.0")));

    // replicating events from other nodes should not trigger
    verifyNoInteractions(taskScheduler);

    underTest.on(new UpgradeCompletedEvent("jsmith", "123", Collections.emptyList(), "Migration_1.0"));

    // non-replicated events should trigger
    verify(taskScheduler).submit(any());
  }

  @Test
  public void testOn_upgradeFailed() throws Exception {
    startQueuingUpgradeTaskScheduler();
    underTest.schedule(taskConfiguration);

    EventHelper.asReplicating(
        () -> underTest.on(new UpgradeFailedEvent("jsmith", "123", "Failed", "Migration_1.0")));

    // replicating events from other nodes should not trigger
    verifyNoInteractions(taskScheduler);

    underTest.on(new UpgradeFailedEvent("jsmith", "123", "Failed", "Migration_1.0"));

    // non-replicated events should trigger
    verify(taskScheduler).submit(any());
  }

  /*
   * Verify behaviour when processing a task completed event.
   */
  @Test
  public void testOn_taskCompleted() throws Exception {
    startQueuingUpgradeTaskScheduler();
    // No task has been registered as an upgrade, the main thing here is that we don't fail
    underTest.on(new TaskEventStoppedDone(taskInfo));
    // we don't expect anything to have been added to the DB
    assertThat(upgradeTaskStore.browse().count(), is(0L));

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID_2, TASK_CONFIG));
    // sanity check
    assertThat(upgradeTaskStore.browse().count(), is(2L));

    // Fire event about the task in the DB
    underTest.on(new TaskEventStoppedDone(taskInfo));
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    // queue is processed
    verify(taskScheduler, times(2)).getTaskById(TASK_ID_2);
    verify(taskScheduler).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with an item in the queue which has no associated task
   */
  @Test
  public void testMaybeStartQueue_nextItem_hasNoTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));

    // Start queue
    underTest.maybeStartQueue();

    verify(taskScheduler, times(2)).getTaskById(TASK_ID);
    verify(taskScheduler).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with an item in the queue and the task indicates OK
   */
  @Test
  public void testMaybeStartQueue_nextItem_hasOkTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    when(taskScheduler.getTaskById(TASK_ID)).thenReturn(taskInfo);
    when(taskScheduler.toExternalTaskState(taskInfo))
        .thenReturn(new ExternalTaskState(TaskState.OK, null, null, null, null, null));

    // Start queue
    underTest.maybeStartQueue();

    verify(taskScheduler).getTaskById(TASK_ID);
    verify(taskScheduler, never()).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with an item in the queue and the associated task is currently running
   */
  @Test
  public void testMaybeStartQueue_nextItem_hasRunningTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    when(taskScheduler.getTaskById(TASK_ID)).thenReturn(taskInfo);
    when(taskScheduler.toExternalTaskState(taskInfo))
        .thenReturn(new ExternalTaskState(TaskState.RUNNING, null, null, null, null, null));

    // Start queue
    underTest.maybeStartQueue();

    verify(taskScheduler).getTaskById(TASK_ID);
    verify(taskScheduler, never()).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with no items in the queue
   */
  @Test
  public void testMaybeStartQueue_noQueuedTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Start queue
    underTest.maybeStartQueue();

    verifyNoInteractions(taskScheduler);
  }

  /*
   * Verify behaviour when processing a task canceled event.
   */
  @Test
  public void testOn_taskCanceled() throws Exception {
    startQueuingUpgradeTaskScheduler();
    // No task has been registered as an upgrade, the main thing here is that we don't fail
    underTest.on(new TaskEventStoppedCanceled(taskInfo));
    // we don't expect anything to have been added to the DB
    assertThat(upgradeTaskStore.browse().count(), is(0L));

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    // sanity check
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    // Fire event about the task in the DB
    underTest.on(new TaskEventStoppedCanceled(taskInfo));
    UpgradeTaskData task = upgradeTaskStore.browse().findFirst().orElse(null);
    assertThat(task, notNullValue());
    assertThat(task.getStatus(), is("canceled"));

    // Verify the log message is WARN (not ERROR) — cancellation is an expected, retriable state
    boolean warnLogged = logCaptor.list.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Will retry on next startup"));
    assertThat(warnLogged, is(true));
    boolean errorLogged = logCaptor.list.stream()
        .anyMatch(e -> e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("Will retry on next startup"));
    assertThat(errorLogged, is(false));
  }

  /*
   * Verify behaviour when processing a task failed event.
   */
  @Test
  public void testOn_taskFailed() throws Exception {
    startQueuingUpgradeTaskScheduler();
    // No task has been registered as an upgrade, the main thing here is that we don't fail
    underTest.on(new TaskEventStoppedFailed(taskInfo, null));
    // we don't expect anything to have been added to the DB
    assertThat(upgradeTaskStore.browse().count(), is(0L));

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    // sanity check
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    // Fire event about the task in the DB
    underTest.on(new TaskEventStoppedFailed(taskInfo, null));
    UpgradeTaskData task = upgradeTaskStore.browse().findFirst().orElse(null);
    assertThat(task, notNullValue());
    assertThat(task.getStatus(), is("failed"));

    // Verify the log message is WARN (not ERROR) — failure is an expected, retriable state
    boolean warnLogged = logCaptor.list.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Will retry on next startup"));
    assertThat(warnLogged, is(true));
    boolean errorLogged = logCaptor.list.stream()
        .anyMatch(e -> e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("Will retry on next startup"));
    assertThat(errorLogged, is(false));
  }

  /*
   * Verify that a non-blocking task that is running does not prevent the next task in the queue from starting.
   */
  @Test
  public void testMaybeStartQueue_nonBlockingRunningTask_doesNotBlockNextTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    TaskInfo taskInfo2 = mock(TaskInfo.class);
    when(taskInfo2.getId()).thenReturn(TASK_ID_2);
    when(taskScheduler.submit(any())).thenReturn(taskInfo2);

    // First task is non-blocking and currently running
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, NON_BLOCKING_TASK_CONFIG));
    when(taskScheduler.getTaskById(TASK_ID)).thenReturn(taskInfo);
    when(taskScheduler.toExternalTaskState(taskInfo))
        .thenReturn(new ExternalTaskState(TaskState.RUNNING, null, null, null, null, null));

    // Second task is queued behind it
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID_2, TASK_CONFIG));

    underTest.maybeStartQueue();

    // The second task must have been submitted despite the first still running
    verify(taskScheduler).submit(any());
    // getTaskById is called twice for TASK_ID_2: once in notRunningAndNotDone, once in scheduleTask
    verify(taskScheduler, times(2)).getTaskById(TASK_ID_2);
  }

  /*
   * Verify that a pending non-blocking task is scheduled AND the next task is also scheduled
   * in the same maybeStartQueue call (they start concurrently).
   */
  @Test
  public void testMaybeStartQueue_pendingNonBlockingTask_schedulesNextTaskToo() throws Exception {
    startQueuingUpgradeTaskScheduler();

    TaskInfo taskInfo2 = mock(TaskInfo.class);
    when(taskInfo2.getId()).thenReturn(TASK_ID_2);

    // First task is non-blocking and pending (not yet started)
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, NON_BLOCKING_TASK_CONFIG));
    when(taskScheduler.submit(any())).thenReturn(taskInfo).thenReturn(taskInfo2);

    // Second task is queued behind it
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID_2, TASK_CONFIG));

    underTest.maybeStartQueue();

    // Both tasks must have been submitted in the same maybeStartQueue call
    verify(taskScheduler, times(2)).submit(any());
  }

  /*
   * Verify that a blocking task that is running prevents the next task in the queue from starting.
   */
  @Test
  public void testMaybeStartQueue_blockingRunningTask_blocksNextTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // First task is blocking (default) and currently running
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    when(taskScheduler.getTaskById(TASK_ID)).thenReturn(taskInfo);
    when(taskScheduler.toExternalTaskState(taskInfo))
        .thenReturn(new ExternalTaskState(TaskState.RUNNING, null, null, null, null, null));

    // Second task is queued behind it
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID_2, TASK_CONFIG));

    underTest.maybeStartQueue();

    // The second task must NOT have been touched
    verify(taskScheduler, never()).getTaskById(TASK_ID_2);
    verify(taskScheduler, never()).submit(any());
  }

  /*
   * Verify that a pending blocking task is scheduled but the next task is NOT scheduled
   * in the same maybeStartQueue call.
   */
  @Test
  public void testMaybeStartQueue_pendingBlockingTask_doesNotScheduleNextTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // First task is blocking (default) and pending
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    when(taskScheduler.submit(any())).thenReturn(taskInfo);

    // Second task is queued behind it
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID_2, TASK_CONFIG));

    underTest.maybeStartQueue();

    // Only the first task must have been submitted — second must not be touched
    verify(taskScheduler, times(1)).submit(any());
    verify(taskScheduler, never()).getTaskById(TASK_ID_2);
  }

  /*
   * Verify that a non-blocking task in OK (done) state does not prevent the next task in the queue from starting.
   */
  @Test
  public void testMaybeStartQueue_nonBlockingDoneTask_doesNotBlockNextTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    TaskInfo taskInfo2 = mock(TaskInfo.class);
    when(taskInfo2.getId()).thenReturn(TASK_ID_2);
    when(taskScheduler.submit(any())).thenReturn(taskInfo2);

    // First task is non-blocking and already done (OK)
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, NON_BLOCKING_TASK_CONFIG));
    when(taskScheduler.getTaskById(TASK_ID)).thenReturn(taskInfo);
    when(taskScheduler.toExternalTaskState(taskInfo))
        .thenReturn(new ExternalTaskState(TaskState.OK, null, null, null, null, null));

    // Second task is queued behind it
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID_2, TASK_CONFIG));

    underTest.maybeStartQueue();

    // The second task must have been submitted despite the first being done
    verify(taskScheduler).submit(any());
    verify(taskScheduler, times(2)).getTaskById(TASK_ID_2);
  }

  /*
   * Verify that schedule(config, false) stamps blockQueue=false into the stored configuration.
   */
  @Test
  public void testSchedule_withBlockQueueFalse_stampsFlag() throws Exception {
    startQueuingUpgradeTaskScheduler();

    TaskConfiguration realConfig = new TaskConfiguration();
    realConfig.setId(TASK_ID);

    underTest.schedule(realConfig, false);

    UpgradeTaskData stored = upgradeTaskStore.browse().findFirst().orElse(null);
    assertThat(stored, notNullValue());
    assertThat(stored.getConfiguration().get(UpgradeTaskData.BLOCK_QUEUE_KEY), is("false"));
  }

  private void mockCooperation() {
    when(localCooperationSelector.select()).thenReturn(cooperationFactory);
    when(distributedCooperationSelector.select()).thenReturn(cooperationFactory);

    Cooperation2Factory.Builder cooperationBuilder = mock(Cooperation2Factory.Builder.class);
    when(cooperationFactory.configure()).thenReturn(cooperationBuilder);

    Cooperation2 disabled = new DefaultCooperation2Factory().configure()
        .enabled(false)
        .build("test");

    when(cooperationBuilder.build(any())).thenReturn(disabled);
  }

  /*
   * Trigger stateguard transition and wait for async behaviour
   */
  private void startQueuingUpgradeTaskScheduler() throws Exception {
    ForkJoinTask<?>[] startup = new ForkJoinTask[1];

    doAnswer(i -> {
      // The runnable passed in must only be executed after StateGuard finishes the START transition, so we need async
      startup[0] = ForkJoinTask.adapt(() -> {
        // Wait for stateguard to finish the transition
        await().atMost(1, TimeUnit.SECONDS).until(underTest::isStarted);
        // invoke the job
        ((Runnable) i.getArguments()[0]).run();
        // dummy return value
        return null;
      });

      // invoke the async
      ForkJoinPool.commonPool().execute(startup[0]);

      return null;
    }).when(periodicJobService).runOnce(any(), anyInt());

    underTest.start();

    // wait for the async task to complete
    startup[0].get();
  }
}
