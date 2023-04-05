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
package org.sonatype.nexus.quartz.internal.datastore

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventHelper
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.log.LastShutdownTimeService
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.quartz.internal.QuartzSchedulerProvider
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener
import org.sonatype.nexus.quartz.internal.task.QuartzTaskState
import org.sonatype.nexus.scheduling.CurrentState
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskState
import org.sonatype.nexus.scheduling.schedule.Daily
import org.sonatype.nexus.scheduling.schedule.Hourly
import org.sonatype.nexus.scheduling.schedule.Manual
import org.sonatype.nexus.scheduling.schedule.Now
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.testcommon.event.SimpleEventManager
import org.sonatype.nexus.thread.DatabaseStatusDelayedExecutor

import com.google.common.collect.Lists
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.ArgumentCaptor
import org.quartz.Job
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.ListenerManager
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SchedulerListener
import org.quartz.Trigger
import org.quartz.TriggerKey
import org.quartz.spi.JobFactory
import org.quartz.spi.JobStore
import org.quartz.spi.OperableTrigger

import static junit.framework.TestCase.assertEquals
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.mockito.Mockito.*
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED
import static org.sonatype.nexus.scheduling.TaskConfiguration.LAST_RUN_STATE_END_STATE
import static org.sonatype.nexus.scheduling.TaskConfiguration.LAST_RUN_STATE_RUN_DURATION
import static org.sonatype.nexus.scheduling.TaskConfiguration.LAST_RUN_STATE_RUN_STARTED
import static org.sonatype.nexus.scheduling.TaskState.FAILED
import static org.sonatype.nexus.scheduling.TaskState.INTERRUPTED
import static org.sonatype.nexus.scheduling.TaskState.OK
import static org.sonatype.nexus.scheduling.TaskState.RUNNING
import static org.sonatype.nexus.scheduling.TaskState.WAITING

/**
 * {@link QuartzSchedulerSPI} tests.
 *
 * @since 3.0
 */
