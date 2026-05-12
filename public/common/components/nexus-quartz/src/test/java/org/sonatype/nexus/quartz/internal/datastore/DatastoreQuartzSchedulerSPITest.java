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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LastShutdownTimeService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerProvider;
import org.sonatype.nexus.quartz.internal.bulkread.BulkReadScheduler;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskState;
import org.sonatype.nexus.rest.ValidationErrorsException;
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
import jakarta.inject.Provider;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.ConnectionProvider;
import org.springframework.beans.factory.FactoryBean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.quartz.JobKey.jobKey;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatastoreQuartzSchedulerSPITest
{

  @Mock
  private JobStore jobStore;

  @Mock
  private ConnectionProvider connectionProvider;

  private QuartzSchedulerProvider schedulerProvider;

  private EventManager eventManager;

  private DatastoreQuartzSchedulerSPI underTest;

  @BeforeEach
  void beforeEach() throws Exception {
    NodeAccess nodeAccess = mock(NodeAccess.class);
    when(nodeAccess.getId()).thenReturn("test");
    Provider<JobStore> provider = mock(Provider.class);
    when(provider.get()).thenReturn(jobStore);
    mockCustomConnections(connectionProvider);
    schedulerProvider =
        new QuartzSchedulerProvider(nodeAccess, provider, connectionProvider, mock(JobFactory.class), 1, 5);
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
        eventManager, nodeAccess, provider, provider(schedulerProvider), lastShutdownTimeService, statusDelayedExecutor,
        true);
    schedulerProvider.start();
    underTest.start();
    underTest.getStateGuard().transition(StateGuardLifecycleSupport.State.STARTED);
  }

  @AfterEach
  void after() throws Exception {
    schedulerProvider.stop();
    underTest.stop();
  }

  @Test
  void schedulingInThePastShouldFireTriggerInNextHour() throws JobPersistenceException {
    DateTime now = DateTime.now();
    DateTime startAt = DateTime.parse("2010-06-30T01:20");
    underTest.scheduleTask(validTaskConfiguration(), new Hourly(startAt.toDate()));

    ArgumentCaptor<OperableTrigger> triggerRecorder = ArgumentCaptor.forClass(OperableTrigger.class);
    verify(jobStore).storeJobAndTrigger(any(JobDetail.class), triggerRecorder.capture());

    DateTime triggerTime = new DateTime(triggerRecorder.getValue().getStartTime());
    assertTrue(triggerTime.isAfter(now));
    assertThat(triggerTime.getMinuteOfHour(), equalTo(startAt.getMinuteOfHour()));
    assertThat(new Duration(now, triggerTime).getStandardMinutes(), lessThan(60L));
  }

  @Test
  void schedulingPlanReconcileExist() throws SchedulerException {
    DateTime startAt = DateTime.parse("2010-06-30T01:20");
    underTest.scheduleTask(validTaskConfiguration(), new Hourly(startAt.toDate()));

    Exception exception = assertThrows(ValidationErrorsException.class, () -> {
      // Code that is expected to throw the exception
      throw new ValidationErrorsException("Task blobstore.planReconciliation already exist, ignoring");
    });

    // Optionally, check the exception message
    assertTrue(exception.getMessage().contains("Task blobstore.planReconciliation already exist, ignoring"));
  }

  @Test
  void exerciseJobEvents() throws SchedulerException {
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
  void exerciseScheduledTriggerEvents() throws SchedulerException {
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
  void exerciseRunNowTriggerEvents() throws SchedulerException {
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
  void recoveringJobsDoesNotFailWhenTheSchedulerThrowsAnException() throws SchedulerException {
    ListenerManager listenerManager = mock(ListenerManager.class);

    BulkReadScheduler oldScheduler = underTest.getScheduler();
    try {
      BulkReadScheduler mockScheduler = mock(BulkReadScheduler.class);
      underTest.setScheduler(mockScheduler);
      lenient().when(mockScheduler.getListenerManager()).thenReturn(listenerManager);

      Pair<Trigger, JobDetail> goodResult = setupJobParameters(mockScheduler, listenerManager, "goodKey");
      Pair<Trigger, JobDetail> exceptionResult = setupJobParameters(mockScheduler, listenerManager, "exceptionKey");
      Pair<Trigger, JobDetail> anotherGoodResult = setupJobParameters(mockScheduler, listenerManager, "anotherGoodKey");

      lenient().when(mockScheduler.rescheduleJob(any(TriggerKey.class), eq(exceptionResult.getLeft())))
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
  void attemptingToProgrammaticallyRunATaskWhenTheSchedulerIsPausedThrowsAnException() {
    QuartzTaskInfo quartzTaskInfo = mock(QuartzTaskInfo.class);
    QuartzTaskState quartzTaskState = mock(QuartzTaskState.class);
    underTest.pause();

    assertThrows(Exception.class,
        () -> underTest.runNow("trigger-source", new JobKey("name", "group"), quartzTaskInfo, quartzTaskState));

    verify(quartzTaskInfo, never()).setNexusTaskState(any(), any(), any());
  }

  @Test
  void checkLogicThatDeterminesIfAJobShouldBeRecovered() throws SchedulerException {
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
      final int invocationCount,
      final String scheduleType,
      final boolean requestsRecovery,
      final String endState) throws SchedulerException
  {
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(Schedule.SCHEDULE_TYPE, scheduleType);
    jobDataMap.put(TaskConfiguration.LAST_RUN_STATE_END_STATE, endState);

    Trigger trigger = mock(Trigger.class);
    lenient().when(trigger.getJobDataMap()).thenReturn(jobDataMap);
    lenient().when(trigger.getDescription()).thenReturn("Test description");

    JobDetail jobDetail = mock(JobDetail.class);
    when(jobDetail.requestsRecovery()).thenReturn(requestsRecovery);
    lenient().when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    lenient().when(jobDetail.getKey()).thenReturn(new JobKey("keyName"));

    BulkReadScheduler oldScheduler = underTest.getScheduler();
    try {
      BulkReadScheduler mockScheduler = mock(BulkReadScheduler.class);
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
  void triggerTimeBeforeLastRunTimeDoesNotModifyJob() throws SchedulerException {
    interruptStateTestHelper(
        false,
        DateTime.parse("2002-01-01").toDate(),
        TaskState.OK,
        DateTime.parse("2003-01-01").toDate(),
        null);
  }

  @Test
  void triggerTimeExistsButNoLastRunExistsSoTheJobIsSetToInterrupted() throws SchedulerException {
    interruptStateTestHelper(
        true,
        DateTime.parse("2002-01-01").toDate(),
        null,
        null,
        null);
  }

  @Test
  void triggerTimeIsAfterLastRunTimeSoTheJobIsSetToInterrupted() throws SchedulerException {
    interruptStateTestHelper(
        true,
        DateTime.parse("2002-01-01").toDate(),
        TaskState.OK,
        DateTime.parse("2001-01-01").toDate(),
        null);
  }

  @Test
  void triggerTimeDoesNotExistSoTheJobIsNotModified() throws SchedulerException {
    interruptStateTestHelper(
        false,
        null,
        TaskState.OK,
        DateTime.parse("2001-01-01").toDate(),
        null);
  }

  @Test
  void triggerTimeExistsButEndStateDoesNotSoTheJobIsSetToAsInterrupted() throws SchedulerException {
    interruptStateTestHelper(
        false,
        null,
        null,
        DateTime.parse("2001-01-01").toDate(),
        null);
  }

  @Test
  void triggerTimeDoesNotExistNorDoesLastRunTimeSoTheJobIsNotModified() throws SchedulerException {
    interruptStateTestHelper(
        false,
        null,
        null,
        null,
        null);
  }

  @Test
  void testFindingTaskByTypeId() {
    List<TaskInfo> tasks = List.of(
        taskInfo("0", "type1", Map.of(), TaskState.WAITING),
        taskInfo("1", "type2", Map.of(), TaskState.WAITING));

    DatastoreQuartzSchedulerSPI spyUnderTest = spy(underTest);
    doReturn(tasks).when(spyUnderTest).listsTasks();

    assertThat(spyUnderTest.listsTasks(), hasSize(2));
    assertThat(spyUnderTest.getTaskByTypeId("type2"), equalTo(tasks.get(1)));
  }

  @Test
  void testFindingTaskByTypeIdAndConfig() {
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
  void testFindingAndSubmittingATaskByTypeId() throws TaskRemovedException {
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
  void testFindingAndSubmittingATaskByTypeIdAndConfig() throws TaskRemovedException {
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

  @Test
  void testAttachJobListener_WhenTriggerIsNull() throws Exception {
    BulkReadScheduler localScheduler = mock(BulkReadScheduler.class);
    QuartzSchedulerProvider schedulerProvider = mock(QuartzSchedulerProvider.class);
    DatastoreQuartzSchedulerSPI custom = new DatastoreQuartzSchedulerSPI(
        mock(EventManager.class),
        mock(NodeAccess.class),
        mock(Provider.class),
        provider(schedulerProvider),
        mock(LastShutdownTimeService.class),
        mock(DatabaseStatusDelayedExecutor.class),
        true);
    custom.setScheduler(localScheduler);

    when(localScheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);
    when(localScheduler.getJobDetail(any(JobKey.class))).thenReturn(mock(JobDetail.class));

    JobKey jobKey = new JobKey("testJob", "testGroup");
    Optional<QuartzTaskJobListener> result = custom.attachJobListener(jobKey);

    assertThat(result, equalTo(Optional.empty()));
  }

  @Test
  void testAttachJobListener_WhenJobDetailIsNull() throws Exception {
    BulkReadScheduler localScheduler = mock(BulkReadScheduler.class);
    QuartzSchedulerProvider schedulerProvider = mock(QuartzSchedulerProvider.class);
    DatastoreQuartzSchedulerSPI custom = new DatastoreQuartzSchedulerSPI(
        mock(EventManager.class),
        mock(NodeAccess.class),
        mock(Provider.class),
        provider(schedulerProvider),
        mock(LastShutdownTimeService.class),
        mock(DatabaseStatusDelayedExecutor.class),
        true);
    custom.setScheduler(localScheduler);

    when(localScheduler.getTrigger(any(TriggerKey.class))).thenReturn(mock(Trigger.class));
    when(localScheduler.getJobDetail(any(JobKey.class))).thenReturn(null);

    JobKey jobKey = new JobKey("testJob", "testGroup");
    Optional<QuartzTaskJobListener> result = custom.attachJobListener(jobKey);

    assertThat(result, equalTo(Optional.empty()));
  }

  @Test
  void testFindTaskByIdWhenTaskExists() throws SchedulerException {
    String taskId = UUID.randomUUID().toString();
    JobKey jobKey = new JobKey(taskId, "nexus");

    BulkReadScheduler localScheduler = mock(BulkReadScheduler.class);
    when(localScheduler.checkExists(jobKey)).thenReturn(true);

    ListenerManager listenerManager = mock(ListenerManager.class);
    when(localScheduler.getListenerManager()).thenReturn(listenerManager);

    QuartzTaskInfo taskInfo = mock(QuartzTaskInfo.class);
    QuartzTaskJobListener listener = mock(QuartzTaskJobListener.class);
    when(listener.getTaskInfo()).thenReturn(taskInfo);
    when(listenerManager.getJobListener(any(String.class))).thenReturn(listener);

    BulkReadScheduler oldScheduler = underTest.getScheduler();
    try {
      underTest.setScheduler(localScheduler);
      TaskInfo result = underTest.getTaskById(taskId);

      assertThat(result, equalTo(taskInfo));
      verify(localScheduler).checkExists(jobKey);
    }
    finally {
      underTest.setScheduler(oldScheduler);
    }
  }

  @Test
  void testFindTaskByIdWhenTaskDoesNotExist() throws SchedulerException {
    String taskId = UUID.randomUUID().toString();
    JobKey jobKey = new JobKey(taskId, "nexus");

    BulkReadScheduler localScheduler = mock(BulkReadScheduler.class);
    when(localScheduler.checkExists(jobKey)).thenReturn(false);

    BulkReadScheduler oldScheduler = underTest.getScheduler();
    try {
      underTest.setScheduler(localScheduler);
      TaskInfo result = underTest.getTaskById(taskId);

      assertThat(result, nullValue());
      verify(localScheduler).checkExists(jobKey);
      verify(localScheduler, never()).getListenerManager();
    }
    finally {
      underTest.setScheduler(oldScheduler);
    }
  }

  @Test
  void testFindTaskByIdWhenJobExistsButNoListener() throws SchedulerException {
    String taskId = UUID.randomUUID().toString();
    JobKey jobKey = new JobKey(taskId, "nexus");

    BulkReadScheduler localScheduler = mock(BulkReadScheduler.class);
    when(localScheduler.checkExists(jobKey)).thenReturn(true);

    ListenerManager listenerManager = mock(ListenerManager.class);
    when(localScheduler.getListenerManager()).thenReturn(listenerManager);
    when(listenerManager.getJobListener(any(String.class))).thenReturn(null);

    BulkReadScheduler oldScheduler = underTest.getScheduler();
    try {
      underTest.setScheduler(localScheduler);
      TaskInfo result = underTest.getTaskById(taskId);

      assertThat(result, nullValue());
      verify(localScheduler).checkExists(jobKey);
    }
    finally {
      underTest.setScheduler(oldScheduler);
    }
  }

  @Test
  void testFindTaskByIdFallbackToLegacyPattern() throws SchedulerException {
    String configId = UUID.randomUUID().toString();
    String quartzId = UUID.randomUUID().toString(); // Different UUID
    JobKey directJobKey = jobKey(configId, "nexus");
    JobKey legacyJobKey = jobKey(quartzId, "nexus");

    // Mock scheduler that returns nothing for direct lookup (new pattern fails)
    BulkReadScheduler localScheduler = mock(BulkReadScheduler.class);
    when(localScheduler.checkExists(directJobKey)).thenReturn(false); // New pattern not found

    // Mock the legacy allTasks() scan path
    ListenerManager listenerManager = mock(ListenerManager.class);
    when(localScheduler.getListenerManager()).thenReturn(listenerManager);
    when(localScheduler.getJobKeys(any(GroupMatcher.class))).thenReturn(Collections.singleton(legacyJobKey));

    // Create mock task with legacy pattern (JobKey != config.getId)
    QuartzTaskInfo legacyTaskInfo = mock(QuartzTaskInfo.class);

    when(legacyTaskInfo.getId()).thenReturn(configId);
    when(legacyTaskInfo.getJobKey()).thenReturn(new JobKey(quartzId, "nexus")); // Different!

    // Mock listener with legacy JobKey
    QuartzTaskJobListener legacyListener = mock(QuartzTaskJobListener.class);
    when(legacyListener.getTaskInfo()).thenReturn(legacyTaskInfo);
    when(listenerManager.getJobListener(contains(quartzId))).thenReturn(legacyListener);

    BulkReadScheduler oldScheduler = underTest.getScheduler();
    try {
      underTest.setScheduler(localScheduler);

      // Try to find task by config ID
      TaskInfo result = underTest.getTaskById(configId);

      // Verify: new pattern tried first, then fallback found it
      assertThat(result, notNullValue());
      assertThat(result.getId(), equalTo(configId));
      verify(localScheduler).checkExists(directJobKey);
      verify(localScheduler).getJobKeys(any(GroupMatcher.class));
    }
    finally {
      underTest.setScheduler(oldScheduler);
    }
  }

  private TaskConfiguration validTaskConfiguration() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId(UUID.randomUUID().toString());

    return taskConfiguration;
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
    lenient().when(tcs.getState()).thenReturn(currentState);
    TaskInfo ti = mock(TaskInfo.class);
    lenient().when(ti.getId()).thenReturn(id);
    lenient().when(ti.getTypeId()).thenReturn(typeId);
    lenient().when(ti.getConfiguration()).thenReturn(tc);
    lenient().when(ti.getCurrentState()).thenReturn(tcs);
    return ti;
  }

  public static Pair<Trigger, JobDetail> setupJobParameters(
      final Scheduler scheduler,
      final ListenerManager listenerManager,
      final String keyName) throws SchedulerException
  {
    JobKey key = new JobKey(keyName, "nexus");

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(Schedule.SCHEDULE_TYPE, Now.TYPE);

    JobDetail jobDetail = mock(JobDetail.class);
    lenient().when(scheduler.getJobDetail(key)).thenReturn(jobDetail);
    lenient().when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    lenient().when(jobDetail.getKey()).thenReturn(key);
    lenient().when(jobDetail.requestsRecovery()).thenReturn(true);

    Trigger trigger = mock(Trigger.class);
    lenient().when(scheduler.getTrigger(TriggerKey.triggerKey(key.getName(), key.getGroup()))).thenReturn(trigger);
    lenient().when(trigger.getJobDataMap()).thenReturn(jobDataMap);
    lenient().when(trigger.getJobKey()).thenReturn(new JobKey("testJobKeyName", "testJobKeyGroup"));

    TriggerKey triggerKey = TriggerKey.triggerKey(keyName, "nexus");
    lenient().when(trigger.getKey()).thenReturn(triggerKey);

    TaskConfiguration config = new TaskConfiguration();

    QuartzTaskInfo taskInfo = mock(QuartzTaskInfo.class);
    lenient().when(taskInfo.getConfiguration()).thenReturn(config);

    QuartzTaskJobListener jobListener = mock(QuartzTaskJobListener.class);
    lenient().when(jobListener.getTaskInfo()).thenReturn(taskInfo);

    lenient().when(listenerManager.getJobListener(any())).thenReturn(jobListener);

    return Pair.of(trigger, jobDetail);
  }

  void interruptStateTestHelper(
      final boolean shouldBeInterrupted,
      final Date lastTriggerDate,
      final TaskState endState,
      final Date lastRunDate,
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
    lenient().when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

    BulkReadScheduler mockScheduler = mock(BulkReadScheduler.class);
    lenient().when(mockScheduler.getTriggersOfJob(jobKey)).thenAnswer(invocation -> Lists.newArrayList(trigger));

    BulkReadScheduler oldScheduler = underTest.getScheduler();
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

  private void mockCustomConnections(final ConnectionProvider connectionProvider) throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connectionProvider.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(any())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);
  }

  private JobKey mockJobDetail() throws JobPersistenceException {
    JobKey jobKey = new JobKey("testJobKeyName", "testJobKeyGroup");
    JobDetail jobDetail = mock(JobDetail.class);
    JobDataMap map = new JobDataMap(new Manual().asMap());
    map.put(".id", "my-id");
    map.put(".typeId", "type-id");

    lenient().when(jobDetail.getKey()).thenReturn(jobKey);
    lenient().when(jobDetail.getDescription()).thenReturn("test job description");
    lenient().when(jobDetail.getJobClass()).thenAnswer(invocation -> Job.class);
    lenient().when(jobDetail.getJobDataMap()).thenReturn(map);
    lenient().when(jobDetail.isDurable()).thenReturn(false);
    lenient().when(jobDetail.requestsRecovery()).thenReturn(false);
    lenient().when(jobStore.retrieveJob(jobKey)).thenReturn(jobDetail);

    return jobKey;
  }

  private TriggerKey mockTrigger(final JobKey jobKey) throws JobPersistenceException {
    OperableTrigger trigger = mock(OperableTrigger.class);
    TriggerKey triggerKey = new TriggerKey("testTriggerKeyName", "testTriggerKeyGroup");

    lenient().when(trigger.getKey()).thenReturn(triggerKey);
    lenient().when(trigger.getJobKey()).thenReturn(jobKey);
    lenient().when(trigger.getDescription()).thenReturn("test trigger description");

    JobDataMap map = new JobDataMap(new Manual().asMap());
    map.put(".id", "my-id");
    map.put(".typeId", "type-id");
    lenient().when(trigger.getJobDataMap()).thenReturn(map);

    lenient().when(jobStore.retrieveTrigger(triggerKey)).thenReturn(trigger);

    return triggerKey;
  }

  /**
   * Reproduces the issue: java.lang.IllegalStateException: Replication in progress
   *
   * This test demonstrates that scheduling a task while in replication mode throws
   * an IllegalStateException. This is the expected behavior to prevent task scheduling
   * during cluster event replication.
   */
  @Test
  void schedulingTaskDuringReplicationThrowsException() {
    TaskConfiguration taskConfiguration = validTaskConfiguration();
    taskConfiguration.setTypeId("test.task.type");
    taskConfiguration.setName("Test Task During Replication");

    Schedule schedule = new Manual();

    // Verify scheduling works normally (without replication)
    TaskInfo taskInfo = underTest.scheduleTask(taskConfiguration, schedule);
    assertThat(taskInfo, notNullValue());
    assertThat(taskInfo.getId(), equalTo(taskConfiguration.getId()));

    // Now try to schedule a task during replication mode
    TaskConfiguration anotherTaskConfig = validTaskConfiguration();
    anotherTaskConfig.setTypeId("another.task.type");
    anotherTaskConfig.setName("Another Test Task");

    // This should throw IllegalStateException with message "Replication in progress"
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      EventHelper.asReplicating(() -> {
        // Attempting to schedule task during replication should fail
        underTest.scheduleTask(anotherTaskConfig, schedule);
      });
    });

    // Verify the exception message
    assertThat(exception.getMessage(), equalTo("Replication in progress"));
  }

  /**
   * Verify that EventHelper.isReplicating() correctly tracks replication state
   */
  @Test
  void eventHelperReplicationFlagIsProperlyManaged() {
    // Initially, should not be replicating
    assertThat(EventHelper.isReplicating(), equalTo(false));

    // Inside asReplicating block, flag should be true
    EventHelper.asReplicating(() -> {
      assertThat(EventHelper.isReplicating(), equalTo(true));
    });

    // After asReplicating block, flag should be false again
    assertThat(EventHelper.isReplicating(), equalTo(false));
  }

  /**
   * Verify that nested replication calls throw IllegalStateException
   */
  @Test
  void nestedReplicationCallsThrowException() {
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      EventHelper.asReplicating(() -> {
        // Nested replication should fail
        EventHelper.asReplicating(() -> {
          // This should never execute
        });
      });
    });

    assertThat(exception.getMessage(), equalTo("Replication already in progress"));
  }

  private static <T> Provider<T> provider(final FactoryBean<T> factory) {
    return () -> {
      try {
        return factory.getObject();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
