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
package org.sonatype.nexus.quartz.internal.datastore;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LastShutdownTimeService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerProvider;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskState;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Hourly;
import org.sonatype.nexus.scheduling.schedule.Manual;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.testcommon.event.SimpleEventManager;
import org.sonatype.nexus.thread.DatabaseStatusDelayedExecutor;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class DatastoreQuartzSchedulerSPITest
    extends TestSupport
{

  private DatastoreQuartzSchedulerSPI underTest;

  private JobStore jobStore;

  private QuartzSchedulerProvider scheduler;

  private EventManager eventManager;

  @Before
  public void before() throws Exception {
    NodeAccess nodeAccess = mock(NodeAccess.class);
    when(nodeAccess.getId()).thenReturn("test");
    Provider<JobStore> provider = mock(Provider.class);
    jobStore = mock(JobStore.class);
    when(provider.get()).thenReturn(jobStore);
    scheduler = new QuartzSchedulerProvider(nodeAccess, provider, mock(JobFactory.class), 1, 5);
    eventManager = new SimpleEventManager();

    LastShutdownTimeService lastShutdownTimeService = mock(LastShutdownTimeService.class);
    when(lastShutdownTimeService.estimateLastShutdownTime()).thenReturn(Optional.empty());

    DatabaseStatusDelayedExecutor statusDelayedExecutor = mock(DatabaseStatusDelayedExecutor.class);
    doAnswer(invocation -> {
      Runnable runnable = (Runnable) invocation.getArguments()[0];
      runnable.run();
      return null;
    }).when(statusDelayedExecutor).execute(any(Runnable.class));

    underTest = new DatastoreQuartzSchedulerSPI(
        eventManager, nodeAccess, provider, scheduler, lastShutdownTimeService, statusDelayedExecutor, true);
    scheduler.start();
    underTest.start();
    underTest.getStateGuard().transition(StateGuardLifecycleSupport.State.STARTED);
  }

  @After
  public void after() throws Exception {
    scheduler.stop();
    underTest.stop();
  }

  @Test
  public void schedulingInThePastShouldFireTriggerInNextHour() throws JobPersistenceException {
    DateTime now = DateTime.now();
    DateTime startAt = DateTime.parse("2010-06-30T01:20");
    underTest.scheduleTask(new TaskConfiguration(), new Hourly(startAt.toDate()));

    ArgumentCaptor<OperableTrigger> triggerRecorder = ArgumentCaptor.forClass(OperableTrigger.class);
    verify(jobStore).storeJobAndTrigger(any(JobDetail.class), triggerRecorder.capture());

    DateTime triggerTime = new DateTime(triggerRecorder.getValue().getStartTime());
    assertTrue(triggerTime.isAfter(now));
    assertThat(triggerTime.getMinuteOfHour(), equalTo(startAt.getMinuteOfHour()));
    assertThat(new Duration(now, triggerTime).getStandardMinutes(), lessThan(60L));
  }

  @Test
  public void exerciseJobEvents() throws SchedulerException {
    JobKey jobKey = mockJobDetail();
    mockTrigger(jobKey);

    SchedulerListener schedulerListener = mock(SchedulerListener.class);
    underTest.getScheduler().getListenerManager().addSchedulerListener(schedulerListener);

    Trigger trigger = mock(OperableTrigger.class);
    JobDataMap map = new JobDataMap(new Now().asMap());
    map.put(".id", "my-id");
    map.put(".typeId", "type-id");
    when(trigger.getJobDataMap()).thenReturn(map);
    when(jobStore.retrieveTrigger(any())).thenReturn((OperableTrigger) trigger);

    eventManager.register(underTest);

    JobCreatedEvent jobCreatedEvent = new JobCreatedEvent(new JobKey("testJobKeyName", "testJobKeyGroup"));
    EventHelper.asReplicating(() -> eventManager.post(jobCreatedEvent));

    ArgumentCaptor<JobDetail> recordCreate = ArgumentCaptor.forClass(JobDetail.class);
    verify(schedulerListener).jobAdded(recordCreate.capture());
    assertThat(recordCreate.getValue().getKey().getName(), equalTo("testJobKeyName"));

    reset(schedulerListener);

    JobUpdatedEvent jobUpdateEvent = new JobUpdatedEvent(new JobKey("testJobKeyName", "testJobKeyGroup"));
    EventHelper.asReplicating(() -> eventManager.post(jobUpdateEvent));

    ArgumentCaptor<JobDetail> recordUpdate = ArgumentCaptor.forClass(JobDetail.class);
    verify(schedulerListener).jobAdded(recordUpdate.capture());
    assertThat(recordUpdate.getValue().getKey().getName(), equalTo("testJobKeyName"));

    reset(schedulerListener);

    JobDeletedEvent jobDeletedEvent = new JobDeletedEvent(new JobKey("testJobKeyName", "testJobKeyGroup"));
    EventHelper.asReplicating(() -> eventManager.post(jobDeletedEvent));

    ArgumentCaptor<JobKey> recordDelete = ArgumentCaptor.forClass(JobKey.class);
    verify(schedulerListener).jobDeleted(recordDelete.capture());
    assertThat(recordDelete.getValue().getName(), equalTo("testJobKeyName"));
  }

  @Test
  public void exerciseScheduledTriggerEvents() throws SchedulerException {
    JobKey jobKey = mockJobDetail();
    TriggerKey triggerKey = mockTrigger(jobKey);

    SchedulerListener schedulerListener = mock(SchedulerListener.class);
    underTest.getScheduler().getListenerManager().addSchedulerListener(schedulerListener);

    eventManager.register(underTest);

    TriggerCreatedEvent triggerCreatedEvent = new TriggerCreatedEvent(triggerKey);
    EventHelper.asReplicating(() -> eventManager.post(triggerCreatedEvent));

    ArgumentCaptor<Trigger> recordCreate = ArgumentCaptor.forClass(Trigger.class);
    verify(schedulerListener).jobScheduled(recordCreate.capture());
    assertThat(recordCreate.getValue().getKey().getName(), equalTo("testTriggerKeyName"));

    reset(schedulerListener);

    TriggerUpdatedEvent triggerUpdatedEvent = new TriggerUpdatedEvent(triggerKey);
    EventHelper.asReplicating(() -> eventManager.post(triggerUpdatedEvent));

    ArgumentCaptor<TriggerKey> recordUpdateUnschedule = ArgumentCaptor.forClass(TriggerKey.class);
    verify(schedulerListener).jobUnscheduled(recordUpdateUnschedule.capture());
    assertThat(recordUpdateUnschedule.getValue().getName(), equalTo("testTriggerKeyName"));

    ArgumentCaptor<Trigger> recordUpdateSchedule = ArgumentCaptor.forClass(Trigger.class);
    verify(schedulerListener).jobScheduled(recordUpdateSchedule.capture());
    assertThat(recordUpdateSchedule.getValue().getKey().getName(), equalTo("testTriggerKeyName"));

    reset(schedulerListener);

    TriggerDeletedEvent triggerDeletedEvent = new TriggerDeletedEvent(triggerKey);
    EventHelper.asReplicating(() -> eventManager.post(triggerDeletedEvent));

    ArgumentCaptor<TriggerKey> recordDelete = ArgumentCaptor.forClass(TriggerKey.class);
    verify(schedulerListener).jobUnscheduled(recordDelete.capture());
    assertThat(recordDelete.getValue().getName(), equalTo("testTriggerKeyName"));
  }

  @Test
  public void exerciseRunNowTriggerEvents() throws SchedulerException {
    JobKey jobKey = mockJobDetail();
    TriggerKey triggerKey = mockTrigger(jobKey);

    SchedulerListener schedulerListener = mock(SchedulerListener.class);
    underTest.getScheduler().getListenerManager().addSchedulerListener(schedulerListener);

    TriggerCreatedEvent triggerCreatedEvent = new TriggerCreatedEvent(triggerKey);
    EventHelper.asReplicating(() -> eventManager.post(triggerCreatedEvent));

    verifyNoMoreInteractions(schedulerListener);

    TriggerUpdatedEvent triggerUpdatedEvent = new TriggerUpdatedEvent(triggerKey);
    EventHelper.asReplicating(() -> eventManager.post(triggerUpdatedEvent));

    verifyNoMoreInteractions(schedulerListener);

    TriggerDeletedEvent triggerDeletedEvent = new TriggerDeletedEvent(triggerKey);
    EventHelper.asReplicating(() -> eventManager.post(triggerDeletedEvent));

    verifyNoMoreInteractions(schedulerListener);
  }

  @Test
  public void recoveringJobsDoesNotFailWhenTheSchedulerThrowsAnException() throws SchedulerException {
    ListenerManager listenerManager = mock(ListenerManager.class);

    Scheduler oldScheduler = underTest.getScheduler();
    try {
      Scheduler mockScheduler = mock(Scheduler.class);
      underTest.setScheduler(mockScheduler);
      when(mockScheduler.getListenerManager()).thenReturn(listenerManager);

      Pair<Trigger, JobDetail> goodResult = setupJobParameters(mockScheduler, listenerManager, "goodKey");
      Pair<Trigger, JobDetail> exceptionResult = setupJobParameters(mockScheduler, listenerManager, "exceptionKey");
      Pair<Trigger, JobDetail> anotherGoodResult = setupJobParameters(mockScheduler, listenerManager, "anotherGoodKey");

      when(mockScheduler.rescheduleJob(any(TriggerKey.class), eq(exceptionResult.getLeft())))
          .thenThrow(new SchedulerException("THIS IS A TEST EXCEPTION"));

      underTest.recoverJob(goodResult.getLeft(), goodResult.getRight());
      underTest.recoverJob(exceptionResult.getLeft(), exceptionResult.getRight());
      underTest.recoverJob(anotherGoodResult.getLeft(), anotherGoodResult.getRight());

      verify(mockScheduler, times(3)).scheduleJob(any(Trigger.class));
    }
    finally {
      underTest.setScheduler(oldScheduler);
    }
  }

  @Test
  public void attemptingToProgrammaticallyRunATaskWhenTheSchedulerIsPausedThrowsAnException() {
    QuartzTaskInfo quartzTaskInfo = mock(QuartzTaskInfo.class);
    QuartzTaskState quartzTaskState = mock(QuartzTaskState.class);
    underTest.pause();

    assertThrows(Exception.class,
        () -> underTest.runNow("trigger-source", new JobKey("name", "group"), quartzTaskInfo, quartzTaskState));

    verify(quartzTaskInfo, never()).setNexusTaskState(any(), any(), any());
  }

  @Test
  public void checkLogicThatDeterminesIfAJobShouldBeRecovered() throws SchedulerException {
    recoveryTest(1, Now.TYPE, true, TaskState.INTERRUPTED.name());
    recoveryTest(1, Now.TYPE, true, TaskState.FAILED.name());
    recoveryTest(1, Now.TYPE, false, TaskState.INTERRUPTED.name());
    recoveryTest(1, Now.TYPE, false, TaskState.FAILED.name());

    recoveryTest(1, Manual.TYPE, true, TaskState.INTERRUPTED.name());
    recoveryTest(0, Manual.TYPE, false, TaskState.INTERRUPTED.name());
    recoveryTest(0, Manual.TYPE, true, TaskState.FAILED.name());
    recoveryTest(0, Manual.TYPE, false, TaskState.FAILED.name());
  }

  private void recoveryTest(
      int invocationCount,
      String scheduleType,
      boolean requestsRecovery,
      String endState) throws SchedulerException
  {
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(Schedule.SCHEDULE_TYPE, scheduleType);
    jobDataMap.put(TaskConfiguration.LAST_RUN_STATE_END_STATE, endState);

    Trigger trigger = mock(Trigger.class);
    when(trigger.getJobDataMap()).thenReturn(jobDataMap);
    when(trigger.getDescription()).thenReturn("Test description");

    JobDetail jobDetail = mock(JobDetail.class);
    when(jobDetail.requestsRecovery()).thenReturn(requestsRecovery);
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    when(jobDetail.getKey()).thenReturn(new JobKey("keyName"));

    Scheduler oldScheduler = underTest.getScheduler();
    try {
      Scheduler mockScheduler = mock(Scheduler.class);
      underTest.setScheduler(mockScheduler);
      underTest.recoverJob(trigger, jobDetail);
      verify(mockScheduler, times(invocationCount)).scheduleJob(any(Trigger.class));
      reset(mockScheduler);
    }
    finally {
      underTest.setScheduler(oldScheduler);
    }
  }

  @Test
  public void triggerTimeBeforeLastRunTimeDoesNotModifyJob() throws SchedulerException {
    interruptStateTestHelper(
        false,
        DateTime.parse("2002-01-01").toDate(),
        TaskState.OK,
        DateTime.parse("2003-01-01").toDate(),
        null);
  }

  @Test
  public void triggerTimeExistsButNoLastRunExistsSoTheJobIsSetToInterrupted() throws SchedulerException {
    interruptStateTestHelper(
        true,
        DateTime.parse("2002-01-01").toDate(),
        null,
        null,
        null);
  }

  @Test
  public void triggerTimeIsAfterLastRunTimeSoTheJobIsSetToInterrupted() throws SchedulerException {
    interruptStateTestHelper(
        true,
        DateTime.parse("2002-01-01").toDate(),
        TaskState.OK,
        DateTime.parse("2001-01-01").toDate(),
        null);
  }

  @Test
  public void triggerTimeDoesNotExistSoTheJobIsNotModified() throws SchedulerException {
    interruptStateTestHelper(
        false,
        null,
        TaskState.OK,
        DateTime.parse("2001-01-01").toDate(),
        null);
  }

  @Test
  public void triggerTimeExistsButEndStateDoesNotSoTheJobIsSetToAsInterrupted() throws SchedulerException {
    interruptStateTestHelper(
        false,
        null,
        null,
        DateTime.parse("2001-01-01").toDate(),
        null);
  }

  @Test
  public void triggerTimeDoesNotExistNorDoesLastRunTimeSoTheJobIsNotModified() throws SchedulerException {
    interruptStateTestHelper(
        false,
        null,
        null,
        null,
        null);
  }

  @Test
  public void testFindingTaskByTypeId() {
    List<TaskInfo> tasks = List.of(
        taskInfo("0", "type1", Map.of(), TaskState.WAITING),
        taskInfo("1", "type2", Map.of(), TaskState.WAITING));

    DatastoreQuartzSchedulerSPI spyUnderTest = spy(underTest);
    doReturn(tasks).when(spyUnderTest).listsTasks();

    assertThat(spyUnderTest.listsTasks(), hasSize(2));
    assertThat(spyUnderTest.getTaskByTypeId("type2"), equalTo(tasks.get(1)));
  }

  @Test
  public void testFindingTaskByTypeIdAndConfig() {
    List<TaskInfo> tasks = List.of(
        taskInfo("0", "type1", Map.of("foo", "bar"), TaskState.WAITING),
        taskInfo("1", "type1", Map.of("moo", "baz"), TaskState.WAITING),
        taskInfo("2", "type2", Map.of("foo", "bar"), TaskState.WAITING),
        taskInfo("3", "type2", Map.of("moo", "baz"), TaskState.WAITING));

    DatastoreQuartzSchedulerSPI spyUnderTest = spy(underTest);
    doReturn(tasks).when(spyUnderTest).listsTasks();

    assertThat(spyUnderTest.listsTasks(), hasSize(tasks.size()));
    assertThat(spyUnderTest.getTaskByTypeId("type1", Map.of("foo", "bar")), equalTo(tasks.get(0)));
  }

  @Test
  public void testFindingAndSubmittingATaskByTypeId() throws TaskRemovedException {
    List<TaskInfo> tasks = List.of(
        taskInfo("0", "type1", Map.of(), TaskState.RUNNING),
        taskInfo("1", "type2", Map.of(), TaskState.WAITING));

    DatastoreQuartzSchedulerSPI spyUnderTest = spy(underTest);
    doReturn(tasks).when(spyUnderTest).listsTasks();

    assertThat(spyUnderTest.findAndSubmit("type1"), equalTo(true));
    assertThat(spyUnderTest.findAndSubmit("type2"), equalTo(true));
    assertThat(spyUnderTest.findAndSubmit("type3"), equalTo(false));
    verify(tasks.get(0), never()).runNow();
    verify(tasks.get(1), times(1)).runNow();
  }

  @Test
  public void testFindingAndSubmittingATaskByTypeIdAndConfig() throws TaskRemovedException {
    List<TaskInfo> tasks = List.of(
        taskInfo("0", "type1", Map.of("foo", "bar"), TaskState.RUNNING),
        taskInfo("1", "type1", Map.of("moo", "baz"), TaskState.WAITING),
        taskInfo("2", "type2", Map.of("foo", "bar"), TaskState.RUNNING),
        taskInfo("3", "type2", Map.of("moo", "baz"), TaskState.WAITING),
        taskInfo("4", "type3", Map.of("foo", "bar"), TaskState.RUNNING),
        taskInfo("5", "type3", Map.of("moo", "baz"), TaskState.WAITING));

    DatastoreQuartzSchedulerSPI spyUnderTest = spy(underTest);
    doReturn(tasks).when(spyUnderTest).listsTasks();

    assertThat(spyUnderTest.findAndSubmit("type1", Map.of("foo", "bar")), equalTo(true));
    assertThat(spyUnderTest.findAndSubmit("type2", Map.of("moo", "baz")), equalTo(true));
    assertThat(spyUnderTest.findAndSubmit("type3", Map.of("foo", "bar", "moo", "baz")), equalTo(false));
    verify(tasks.get(0), never()).runNow();
    verify(tasks.get(1), never()).runNow();
    verify(tasks.get(2), never()).runNow();
    verify(tasks.get(3)).runNow();
    verify(tasks.get(4), never()).runNow();
    verify(tasks.get(5), never()).runNow();
  }

  private static TaskInfo taskInfo(
      final String id,
      final String typeId,
      final Map<String, String> config,
      final TaskState currentState)
  {
    TaskConfiguration tc = new TaskConfiguration();
    tc.setId(id);
    tc.setTypeId(typeId);
    config.forEach(tc::setString);
    CurrentState tcs = mock(CurrentState.class);
    when(tcs.getState()).thenReturn(currentState);
    TaskInfo ti = mock(TaskInfo.class);
    when(ti.getId()).thenReturn(id);
    when(ti.getTypeId()).thenReturn(typeId);
    when(ti.getConfiguration()).thenReturn(tc);
    when(ti.getCurrentState()).thenReturn(tcs);
    return ti;
  }

  public static Pair<Trigger, JobDetail> setupJobParameters(
      Scheduler scheduler,
      ListenerManager listenerManager,
      String keyName) throws SchedulerException
  {
    JobKey key = new JobKey(keyName, "nexus");

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(Schedule.SCHEDULE_TYPE, Now.TYPE);

    JobDetail jobDetail = mock(JobDetail.class);
    when(scheduler.getJobDetail(key)).thenReturn(jobDetail);
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    when(jobDetail.getKey()).thenReturn(key);
    when(jobDetail.requestsRecovery()).thenReturn(true);

    Trigger trigger = mock(Trigger.class);
    when(scheduler.getTrigger(TriggerKey.triggerKey(key.getName(), key.getGroup()))).thenReturn(trigger);
    when(trigger.getJobDataMap()).thenReturn(jobDataMap);
    when(trigger.getJobKey()).thenReturn(new JobKey("testJobKeyName", "testJobKeyGroup"));

    TriggerKey triggerKey = TriggerKey.triggerKey(keyName, "nexus");
    when(trigger.getKey()).thenReturn(triggerKey);

    TaskConfiguration config = new TaskConfiguration();

    QuartzTaskInfo taskInfo = mock(QuartzTaskInfo.class);
    when(taskInfo.getConfiguration()).thenReturn(config);

    QuartzTaskJobListener jobListener = mock(QuartzTaskJobListener.class);
    when(jobListener.getTaskInfo()).thenReturn(taskInfo);

    when(listenerManager.getJobListener(any())).thenReturn(jobListener);

    return Pair.of(trigger, jobDetail);
  }

  void interruptStateTestHelper(
      boolean shouldBeInterrupted,
      Date lastTriggerDate,
      TaskState endState,
      Date lastRunDate,
      Date shutdownDate) throws SchedulerException
  {

    Date defaultShutdownDate = DateTime.parse("2003-01-01T00:00").toDate();

    if (shutdownDate == null) {
      shutdownDate = defaultShutdownDate;
    }

    JobKey jobKey = new JobKey("testJobKeyName", "testJobKeyGroup");
    Trigger trigger = mock(Trigger.class);
    when(trigger.getPreviousFireTime()).thenReturn(lastTriggerDate);

    JobDataMap jobDataMap = new JobDataMap();
    if (endState != null) {
      jobDataMap.put(TaskConfiguration.LAST_RUN_STATE_END_STATE, endState.name());
      jobDataMap.put(TaskConfiguration.LAST_RUN_STATE_RUN_STARTED, String.valueOf(lastRunDate.getTime()));
      jobDataMap.put(TaskConfiguration.LAST_RUN_STATE_RUN_DURATION, "1000");
    }

    JobDetail jobDetail = mock(JobDetail.class);
    when(jobDetail.getKey()).thenReturn(jobKey);
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

    Scheduler mockScheduler = mock(Scheduler.class);
    when(mockScheduler.getTriggersOfJob(jobKey)).thenAnswer(invocation -> Lists.newArrayList(trigger));

    Scheduler oldScheduler = underTest.getScheduler();
    try {
      underTest.setScheduler(mockScheduler);
      underTest.updateLastRunStateInfo(jobDetail, Optional.of(shutdownDate));
    }
    finally {
      underTest.setScheduler(oldScheduler);
    }

    if (shouldBeInterrupted) {
      assertThat(jobDataMap.get(TaskConfiguration.LAST_RUN_STATE_END_STATE), equalTo(TaskState.INTERRUPTED.name()));
      assertThat(jobDataMap.get(TaskConfiguration.LAST_RUN_STATE_RUN_STARTED),
          equalTo(String.valueOf(lastTriggerDate.getTime())));
      assertThat(jobDataMap.get(TaskConfiguration.LAST_RUN_STATE_RUN_DURATION),
          equalTo(String.valueOf(shutdownDate.getTime() - lastTriggerDate.getTime())));

      verify(mockScheduler).addJob(jobDetail, true, true);
    }
    else {
      verify(mockScheduler, never()).addJob(jobDetail, true, true);
    }
  }

  private JobKey mockJobDetail() throws JobPersistenceException {
    JobKey jobKey = new JobKey("testJobKeyName", "testJobKeyGroup");
    JobDetail jobDetail = mock(JobDetail.class);
    JobDataMap map = new JobDataMap(new Manual().asMap());
    map.put(".id", "my-id");
    map.put(".typeId", "type-id");

    when(jobDetail.getKey()).thenReturn(jobKey);
    when(jobDetail.getDescription()).thenReturn("test job description");
    when(jobDetail.getJobClass()).thenAnswer(invocation -> Job.class);
    when(jobDetail.getJobDataMap()).thenReturn(map);
    when(jobDetail.isDurable()).thenReturn(false);
    when(jobDetail.requestsRecovery()).thenReturn(false);
    when(jobStore.retrieveJob(jobKey)).thenReturn(jobDetail);

    return jobKey;
  }

  private TriggerKey mockTrigger(JobKey jobKey) throws JobPersistenceException {
    OperableTrigger trigger = mock(OperableTrigger.class);
    TriggerKey triggerKey = new TriggerKey("testTriggerKeyName", "testTriggerKeyGroup");

    when(trigger.getKey()).thenReturn(triggerKey);
    when(trigger.getJobKey()).thenReturn(jobKey);
    when(trigger.getDescription()).thenReturn("test trigger description");

    JobDataMap map = new JobDataMap(new Manual().asMap());
    map.put(".id", "my-id");
    map.put(".typeId", "type-id");
    when(trigger.getJobDataMap()).thenReturn(map);

    when(jobStore.retrieveTrigger(triggerKey)).thenReturn(trigger);

    return triggerKey;
  }
}
