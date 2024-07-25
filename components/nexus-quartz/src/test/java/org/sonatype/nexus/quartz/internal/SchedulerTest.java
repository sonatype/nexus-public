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
package org.sonatype.nexus.quartz.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.quartz.TaskSchedulerHelper;

import com.google.common.base.Throwables;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.SimpleJobFactory;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

/**
 * IT for {@link Scheduler} created by {@link QuartzSchedulerSPI}. Based on original Quartz 2.2.2
 * AbstractSchedulerTest.
 *
 * @see <a href="http://svn.terracotta.org/svn/quartz/tags/quartz-2.2.2/quartz-core/src/test/java/org/quartz/AbstractSchedulerTest.java">AbstractSchedulerTest.java</a>
 */
@Ignore("NEXUS-43375")
public class SchedulerTest
    extends TestSupport
{
  //@Rule
  //public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  public TaskSchedulerHelper taskSchedulerHelper;

  @After
  public void after() throws Exception {
    if (taskSchedulerHelper != null) {
      taskSchedulerHelper.stop();
      taskSchedulerHelper = null;
    }
  }

  private static final String BARRIER = "BARRIER";

  private static final String DATE_STAMPS = "DATE_STAMPS";

  private static final String JOB_THREAD = "JOB_THREAD";

  public static class TestJob
      implements Job
  {
    public void execute(JobExecutionContext context)
        throws JobExecutionException
    {
      // nop
    }
  }

  public static final long TEST_TIMEOUT_SECONDS = 125;

  public static class TestJobWithSync
      implements Job
  {
    public void execute(JobExecutionContext context)
        throws JobExecutionException
    {
      try {
        @SuppressWarnings("unchecked")
        List<Long> jobExecTimestamps = (List<Long>) context.getScheduler().getContext().get(DATE_STAMPS);
        CyclicBarrier barrier = (CyclicBarrier) context.getScheduler().getContext().get(BARRIER);

        jobExecTimestamps.add(System.currentTimeMillis());

        barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
      catch (Throwable e) {
        e.printStackTrace();
        throw new AssertionError("Await on barrier was interrupted: " + e.toString());
      }
    }
  }

  protected Scheduler createScheduler(String name, int threadPoolSize) throws SchedulerException {
    try {
      //this.taskSchedulerHelper = new TaskSchedulerHelper(database.getInstance());
      this.taskSchedulerHelper.init(threadPoolSize, new SimpleJobFactory());
      this.taskSchedulerHelper.start();
      return ((QuartzSchedulerSPI) taskSchedulerHelper.getScheduler()).getScheduler();
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testBasicStorageFunctions() throws Exception {
    Scheduler sched = createScheduler("testBasicStorageFunctions", 2);

    // test basic storage functions of scheduler...

    JobDetail job = newJob()
        .ofType(TestJob.class)
        .withIdentity("j1")
        .storeDurably()
        .build();

    assertFalse("Unexpected existence of job named 'j1'.", sched.checkExists(jobKey("j1")));

    sched.addJob(job, false);

    assertTrue("Expected existence of job named 'j1' but checkExists return false.", sched.checkExists(jobKey("j1")));

    job = sched.getJobDetail(jobKey("j1"));

    assertNotNull("Stored job not found!", job);

    sched.deleteJob(jobKey("j1"));

    Trigger trigger = newTrigger()
        .withIdentity("t1")
        .forJob(job)
        .startNow()
        .withSchedule(simpleSchedule()
            .repeatForever()
            .withIntervalInSeconds(5))
        .build();

    assertFalse("Unexpected existence of trigger named 't1'.", sched.checkExists(triggerKey("t1")));

    sched.scheduleJob(job, trigger);

    assertTrue("Expected existence of trigger named 't1' but checkExists return false.",
        sched.checkExists(triggerKey("t1")));

    job = sched.getJobDetail(jobKey("j1"));

    assertNotNull("Stored job not found!", job);

    trigger = sched.getTrigger(triggerKey("t1"));

    assertNotNull("Stored trigger not found!", trigger);

    job = newJob()
        .ofType(TestJob.class)
        .withIdentity("j2", "g1")
        .build();

    trigger = newTrigger()
        .withIdentity("t2", "g1")
        .forJob(job)
        .startNow()
        .withSchedule(simpleSchedule()
            .repeatForever()
            .withIntervalInSeconds(5))
        .build();

    sched.scheduleJob(job, trigger);

    job = newJob()
        .ofType(TestJob.class)
        .withIdentity("j3", "g1")
        .build();

    trigger = newTrigger()
        .withIdentity("t3", "g1")
        .forJob(job)
        .startNow()
        .withSchedule(simpleSchedule()
            .repeatForever()
            .withIntervalInSeconds(5))
        .build();

    sched.scheduleJob(job, trigger);


    List<String> jobGroups = sched.getJobGroupNames();
    List<String> triggerGroups = sched.getTriggerGroupNames();

    assertTrue("Job group list size expected to be = 2 ", jobGroups.size() == 2);
    assertTrue("Trigger group list size expected to be = 2 ", triggerGroups.size() == 2);

    Set<JobKey> jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP));
    Set<TriggerKey> triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals(TriggerKey.DEFAULT_GROUP));

    assertTrue("Number of jobs expected in default group was 1 ", jobKeys.size() == 1);
    assertTrue("Number of triggers expected in default group was 1 ", triggerKeys.size() == 1);

    jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals("g1"));
    triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals("g1"));

    assertTrue("Number of jobs expected in 'g1' group was 2 ", jobKeys.size() == 2);
    assertTrue("Number of triggers expected in 'g1' group was 2 ", triggerKeys.size() == 2);


    TriggerState s = sched.getTriggerState(triggerKey("t2", "g1"));
    assertTrue("State of trigger t2 expected to be NORMAL ", s.equals(TriggerState.NORMAL));

    sched.pauseTrigger(triggerKey("t2", "g1"));
    s = sched.getTriggerState(triggerKey("t2", "g1"));
    assertTrue("State of trigger t2 expected to be PAUSED ", s.equals(TriggerState.PAUSED));

    sched.resumeTrigger(triggerKey("t2", "g1"));
    s = sched.getTriggerState(triggerKey("t2", "g1"));
    assertTrue("State of trigger t2 expected to be NORMAL ", s.equals(TriggerState.NORMAL));

    Set<String> pausedGroups = sched.getPausedTriggerGroups();
    assertTrue("Size of paused trigger groups list expected to be 0 ", pausedGroups.size() == 0);

    sched.pauseTriggers(GroupMatcher.triggerGroupEquals("g1"));

    // test that adding a trigger to a paused group causes the new trigger to be paused also...
    job = newJob()
        .ofType(TestJob.class)
        .withIdentity("j4", "g1")
        .build();

    trigger = newTrigger()
        .withIdentity("t4", "g1")
        .forJob(job)
        .startNow()
        .withSchedule(simpleSchedule()
            .repeatForever()
            .withIntervalInSeconds(5))
        .build();

    sched.scheduleJob(job, trigger);
    // TODO: nexus hack: JobStoreImpl DOES NOT "remember" paused groups
    sched.pauseJob(jobKey("j4", "g1"));

    pausedGroups = sched.getPausedTriggerGroups();
    assertTrue("Size of paused trigger groups list expected to be 1: " + pausedGroups, pausedGroups.size() == 1);

    s = sched.getTriggerState(triggerKey("t2", "g1"));
    assertTrue("State of trigger t2 expected to be PAUSED ", s.equals(TriggerState.PAUSED));

    s = sched.getTriggerState(triggerKey("t4", "g1"));
    assertTrue("State of trigger t4 expected to be PAUSED ", s.equals(TriggerState.PAUSED));

    sched.resumeTriggers(GroupMatcher.triggerGroupEquals("g1"));
    s = sched.getTriggerState(triggerKey("t2", "g1"));
    assertTrue("State of trigger t2 expected to be NORMAL ", s.equals(TriggerState.NORMAL));
    s = sched.getTriggerState(triggerKey("t4", "g1"));
    assertTrue("State of trigger t4 expected to be NORMAL ", s.equals(TriggerState.NORMAL));
    pausedGroups = sched.getPausedTriggerGroups();
    assertTrue("Size of paused trigger groups list expected to be 0 ", pausedGroups.size() == 0);


    assertFalse("Scheduler should have returned 'false' from attempt to unschedule non-existing trigger. ",
        sched.unscheduleJob(triggerKey("foasldfksajdflk")));

    assertTrue("Scheduler should have returned 'true' from attempt to unschedule existing trigger. ",
        sched.unscheduleJob(triggerKey("t3", "g1")));

    jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals("g1"));
    triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals("g1"));

    assertTrue("Number of jobs expected in 'g1' group was 1 ",
        jobKeys.size() == 2); // job should have been deleted also, because it is non-durable
    assertTrue("Number of triggers expected in 'g1' group was 1 ", triggerKeys.size() == 2);

    assertTrue("Scheduler should have returned 'true' from attempt to unschedule existing trigger. ",
        sched.unscheduleJob(triggerKey("t1")));

    jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP));
    triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals(TriggerKey.DEFAULT_GROUP));

    assertTrue("Number of jobs expected in default group was 1 ",
        jobKeys.size() == 1); // job should have been left in place, because it is non-durable
    assertTrue("Number of triggers expected in default group was 0 ", triggerKeys.size() == 0);

    sched.shutdown(true);
  }

  @Test
  public void testDurableStorageFunctions() throws Exception {
    Scheduler sched = createScheduler("testDurableStorageFunctions", 2);
    try {
      // test basic storage functions of scheduler...

      JobDetail job = newJob()
          .ofType(TestJob.class)
          .withIdentity("j1")
          .storeDurably()
          .build();

      assertFalse("Unexpected existence of job named 'j1'.", sched.checkExists(jobKey("j1")));

      sched.addJob(job, false);

      assertTrue("Unexpected non-existence of job named 'j1'.", sched.checkExists(jobKey("j1")));

      JobDetail nonDurableJob = newJob()
          .ofType(TestJob.class)
          .withIdentity("j2")
          .build();

      try {
        sched.addJob(nonDurableJob, false);
        fail("Storage of non-durable job should not have succeeded.");
      }
      catch (SchedulerException expected) {
        assertFalse("Unexpected existence of job named 'j2'.", sched.checkExists(jobKey("j2")));
      }

      sched.addJob(nonDurableJob, false, true);

      assertTrue("Unexpected non-existence of job named 'j2'.", sched.checkExists(jobKey("j2")));
    }
    finally {
      sched.shutdown(true);
    }
  }

  @Test
  public void testShutdownWithSleepReturnsAfterAllThreadsAreStopped() throws Exception {
    Map<Thread, StackTraceElement[]> allThreadsStart = Thread.getAllStackTraces();
    int threadPoolSize = 5;
    Scheduler scheduler = createScheduler("testShutdownWithSleepReturnsAfterAllThreadsAreStopped", threadPoolSize);

    Map<Thread, StackTraceElement[]> allThreadsRunning = Thread.getAllStackTraces();

    scheduler.shutdown(true);

    Map<Thread, StackTraceElement[]> allThreadsEnd = Thread.getAllStackTraces();
    Set<Thread> endingThreads = new HashSet<Thread>(allThreadsEnd.keySet());
    // remove all pre-existing threads from the set
    for (Thread t : allThreadsStart.keySet()) {
      allThreadsEnd.remove(t);
    }
    // remove threads that are known artifacts of the test
    for (Thread t : endingThreads) {
      if (t.getName().contains("derby") && t.getThreadGroup().getName().contains("derby")) {
        allThreadsEnd.remove(t);
      }
      if (t.getThreadGroup() != null && t.getThreadGroup().getName().equals("system")) {
        allThreadsEnd.remove(t);

      }
      if (t.getThreadGroup() != null && t.getThreadGroup().getName().equals("main")) {
        allThreadsEnd.remove(t);
      }
    }
    if (allThreadsEnd.size() > 0) {
      // log the additional threads
      for (Thread t : allThreadsEnd.keySet()) {
        System.out.println(
            "*** Found additional thread: " + t.getName() + " (of type " + t.getClass().getName() + ")  in group: " +
                t.getThreadGroup().getName() + " with parent group: " +
                (t.getThreadGroup().getParent() == null ? "-none-" : t.getThreadGroup().getParent().getName()));
      }
      // log all threads that were running before shutdown
      for (Thread t : allThreadsRunning.keySet()) {
        System.out.println(
            "- Test runtime thread: " + t.getName() + " (of type " + t.getClass().getName() + ")  in group: " +
                (t.getThreadGroup() == null ? "-none-" : (t.getThreadGroup().getName() + " with parent group: " +
                    (t.getThreadGroup().getParent() == null ? "-none-" : t.getThreadGroup().getParent().getName()))));
      }
    }
    assertTrue("Found unexpected new threads (see console output for listing)", allThreadsEnd.size() == 0);
  }

  @Test
  public void testAbilityToFireImmediatelyWhenStartedBefore() throws Exception {
    List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
    CyclicBarrier barrier = new CyclicBarrier(2);

    Scheduler sched = createScheduler("testAbilityToFireImmediatelyWhenStartedBefore", 5);
    sched.getContext().put(BARRIER, barrier);
    sched.getContext().put(DATE_STAMPS, jobExecTimestamps);
    sched.start();

    Thread.yield();

    JobDetail job1 = JobBuilder.newJob(TestJobWithSync.class).withIdentity("job1").build();
    Trigger trigger1 = TriggerBuilder.newTrigger().forJob(job1).build();

    long sTime = System.currentTimeMillis();

    sched.scheduleJob(job1, trigger1);

    barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    sched.shutdown(true);

    long fTime = jobExecTimestamps.get(0);

    assertTrue("Immediate trigger did not fire within a reasonable amount of time.",
        (fTime - sTime < 7000L));  // This is dangerously subjective!  but what else to do?
  }

  @Test
  public void testAbilityToFireImmediatelyWhenStartedBeforeWithTriggerJob() throws Exception {
    List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
    CyclicBarrier barrier = new CyclicBarrier(2);

    Scheduler sched = createScheduler("testAbilityToFireImmediatelyWhenStartedBeforeWithTriggerJob", 5);
    sched.getContext().put(BARRIER, barrier);
    sched.getContext().put(DATE_STAMPS, jobExecTimestamps);

    sched.start();

    Thread.yield();

    JobDetail job1 = JobBuilder.newJob(TestJobWithSync.class).withIdentity("job1").storeDurably().build();
    sched.addJob(job1, false);

    long sTime = System.currentTimeMillis();

    sched.triggerJob(job1.getKey());

    barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    sched.shutdown(true);

    long fTime = jobExecTimestamps.get(0);

    assertTrue("Immediate trigger did not fire within a reasonable amount of time.",
        (fTime - sTime < 7000L));  // This is dangerously subjective!  but what else to do?
  }

  @Test
  public void testAbilityToFireImmediatelyWhenStartedAfter() throws Exception {
    List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
    CyclicBarrier barrier = new CyclicBarrier(2);

    Scheduler sched = createScheduler("testAbilityToFireImmediatelyWhenStartedAfter", 5);
    sched.getContext().put(BARRIER, barrier);
    sched.getContext().put(DATE_STAMPS, jobExecTimestamps);

    JobDetail job1 = JobBuilder.newJob(TestJobWithSync.class).withIdentity("job1").build();
    Trigger trigger1 = TriggerBuilder.newTrigger().forJob(job1).build();

    long sTime = System.currentTimeMillis();

    sched.scheduleJob(job1, trigger1);
    sched.start();

    barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    sched.shutdown(true);

    long fTime = jobExecTimestamps.get(0);

    assertTrue("Immediate trigger did not fire within a reasonable amount of time.",
        (fTime - sTime < 7000L));  // This is dangerously subjective!  but what else to do?
  }

  @Test
  public void testScheduleMultipleTriggersForAJob() throws SchedulerException {
    JobDetail job = newJob(TestJob.class).withIdentity("job1", "group1").build();
    Trigger trigger1 = newTrigger()
        .withIdentity("trigger1", "group1")
        .startNow()
        .withSchedule(
            SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1)
                .repeatForever())
        .build();
    Trigger trigger2 = newTrigger()
        .withIdentity("trigger2", "group1")
        .startNow()
        .withSchedule(
            SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1)
                .repeatForever())
        .build();
    Set<Trigger> triggersForJob = new HashSet<Trigger>();
    triggersForJob.add(trigger1);
    triggersForJob.add(trigger2);

    Scheduler sched = createScheduler("testScheduleMultipleTriggersForAJob", 5);
    sched.scheduleJob(job, triggersForJob, true);

    List<? extends Trigger> triggersOfJob = sched.getTriggersOfJob(job.getKey());
    assertEquals(2, triggersOfJob.size());
    assertTrue(triggersOfJob.contains(trigger1));
    assertTrue(triggersOfJob.contains(trigger2));

    sched.shutdown(true);
  }

  @Test
  public void testShutdownWithoutWaitIsUnclean() throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    Scheduler scheduler = createScheduler("testShutdownWithoutWaitIsUnclean", 8);
    try {
      scheduler.getContext().put(BARRIER, barrier);
      scheduler.start();
      scheduler.addJob(newJob().ofType(UncleanShutdownJob.class).withIdentity("job").storeDurably().build(), false);
      scheduler.scheduleJob(newTrigger().forJob("job").startNow().build());
      while (scheduler.getCurrentlyExecutingJobs().isEmpty()) {
        Thread.sleep(50);
      }
    }
    finally {
      scheduler.shutdown(false);
    }

    barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    Thread jobThread = (Thread) scheduler.getContext().get(JOB_THREAD);
    assertThat(jobThread, notNullValue());
    jobThread.join(TimeUnit.SECONDS.toMillis(TEST_TIMEOUT_SECONDS));
  }

  public static class UncleanShutdownJob
      implements Job
  {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        SchedulerContext schedulerContext = context.getScheduler().getContext();
        schedulerContext.put(JOB_THREAD, Thread.currentThread());
        CyclicBarrier barrier = (CyclicBarrier) schedulerContext.get(BARRIER);
        barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
      catch (Throwable e) {
        e.printStackTrace();
        throw new AssertionError("Await on barrier was interrupted: " + e.toString());
      }
    }
  }

  @Test
  public void testShutdownWithWaitIsClean() throws Exception {
    final AtomicBoolean shutdown = new AtomicBoolean(false);
    List<Long> jobExecTimestamps = Collections.synchronizedList(new ArrayList<Long>());
    CyclicBarrier barrier = new CyclicBarrier(2);
    final Scheduler scheduler = createScheduler("testShutdownWithWaitIsClean", 8);
    try {
      scheduler.getContext().put(BARRIER, barrier);
      scheduler.getContext().put(DATE_STAMPS, jobExecTimestamps);
      scheduler.start();
      scheduler.addJob(newJob().ofType(TestJobWithSync.class).withIdentity("job").storeDurably().build(), false);
      scheduler.scheduleJob(newTrigger().forJob("job").startNow().build());
      while (scheduler.getCurrentlyExecutingJobs().isEmpty()) {
        Thread.sleep(50);
      }
    }
    finally {
      Thread t = new Thread()
      {
        @Override
        public void run() {
          try {
            scheduler.shutdown(true);
            shutdown.set(true);
          }
          catch (SchedulerException ex) {
            throw new RuntimeException(ex);
          }
        }
      };
      t.start();
      Thread.sleep(1000);
      assertFalse(shutdown.get());
      barrier.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      t.join();
    }
  }
}
