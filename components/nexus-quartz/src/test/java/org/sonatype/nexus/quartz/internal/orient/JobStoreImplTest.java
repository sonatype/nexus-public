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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JobStoreImpl}. Based on original Quartz 2.2.2 AbstractJobStoreTest.
 *
 * @see <a href="http://svn.terracotta.org/svn/quartz/tags/quartz-2.2.2/quartz-core/src/test/java/org/quartz/AbstractJobStoreTest.java">AbstractJobStoreTest.java</a>
 */
public class JobStoreImplTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private JobStoreImpl jobStore;

  @Before
  public void setUp() throws Exception {
    ClassLoadHelper loadHelper = new CascadingClassLoadHelper();
    loadHelper.initialize();
    this.jobStore = createJobStore();
    this.jobStore.initialize(loadHelper, new SampleSignaler());
    this.jobStore.schedulerStarted();
  }

  @After
  public void tearDown() {
    jobStore.shutdown();
  }

  private JobStoreImpl createJobStore() {
    final JobDetailEntityAdapter jobDetailEntityAdapter = new JobDetailEntityAdapter();
    final TriggerEntityAdapter triggerEntityAdapter = new TriggerEntityAdapter();
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
        mock(NodeAccess.class)
    );
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
  public static class MyJob
      implements Job
  {
    public void execute(JobExecutionContext context) throws JobExecutionException {
      //
    }
  }
}
