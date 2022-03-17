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

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 * UT just for reference what is happening in SOME cases of OOM with scheduled task (related to NXCM-4979). This test
 * is
 * disabled as "testing OOM" is not quite reliable, but the code should explain itself.
 *
 * @author cstamas
 */
@Ignore("This test intentionally produces OOM and hence is unreliable, run it locally if needed")
public class OOMTest
    extends TestSupport
{
  public static class OOMRunnable
      implements Runnable
  {

    public void run() {
      List<String> leaker = Lists.newArrayList("This is list element");
      while (true) {
        final List<String> newLeaker = Lists.newArrayList(leaker);
        newLeaker.addAll(leaker);
        leaker = newLeaker;
      }
    }
  }

  // ==

  protected final DefaultScheduler defaultScheduler;

  public OOMTest() {
    defaultScheduler = new DefaultScheduler(new SimpleTaskConfigManager());
  }

  /**
   * The purpose of this test is to represent why OOMed tasks are not "visible" nor detectable in Nexus. While
   * querying {@link ScheduledTask} works, it is usable only if you hold the "handle" (that instance) you got back
   * from {@link Scheduler} once you submitted it. That's not the case in Nexus. If you try to ask for the
   * {@link ScheduledTask} from {@link Scheduler} by ID, you got an exception, as the
   * "task is not here anymore, baby".
   */
  @Test
  public void taskWithOOMIsDetectableOnlyIfYouHaveAHandleForIt()
      throws Exception
  {
    final ScheduledTask<Object> scheduledTask = defaultScheduler.submit("OOM", new OOMRunnable());
    TaskExecutionException brokenCause = null;
    try {
      // block until we are "done" (whether job done or died)
      scheduledTask.get();
    }
    catch (ExecutionException e) {
      // this is Future API, hence our "wrapped" exception will be wrapped into concurrent ExecutionException
      brokenCause = (TaskExecutionException) e.getCause();
    }
    assertThat(brokenCause, notNullValue());
    assertThat(brokenCause.getCause(), instanceOf(OutOfMemoryError.class));

    // scheduled task tells the "naked truth": the actual cause
    assertThat(scheduledTask.getBrokenCause(), instanceOf(OutOfMemoryError.class));
    // both of exceptions actually points to the same instance of OOMError
    assertThat(scheduledTask.getBrokenCause(), equalTo(brokenCause.getCause()));

    // finally, after OOM happened, and you don't have scheduledTask instance,
    // you'd might want to ask Scheduler for it, but nada
    try {
      defaultScheduler.getTaskById(scheduledTask.getId());
      assertThat("We should not get here!", false);
    }
    catch (NoSuchTaskException e) {
      // as the scheduler does not have it anymore
    }
  }
}
