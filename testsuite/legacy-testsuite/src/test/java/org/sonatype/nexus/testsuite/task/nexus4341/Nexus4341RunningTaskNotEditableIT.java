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
package org.sonatype.nexus.testsuite.task.nexus4341;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.sisu.goodies.common.Time;

import org.junit.Test;
import org.restlet.data.Status;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isClientError;

public class Nexus4341RunningTaskNotEditableIT
    extends AbstractNexusIntegrationTest
{

  private ScheduledServiceListResource createTask()
      throws Exception
  {
    final String taskName = "SleepRepositoryTask_" + getTestRepositoryId() + "_" + System.nanoTime();
    TaskScheduleUtil.runTask(taskName, "SleepRepositoryTask", 0,
        TaskScheduleUtil.newProperty("repositoryId", getTestRepositoryId()),
        TaskScheduleUtil.newProperty("time", String.valueOf(10)));

    final ScheduledServiceListResource task = TaskScheduleUtil.getTask(taskName);
    checkNotNull(task, "Task not created!");

    return task;
  }

  private void verifyNoUpdate(ScheduledServiceListResource resource)
      throws IOException
  {
    log.info("Trying to update {} ({})", resource.getName(), resource.getStatus());

    ScheduledServiceBaseResource changed = new ScheduledServiceBaseResource();
    changed.setEnabled(true);
    changed.setId(resource.getId());
    changed.setName("otherName");
    changed.setTypeId(resource.getTypeId());
    changed.setSchedule(resource.getSchedule());
    changed.addProperty(TaskScheduleUtil.newProperty("repositoryId", getTestRepositoryId()));
    changed.addProperty(TaskScheduleUtil.newProperty("time", String.valueOf(10)));

    Status status = TaskScheduleUtil.update(changed);

    assertThat("Should not have been able to update task with state " + resource.getStatus() + ", "
        + status.getDescription(), status, isClientError());
  }

  @Test
  public void testNoUpdateForRunningTasks()
      throws Exception
  {
    ScheduledServiceListResource running = createTask();

    waitForState(running, "RUNNING");
    verifyNoUpdate(running);

    ScheduledServiceListResource sleeping = createTask();

    waitForState(sleeping, "SLEEPING");

    verifyNoUpdate(sleeping);
    TaskScheduleUtil.cancel(sleeping.getId());

    TaskScheduleUtil.cancel(running.getId());
    waitForState(running, "CANCELLING");
    verifyNoUpdate(running);
  }

  private void waitForState(ScheduledServiceListResource task, String state)
      throws Exception
  {
    final long start = System.currentTimeMillis();
    final String name = task.getName();

    String actualState;
    while (!(actualState = task.getStatus()).equals(state)) {
      log.info("Seeing state '{}' for task '{}', waiting for '{}'", new String[]{actualState, name, state});
      if (System.currentTimeMillis() - start > 15000) {
        throw new IllegalStateException(
            String.format(
                "Task '%s' was not in expected state '%s' after waiting for 15s (actual state: '%s')",
                name, state, actualState));
      }

      Time.millis(500).sleep();

      task = TaskScheduleUtil.getTask(name);
      checkNotNull(task, "Task not found!");
    }
  }
}
