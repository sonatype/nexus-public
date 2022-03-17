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

import java.util.concurrent.Callable;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RunNowSchedulerTest
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
  public void testRunNowRunnable()
      throws Exception
  {
    TestRunnable tr = new TestRunnable();

    ScheduledTask<Object> st = defaultScheduler.submit("default", tr);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    while (!st.getTaskState().isEndingState()) {
      Thread.sleep(300);
    }

    assertEquals(1, tr.getRunCount());

    assertEquals(TaskState.FINISHED, st.getTaskState());

    assertNull(st.getNextRun());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
  }

  @Test
  public void testRunNowCallable()
      throws Exception
  {
    TestCallable tr = new TestCallable();

    ScheduledTask<Integer> st = defaultScheduler.submit("default", tr);

    assertEquals(1, defaultScheduler.getActiveTasks().size());

    while (!st.getTaskState().isEndingState()) {
      Thread.sleep(300);
    }

    assertEquals(1, tr.getRunCount());

    assertEquals(1, st.getResults().size());

    assertEquals(Integer.valueOf(0), st.getResults().get(0));

    assertEquals(TaskState.FINISHED, st.getTaskState());

    assertNull(st.getNextRun());

    assertEquals(0, defaultScheduler.getActiveTasks().size());
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
    private int runCount = 0;

    public Integer call()
        throws Exception
    {
      return runCount++;
    }

    public int getRunCount() {
      return runCount;
    }
  }

}
