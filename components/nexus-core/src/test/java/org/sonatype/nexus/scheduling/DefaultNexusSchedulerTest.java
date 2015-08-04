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
package org.sonatype.nexus.scheduling;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.SchedulerTask;

import org.junit.Test;

public class DefaultNexusSchedulerTest
    extends NexusAppTestSupport
{
  private NexusScheduler nexusScheduler;

  @Override
  protected boolean runWithSecurityDisabled() {
    // IT IS NEEDED FROM NOW ON!
    return true;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    nexusScheduler = lookup(NexusScheduler.class);
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    super.tearDown();
  }

  @Test
  public void testDoubleSubmission()
      throws Exception
  {
    DummyWaitingNexusTask rt1 =
        (DummyWaitingNexusTask) lookup(SchedulerTask.class, DummyWaitingNexusTask.class.getName());

    rt1.setAllowConcurrentSubmission(true);

    ScheduledTask<?> t1 = nexusScheduler.submit("test1", rt1);

    DummyWaitingNexusTask rt2 =
        (DummyWaitingNexusTask) lookup(SchedulerTask.class, DummyWaitingNexusTask.class.getName());

    rt2.setAllowConcurrentSubmission(false);

    ScheduledTask<?> t2 = null;

    try {
      // the second submission should fail, since there is already a task of this type submitted
      t2 = nexusScheduler.submit("test2", rt2);

      fail();
    }
    catch (RejectedExecutionException e) {
      // cool
    }
    finally {
      t1.cancel();

      if (t2 != null) {
        t2.cancel();
      }
    }
  }

  @Test
  public void testDoubleSubmissionAllowed()
      throws Exception
  {
    DummyWaitingNexusTask rt1 =
        (DummyWaitingNexusTask) lookup(SchedulerTask.class, DummyWaitingNexusTask.class.getName());

    rt1.setAllowConcurrentSubmission(true);

    ScheduledTask<?> t1 = nexusScheduler.submit("test1", rt1);

    DummyWaitingNexusTask rt2 =
        (DummyWaitingNexusTask) lookup(SchedulerTask.class, DummyWaitingNexusTask.class.getName());

    rt2.setAllowConcurrentSubmission(true);

    ScheduledTask<?> t2 = null;
    try {
      // the second submission should succeed, since it is allowed
      t2 = nexusScheduler.submit("test2", rt2);
    }
    catch (RejectedExecutionException e) {
      fail("Concurrent submission should succeed.");
    }
    finally {
      t1.cancel();

      if (t2 != null) {
        t2.cancel();
      }
    }
  }

  @Test
  public void testGetAsThreadJoinner()
      throws Exception
  {
    DummyWaitingNexusTask rt =
        (DummyWaitingNexusTask) lookup(SchedulerTask.class, DummyWaitingNexusTask.class.getName());
    rt.setResult("result");
    rt.setSleepTime(1000);
    rt.setAllowConcurrentExecution(true);
    rt.setAllowConcurrentSubmission(true);
    long start = System.currentTimeMillis();
    ScheduledTask<Object> schedule = nexusScheduler.submit("getTester", rt);
    assertEquals("Invalid return from schedule.get() after " + ((double) System.currentTimeMillis() - start)
        / 1000, "result", schedule.get());

    double took = ((double) System.currentTimeMillis() - start) / 1000;
    assertTrue(took > 1);
  }

  @Test
  public void testGetAsThreadJoinnerException()
      throws Exception
  {
    ExceptionerNexusTask rt = nexusScheduler.createTaskInstance(ExceptionerNexusTask.class);
    rt.setSleepTime(1000);
    rt.setAllowConcurrentExecution(true);
    rt.setAllowConcurrentSubmission(true);
    long start = System.currentTimeMillis();
    ScheduledTask<Object> schedule = nexusScheduler.submit("getException", rt);
    try {
      assertEquals("Invalid return from schedule.get() after " + ((double) System.currentTimeMillis() - start)
          / 1000, "result", schedule.get());
      fail("Should throw error");
    }
    catch (ExecutionException e) {
      assertEquals(RuntimeException.class, e.getCause().getClass());
      assertEquals("Error", e.getCause().getMessage());
    }

    double took = ((double) System.currentTimeMillis() - start) / 1000;
    assertTrue(took > 1);
  }
}
