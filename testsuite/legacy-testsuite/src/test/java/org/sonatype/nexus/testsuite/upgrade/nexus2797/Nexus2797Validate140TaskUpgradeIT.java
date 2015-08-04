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
package org.sonatype.nexus.testsuite.upgrade.nexus2797;

import java.util.Calendar;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Nexus2797Validate140TaskUpgradeIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void validateTaskHasNextRunDate()
      throws Exception
  {
    doIt();

    // now stop and restart nexus, make sure still ok
    restartNexus();

    doIt();
  }

  private void doIt()
      throws Exception
  {
    // not quite sure why, but we add 20 ms to the last run time when calling
    // setLastRun in DefaultScheduledTask
    Date lastRunTime = new Date(1111111111131l);

    assertEquals(TaskScheduleUtil.getTask("task1").getLastRunTime(), lastRunTime.toString());
    assertEquals(TaskScheduleUtil.getTask("task2").getLastRunTime(), lastRunTime.toString());
    assertEquals(TaskScheduleUtil.getTask("task3").getLastRunTime(), lastRunTime.toString());
    assertEquals(TaskScheduleUtil.getTask("task4").getLastRunTime(), "n/a");

    assertEquals(TaskScheduleUtil.getTask("task1").getLastRunTimeInMillis(), (Long) lastRunTime.getTime());
    assertEquals(TaskScheduleUtil.getTask("task2").getLastRunTimeInMillis(), (Long) lastRunTime.getTime());
    assertEquals(TaskScheduleUtil.getTask("task3").getLastRunTimeInMillis(), (Long) lastRunTime.getTime());
    assertNull(TaskScheduleUtil.getTask("task4").getLastRunTimeInMillis());

    //problem was simply that next run time was invalidly calculated, and never set
    //we simply want to make sure it is set
    //we need to fix the next run time, as it will change depending on current date
    Date nextRunTime = fixNextRunTime(new Date(1230777000000l));

    assertEquals(TaskScheduleUtil.getTask("task1").getNextRunTime(), nextRunTime.toString());
    assertEquals(TaskScheduleUtil.getTask("task2").getNextRunTime(), nextRunTime.toString());
    assertEquals(TaskScheduleUtil.getTask("task3").getNextRunTime(), nextRunTime.toString());
    assertEquals(TaskScheduleUtil.getTask("task4").getNextRunTime(), nextRunTime.toString());

    assertEquals(TaskScheduleUtil.getTask("task1").getNextRunTimeInMillis(), (Long) nextRunTime.getTime());
    assertEquals(TaskScheduleUtil.getTask("task2").getNextRunTimeInMillis(), (Long) nextRunTime.getTime());
    assertEquals(TaskScheduleUtil.getTask("task3").getNextRunTimeInMillis(), (Long) nextRunTime.getTime());
    assertEquals(TaskScheduleUtil.getTask("task4").getNextRunTimeInMillis(), (Long) nextRunTime.getTime());
  }

  private Date fixNextRunTime(Date nextRunTime) {
    Calendar now = Calendar.getInstance();

    Calendar cal = Calendar.getInstance();

    cal.setTime(nextRunTime);

    cal.set(Calendar.YEAR, now.get(Calendar.YEAR));
    cal.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR));

    if (cal.before(now)) {
      cal.add(Calendar.DAY_OF_YEAR, 1);
    }

    return cal.getTime();
  }
}
