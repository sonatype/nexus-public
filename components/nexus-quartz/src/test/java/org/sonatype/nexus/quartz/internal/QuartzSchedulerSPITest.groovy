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
package org.sonatype.nexus.quartz.internal

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.log.LastShutdownTimeService
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.quartz.internal.orient.JobCreatedEvent
import org.sonatype.nexus.quartz.internal.orient.JobDeletedEvent
import org.sonatype.nexus.quartz.internal.orient.JobDetailEntity
import org.sonatype.nexus.quartz.internal.orient.JobUpdatedEvent
import org.sonatype.nexus.quartz.internal.orient.TriggerCreatedEvent
import org.sonatype.nexus.quartz.internal.orient.TriggerDeletedEvent
import org.sonatype.nexus.quartz.internal.orient.TriggerEntity
import org.sonatype.nexus.quartz.internal.orient.TriggerUpdatedEvent
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJobListener
import org.sonatype.nexus.quartz.internal.task.QuartzTaskState
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo.EndState
import org.sonatype.nexus.scheduling.schedule.Daily
import org.sonatype.nexus.scheduling.schedule.Hourly
import org.sonatype.nexus.scheduling.schedule.Manual
import org.sonatype.nexus.scheduling.schedule.Now
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.testcommon.event.SimpleEventManager

import com.google.common.collect.Lists
import com.google.common.collect.Sets
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
import org.quartz.SchedulerListener
import org.quartz.Trigger
import org.quartz.TriggerKey
import org.quartz.spi.JobStore
import org.quartz.spi.OperableTrigger

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Matchers.anyObject
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.reset
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoMoreInteractions
import static org.mockito.Mockito.when
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskState.LAST_RUN_STATE_END_STATE
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskState.LAST_RUN_STATE_RUN_STARTED
import static org.sonatype.nexus.quartz.internal.task.QuartzTaskState.LAST_RUN_STATE_RUN_DURATION

/**
 * {@link QuartzSchedulerSPI} tests.
 *
 * @since 3.0
 */
