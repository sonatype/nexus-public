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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.quartz.SleeperTask;
import org.sonatype.nexus.quartz.internal.task.QuartzTaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskState;

import org.sonatype.nexus.testdb.DatabaseTest;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * Integration tests for {@link QuartzJobKeyUnificationTask}.
 */
@TestPropertySource(properties = {
    "nexus.quartz.jobkey.unification.timeout=30s",
    "nexus.quartz.jobkey.unification.check.interval=1s"
})
class QuartzJobKeyUnificationTaskTest
    extends QuartzJobKeyUnificationTaskTestSupport
{
  @DatabaseTest
  void shouldMigrateNonRunningTasks() {
    // Create legacy tasks with mismatched job_name and config_id
    QuartzTaskInfo task1 = createLegacyTask("Legacy Task 1");
    QuartzTaskInfo task2 = createLegacyTask("Legacy Task 2");
    QuartzTaskInfo task3 = createLegacyTask("Legacy Task 3");

    // Verify they have mismatched IDs (legacy state)
    assertTaskLegacy(task1);
    assertTaskLegacy(task2);
    assertTaskLegacy(task3);

    // Run the migration task
    TaskInfo migrationTask = runMigrationTask();

    // Wait for completion
    assertTaskState(WAIT_TIMEOUT, migrationTask, TaskState.OK);

    // Verify all tasks are now unified
    QuartzTaskInfo migratedTask1 = findTaskByName("Legacy Task 1");
    QuartzTaskInfo migratedTask2 = findTaskByName("Legacy Task 2");
    QuartzTaskInfo migratedTask3 = findTaskByName("Legacy Task 3");

    assertTaskUnified(migratedTask1);
    assertTaskUnified(migratedTask2);
    assertTaskUnified(migratedTask3);
  }

  @DatabaseTest
  void shouldSkipRunningTaskAndMigrateAfterCompletion() throws Exception {
    // Create a legacy task
    QuartzTaskInfo legacyTask = createLegacyTask("Running Legacy Task");
    assertTaskLegacy(legacyTask);

    // Prepare the SleeperTask to block when run
    SleeperTask.reset();
    // Run the legacy task - it will wait for meWait signal
    legacyTask.runNow();
    // Wait for task to signal "started"
    SleeperTask.youWait.await(3, TimeUnit.SECONDS);

    // Run migration task - it should skip the running task
    TaskInfo migrationTask = runMigrationTask();

    // since task is running, verify it is still legacy
    assertTaskLegacy(legacyTask);

    // Let the running task complete after 3 seconds
    Thread.sleep(3000L);
    SleeperTask.meWait.countDown();

    // Wait for migration to complete
    assertTaskState(WAIT_TIMEOUT, migrationTask, TaskState.OK);

    // After migration completes, the previously running task should now be unified
    QuartzTaskInfo migratedTask = findTaskByName("Running Legacy Task");
    assertTaskUnified(migratedTask);
  }

  @DatabaseTest
  void shouldSkipAlreadyMigratedTasks() {
    // Create tasks WITHOUT the mock - they get unified IDs by default
    QuartzTaskInfo task1 = createNamedTask("Already Unified Task 1");
    QuartzTaskInfo task2 = createNamedTask("Already Unified Task 2");

    // Verify they are already unified
    assertTaskUnified(task1);
    assertTaskUnified(task2);

    // Run migration - should complete quickly with nothing to migrate
    TaskInfo migrationTask = runMigrationTask();

    assertTaskState(WAIT_TIMEOUT, migrationTask, TaskState.OK);

    List<QuartzTaskInfo> tasks = taskScheduler.listsTasks()
        .stream()
        .map(QuartzTaskInfo.class::cast)
        .toList();

    assertThat(tasks, hasSize(2));
    tasks.forEach(this::assertTaskUnified);
  }
}