class DatastoreQuartzSchedulerSPITest
    extends TestSupport
{
  @Rule
  public final ExpectedException thrown = ExpectedException.none()

  DatastoreQuartzSchedulerSPI underTest

  JobStore jobStore

  QuartzSchedulerProvider scheduler

  EventManager eventManager

  @Before
  void before() {
    def nodeAccess = mock(NodeAccess)
    when(nodeAccess.id).thenReturn('test')
    def provider = mock(Provider)
    jobStore = mock(JobStore)
    when(provider.get()).thenReturn(jobStore)
    scheduler = new QuartzSchedulerProvider(nodeAccess, provider, mock(JobFactory), 1, 5);
    eventManager = new SimpleEventManager()

    def lastShutdownTimeService = mock(LastShutdownTimeService)
    when(lastShutdownTimeService.estimateLastShutdownTime()).thenReturn(Optional.empty())

    def statusDelayedExecutor = mock(DatabaseStatusDelayedExecutor.class)
    doAnswer({ it.getArguments()[0].run() }).when(statusDelayedExecutor).execute(any(Runnable.class))

    underTest = new DatastoreQuartzSchedulerSPI(
        eventManager, nodeAccess, provider, scheduler, lastShutdownTimeService, statusDelayedExecutor, true
    )
    scheduler.start()
    underTest.start()
    underTest.states.current = STARTED
  }

  @After
  void after() {
    scheduler.stop()
    underTest.stop()
  }

  @Test
  void 'Scheduling in the past should fire trigger in next hour'() {
    def now = DateTime.now()
    def startAt = DateTime.parse('2010-06-30T01:20')
    underTest.scheduleTask(new TaskConfiguration(id: 'test', typeId: 'test'), new Hourly(startAt.toDate()))

    def triggerRecorder = ArgumentCaptor.forClass(OperableTrigger)
    verify(jobStore).storeJobAndTrigger(any(JobDetail), triggerRecorder.capture())


    def triggerTime = new DateTime(triggerRecorder.getValue().startTime)
    // trigger should be in the future
    assert triggerTime.isAfter(now)
    // trigger should have the same minute as start time
    assert triggerTime.minuteOfHour() == startAt.minuteOfHour()
    // trigger should fire in the next hour
    assert new Duration(now, triggerTime).standardMinutes < 60
  }

  @Test
  void 'Exercise job events'() {
    def jobKey = mockJobDetail()
    mockTrigger(jobKey, new Manual())

    def schedulerListener = mock(SchedulerListener)
    underTest.scheduler.getListenerManager().addSchedulerListener(schedulerListener)

    Trigger trigger = mock(OperableTrigger)
    JobDataMap map = new JobDataMap(new Now().asMap());
    map.put(".id", "my-id")
    map.put(".typeId", "type-id")
    when(trigger.getJobDataMap()).thenReturn(map)
    when(jobStore.retrieveTrigger(any())).thenReturn(trigger)

    eventManager.register(underTest)

    // on(JobCreatedEvent)

    def jobCreatedEvent = new JobCreatedEvent(new JobKey('testJobKeyName', 'testJobKeyGroup'))
    EventHelper.asReplicating({ eventManager.post(jobCreatedEvent) })

    def recordCreate = ArgumentCaptor.forClass(JobDetail)
    verify(schedulerListener).jobAdded(recordCreate.capture())
    assert recordCreate.value.key.name == 'testJobKeyName'

    reset(schedulerListener)

    // on(JobUpdatedEvent)

    def jobUpdateEvent = new JobUpdatedEvent(new JobKey('testJobKeyName', 'testJobKeyGroup'))
    EventHelper.asReplicating({ eventManager.post(jobUpdateEvent) })

    def recordUpdate = ArgumentCaptor.forClass(JobDetail)
    verify(schedulerListener).jobAdded(recordUpdate.capture())
    assert recordUpdate.value.key.name == 'testJobKeyName'

    reset(schedulerListener)

    // on(JobDeletedEvent)

    def jobDeletedEvent = new JobDeletedEvent(new JobKey('testJobKeyName', 'testJobKeyGroup'))
    EventHelper.asReplicating({ eventManager.post(jobDeletedEvent) })

    def recordDelete = ArgumentCaptor.forClass(JobKey)
    verify(schedulerListener).jobDeleted(recordDelete.capture())
    assert recordDelete.value.name == 'testJobKeyName'
  }

  @Test
  void 'Exercise scheduled trigger events'() {
    def jobKey = mockJobDetail()
    def startAt = DateTime.parse('2010-06-30T01:20')
    def triggerKey = mockTrigger(jobKey, new Daily(startAt.toDate()))

    def schedulerListener = mock(SchedulerListener)
    underTest.scheduler.getListenerManager().addSchedulerListener(schedulerListener)

    eventManager.register(underTest)

    // on(TriggerCreatedEvent)

    def triggerCreatedEvent = new TriggerCreatedEvent(triggerKey)
    EventHelper.asReplicating({ eventManager.post(triggerCreatedEvent) })

    def recordCreate = ArgumentCaptor.forClass(Trigger)
    verify(schedulerListener).jobScheduled(recordCreate.capture())
    assert recordCreate.value.key.name == 'testTriggerKeyName'

    reset(schedulerListener)

    // on(TriggerUpdatedEvent)

    def triggerUpdatedEvent = new TriggerUpdatedEvent(triggerKey)
    EventHelper.asReplicating({ eventManager.post(triggerUpdatedEvent) })

    def recordUpdateUnschedule = ArgumentCaptor.forClass(TriggerKey)
    verify(schedulerListener).jobUnscheduled(recordUpdateUnschedule.capture())
    assert recordUpdateUnschedule.value.name == 'testTriggerKeyName'

    def recordUpdateSchedule = ArgumentCaptor.forClass(Trigger)
    verify(schedulerListener).jobScheduled(recordUpdateSchedule.capture())
    assert recordUpdateSchedule.value.key.name == 'testTriggerKeyName'

    reset(schedulerListener)

    // on(TriggerDeletedEvent)

    def triggerDeletedEvent = new TriggerDeletedEvent(triggerKey)
    EventHelper.asReplicating({ eventManager.post(triggerDeletedEvent) })

    def recordDelete = ArgumentCaptor.forClass(TriggerKey)
    verify(schedulerListener).jobUnscheduled(recordDelete.capture())
    assert recordDelete.value.name == 'testTriggerKeyName'
  }

  @Test
  void 'Exercise run-now trigger events'() {
    def jobKey = mockJobDetail()
    def triggerKey = mockTrigger(jobKey, new Now())

    def schedulerListener = mock(SchedulerListener)
    underTest.scheduler.getListenerManager().addSchedulerListener(schedulerListener)

    // on(TriggerCreatedEvent)

    def triggerCreatedEvent = new TriggerCreatedEvent(triggerKey)
    EventHelper.asReplicating({ eventManager.post(triggerCreatedEvent) })

    verifyNoMoreInteractions(schedulerListener) // run-now triggers don't affect remote schedulerListeners

    // on(TriggerUpdatedEvent)

    def triggerUpdatedEvent = new TriggerUpdatedEvent(triggerKey)
    EventHelper.asReplicating({ eventManager.post(triggerUpdatedEvent) })

    verifyNoMoreInteractions(schedulerListener) // run-now triggers don't affect remote schedulerListeners

    // on(TriggerDeletedEvent)

    def triggerDeletedEvent = new TriggerDeletedEvent(triggerKey)
    EventHelper.asReplicating({ eventManager.post(triggerDeletedEvent) })

    verifyNoMoreInteractions(schedulerListener) // run-now triggers don't affect remote schedulerListeners
  }


  @Test
  void 'Recovering jobs does not fail when the scheduler throws an exception'() {
    def listenerManager = mock(ListenerManager)

    def oldScheduler = underTest.@scheduler
    try {
      def scheduler = mock(Scheduler)
      underTest.@scheduler = scheduler
      when(scheduler.getListenerManager()).thenReturn(listenerManager)

      def (goodTrigger, goodDetail) = setupJobParameters(scheduler, listenerManager, 'goodKey')
      def (exceptionTrigger, exceptionDetail) = setupJobParameters(scheduler, listenerManager, 'exceptionKey')
      def (anotherGoodTrigger, anotherGoodDetail) = setupJobParameters(scheduler, listenerManager, 'anotherGoodKey')

      when(scheduler.rescheduleJob(any(TriggerKey), eq(exceptionTrigger))).
          thenThrow(new SchedulerException("THIS IS A TEST EXCEPTION"))

      underTest.recoverJob(goodTrigger, goodDetail)
      underTest.recoverJob(exceptionTrigger, exceptionDetail)
      underTest.recoverJob(anotherGoodTrigger, anotherGoodDetail)

      verify(scheduler, times(3)).scheduleJob(any(Trigger))
    }
    finally {
      underTest.@scheduler = oldScheduler
    }
  }

  @Test
  void 'Attempting to programmatically run a task when the scheduler is paused throws an exception'() {
    thrown.expect(IllegalStateException.class)
    def quartzTaskInfo = mock(QuartzTaskInfo)
    def quartzTaskState = mock(QuartzTaskState)
    underTest.pause()
    underTest.runNow('trigger-source', new JobKey('name', 'group'), quartzTaskInfo, quartzTaskState)
    verify(quartzTaskInfo, never()).setNexusTaskState(isNotNull(), isNotNull(), isNotNull(), isNotNull())
  }

  @Test
  void 'check logic that determines if a job should be recovered'() {
    recoveryTest(1, Now.TYPE, true, INTERRUPTED.name())
    recoveryTest(1, Now.TYPE, true, FAILED.name())
    recoveryTest(1, Now.TYPE, false, INTERRUPTED.name())
    recoveryTest(1, Now.TYPE, false, FAILED.name())

    recoveryTest(1, Manual.TYPE, true, INTERRUPTED.name())
    recoveryTest(0, Manual.TYPE, false, INTERRUPTED.name())
    recoveryTest(0, Manual.TYPE, true, FAILED.name())
    recoveryTest(0, Manual.TYPE, false, FAILED.name())
  }

  private void recoveryTest(int invocationCount, String scheduleType, boolean requestsRecovery, String endState) {
    JobDataMap jobDataMap = new JobDataMap()
    jobDataMap.put(Schedule.SCHEDULE_TYPE, scheduleType)
    jobDataMap.put(LAST_RUN_STATE_END_STATE, endState)

    def trigger = mock(Trigger)
    when(trigger.getJobDataMap()).thenReturn(jobDataMap)
    when(trigger.getDescription()).thenReturn("Test description")

    def jobDetail = mock(JobDetail)
    when(jobDetail.requestsRecovery()).thenReturn(requestsRecovery)
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap)
    when(jobDetail.getKey()).thenReturn(new JobKey("keyName"))

    def oldScheduler = underTest.@scheduler
    try {
      def scheduler = mock(Scheduler)
      underTest.@scheduler = scheduler
      underTest.recoverJob(trigger, jobDetail)
      verify(scheduler, times(invocationCount)).scheduleJob(any(Trigger))
      reset(scheduler)
    }
    finally {
      underTest.@scheduler = oldScheduler
    }
  }

  @Test
  void 'Trigger time before last run time does not modify job'() {
    interruptStateTestHelper(
        false,
        DateTime.parse("2002-01-01").toDate(),
        OK,
        DateTime.parse("2003-01-01").toDate()
    )
  }

  @Test
  void 'Trigger time exists but no last run exists so the job is set to interrupted'() {
    interruptStateTestHelper(
        true,
        DateTime.parse("2002-01-01").toDate(),
        null,
        null
    )
  }

  @Test
  void 'Trigger time is after last run time so the job is set to interrupted'() {
    interruptStateTestHelper(
        true,
        DateTime.parse("2002-01-01").toDate(),
        OK,
        DateTime.parse("2001-01-01").toDate()
    )
  }

  @Test
  void 'Trigger time does not exist so the job is not modified'() {
    interruptStateTestHelper(
        false,
        null,
        OK,
        DateTime.parse("2001-01-01").toDate()
    )
  }

  @Test
  void 'Trigger time exists but end state does not, so the job is set to as INTERRUPTED'() {
    interruptStateTestHelper(
        false,
        null,
        null,
        DateTime.parse("2001-01-01").toDate()
    )
  }

  @Test
  void 'Trigger time does not exist nor does last run time so the job is not modified'() {
    interruptStateTestHelper(
        false,
        null,
        null,
        null
    )
  }

  @Test
  void 'Test finding task by type id'() {
    def tasks = [
      taskInfo('0', 'type1', [:], WAITING),
      taskInfo('1', 'type2', [:], WAITING)
    ]

    def spyUnderTest = spy(underTest)
    doReturn(tasks).when(spyUnderTest).listsTasks()

    assertThat(spyUnderTest.listsTasks(), hasSize(2))
    assertThat(spyUnderTest.getTaskByTypeId('type2'), equalTo(tasks[1]))
  }

  @Test
  void 'Test finding task by type id and config'() {
    def tasks = [
        taskInfo('0', 'type1', [foo: 'bar'], WAITING),
        taskInfo('1', 'type1', [moo: 'baz'], WAITING),
        taskInfo('2', 'type2', [foo: 'bar'], WAITING),
        taskInfo('3', 'type2', [moo: 'baz'], WAITING)
    ]

    def spyUnderTest = spy(underTest)
    doReturn(tasks).when(spyUnderTest).listsTasks()

    assertThat(spyUnderTest.listsTasks(), hasSize(tasks.size()))
    assertThat(spyUnderTest.getTaskByTypeId('type1', [foo: 'bar']), equalTo(tasks[0]))
  }

  @Test
  void 'Test finding and submitting a task by type id'() {
    def tasks = [
        taskInfo('0', 'type1', [:], RUNNING),
        taskInfo('1', 'type2', [:], WAITING)
    ]

    def spyUnderTest = spy(underTest)
    doReturn(tasks).when(spyUnderTest).listsTasks()

    assertThat(spyUnderTest.findAndSubmit('type1'), equalTo(true))
    assertThat(spyUnderTest.findAndSubmit('type2'), equalTo(true))
    assertThat(spyUnderTest.findAndSubmit('type3'), equalTo(false))
    verify(tasks[0], never()).runNow()
    verify(tasks[1], times(1)).runNow()
  }

  @Test
  void 'Test finding and submitting a task by type id and config'() {
    def tasks = [
        taskInfo('0', 'type1', [foo: 'bar'], RUNNING),
        taskInfo('1', 'type1', [moo: 'baz'], WAITING),
        taskInfo('2', 'type2', [foo: 'bar'], RUNNING),
        taskInfo('3', 'type2', [moo: 'baz'], WAITING),
        taskInfo('4', 'type3', [foo: 'bar'], RUNNING),
        taskInfo('5', 'type3', [moo: 'baz'], WAITING)
    ]

    def spyUnderTest = spy(underTest)
    doReturn(tasks).when(spyUnderTest).listsTasks()

    assertThat(spyUnderTest.findAndSubmit('type1', [foo: 'bar']), equalTo(true))
    assertThat(spyUnderTest.findAndSubmit('type2', [moo: 'baz']), equalTo(true))
    assertThat(spyUnderTest.findAndSubmit('type3', [foo: 'bar', moo: 'baz']), equalTo(false))
    verify(tasks[0], never()).runNow()
    verify(tasks[1], never()).runNow()
    verify(tasks[2], never()).runNow()
    verify(tasks[3], times(1)).runNow()
    verify(tasks[4], never()).runNow()
    verify(tasks[5], never()).runNow()
  }

  static taskInfo(final String id, final String typeId, final Map<String, String> config, final TaskState currentState)
  {
    def tc = new TaskConfiguration(id: id, typeId: typeId)
    config.each { k, v -> tc.setString(k, v)}
    def tcs = mock(CurrentState)
    when(tcs.state).thenReturn(currentState)
    def ti = mock(TaskInfo)
    when(ti.id).thenReturn(id)
    when(ti.typeId).thenReturn(typeId)
    when(ti.configuration).thenReturn(tc)
    when(ti.currentState).thenReturn(tcs)
    return ti
  }

  def static setupJobParameters(Scheduler scheduler, ListenerManager listenerManager, String keyName) {
    def key = new JobKey(keyName, 'nexus')

    JobDataMap jobDataMap = new JobDataMap()
    jobDataMap.put(Schedule.SCHEDULE_TYPE, Now.TYPE)

    JobDetail jobDetail = mock(JobDetail)
    when(scheduler.getJobDetail(eq(key))).thenReturn(jobDetail)
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap)
    when(jobDetail.getKey()).thenReturn(key)
    when(jobDetail.requestsRecovery()).thenReturn(true)

    Trigger trigger = mock(Trigger)
    when(scheduler.getTrigger(eq(TriggerKey.triggerKey(key.getName(), key.getGroup())))).thenReturn(trigger)
    when(trigger.getJobDataMap()).thenReturn(jobDataMap)
    when(trigger.getJobKey()).thenReturn(new JobKey('testJobKeyName', 'testJobKeyGroup'))

    TriggerKey triggerKey = TriggerKey.triggerKey(keyName, 'nexus')
    when(trigger.getKey()).thenReturn(triggerKey)

    def config = new TaskConfiguration()

    def taskInfo = mock(QuartzTaskInfo)
    when(taskInfo.getConfiguration()).thenReturn(config)

    def jobListener = mock(QuartzTaskJobListener)
    when(jobListener.taskInfo).thenReturn(taskInfo)

    when(listenerManager.getJobListener(any())).thenReturn(jobListener)

    return [trigger, jobDetail]
  }

  void interruptStateTestHelper(
      boolean shouldBeInterrupted,
      Date lastTriggerDate,
      TaskState endState,
      Date lastRunDate,
      Date shutdownDate = DateTime.parse("2003-01-01T00:00").toDate())
  {

    def jobKey = new JobKey('testJobKeyName', 'testJobKeyGroup')
    def trigger = mock(Trigger)
    when(trigger.getPreviousFireTime()).thenReturn(lastTriggerDate)

    JobDataMap jobDataMap = new JobDataMap()
    if (endState != null) {
      jobDataMap.put(LAST_RUN_STATE_END_STATE, endState.name())
      jobDataMap.put(LAST_RUN_STATE_RUN_STARTED, String.valueOf(lastRunDate.time))
      jobDataMap.put(LAST_RUN_STATE_RUN_DURATION, "1000")
    }

    def jobDetail = mock(JobDetail)
    when(jobDetail.getKey()).thenReturn(jobKey)
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap)

    def scheduler = mock(Scheduler)
    when(scheduler.getTriggersOfJob(eq(jobKey))).thenReturn(Lists.newArrayList(trigger))

    def old = underTest.@scheduler
    try {
      underTest.@scheduler = scheduler
      underTest.updateLastRunStateInfo(jobDetail, Optional.of(shutdownDate))
    }
    finally {
      underTest.@scheduler = old
    }

    if (shouldBeInterrupted) {
      assertEquals(INTERRUPTED.name(), jobDataMap.get(LAST_RUN_STATE_END_STATE),)
      assertEquals(lastTriggerDate.time.toString(), jobDataMap.get(LAST_RUN_STATE_RUN_STARTED),)
      assertEquals((shutdownDate.time - lastTriggerDate.time).toString(), jobDataMap.get(LAST_RUN_STATE_RUN_DURATION))

      verify(scheduler, times(1)).addJob(jobDetail, true, true)
    }
    else {
      verify(scheduler, times(0)).addJob(jobDetail, true, true)
    }

  }

  private JobKey mockJobDetail() {
    def jobKey = new JobKey('testJobKeyName', 'testJobKeyGroup')
    def jobDetail = mock(JobDetail)
    JobDataMap map = new JobDataMap(new Manual().asMap());
    map.put(".id", "my-id")
    map.put(".typeId", "type-id")

    when(jobDetail.key).thenReturn(jobKey)
    when(jobDetail.description).thenReturn('test job description')
    when(jobDetail.jobClass).thenReturn(Job)
    when(jobDetail.jobDataMap).thenReturn(map)
    when(jobDetail.durable).thenReturn(false)
    when(jobDetail.requestsRecovery()).thenReturn(false)
    when(jobStore.retrieveJob(jobKey)).thenReturn(jobDetail)

    return jobKey
  }

  private TriggerKey mockTrigger(JobKey jobKey, Schedule schedule) {
    def trigger = mock(OperableTrigger)
    def triggerKey = new TriggerKey('testTriggerKeyName', 'testTriggerKeyGroup')

    when(trigger.key).thenReturn(triggerKey)
    when(trigger.jobKey).thenReturn(jobKey)
    when(trigger.description).thenReturn('test trigger description')

    JobDataMap map = new JobDataMap(new Manual().asMap());
    map.put(".id", "my-id")
    map.put(".typeId", "type-id")
    when(trigger.jobDataMap).thenReturn(map)

    when(jobStore.retrieveTrigger(triggerKey)).thenReturn(trigger);

   return triggerKey;
  }
}
