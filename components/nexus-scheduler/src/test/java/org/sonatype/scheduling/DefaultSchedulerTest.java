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
package org.sonatype.scheduling;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.sonatype.scheduling.schedules.HourlySchedule;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.litmus.testsupport.group.Slow;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DefaultScheduler}.
 */
@Category(Slow.class) // ~15s
public class DefaultSchedulerTest
    extends TestSupport
{
  protected DefaultScheduler defaultScheduler;

  @Before
  public void setUp()
      throws Exception
  {
    defaultScheduler = new DefaultScheduler(new SimpleTaskConfigManager());
  }

  @Test
  public void testSimpleRunnable()
      throws Exception
  {
    TestRunnable tr = null;

    tr = new TestRunnable();

    ScheduledTask<Object> st = defaultScheduler.submit("default", tr);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    while (!st.getTaskState().isEndingState()) {
      Thread.sleep(300);
    }

    assertEquals(1, tr.getRunCount());

    assertEquals(TaskState.FINISHED, st.getTaskState());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  @Test
  public void testSimpleCallable()
      throws Exception
  {
    TestCallable tr = null;

    tr = new TestCallable();

    ScheduledTask<Integer> st = defaultScheduler.submit("default", tr);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    while (!st.getTaskState().isEndingState()) {
      Thread.sleep(300);
    }

    assertEquals(1, tr.getRunCount());

    assertEquals(Integer.valueOf(0), st.getIfDone());

    assertEquals(TaskState.FINISHED, st.getTaskState());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  @Test
  public void testManual()
      throws Exception
  {
    TestCallable tr = new TestCallable();

    ScheduledTask<Integer> st = defaultScheduler.schedule("default", tr, new ManualRunSchedule());

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    // Give the scheduler a chance to start if it would (it shouldn't that's the test)
    Thread.sleep(100);

    assertEquals(TaskState.SUBMITTED, st.getTaskState());

    st.runNow();

    // Give the task a chance to start
    Thread.sleep(100);

    // Now wait for it to finish
    while (!st.getTaskState().equals(TaskState.SUBMITTED)) {
      Thread.sleep(100);
    }

    assertEquals(1, tr.getRunCount());

    assertEquals(TaskState.SUBMITTED, st.getTaskState());

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    st.cancel();

    while (defaultScheduler.getActiveTasks().size() > 0) {
      Thread.sleep(100);
    }
  }

  @Test
  public void testSecondsRunnable()
      throws Exception
  {
    TestRunnable tr = null;

    tr = new TestRunnable();

    long nearFuture = System.currentTimeMillis() + 500;

    Schedule schedule = getEverySecondSchedule(new Date(nearFuture), new Date(nearFuture + 4900));

    ScheduledTask<Object> st = defaultScheduler.schedule("default", tr, schedule);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    while (!st.getTaskState().isEndingState()) {
      Thread.sleep(300);
    }

    assertEquals(5, tr.getRunCount());

    assertEquals(TaskState.FINISHED, st.getTaskState());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  @Test
  public void testSecondsCallable()
      throws Exception
  {
    TestCallable tr = null;

    tr = new TestCallable();

    long nearFuture = System.currentTimeMillis() + 500;

    Schedule schedule = getEverySecondSchedule(new Date(nearFuture), new Date(nearFuture + 4900));

    ScheduledTask<Integer> st = defaultScheduler.schedule("default", tr, schedule);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    while (!st.getTaskState().isEndingState()) {
      Thread.sleep(300);
    }

    assertEquals(5, tr.getRunCount());

    assertEquals(5, st.getResults().size());

    assertEquals(Integer.valueOf(0), st.getResults().get(0));

    assertEquals(Integer.valueOf(1), st.getResults().get(1));

    assertEquals(Integer.valueOf(2), st.getResults().get(2));

    assertEquals(Integer.valueOf(3), st.getResults().get(3));

    assertEquals(Integer.valueOf(4), st.getResults().get(4));

    assertEquals(TaskState.FINISHED, st.getTaskState());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  @Test
  public void testCancelRunnable()
      throws Exception
  {
    TestRunnable tr = null;

    tr = new TestRunnable();

    long nearFuture = System.currentTimeMillis() + 500;

    Schedule schedule = getEverySecondSchedule(new Date(nearFuture), new Date(nearFuture + 4900));

    ScheduledTask<Object> st = defaultScheduler.schedule("default", tr, schedule);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    st.cancel();

    assertEquals(0, tr.getRunCount());

    assertTrue(st.getTaskState().isEndingState());

    assertEquals(TaskState.CANCELLED, st.getTaskState());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  @Test
  public void testCancelCallable()
      throws Exception
  {
    TestCallable tr = null;

    tr = new TestCallable();

    long nearFuture = System.currentTimeMillis() + 500;

    Schedule schedule = getEverySecondSchedule(new Date(nearFuture), new Date(nearFuture + 4900));

    ScheduledTask<Integer> st = defaultScheduler.schedule("default", tr, schedule);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    st.cancel();

    assertEquals(0, tr.getRunCount());

    assertTrue(st.getTaskState().isEndingState());

    assertEquals(TaskState.CANCELLED, st.getTaskState());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  @Test
  public void testBrokenCallable()
      throws Exception
  {
    BrokenTestCallable callable = new BrokenTestCallable();

    long nearFuture = System.currentTimeMillis() + 500;

    Schedule schedule = getEverySecondSchedule(new Date(nearFuture), new Date(nearFuture + 1200));

    ScheduledTask<Integer> task = defaultScheduler.schedule("default", callable, schedule);

    Thread.sleep(700);

    assertEquals(TaskState.BROKEN, task.getTaskState());

    Thread.sleep(1000);

    assertEquals(0, defaultScheduler.getAllTasks().size());

    // assertEquals( TaskState.BROKEN, task.getTaskState() );
  }

  /**
   * Validate that setting schedule during run properly sets next schedule time
   */
  @Test
  @Ignore("FIXME: This test is unstable")
  public void testChangeScheduleDuringRunCallable()
      throws Exception
  {
    // FIXME: This test is unstable

    TestChangeScheduleDuringRunCallable callable = new TestChangeScheduleDuringRunCallable(200000);

    long nearFuture = System.currentTimeMillis() + 500;

    Schedule schedule = getEverySecondSchedule(new Date(nearFuture), null);

    ScheduledTask<Integer> task = defaultScheduler.schedule("default", callable, schedule);

    callable.setTask(task);

    // save some time and loop until we see time is set properly
    for (int i = 0; i < 11 && callable.getRunCount() < 1; i++) {
      if (i == 11) {
        Assert.fail("Waited too long for callable to have run count greater than 0 it is "
            + callable.getRunCount());
      }
      Thread.sleep(500);
    }

    // if the next run we set, and the next run of task are the same, its proof that we
    // have broken the cycle and introduced new schedule
    Assert.assertEquals(callable.getNextRun(), task.getNextRun());

    task.cancel(true);
  }

  @Ignore("FIXME: This test is unstable")
  @Test
  public void testCallableStepOnEachOtherToe()
      throws Exception
  {
    // FIXME: This test is unstable

    TestCallable tr = null;

    // work that will sleep 3 seconds
    tr = new TestCallable(3000L);

    long nearFuture = System.currentTimeMillis() + 500;

    Schedule schedule = getEverySecondSchedule(new Date(nearFuture), new Date(nearFuture + 4900));

    ScheduledTask<Integer> st = defaultScheduler.schedule("default", tr, schedule);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    Thread.sleep(1200);

    int count = 0;

    // hack: we used deprecated constructor, so we know actual imple here
    while ((count =
        ((ScheduledThreadPoolExecutor) defaultScheduler.getScheduledExecutorService()).getActiveCount()) > 0) {
      assertEquals("We scheduled one task, but more than one is executing?", 1,
          count);

      Thread.sleep(10);
    }

    assertEquals(3, tr.getRunCount());

    assertEquals(3, st.getResults().size());

    assertEquals(Integer.valueOf(0), st.getResults().get(0));

    assertEquals(Integer.valueOf(1), st.getResults().get(1));

    assertEquals(Integer.valueOf(2), st.getResults().get(2));

    assertEquals(TaskState.FINISHED, st.getTaskState());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  protected Schedule getEverySecondSchedule(Date start, Date stop) {
    return new FewSecondSchedule(start, stop, 1);
  }

  // Helper classes

  public class TestRunnable
      implements Runnable
  {
    private int runCount = 0;

    public void run() {
      runCount++;
    }

    public int getRunCount() {
      return runCount;
    }
  }

  public class TestCallable
      implements Callable<Integer>
  {
    private final Long sleepTime;

    private int runCount = 0;

    public TestCallable() {
      this(null);
    }

    public TestCallable(Long sleepTime) {
      this.sleepTime = sleepTime;
    }

    public Integer call()
        throws Exception
    {
      if (sleepTime != null) {
        Thread.sleep(sleepTime);
      }

      return runCount++;
    }

    public int getRunCount() {
      return runCount;
    }
  }

  public class BrokenTestCallable
      implements Callable<Integer>
  {
    public Integer call()
        throws Exception
    {
      throw new Exception("Test task failed to run");
    }
  }

  public class TestChangeScheduleDuringRunCallable
      implements Callable<Integer>
  {
    private int runCount = 0;

    private ScheduledTask<?> task;

    private Date futureRun;

    private final long futureMillis;

    public TestChangeScheduleDuringRunCallable(long futureMillis) {
      this.futureMillis = futureMillis;
    }

    public Integer call()
        throws Exception
    {
      futureRun = new Date(System.currentTimeMillis() + futureMillis);

      // by doing this, we should see the next scheduled time 200 seconds in future
      task.setSchedule(new HourlySchedule(futureRun, null));

      return runCount++;
    }

    public int getRunCount() {
      return runCount;
    }

    public void setTask(ScheduledTask<?> task) {
      this.task = task;
    }

    public Date getNextRun() {
      return futureRun;
    }
  }
}
