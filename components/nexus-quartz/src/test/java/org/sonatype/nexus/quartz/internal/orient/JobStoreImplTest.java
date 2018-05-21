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
package org.sonatype.nexus.quartz.internal.orient;

/*
 * Copyright 2001-2009 Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State;

import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;

/**
 * Tests for {@link JobStoreImpl}. Based on original Quartz 2.2.2 AbstractJobStoreTest.
 *
 * @see <a href="http://svn.terracotta.org/svn/quartz/tags/quartz-2.2.2/quartz-core/src/test/java/org/quartz/AbstractJobStoreTest.java">AbstractJobStoreTest.java</a>
 */
public class JobStoreImplTest
    extends TestSupport
{
  private static final Time ACQUIRE_RETRY_DELAY = Time.seconds(15);

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private JobStoreImpl jobStore;

  private TriggerEntityAdapter triggerEntityAdapter;

  private NodeAccess nodeAccess;

  @Before
  public void setUp() throws Exception {
    ClassLoadHelper loadHelper = new CascadingClassLoadHelper();

    nodeAccess = mock(NodeAccess.class);

    loadHelper.initialize();
    this.jobStore = createJobStore();
    this.jobStore.start();
    this.jobStore.initialize(loadHelper, new SampleSignaler());
    this.jobStore.schedulerStarted();
  }

  @After
  public void tearDown() throws Exception {
    jobStore.shutdown();
    jobStore.stop();
  }

  private JobStoreImpl createJobStore() {
    final JobDetailEntityAdapter jobDetailEntityAdapter = new JobDetailEntityAdapter();
    triggerEntityAdapter = new TriggerEntityAdapter();
    final CalendarEntityAdapter calendarEntityAdapter = new CalendarEntityAdapter();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      jobDetailEntityAdapter.register(db);
      triggerEntityAdapter.register(db);
      calendarEntityAdapter.register(db);

      // nuke quartz data
      jobDetailEntityAdapter.deleteAll(db);
      triggerEntityAdapter.deleteAll(db);
      calendarEntityAdapter.deleteAll(db);
    }

    return new JobStoreImpl(
        database.getInstanceProvider(),
        jobDetailEntityAdapter,
        triggerEntityAdapter,
        calendarEntityAdapter,
        nodeAccess,
        ACQUIRE_RETRY_DELAY
    );
  }

  @Test
  public void testJobsAreAcquiredCorrectly() throws Exception {
    testTriggerAcquiredCorrectly(State.ACQUIRED, false, false);
    testTriggerAcquiredCorrectly(State.BLOCKED, false, false);
    testTriggerAcquiredCorrectly(State.COMPLETE, false, false);
    testTriggerAcquiredCorrectly(State.ERROR, false, false);
    testTriggerAcquiredCorrectly(State.PAUSED, false, false);
    testTriggerAcquiredCorrectly(State.PAUSED_BLOCKED, false, false);
    testTriggerAcquiredCorrectly(State.WAITING, false, true);

    testTriggerAcquiredCorrectly(State.ACQUIRED, true, true);
    testTriggerAcquiredCorrectly(State.BLOCKED, true, true);
    testTriggerAcquiredCorrectly(State.COMPLETE, true, false);
    testTriggerAcquiredCorrectly(State.ERROR, true, false);
    testTriggerAcquiredCorrectly(State.PAUSED, true, false);
    testTriggerAcquiredCorrectly(State.PAUSED_BLOCKED, true, false);
    testTriggerAcquiredCorrectly(State.WAITING, true, true);
  }

  private void testTriggerAcquiredCorrectly(State initialState, boolean isOrphaned, boolean shouldBeAcquired) throws Exception  {
    when(nodeAccess.isClustered()).thenReturn(true);
    when(nodeAccess.getId()).thenReturn(isOrphaned ? "C" : "A");
    when(nodeAccess.getMemberIds()).thenReturn(Sets.newHashSet("A", "B"));

    JobDetail orphanedJob = newJob()
        .ofType(MyJob.class)
        .withIdentity("jobName1", "jobGroup1")
        .build();

    OperableTrigger orphanedTrigger = (OperableTrigger) newTrigger()
        .forJob(orphanedJob)
        .withIdentity("orphan:" + isOrphaned, "state:" + initialState.name())
        .startNow()
        .build();

    orphanedTrigger.computeFirstFireTime(null);

    this.jobStore.storeTrigger(orphanedTrigger, false);

    Iterator<TriggerEntity> triggerEntities = inTx(database.getInstanceProvider())
        .throwing(JobPersistenceException.class)
        .call(db -> triggerEntityAdapter.browseByJobKey(db, orphanedJob.getKey())).iterator();

    assertTrue(triggerEntities.hasNext());
    TriggerEntity entity = triggerEntities.next();
    entity.setState(initialState);

    inTx(database.getInstanceProvider())
        .throwing(JobPersistenceException.class)
        .call(db -> triggerEntityAdapter.editEntity(db, entity));

    long firstFireTime = orphanedTrigger.getNextFireTime().getTime();

    when(nodeAccess.getId()).thenReturn("A");

    List<OperableTrigger> acquiredTriggers = this.jobStore.acquireNextTriggers(firstFireTime + 10000, 10, 1L);

    if (shouldBeAcquired) {
      assertEquals(1 , acquiredTriggers.size());
      assertTrue(orphanedTrigger.equals(acquiredTriggers.get(0)));
    } else {
      assertEquals(0, acquiredTriggers.size());
    }

    jobStore.removeJob(orphanedJob.getKey());
  }

  @Test
  public void testAcquireNextTrigger() throws Exception {
    JobDetail jobDetail = JobBuilder.newJob(MyJob.class).withIdentity("job1", "jobGroup1").storeDurably(true).build();
    this.jobStore.storeJob(jobDetail, false);

    Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow();
    long baseFireTime = baseFireTimeDate.getTime();

    OperableTrigger trigger1 =
        new SimpleTriggerImpl("trigger1", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 200000),
            new Date(baseFireTime + 200000), 2, 2000);
    OperableTrigger trigger2 =
        new SimpleTriggerImpl("trigger2", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 50000),
            new Date(baseFireTime + 200000), 2, 2000);
    OperableTrigger trigger3 =
        new SimpleTriggerImpl("trigger1", "triggerGroup2", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 100000),
            new Date(baseFireTime + 200000), 2, 2000);

    trigger1.computeFirstFireTime(null);
    trigger2.computeFirstFireTime(null);
    trigger3.computeFirstFireTime(null);
    this.jobStore.storeTrigger(trigger1, false);
    this.jobStore.storeTrigger(trigger2, false);
    this.jobStore.storeTrigger(trigger3, false);

    long firstFireTime = new Date(trigger1.getNextFireTime().getTime()).getTime();

    assertTrue(this.jobStore.acquireNextTriggers(10, 1, 0L).isEmpty());
    assertEquals(
        trigger2.getKey(),
        this.jobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).get(0).getKey());
    assertEquals(
        trigger3.getKey(),
        this.jobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).get(0).getKey());
    assertEquals(
        trigger1.getKey(),
        this.jobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).get(0).getKey());
    assertTrue(
        this.jobStore.acquireNextTriggers(firstFireTime + 10000, 1, 0L).isEmpty());


    // release trigger3
    this.jobStore.releaseAcquiredTrigger(trigger3);
    assertEquals(
        trigger3,
        this.jobStore.acquireNextTriggers(new Date(trigger1.getNextFireTime().getTime()).getTime() + 10000, 1, 1L)
            .get(0));
  }

  @Test
  public void testAcquireNextTriggerBatch() throws Exception {
    JobDetail jobDetail = JobBuilder.newJob(MyJob.class).withIdentity("job1", "jobGroup1").storeDurably(true).build();
    this.jobStore.storeJob(jobDetail, false);

    Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow();
    long baseFireTime = baseFireTimeDate.getTime();

    OperableTrigger early =
        new SimpleTriggerImpl("early", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime),
            new Date(baseFireTime + 5), 2, 2000);
    OperableTrigger trigger1 =
        new SimpleTriggerImpl("trigger1", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 200000),
            new Date(baseFireTime + 200005), 2, 2000);
    OperableTrigger trigger2 =
        new SimpleTriggerImpl("trigger2", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 200100),
            new Date(baseFireTime + 200105), 2, 2000);
    OperableTrigger trigger3 =
        new SimpleTriggerImpl("trigger3", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 200200),
            new Date(baseFireTime + 200205), 2, 2000);
    OperableTrigger trigger4 =
        new SimpleTriggerImpl("trigger4", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 200300),
            new Date(baseFireTime + 200305), 2, 2000);

    OperableTrigger trigger10 =
        new SimpleTriggerImpl("trigger10", "triggerGroup2", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 500000),
            new Date(baseFireTime + 700000), 2, 2000);

    early.computeFirstFireTime(null);
    trigger1.computeFirstFireTime(null);
    trigger2.computeFirstFireTime(null);
    trigger3.computeFirstFireTime(null);
    trigger4.computeFirstFireTime(null);
    trigger10.computeFirstFireTime(null);
    this.jobStore.storeTrigger(early, false);
    this.jobStore.storeTrigger(trigger1, false);
    this.jobStore.storeTrigger(trigger2, false);
    this.jobStore.storeTrigger(trigger3, false);
    this.jobStore.storeTrigger(trigger4, false);
    this.jobStore.storeTrigger(trigger10, false);

    long firstFireTime = new Date(trigger1.getNextFireTime().getTime()).getTime();

    List<OperableTrigger> acquiredTriggers = this.jobStore.acquireNextTriggers(firstFireTime + 10000, 4, 1000L);
    assertEquals(4, acquiredTriggers.size());
    assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
    assertEquals(trigger1.getKey(), acquiredTriggers.get(1).getKey());
    assertEquals(trigger2.getKey(), acquiredTriggers.get(2).getKey());
    assertEquals(trigger3.getKey(), acquiredTriggers.get(3).getKey());
    this.jobStore.releaseAcquiredTrigger(early);
    this.jobStore.releaseAcquiredTrigger(trigger1);
    this.jobStore.releaseAcquiredTrigger(trigger2);
    this.jobStore.releaseAcquiredTrigger(trigger3);

    acquiredTriggers = this.jobStore.acquireNextTriggers(firstFireTime + 10000, 5, 1000L);
    assertEquals(5, acquiredTriggers.size());
    assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
    assertEquals(trigger1.getKey(), acquiredTriggers.get(1).getKey());
    assertEquals(trigger2.getKey(), acquiredTriggers.get(2).getKey());
    assertEquals(trigger3.getKey(), acquiredTriggers.get(3).getKey());
    assertEquals(trigger4.getKey(), acquiredTriggers.get(4).getKey());
    this.jobStore.releaseAcquiredTrigger(early);
    this.jobStore.releaseAcquiredTrigger(trigger1);
    this.jobStore.releaseAcquiredTrigger(trigger2);
    this.jobStore.releaseAcquiredTrigger(trigger3);
    this.jobStore.releaseAcquiredTrigger(trigger4);

    acquiredTriggers = this.jobStore.acquireNextTriggers(firstFireTime + 10000, 6, 1000L);
    assertEquals(5, acquiredTriggers.size());
    assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
    assertEquals(trigger1.getKey(), acquiredTriggers.get(1).getKey());
    assertEquals(trigger2.getKey(), acquiredTriggers.get(2).getKey());
    assertEquals(trigger3.getKey(), acquiredTriggers.get(3).getKey());
    assertEquals(trigger4.getKey(), acquiredTriggers.get(4).getKey());
    this.jobStore.releaseAcquiredTrigger(early);
    this.jobStore.releaseAcquiredTrigger(trigger1);
    this.jobStore.releaseAcquiredTrigger(trigger2);
    this.jobStore.releaseAcquiredTrigger(trigger3);
    this.jobStore.releaseAcquiredTrigger(trigger4);

    acquiredTriggers = this.jobStore.acquireNextTriggers(firstFireTime + 1, 5, 0L);
    assertEquals(2, acquiredTriggers.size());
    assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
    assertEquals(trigger1.getKey(), acquiredTriggers.get(1).getKey());
    this.jobStore.releaseAcquiredTrigger(early);
    this.jobStore.releaseAcquiredTrigger(trigger1);

    acquiredTriggers = this.jobStore.acquireNextTriggers(firstFireTime + 250, 5, 199L);
    assertEquals(5, acquiredTriggers.size());
    assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
    assertEquals(trigger1.getKey(), acquiredTriggers.get(1).getKey());
    assertEquals(trigger2.getKey(), acquiredTriggers.get(2).getKey());
    assertEquals(trigger3.getKey(), acquiredTriggers.get(3).getKey());
    assertEquals(trigger4.getKey(), acquiredTriggers.get(4).getKey());
    this.jobStore.releaseAcquiredTrigger(early);
    this.jobStore.releaseAcquiredTrigger(trigger1);
    this.jobStore.releaseAcquiredTrigger(trigger2);
    this.jobStore.releaseAcquiredTrigger(trigger3);
    this.jobStore.releaseAcquiredTrigger(trigger4);

    acquiredTriggers = this.jobStore.acquireNextTriggers(firstFireTime + 150, 5, 50L);
    assertEquals(4, acquiredTriggers.size());
    assertEquals(early.getKey(), acquiredTriggers.get(0).getKey());
    assertEquals(trigger1.getKey(), acquiredTriggers.get(1).getKey());
    assertEquals(trigger2.getKey(), acquiredTriggers.get(2).getKey());
    assertEquals(trigger3.getKey(), acquiredTriggers.get(3).getKey());
    this.jobStore.releaseAcquiredTrigger(early);
    this.jobStore.releaseAcquiredTrigger(trigger1);
    this.jobStore.releaseAcquiredTrigger(trigger2);
    this.jobStore.releaseAcquiredTrigger(trigger3);
  }

  /**
   * Simulate a job that has run longer than the next fire time such that it misfires.
   */
  @Test
  public void testTriggerPastDueMisfire() throws Exception {
    JobDetail jobDetail = JobBuilder.newJob(MyNonConcurrentJob.class)
        .storeDurably(true)
        .build();
    jobStore.storeJob(jobDetail, false);

    ZoneId zone = ZoneId.systemDefault();

    Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow();
    LocalDateTime baseDateTime = LocalDateTime.ofInstant(baseFireTimeDate.toInstant(), zone);
    LocalDateTime startAt = baseDateTime.minusMinutes(5);
    LocalDateTime nextFireTime = startAt.plusMinutes(1);

    CronScheduleBuilder schedule = CronScheduleBuilder.cronSchedule("0 1 * * * ? *");
    OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger()
        .forJob(jobDetail)
        .withSchedule(schedule)
        .startAt(Date.from(startAt.atZone(zone).toInstant()))
        .build();

    // misfire the trigger and set the next fire time in the past
    trigger.updateAfterMisfire(null);
    trigger.setNextFireTime(Date.from(nextFireTime.atZone(zone).toInstant()));
    jobStore.storeTrigger(trigger, false);

    List<OperableTrigger> acquiredTriggers =
        jobStore.acquireNextTriggers(DateBuilder.evenMinuteDateAfterNow().getTime(), 4, 1000L);
    assertEquals(1, acquiredTriggers.size());
  }

  /**
   * Simulate a job that has run longer than the next fire time such that it misfires, but will not fire again because
   * the end of the trigger window has passed.
   */
  @Test
  public void testTriggerPastDueMisfireButWillNotFire() throws Exception {
    JobDetail jobDetail = JobBuilder.newJob(MyNonConcurrentJob.class).storeDurably(true).build();
    jobStore.storeJob(jobDetail, false);

    ZoneId zone = ZoneId.systemDefault();

    Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow();
    LocalDateTime baseDateTime = LocalDateTime.ofInstant(baseFireTimeDate.toInstant(), zone);
    LocalDateTime startAt = baseDateTime.minusMinutes(5);
    LocalDateTime endAt = baseDateTime.minusMinutes(1);
    LocalDateTime nextFireTime = startAt.plusMinutes(1);

    SimpleScheduleBuilder simple = SimpleScheduleBuilder.simpleSchedule()
        .withIntervalInMinutes(1);
    OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger()
        .forJob(jobDetail)
        .withSchedule(simple)
        .startAt(Date.from(startAt.atZone(zone).toInstant()))
        .endAt(Date.from(endAt.atZone(zone).toInstant()))
        .build();

    // misfire the trigger and set the next fire time in the past
    trigger.updateAfterMisfire(null);
    trigger.setNextFireTime(Date.from(nextFireTime.atZone(zone).toInstant()));
    trigger.setMisfireInstruction(MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);
    jobStore.storeTrigger(trigger, false);

    List<OperableTrigger> acquiredTriggers =
        jobStore.acquireNextTriggers(baseFireTimeDate.getTime(), 4, 1000L);
    assertEquals(0, acquiredTriggers.size());
  }

  @Test
  public void testTriggerStates() throws Exception {
    JobDetail jobDetail = JobBuilder.newJob(MyJob.class).withIdentity("job1", "jobGroup1").storeDurably(true).build();
    this.jobStore.storeJob(jobDetail, false);

    OperableTrigger trigger =
        new SimpleTriggerImpl("trigger1", "triggerGroup1", jobDetail.getKey().getName(), jobDetail.getKey().getGroup(),
            new Date(System.currentTimeMillis() + 100000), new Date(System.currentTimeMillis() + 200000), 2, 2000);
    trigger.computeFirstFireTime(null);
    assertEquals(TriggerState.NONE, this.jobStore.getTriggerState(trigger.getKey()));
    this.jobStore.storeTrigger(trigger, false);
    assertEquals(TriggerState.NORMAL, this.jobStore.getTriggerState(trigger.getKey()));

    this.jobStore.pauseTrigger(trigger.getKey());
    assertEquals(TriggerState.PAUSED, this.jobStore.getTriggerState(trigger.getKey()));

    this.jobStore.resumeTrigger(trigger.getKey());
    assertEquals(TriggerState.NORMAL, this.jobStore.getTriggerState(trigger.getKey()));

    trigger = this.jobStore.acquireNextTriggers(
        new Date(trigger.getNextFireTime().getTime()).getTime() + 10000, 1, 1L).get(0);
    assertNotNull(trigger);
    this.jobStore.releaseAcquiredTrigger(trigger);
    trigger = this.jobStore.acquireNextTriggers(
        new Date(trigger.getNextFireTime().getTime()).getTime() + 10000, 1, 1L).get(0);
    assertNotNull(trigger);
    assertTrue(this.jobStore.acquireNextTriggers(
        new Date(trigger.getNextFireTime().getTime()).getTime() + 10000, 1, 1L).isEmpty());
  }

  // See: http://jira.opensymphony.com/browse/QUARTZ-606
  @Test
  public void testStoreTriggerReplacesTrigger() throws Exception {
    String jobName = "StoreTriggerReplacesTrigger";
    String jobGroup = "StoreTriggerReplacesTriggerGroup";
    JobDetailImpl detail = new JobDetailImpl(jobName, jobGroup, MyJob.class);
    jobStore.storeJob(detail, false);

    String trName = "StoreTriggerReplacesTrigger";
    String trGroup = "StoreTriggerReplacesTriggerGroup";
    OperableTrigger tr = new SimpleTriggerImpl(trName, trGroup, new Date());
    tr.setJobKey(new JobKey(jobName, jobGroup));
    tr.setCalendarName(null);

    jobStore.storeTrigger(tr, false);
    assertEquals(tr, jobStore.retrieveTrigger(tr.getKey()));

    try {
      jobStore.storeTrigger(tr, false);
      fail("an attempt to store duplicate trigger succeeded");
    }
    catch (ObjectAlreadyExistsException oaee) {
      // expected
    }

    tr.setCalendarName("QQ");
    jobStore.storeTrigger(tr, true); //fails here
    assertEquals(tr, jobStore.retrieveTrigger(tr.getKey()));
    assertEquals("StoreJob doesn't replace triggers", "QQ", jobStore.retrieveTrigger(tr.getKey()).getCalendarName());
  }

  @Ignore("Same stands as for JDBC job store, see https://jira.terracotta.org/jira/browse/QTZ-208")
  @Test
  public void testPauseJobGroupPausesNewJob() throws Exception {
    final String jobName1 = "PauseJobGroupPausesNewJob";
    final String jobName2 = "PauseJobGroupPausesNewJob2";
    final String jobGroup = "PauseJobGroupPausesNewJobGroup";

    JobDetailImpl detail = new JobDetailImpl(jobName1, jobGroup, MyJob.class);
    detail.setDurability(true);
    jobStore.storeJob(detail, false);
    jobStore.pauseJobs(GroupMatcher.jobGroupEquals(jobGroup));

    detail = new JobDetailImpl(jobName2, jobGroup, MyJob.class);
    detail.setDurability(true);
    jobStore.storeJob(detail, false);

    String trName = "PauseJobGroupPausesNewJobTrigger";
    String trGroup = "PauseJobGroupPausesNewJobTriggerGroup";
    OperableTrigger tr = new SimpleTriggerImpl(trName, trGroup, new Date());
    tr.setJobKey(new JobKey(jobName2, jobGroup));
    jobStore.storeTrigger(tr, false);
    assertEquals(TriggerState.PAUSED, jobStore.getTriggerState(tr.getKey()));
  }

  @Test
  public void testStoreAndRetrieveJobs() throws Exception {
    // Store jobs.
    for (int i = 0; i < 10; i++) {
      String group = i < 5 ? "a" : "b";
      JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i, group).build();
      jobStore.storeJob(job, false);
    }
    // Retrieve jobs.
    for (int i = 0; i < 10; i++) {
      String group = i < 5 ? "a" : "b";
      JobKey jobKey = JobKey.jobKey("job" + i, group);
      JobDetail storedJob = jobStore.retrieveJob(jobKey);
      assertEquals(jobKey, storedJob.getKey());
    }
    // Retrieve by group
    assertEquals("Wrong number of jobs in group 'a'", jobStore.getJobKeys(GroupMatcher.jobGroupEquals("a")).size(), 5);
    assertEquals("Wrong number of jobs in group 'b'", jobStore.getJobKeys(GroupMatcher.jobGroupEquals("b")).size(), 5);
  }

  @Test
  public void testStoreAndRetriveTriggers() throws Exception {
    // Store jobs and triggers.
    for (int i = 0; i < 10; i++) {
      String group = i < 5 ? "a" : "b";
      JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i, group).build();
      jobStore.storeJob(job, true);
      SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule();
      Trigger trigger = TriggerBuilder.newTrigger().withIdentity("job" + i, group).withSchedule(schedule).forJob(job)
          .build();
      jobStore.storeTrigger((OperableTrigger) trigger, true);
    }
    // Retrieve job and trigger.
    for (int i = 0; i < 10; i++) {
      String group = i < 5 ? "a" : "b";
      JobKey jobKey = JobKey.jobKey("job" + i, group);
      JobDetail storedJob = jobStore.retrieveJob(jobKey);
      assertEquals(jobKey, storedJob.getKey());

      TriggerKey triggerKey = TriggerKey.triggerKey("job" + i, group);
      Trigger storedTrigger = jobStore.retrieveTrigger(triggerKey);
      assertEquals(triggerKey, storedTrigger.getKey());
    }
    // Retrieve by group
    assertEquals("Wrong number of triggers in group 'a'",
        jobStore.getTriggerKeys(GroupMatcher.triggerGroupEquals("a")).size(), 5);
    assertEquals("Wrong number of triggers in group 'b'",
        jobStore.getTriggerKeys(GroupMatcher.triggerGroupEquals("b")).size(), 5);
  }

  @Test
  public void testMatchers() throws Exception {
    JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job1", "aaabbbccc").build();
    jobStore.storeJob(job, true);
    SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule();
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trig1", "aaabbbccc").withSchedule(schedule).forJob(job)
        .build();
    jobStore.storeTrigger((OperableTrigger) trigger, true);

    job = JobBuilder.newJob(MyJob.class).withIdentity("job1", "xxxyyyzzz").build();
    jobStore.storeJob(job, true);
    schedule = SimpleScheduleBuilder.simpleSchedule();
    trigger = TriggerBuilder.newTrigger().withIdentity("trig1", "xxxyyyzzz").withSchedule(schedule).forJob(job).build();
    jobStore.storeTrigger((OperableTrigger) trigger, true);

    job = JobBuilder.newJob(MyJob.class).withIdentity("job2", "xxxyyyzzz").build();
    jobStore.storeJob(job, true);
    schedule = SimpleScheduleBuilder.simpleSchedule();
    trigger = TriggerBuilder.newTrigger().withIdentity("trig2", "xxxyyyzzz").withSchedule(schedule).forJob(job).build();
    jobStore.storeTrigger((OperableTrigger) trigger, true);

    Set<JobKey> jkeys = jobStore.getJobKeys(GroupMatcher.anyJobGroup());
    assertEquals("Wrong number of jobs found by anything matcher", 3, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupEquals("xxxyyyzzz"));
    assertEquals("Wrong number of jobs found by equals matcher", 2, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupEquals("aaabbbccc"));
    assertEquals("Wrong number of jobs found by equals matcher", 1, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupStartsWith("aa"));
    assertEquals("Wrong number of jobs found by starts with matcher", 1, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupStartsWith("xx"));
    assertEquals("Wrong number of jobs found by starts with matcher", 2, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupEndsWith("cc"));
    assertEquals("Wrong number of jobs found by ends with matcher", 1, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupEndsWith("zzz"));
    assertEquals("Wrong number of jobs found by ends with matcher", 2, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupContains("bc"));
    assertEquals("Wrong number of jobs found by contains with matcher", 1, jkeys.size());

    jkeys = jobStore.getJobKeys(GroupMatcher.jobGroupContains("yz"));
    assertEquals("Wrong number of jobs found by contains with matcher", 2, jkeys.size());

    Set<TriggerKey> tkeys = jobStore.getTriggerKeys(GroupMatcher.anyTriggerGroup());
    assertEquals("Wrong number of triggers found by anything matcher", 3, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupEquals("xxxyyyzzz"));
    assertEquals("Wrong number of triggers found by equals matcher", 2, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupEquals("aaabbbccc"));
    assertEquals("Wrong number of triggers found by equals matcher", 1, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("aa"));
    assertEquals("Wrong number of triggers found by starts with matcher", 1, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("xx"));
    assertEquals("Wrong number of triggers found by starts with matcher", 2, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("cc"));
    assertEquals("Wrong number of triggers found by ends with matcher", 1, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("zzz"));
    assertEquals("Wrong number of triggers found by ends with matcher", 2, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupContains("bc"));
    assertEquals("Wrong number of triggers found by contains with matcher", 1, tkeys.size());

    tkeys = jobStore.getTriggerKeys(GroupMatcher.triggerGroupContains("yz"));
    assertEquals("Wrong number of triggers found by contains with matcher", 2, tkeys.size());
  }

  @Test
  public void testAcquireTriggers() throws Exception {
    // Setup: Store jobs and triggers.
    long MIN = 60 * 1000L;
    Date startTime0 = new Date(System.currentTimeMillis() + MIN); // a min from now.
    for (int i = 0; i < 10; i++) {
      Date startTime = new Date(startTime0.getTime() + i * MIN); // a min apart
      JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i).build();
      SimpleScheduleBuilder schedule = SimpleScheduleBuilder.repeatMinutelyForever(2);
      OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger().withIdentity("job" + i)
          .withSchedule(schedule).forJob(job).startAt(startTime).build();

      // Manually trigger the first fire time computation that scheduler would do. Otherwise
      // the store.acquireNextTriggers() will not work properly.
      Date fireTime = trigger.computeFirstFireTime(null);
      assertEquals(true, fireTime != null);

      jobStore.storeJobAndTrigger(job, trigger);
    }

    // Test acquire one trigger at a time
    for (int i = 0; i < 10; i++) {
      long noLaterThan = (startTime0.getTime() + i * MIN);
      int maxCount = 1;
      long timeWindow = 0;
      List<OperableTrigger> triggers = jobStore.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
      assertEquals(1, triggers.size());
      assertEquals("job" + i, triggers.get(0).getKey().getName());

      // Let's remove the trigger now.
      jobStore.removeJob(triggers.get(0).getJobKey());
    }
  }

  @Test
  public void testAcquireTriggersInBatch() throws Exception {
    // Setup: Store jobs and triggers.
    long MIN = 60 * 1000L;
    Date startTime0 = new Date(System.currentTimeMillis() + MIN); // a min from now.
    for (int i = 0; i < 10; i++) {
      Date startTime = new Date(startTime0.getTime() + i * MIN); // a min apart
      JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("job" + i).build();
      SimpleScheduleBuilder schedule = SimpleScheduleBuilder.repeatMinutelyForever(2);
      OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger().withIdentity("job" + i)
          .withSchedule(schedule).forJob(job).startAt(startTime).build();

      // Manually trigger the first fire time computation that scheduler would do. Otherwise
      // the store.acquireNextTriggers() will not work properly.
      Date fireTime = trigger.computeFirstFireTime(null);
      assertEquals(true, fireTime != null);

      jobStore.storeJobAndTrigger(job, trigger);
    }

    // Test acquire batch of triggers at a time
    long noLaterThan = startTime0.getTime() + 10 * MIN;
    int maxCount = 7;
    // time window needs to be big to be able to pick up multiple triggers when they are a minute apart
    long timeWindow = 8 * MIN;
    List<OperableTrigger> triggers = jobStore.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
    assertEquals(7, triggers.size());
    for (int i = 0; i < 7; i++) {
      assertEquals("job" + i, triggers.get(i).getKey().getName());
    }
  }

  @Test
  public void testResetErrorTrigger() throws Exception {
    JobDetail jobDetail = JobBuilder.newJob(MyJob.class).withIdentity("job1", "jobGroup1").storeDurably(true).build();
    this.jobStore.storeJob(jobDetail, false);
    Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow();
    long baseFireTime = baseFireTimeDate.getTime();

    // create and store a trigger
    OperableTrigger trigger1 =
        new SimpleTriggerImpl("trigger1", "triggerGroup1", jobDetail.getKey().getName(),
            jobDetail.getKey().getGroup(), new Date(baseFireTime + 200000),
            new Date(baseFireTime + 200000), 2, 2000);

    trigger1.computeFirstFireTime(null);
    jobStore.storeTrigger(trigger1, false);

    long firstFireTime = new Date(trigger1.getNextFireTime().getTime()).getTime();


    // pretend to fire it
    List<OperableTrigger> aqTs = jobStore.acquireNextTriggers(
        firstFireTime + 10000, 1, 0L);
    assertEquals(trigger1.getKey(), aqTs.get(0).getKey());

    List<TriggerFiredResult> fTs = jobStore.triggersFired(aqTs);
    TriggerFiredResult ft = fTs.get(0);

    // get the trigger into error state
    jobStore.triggeredJobComplete(ft.getTriggerFiredBundle().getTrigger(), ft.getTriggerFiredBundle().getJobDetail(),
        Trigger.CompletedExecutionInstruction.SET_TRIGGER_ERROR);
    TriggerState state = jobStore.getTriggerState(trigger1.getKey());
    assertEquals(TriggerState.ERROR, state);

    // test reset
    jobStore.resetTriggerFromErrorState(trigger1.getKey());
    state = jobStore.getTriggerState(trigger1.getKey());
    assertEquals(TriggerState.NORMAL, state);
  }

  @Test
  public void jobDataOnlySavedWhenDirty() throws Exception {
    JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("testJob").build();
    OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger().withIdentity("testJob").forJob(job).build();
    jobStore.storeJobAndTrigger(job, trigger);

    int baseRecordVersion = queryJobDetail("testJob").getVersion();

    // same job data after trigger fired...
    jobStore.triggersFired(Arrays.asList(trigger));
    jobStore.triggeredJobComplete(trigger, job, CompletedExecutionInstruction.NOOP);

    // ...should not save the job
    assertThat(queryJobDetail("testJob").getVersion(), is(baseRecordVersion));

    // different job data after trigger fired...
    jobStore.triggersFired(Arrays.asList(trigger));
    job.getJobDataMap().put("testKey", "testValue");
    jobStore.triggeredJobComplete(trigger, job, CompletedExecutionInstruction.NOOP);

    // ...should save the job
    assertThat(queryJobDetail("testJob").getVersion(), greaterThan(baseRecordVersion));
  }

  @Test
  public void testConcurrentExecutionPrevented() throws Exception {
    JobDetail job = JobBuilder.newJob(MyNonConcurrentJob.class).withIdentity("testJob").build();
    SimpleScheduleBuilder schedule = SimpleScheduleBuilder.repeatSecondlyForever(5);
    ZoneOffset offset = OffsetDateTime.now().getOffset();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = now.plusMinutes(1);
    OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger().withIdentity("testJob").forJob(job)
        .withSchedule(schedule).startAt(Date.from(startTime.toInstant(offset))).build();
    Date fireTime = trigger.computeFirstFireTime(null);
    assertEquals(true, fireTime != null);
    jobStore.storeJobAndTrigger(job, trigger);

    List<OperableTrigger> triggers = jobStore
        .acquireNextTriggers(startTime.plusSeconds(5).toInstant(offset).toEpochMilli(), Integer.MAX_VALUE, 1);

    assertEquals(1, triggers.size());
    jobStore.triggersFired(triggers); // start the jobs

    // do not notify jobStore that jobs have completed (simulate long running task)
    triggers = jobStore
        .acquireNextTriggers(startTime.plusSeconds(10).toInstant(offset).toEpochMilli(), Integer.MAX_VALUE, 1);
    assertEquals(0, triggers.size());

    List<OperableTrigger> triggersForJob = jobStore.getTriggersForJob(job.getKey());
    assertEquals(1, triggersForJob.size());
    Iterable<TriggerEntity> entities = inTx(database.getInstanceProvider())
        .throwing(JobPersistenceException.class)
        .call(db -> triggerEntityAdapter.browseByJobKey(db, job.getKey()));

    for (TriggerEntity entity : entities) {
      assertThat("all job triggers should be blocked", entity.getState(),
          anyOf(is(State.BLOCKED), is(State.PAUSED_BLOCKED)));
    }
  }

  private ODocument queryJobDetail(final String name) {
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      String selectJob = "select from quartz_job_detail where name=?";
      return (ODocument) db.query(new OSQLSynchQuery<>(selectJob), name).get(0);
    }
  }

  public static class SampleSignaler
      implements SchedulerSignaler
  {
    volatile int fMisfireCount = 0;

    public void notifyTriggerListenersMisfired(Trigger trigger) {
      System.out.println("Trigger misfired: " + trigger.getKey() + ", fire time: " + trigger.getNextFireTime());
      fMisfireCount++;
    }

    public void signalSchedulingChange(long candidateNewNextFireTime) {
    }

    public void notifySchedulerListenersFinalized(Trigger trigger) {
    }

    public void notifySchedulerListenersJobDeleted(JobKey jobKey) {
    }

    public void notifySchedulerListenersError(String string, SchedulerException jpe) {
    }
  }

  /**
   * An empty job for testing purpose.
   */
  @PersistJobDataAfterExecution
  public static class MyJob
      implements Job
  {
    public void execute(JobExecutionContext context) throws JobExecutionException {
      //
    }
  }

  /**
   * An empty, non-concurrent job for testing purpose.
   */
  @PersistJobDataAfterExecution
  @DisallowConcurrentExecution
  public static class MyNonConcurrentJob
      implements Job
  {
    public void execute(JobExecutionContext context) throws JobExecutionException {
      //
    }
  }
}
