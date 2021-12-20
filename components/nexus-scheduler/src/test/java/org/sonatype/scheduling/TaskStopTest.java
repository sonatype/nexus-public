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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.scheduling.schedules.RunNowSchedule;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskStopTest
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
  public void testStopTask()
      throws Exception
  {
    RunForeverCallable callable = new RunForeverCallable();

    assertFalse(callable.isAllDone());

    ScheduledTask<Integer> task = defaultScheduler.submit("Test Task", callable);

    assertFalse(callable.isAllDone());

    // Give task a chance to get going for a bit
    callable.blockForStart();

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    assertEquals(TaskState.RUNNING, task.getTaskState());

    task.cancel(true);

    callable.blockForDone();

    // Now check and see if task is still running...
    assertTrue(callable.isAllDone());
  }

  @Test
  public void testCancelOnlyWaitForFinishExecution()
      throws Exception
  {
    RunForeverCallable callable = new RunForeverCallable(500);

    assertFalse(callable.isAllDone());

    ScheduledTask<Integer> task = defaultScheduler.submit("Test Task", callable);

    assertFalse(callable.isAllDone());

    callable.blockForStart();

    assertEquals(1, defaultScheduler.getAllTasks().size());

    assertEquals(TaskState.RUNNING, task.getTaskState());

    task.cancelOnly();

    assertEquals(TaskState.CANCELLING, task.getTaskState());

    assertFalse("task was killed immediately", callable.isAllDone());
    assertFalse("running task was eagerly removed", defaultScheduler.getAllTasks().isEmpty());

    callable.blockForDone();

    Utils.awaitTaskState(task, 1000, TaskState.CANCELLED);

    assertTrue("task was not done", callable.isAllDone());
    assertTrue("task not removed", defaultScheduler.getAllTasks().isEmpty());
  }

  @Test
  public void testCancelDoesNotRemoveRunningTask()
      throws Exception
  {
    RunForeverCallable callable = new RunForeverCallable(500);

    assertFalse(callable.isAllDone());

    ScheduledTask<Integer> task = defaultScheduler.submit("Test Task", callable);

    assertFalse(callable.isAllDone());

    callable.blockForStart();

    assertEquals(1, defaultScheduler.getAllTasks().size());

    assertEquals(TaskState.RUNNING, task.getTaskState());

    task.cancel();

    assertEquals(TaskState.CANCELLING, task.getTaskState());

    assertFalse("task was killed immediately", callable.isAllDone());
    assertFalse("running task was eagerly removed", defaultScheduler.getAllTasks().isEmpty());

    callable.blockForDone();

    Utils.awaitTaskState(task, 1000, TaskState.CANCELLED);

    assertTrue("task was not done", callable.isAllDone());
    assertTrue("task not removed", defaultScheduler.getAllTasks().isEmpty());
  }

  @Test
  public void testCancelRemovesIdleTask() {
    RunForeverCallable callable = new RunForeverCallable(500);

    assertFalse(callable.isAllDone());

    ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", callable, new ManualRunSchedule());

    assertFalse(callable.isAllDone());

    assertEquals(1, defaultScheduler.getAllTasks().size());

    assertEquals(TaskState.SUBMITTED, task.getTaskState());

    task.cancel();

    assertTrue("idle task was not removed", defaultScheduler.getAllTasks().isEmpty());
    assertFalse("task was killed immediately", callable.isAllDone());
  }

  @Test
  public void testCancelledRunningTaskWithScheduleIsRemovedLater()
      throws Exception
  {
    RunForeverCallable callable = new RunForeverCallable(500);

    assertFalse(callable.isAllDone());

    ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", callable, new ManualRunSchedule());

    assertFalse(callable.isAllDone());

    assertEquals(1, defaultScheduler.getAllTasks().size());

    assertEquals(TaskState.SUBMITTED, task.getTaskState());

    task.runNow();

    callable.blockForStart();

    assertEquals(TaskState.RUNNING, task.getTaskState());

    task.cancel();

    assertEquals(TaskState.CANCELLING, task.getTaskState());

    callable.blockForDone();

    Utils.awaitTaskState(task, 1000, TaskState.CANCELLED);

    Utils.awaitZeroTaskCount(defaultScheduler, 1000);
    assertTrue("task was killed immediately", callable.isAllDone());
  }

  @Test
  public void testCancelledRunningTaskWithPeriodicScheduleWhichFailsIsRemovedLater()
      throws Exception
  {
    FailUponCancelCallable callable = new FailUponCancelCallable();

    ScheduledTask<?> task = defaultScheduler.schedule("Test Task", callable, new FewSecondSchedule());

    assertFalse(callable.done.getCount() == 0);

    assertEquals(1, defaultScheduler.getAllTasks().size());

    callable.started.await();

    assertEquals(TaskState.RUNNING, task.getTaskState());

    task.cancel();

    assertEquals(TaskState.CANCELLING, task.getTaskState());

    callable.done.await();

    Utils.awaitZeroTaskCount(defaultScheduler, 1000);
    assertEquals(TaskState.CANCELLED, task.getTaskState());
  }

  @Test
  public void testCancelRemovesBlockedOneShotTasks()
      throws Exception
  {
    RunForeverTask callable = new RunForeverTask(5000);

    assertFalse(callable.isAllDone());

    ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", callable, new RunNowSchedule());

    callable.blockForStart();

    RunForeverTask blockedCallable = new RunForeverTask(5000);
    ScheduledTask<Integer> blockedTask =
        defaultScheduler.schedule("Blocked Task", blockedCallable, new RunNowSchedule());

    Utils.awaitTaskState(blockedTask, 1000, TaskState.SLEEPING);

    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());

    blockedTask.cancelOnly();

    assertEquals(TaskState.CANCELLED, blockedTask.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(1, defaultScheduler.getAllTasks().get(task.getType()).size());

    task.cancel(true);
    callable.blockForDone();
    Utils.awaitZeroTaskCount(defaultScheduler, 1000);
  }

  @Test
  public void testCancelReschedulesBlockedTasks()
      throws Exception
  {
    RunForeverTask callable = new RunForeverTask(3000);

    assertFalse(callable.isAllDone());

    ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", callable, new RunNowSchedule());

    RunForeverTask blockedCallable = new RunForeverTask(3000);
    ScheduledTask<Integer> blockedTask =
        defaultScheduler.schedule("Blocked Task", blockedCallable,
            new DailySchedule(new Date(System.currentTimeMillis() + 5000), null));

    callable.blockForStart();
    blockedTask.runNow();

    Utils.awaitTaskState(blockedTask, 1000, TaskState.SLEEPING);

    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());

    blockedTask.cancelOnly();

    assertEquals(TaskState.WAITING, blockedTask.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());
    assertFalse(blockedTask.getScheduleIterator().isFinished());

    task.cancel(true);
    callable.blockForDone();

    assertEquals(TaskState.WAITING, blockedTask.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(1, defaultScheduler.getAllTasks().get(task.getType()).size());

    blockedTask.cancel();
    Utils.awaitZeroTaskCount(defaultScheduler, 1000);
  }

  @Test
  public void testCancellingStateBlocksTasks()
      throws Exception
  {
    RunForeverTask callable = new RunForeverTask(2000);

    assertFalse(callable.isAllDone());
    assertEquals(0, defaultScheduler.getAllTasks().size());

    ScheduledTask<Integer> task = defaultScheduler.submit("Test Task", callable);

    callable.blockForStart();

    task.cancelOnly();
    assertEquals(TaskState.CANCELLING, task.getTaskState());

    RunForeverTask blockedCallable = new RunForeverTask(5000);
    ScheduledTask<Integer> blockedTask =
        defaultScheduler.schedule("Blocked Task", blockedCallable, new RunNowSchedule());

    Utils.awaitTaskState(blockedTask, 1000, TaskState.SLEEPING);
    assertFalse(blockedCallable.isStarted());

    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());

    blockedTask.cancel();
    assertFalse(blockedCallable.isStarted());

    assertEquals(TaskState.CANCELLED, blockedTask.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(1, defaultScheduler.getAllTasks().get(task.getType()).size());

    // task is already cancelled without interruption, so we have to wait for normal completion
    // task.cancel( true );

    callable.blockForDone();

    assertEquals(0, defaultScheduler.getAllTasks().size());
  }

  @Test
  public void testCancelBlockedTask()
      throws Exception
  {
    RunForeverTask callable = new RunForeverTask();

    assertFalse(callable.isAllDone());
    assertEquals(0, defaultScheduler.getAllTasks().size());

    final ScheduledTask<Integer> task = defaultScheduler.submit("Test Task", callable);

    callable.blockForStart();

    final RunForeverTask blockedCallable = new RunForeverTask();
    final ScheduledTask<Integer> blockedTask =
        defaultScheduler.schedule("Blocked Task", blockedCallable, new ManualRunSchedule());

    Runnable runCancelBlockedTask = new Runnable()
    {

      public void run() {
        blockedTask.runNow();

        Utils.awaitTaskState(blockedTask, 1000, TaskState.SLEEPING);
        assertFalse(blockedCallable.isStarted());

        assertEquals(1, defaultScheduler.getAllTasks().size());
        assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());

        blockedTask.cancelOnly();

        assertFalse(blockedCallable.isStarted());

        assertEquals(TaskState.SUBMITTED, blockedTask.getTaskState());
        assertEquals(1, defaultScheduler.getAllTasks().size());
        assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());
      }
    };

    runCancelBlockedTask.run();
    runCancelBlockedTask.run();
    runCancelBlockedTask.run();

    task.cancel(true);
    blockedTask.cancel(true);

    callable.blockForDone();

    assertEquals(0, defaultScheduler.getAllTasks().size());
  }

  @Test
  public void testDoNotCancelWaitingState()
      throws Exception
  {
    final RunForeverTask callable = new RunForeverTask(2000);

    assertFalse(callable.isAllDone());
    assertEquals(0, defaultScheduler.getAllTasks().size());

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, 1);
    Date start = cal.getTime();
    cal.add(Calendar.DAY_OF_YEAR, 7);
    Date end = cal.getTime();

    final DefaultScheduledTask<Integer> task =
        (DefaultScheduledTask<Integer>) defaultScheduler.schedule("Blocked Task", callable, new DailySchedule(
            start, end));

    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(TaskState.SUBMITTED, task.getTaskState());

    task.cancelOnly();

    assertEquals(TaskState.SUBMITTED, task.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());

    task.runNow();

    callable.blockForStart();

    task.cancelOnly();

    callable.blockForDone();

    assertEquals(TaskState.WAITING, task.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());

    task.cancelOnly();

    assertEquals(TaskState.WAITING, task.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());

    task.setTaskState(TaskState.BROKEN);
    assertEquals(TaskState.BROKEN, task.getTaskState());

    task.cancelOnly();

    assertEquals(TaskState.BROKEN, task.getTaskState());

    task.cancel();
  }

  @Test
  public void testCancelManualRunStateIsSubmitted()
      throws Exception
  {
    RunForeverTask callable = new RunForeverTask(2000);

    assertFalse(callable.isAllDone());
    assertEquals(0, defaultScheduler.getAllTasks().size());

    final ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", callable, new ManualRunSchedule());

    task.runNow();

    callable.blockForStart();

    final RunForeverTask blockedCallable = new RunForeverTask(2000);
    final ScheduledTask<Integer> blockedTask =
        defaultScheduler.schedule("Blocked Task", blockedCallable, new ManualRunSchedule());

    blockedTask.runNow();

    Utils.awaitTaskState(blockedTask, 1000, TaskState.SLEEPING);
    assertFalse(blockedCallable.isStarted());

    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());

    blockedTask.cancelOnly();

    assertFalse(blockedCallable.isStarted());

    assertEquals(TaskState.SUBMITTED, blockedTask.getTaskState());
    assertEquals(1, defaultScheduler.getAllTasks().size());
    assertEquals(2, defaultScheduler.getAllTasks().get(task.getType()).size());

    blockedTask.cancel();

    task.cancelOnly();
    callable.blockForDone();

    Utils.awaitTaskState(task, 1000, TaskState.SUBMITTED);

    task.cancel();

    assertEquals(0, defaultScheduler.getAllTasks().size());
  }

  public class RunForeverCallable
      implements Callable<Integer>
  {
    private volatile boolean allDone = false;

    private final int runTicks;

    public RunForeverCallable() {
      this.runTicks = Integer.MAX_VALUE;
    }

    public RunForeverCallable(int ticks) {
      this.runTicks = ticks;
    }

    private volatile boolean started = false;

    public Integer call()
        throws Exception
    {
      try {
        int ticks = 0;
        while (ticks++ < runTicks) {
          // Replace with Thread.yield() to see the problem. The sleep state will
          // cause the thread to stop
          Thread.sleep(1);
          started = true;
        }
        System.out.println("done running");
      }
      finally {
        allDone = true;
      }

      return null;
    }

    public boolean isAllDone() {
      return allDone;
    }

    public boolean isStarted() {
      return started;
    }

    public void blockForStart()
        throws Exception
    {
      while (started == false) {
        Thread.sleep(10);
      }
    }

    public void blockForDone()
        throws Exception
    {
      while (allDone == false) {
        Thread.sleep(10);
      }
    }
  }

  public class RunForeverTask
      extends RunForeverCallable
      implements SchedulerTask<Integer>
  {

    public RunForeverTask() {
      super();
    }

    public RunForeverTask(int i) {
      super(i);
    }

    public boolean allowConcurrentSubmission(Map<String, List<ScheduledTask<?>>> currentActiveTasks) {
      return true;
    }

    public boolean allowConcurrentExecution(Map<String, List<ScheduledTask<?>>> currentActiveTasks) {
      for (List<ScheduledTask<?>> list : currentActiveTasks.values()) {
        for (ScheduledTask<?> task : list) {
          if (task.getTaskState().isExecuting()) {
            System.out.println("concurrent execution not allowed");
            return false;
          }
        }
      }
      return true;
    }

    public void addParameter(String key, String value) {
    }

    public String getParameter(String key) {
      return null;
    }

    public Map<String, String> getParameters() {
      return null;
    }

  }

  public static class FailUponCancelCallable
      implements Callable<Object>
  {

    public final CountDownLatch started = new CountDownLatch(1);

    public final CountDownLatch done = new CountDownLatch(1);

    public Object call()
        throws Exception
    {
      try {
        started.countDown();
        while (!TaskUtil.getCurrentProgressListener().isCanceled()) {
          try {
            Thread.sleep(100);
          }
          catch (InterruptedException e) {
            // ignored
          }
        }
        Thread.sleep(200);
        throw new Exception("Cancelled, erroring out");
      }
      finally {
        done.countDown();
      }
    }

  }

}