class QuartzSchedulerSPITest
    extends TestSupport
{
  @Rule
  public final ExpectedException thrown = ExpectedException.none()

  QuartzSchedulerSPI underTest

  JobStore jobStore

  EventManager eventManager

  @Before
  void before() {
    def nodeAccess = mock(NodeAccess)
    when(nodeAccess.id).thenReturn('test')
    def provider = mock(Provider)
    jobStore = mock(JobStore)
    when(provider.get()).thenReturn(jobStore)
    eventManager = new SimpleEventManager()

    def lastShutdownTimeService = mock(LastShutdownTimeService)
    when(lastShutdownTimeService.estimateLastShutdownTime()).thenReturn(Optional.empty())

    underTest = new QuartzSchedulerSPI(
        eventManager, nodeAccess, provider, mock(JobFactoryImpl), lastShutdownTimeService, 1
    )
    underTest.start()
    underTest.states.current = STARTED
    eventManager.register(underTest)
  }

  @After
  void after() {
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
    def jobDetailEntity = mockJobDetailEntity()

    def schedulerListener = mock(SchedulerListener)
    underTest.scheduler.getListenerManager().addSchedulerListener(schedulerListener)

    // on(JobCreatedEvent)

    def jobCreatedEvent = mock(JobCreatedEvent)
    when(jobCreatedEvent.job).thenReturn(jobDetailEntity)
    eventManager.post(jobCreatedEvent)

    def recordCreate = ArgumentCaptor.forClass(JobDetail)
    verify(schedulerListener).jobAdded(recordCreate.capture())
    assert recordCreate.value.key.name == 'testJobKeyName'

    reset(schedulerListener)

    // on(JobUpdatedEvent)

    def jobUpdateEvent = mock(JobUpdatedEvent)
    when(jobUpdateEvent.job).thenReturn(jobDetailEntity)
    eventManager.post(jobUpdateEvent)

    def recordUpdate = ArgumentCaptor.forClass(JobDetail)
    verify(schedulerListener).jobAdded(recordUpdate.capture())
    assert recordUpdate.value.key.name == 'testJobKeyName'

    reset(schedulerListener)

    // on(JobDeletedEvent)

    def jobDeletedEvent = mock(JobDeletedEvent)
    when(jobDeletedEvent.job).thenReturn(jobDetailEntity)
    eventManager.post(jobDeletedEvent)

    def recordDelete = ArgumentCaptor.forClass(JobKey)
    verify(schedulerListener).jobDeleted(recordDelete.capture())
    assert recordDelete.value.name == 'testJobKeyName'
  }

  @Test
  void 'Exercise scheduled trigger events'() {
    def jobDetailEntity = mockJobDetailEntity()
    def jobKey = jobDetailEntity.value.key
    def startAt = DateTime.parse('2010-06-30T01:20')
    def triggerEntity = mockTriggerEntity(jobKey, new Daily(startAt.toDate()))

    def schedulerListener = mock(SchedulerListener)
    underTest.scheduler.getListenerManager().addSchedulerListener(schedulerListener)

    // on(TriggerCreatedEvent)

    def triggerCreatedEvent = mock(TriggerCreatedEvent)
    when(triggerCreatedEvent.trigger).thenReturn(triggerEntity)
    eventManager.post(triggerCreatedEvent)

    def recordCreate = ArgumentCaptor.forClass(Trigger)
    verify(schedulerListener).jobScheduled(recordCreate.capture())
    assert recordCreate.value.key.name == 'testTriggerKeyName'

    reset(schedulerListener)

    // on(TriggerUpdatedEvent)

    def triggerUpdatedEvent = mock(TriggerUpdatedEvent)
    when(triggerUpdatedEvent.trigger).thenReturn(triggerEntity)
    eventManager.post(triggerUpdatedEvent)

    def recordUpdateUnschedule = ArgumentCaptor.forClass(TriggerKey)
    verify(schedulerListener).jobUnscheduled(recordUpdateUnschedule.capture())
    assert recordUpdateUnschedule.value.name == 'testTriggerKeyName'

    def recordUpdateSchedule = ArgumentCaptor.forClass(Trigger)
    verify(schedulerListener).jobScheduled(recordUpdateSchedule.capture())
    assert recordUpdateSchedule.value.key.name == 'testTriggerKeyName'

    reset(schedulerListener)

    // on(TriggerDeletedEvent)

    def triggerDeletedEvent = mock(TriggerDeletedEvent)
    when(triggerDeletedEvent.trigger).thenReturn(triggerEntity)
    eventManager.post(triggerDeletedEvent)

    def recordDelete = ArgumentCaptor.forClass(TriggerKey)
    verify(schedulerListener).jobUnscheduled(recordDelete.capture())
    assert recordDelete.value.name == 'testTriggerKeyName'
  }

  @Test
  void 'Exercise run-now trigger events'() {
    def jobDetailEntity = mockJobDetailEntity()
    def jobKey = jobDetailEntity.value.key
    def triggerEntity = mockTriggerEntity(jobKey, new Now())

    def schedulerListener = mock(SchedulerListener)
    underTest.scheduler.getListenerManager().addSchedulerListener(schedulerListener)

    // on(TriggerCreatedEvent)

    def triggerCreatedEvent = mock(TriggerCreatedEvent)
    when(triggerCreatedEvent.trigger).thenReturn(triggerEntity)
    eventManager.post(triggerCreatedEvent)

    verifyNoMoreInteractions(schedulerListener) // run-now triggers don't affect remote schedulerListeners

    // on(TriggerUpdatedEvent)

    def triggerUpdatedEvent = mock(TriggerUpdatedEvent)
    when(triggerUpdatedEvent.trigger).thenReturn(triggerEntity)
    eventManager.post(triggerUpdatedEvent)

    verifyNoMoreInteractions(schedulerListener) // run-now triggers don't affect remote schedulerListeners

    // on(TriggerDeletedEvent)

    def triggerDeletedEvent = mock(TriggerDeletedEvent)
    when(triggerDeletedEvent.trigger).thenReturn(triggerEntity)
    eventManager.post(triggerDeletedEvent)

    verifyNoMoreInteractions(schedulerListener) // run-now triggers don't affect remote schedulerListeners
  }

  @Test
  void 'Attempting to programmatically run a task when the scheduler is paused throws an exception'() {
    thrown.expect(IllegalStateException.class)
    def quartzTaskInfo = mock(QuartzTaskInfo)
    def quartzTaskState = mock(QuartzTaskState)
    underTest.pause()
    underTest.runNow('trigger-source', new JobKey('name', 'group'), quartzTaskInfo, quartzTaskState)
    verify(quartzTaskInfo, never()).setNexusTaskState(anyObject(), anyObject(), anyObject(), anyObject())
  }

  @Test
  void 'reattachListeners also starts Now type tasks'() {
    testReattachListeners(new Now(), true)
  }

  @Test
  void 'reattachListeners does not start Manual type tasks'() {
    testReattachListeners(new Manual(), false)
  }

  @Test
  void 'Trigger time before last run time does not modify job' () {
    interruptStateTestHelper(
        false,
        DateTime.parse("2002-01-01").toDate(),
        EndState.OK,
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
        EndState.OK,
        DateTime.parse("2001-01-01").toDate()
    )
  }

  @Test
  void 'Trigger time does not exist so the job is not modified'() {
    interruptStateTestHelper(
        false,
        null,
        EndState.OK,
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

  void interruptStateTestHelper(
      boolean shouldBeInterrupted,
      Date lastTriggerDate,
      EndState initialEndState,
      Date runStart,
      long runTime = 1000,
      Date shutdownDate = DateTime.parse("2003-01-01T00:00").toDate()) {

    def jobKey = new JobKey('testJobKeyName', 'testJobKeyGroup')
    def trigger = mock(Trigger)
    when(trigger.getPreviousFireTime()).thenReturn(lastTriggerDate)

    def jobDataMap = mock(JobDataMap)
    def jobDetail = mock(JobDetail)
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap)

    def config = new TaskConfiguration()
    if (initialEndState != null) {
      QuartzTaskState.setLastRunState(config, initialEndState, runStart, runTime)
    }

    def taskInfo = mock(QuartzTaskInfo)
    when(taskInfo.getConfiguration()).thenReturn(config)

    def jobListener = mock(QuartzTaskJobListener)
    when(jobListener.taskInfo).thenReturn(taskInfo)

    def listenerManager = mock(ListenerManager)
    when(listenerManager.getJobListener(any())).thenReturn(jobListener)

    def scheduler = mock(Scheduler)
    when(scheduler.getListenerManager()).thenReturn(listenerManager)
    when(scheduler.getJobKeys(any())).thenReturn(Sets.newHashSet(jobKey))
    when(scheduler.getTriggersOfJob(eq(jobKey))).thenReturn(Lists.newArrayList(trigger))
    when(scheduler.getJobDetail(eq(jobKey))).thenReturn(jobDetail)

    def old = underTest.@scheduler
    try {
      underTest.@scheduler = scheduler
      underTest.updateLastRunStateInfo(Optional.of(shutdownDate))
    } finally {
      underTest.@scheduler = old
    }

    if (shouldBeInterrupted) {
      assert config.getString(LAST_RUN_STATE_END_STATE) == EndState.INTERRUPTED.name()
      assert config.getLong(LAST_RUN_STATE_RUN_STARTED, -1) == lastTriggerDate.time
      assert config.getLong(LAST_RUN_STATE_RUN_DURATION, -1) == shutdownDate.time - lastTriggerDate.time

      verify(jobDataMap, times(1)).put(LAST_RUN_STATE_END_STATE, EndState.INTERRUPTED.name())
      verify(jobDataMap, times(1)).put(LAST_RUN_STATE_RUN_STARTED,  lastTriggerDate.time.toString())
      verify(jobDataMap, times(1)).put(LAST_RUN_STATE_RUN_DURATION, (shutdownDate.time - lastTriggerDate.time).toString())

      verify(scheduler, times(1)).addJob(jobDetail, true, true)
    } else if (initialEndState != null) {
      assert config.getString(LAST_RUN_STATE_END_STATE) == initialEndState.name()
      assert config.getLong(LAST_RUN_STATE_RUN_STARTED, -1) == runStart.time
      assert config.getLong(LAST_RUN_STATE_RUN_DURATION, -1) == runTime

      verify(scheduler, times(0)).addJob(jobDetail, true, true)
    } else {
      verify(scheduler, times(0)).addJob(jobDetail, true, true)
    }

  }


  private Scheduler testReattachListeners(schedule, shouldRun) {
    JobDetailEntity jobDetailEntity = mockJobDetailEntity()
    TriggerEntity triggerEntity = mockTriggerEntity(jobDetailEntity.value.key, schedule)
    Scheduler scheduler = mock(Scheduler)
    Set<JobKey> jobs = Collections.singleton(jobDetailEntity.getValue().getKey())
    JobDetail jobDetail = jobDetailEntity.getValue()
    Trigger trigger = triggerEntity.getValue()
    ListenerManager listenerManager = mock(ListenerManager)

    //note the variables are all defined above as groovy/mockito is choking when i try to use the values inline
    when(scheduler.getJobKeys(anyObject())).thenReturn(jobs)
    when(scheduler.getJobDetail(jobDetailEntity.value.key)).thenReturn(jobDetail)
    when(scheduler.getTrigger(new TriggerKey(jobDetailEntity.value.key.name, jobDetailEntity.value.key.group))).thenReturn(trigger)
    when(scheduler.getListenerManager()).thenReturn(listenerManager)

    def old = underTest.@scheduler
    underTest.@scheduler = scheduler
    underTest.reattachJobListeners()
    underTest.@scheduler = old

    verify(scheduler, shouldRun ? times(1) : never()).rescheduleJob(triggerEntity.value.getKey(), triggerEntity.value)
    verify(scheduler, shouldRun ? times(1) : never()).resumeJob(jobDetailEntity.value.key)
  }

  private JobDetailEntity mockJobDetailEntity() {
    def jobDetailEntity = mock(JobDetailEntity)
    def jobDetail = mock(JobDetail)
    def jobKey = new JobKey('testJobKeyName', 'testJobKeyGroup')
    when(jobDetailEntity.value).thenReturn(jobDetail)
    when(jobDetail.key).thenReturn(jobKey)
    when(jobDetail.description).thenReturn('test job description')
    when(jobDetail.jobClass).thenReturn(Job)
    when(jobDetail.jobDataMap).thenReturn(new JobDataMap())
    when(jobDetail.durable).thenReturn(false)
    when(jobDetail.requestsRecovery()).thenReturn(false)
    when(jobStore.retrieveJob(jobKey)).thenReturn(jobDetail)
    return jobDetailEntity
  }

  private TriggerEntity mockTriggerEntity(JobKey jobKey, Schedule schedule) {
    def triggerEntity = mock(TriggerEntity)
    def trigger = mock(OperableTrigger)
    def triggerKey = new TriggerKey('testTriggerKeyName', 'testTriggerKeyGroup')
    when(triggerEntity.value).thenReturn(trigger)
    when(trigger.key).thenReturn(triggerKey)
    when(trigger.jobKey).thenReturn(jobKey)
    when(trigger.description).thenReturn('test trigger description')
    when(trigger.jobDataMap).thenReturn(new JobDataMap(schedule.asMap()))
    return triggerEntity
  }
}
