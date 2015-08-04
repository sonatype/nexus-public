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
import java.util.concurrent.Callable;

import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DisabledScheduledTaskTest
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
  public void testRunDisabledTask()
      throws Exception
  {
    ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", new TestIntegerCallable(), this
        .getTestSchedule(0));
    task.setEnabled(false);

    // manually run the task
    task.runNow();

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    // just loop until there is more than 1 run count, which means we should have a new scheduled time
    for (int i = 0; i < 11 && !TaskState.WAITING.equals(task.getTaskState()); i++) {
      if (i == 11) {
        Assert.fail("Waited too long for task to be in waiting state");
      }
      Thread.sleep(500);
    }

    assertEquals(1, task.getResults().get(0).intValue());

    assertNotNull(task.getNextRun());

    // make sure the task is still disabled
    assertFalse(task.isEnabled());

    assertEquals(1, defaultScheduler.getAllTasks().size());
  }

  @Test
  public void testDisabledTaskOnSchedule()
      throws Exception
  {
    ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", new TestIntegerCallable(), this
        .getTestSchedule(200));
    task.setEnabled(false);

    assertEquals(1, defaultScheduler.getAllTasks().size());

    Thread.sleep(300);

    assertNull(task.getLastRun());

    assertNotNull(task.getNextRun());

    assertEquals(1, defaultScheduler.getAllTasks().size());
  }

  @Test
  public void testRestoreDisabledTask()
      throws Exception
  {
    ScheduledTask<Integer> task = defaultScheduler.schedule("Test Task", new TestIntegerCallable(), this
        .getTestSchedule(200));

    task.setEnabled(false);

    task = defaultScheduler.initialize(
        task.getId(),
        task.getName(),
        task.getType(),
        new TestIntegerCallable(),
        task.getSchedule(),
        task.isEnabled());

    assertEquals(false, task.isEnabled());
  }

  private Schedule getTestSchedule(long waitTime) {
    Date startDate = new Date(System.currentTimeMillis() + waitTime);
    Calendar tempCalendar = Calendar.getInstance();
    tempCalendar.setTime(startDate);
    tempCalendar.add(Calendar.DATE, 7);
    Date endDate = tempCalendar.getTime();

    return new DailySchedule(startDate, endDate);
  }

  public class TestIntegerCallable
      implements Callable<Integer>
  {
    private int runCount = 0;

    public Integer call()
        throws Exception
    {
      return ++runCount;
    }

    public int getRunCount() {
      return runCount;
    }
  }

}
