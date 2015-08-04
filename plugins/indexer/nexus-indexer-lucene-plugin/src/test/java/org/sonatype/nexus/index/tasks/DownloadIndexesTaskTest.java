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
package org.sonatype.nexus.index.tasks;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.TaskState;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class DownloadIndexesTaskTest
    extends NexusAppTestSupport
{
  protected NexusScheduler nexusScheduler;

  protected void setUp()
      throws Exception
  {
    super.setUp();

    nexusScheduler = lookup(NexusScheduler.class);
    MockedIndexerManager.mockInvoked = false;
  }

  protected void tearDown()
      throws Exception
  {
    super.tearDown();
  }

  @Test
  public void testError()
      throws Exception
  {
    MockedIndexerManager.returnError = true;

    DownloadIndexesTask task = nexusScheduler.createTaskInstance(DownloadIndexesTask.class);
    ScheduledTask<Object> handle = nexusScheduler.submit("task", task);
    // block until it finishes
    try {
      handle.get();
      fail("it should have throwed an exception");
    }
    catch (Exception e) {
      assertThat(e.getMessage(), containsString("Mocked Index Error"));
    }

    assertThat("Mock was not invoked", MockedIndexerManager.mockInvoked);
    assertThat(handle.getTaskState(), equalTo(TaskState.BROKEN));
  }

  @Test
  public void testFine()
      throws Exception
  {
    MockedIndexerManager.returnError = false;

    DownloadIndexesTask task = nexusScheduler.createTaskInstance(DownloadIndexesTask.class);
    ScheduledTask<Object> handle = nexusScheduler.submit("task", task);
    // block until it finishes
    handle.get();

    assertThat("Mock was not invoked", MockedIndexerManager.mockInvoked);
    assertThat(handle.getTaskState(), equalTo(TaskState.FINISHED));
  }

}
