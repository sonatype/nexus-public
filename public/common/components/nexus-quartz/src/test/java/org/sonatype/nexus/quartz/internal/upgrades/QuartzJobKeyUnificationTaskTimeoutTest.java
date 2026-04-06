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
package org.sonatype.nexus.quartz.internal.upgrades;

import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.quartz.SleeperTask;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.testdb.DatabaseTest;

import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for {@link QuartzJobKeyUnificationTask} timeout behavior.
 */
@TestPropertySource(properties = {
    "nexus.quartz.jobkey.unification.timeout=2s",
    "nexus.quartz.jobkey.unification.check.interval=500ms"
})
class QuartzJobKeyUnificationTaskTimeoutTest
    extends QuartzJobKeyUnificationTaskTestSupport
{
  @DatabaseTest
  void shouldTimeoutWhenRunningTaskBlocksMigration() throws Exception {
    try {
      // Create a legacy task
      QuartzTaskInfo legacyTask = createLegacyTask("Blocking Legacy Task");
      assertTaskLegacy(legacyTask);

      // Prepare the SleeperTask to block indefinitely when run
      SleeperTask.reset();

      // Run the legacy task - it will wait for meWait signal (which we won't send until after timeout)
      legacyTask.runNow();
      // Wait for task to signal "started"
      SleeperTask.youWait.await(3, TimeUnit.SECONDS);

      // Run migration task - it should timeout because the running task blocks migration
      TaskInfo migrationTask = runMigrationTask();

      // Wait for migration task to finish
      assertTaskState(WAIT_TIMEOUT, migrationTask, TaskState.OK);

      assertThat(migrationTask.getLastRunState().getEndState(), is(TaskState.FAILED));
      // Verify the error message for the last run contains expected text

      IllegalStateException expected =
          assertThrows(IllegalStateException.class, () -> ((QuartzTaskInfo) migrationTask).getTaskFuture().get());
      String errorMessage = expected.getMessage();

      assertThat(errorMessage, containsString("Timeout reached after 2 sec"));
      assertThat(errorMessage, containsString("1 tasks still need migration (running or blocked)"));
      assertThat(errorMessage, containsString("Task will retry on next node startup"));
    }
    finally {
      // Let the blocking task complete to clean up
      SleeperTask.meWait.countDown();
    }
  }
}
