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
package org.sonatype.nexus.testsuite.task.nexus4066;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.scheduling.TaskState;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.sonatype.nexus.test.utils.TaskScheduleUtil.newProperty;

/**
 * Check for tasks mutual exclusion (like two reindex tasks for same repository will run serialized, one will "win" and
 * run, one will "loose" and wait for winner to finish).
 */
@RunWith(Parameterized.class)
public class Nexus4066TaskMutualExclusionIT
    extends AbstractNexusIntegrationTest
{

  /*
   * When last argument is false mean task should run in parallel. When it is true task should run serialized.
   */
  @Parameters
  public static List<Object[]> createData() {
    // GofG == group of groups
    return Arrays.asList(new Object[][]{//
                                        {"repo", "group", true},//
                                        {"repo", "repo2", false},//
                                        {"repo", "group2", false},//
                                        {"group", "group2", false},//
                                        {"repo2", "group", false},//
                                        {"repo2", "group2", true},//
                                        {"repo", "GofG", true},//
                                        {"group", "GofG", true},//
                                        {"repo2", "GofG", false},//
                                        {"group2", "GofG", false},//
                                        {"GofG2", "GofG", false},//
                                        {"repo2", "GofG2", true},//
                                        {"group2", "GofG2", true},//
                                        {"repo", "GofG2", false},//
                                        {"group", "GofG2", false},//
    });
  }

  private final String repo1;

  private final String repo2;

  private final boolean shouldWait;

  public Nexus4066TaskMutualExclusionIT(String repo1, String repo2, boolean shouldWait) {
    this.repo1 = repo1;
    this.repo2 = repo2;
    this.shouldWait = shouldWait;
  }

  private List<ScheduledServiceListResource> tasks;

  @Before
  public void w8()
      throws Exception
  {
    tasks = Lists.newArrayList();

    TaskScheduleUtil.waitForAllTasksToStop();
  }

  @After
  public void killTasks()
      throws Exception
  {
    // first I wanna cancel any blocked task, then I cancel the blocker
    Collections.reverse(tasks);

    for (ScheduledServiceListResource task : tasks) {
      TaskScheduleUtil.cancel(task.getId());
    }

    TaskScheduleUtil.deleteAllTasks();
  }

  @Test
  public void run()
      throws Exception
  {

    StringBuilder msg = new StringBuilder("Running tasks:\n");
    try {
      ScheduledServiceListResource task1 = createTask(repo1);
      assertThat(task1.getStatus(), equalTo(TaskState.RUNNING.name()));

      ScheduledServiceListResource task2 = createTask(repo2);

      final List<ScheduledServiceListResource> allTasks = TaskScheduleUtil.getAllTasks();
      for (ScheduledServiceListResource allTask : allTasks) {
        msg.append(allTask.getName()).append(": ");
        msg.append(allTask.getStatus()).append("\n");
      }

      if (shouldWait) {
        assertThat(task2.getStatus(), equalTo(TaskState.SLEEPING.name()));
      }
      else {
        assertThat(task2.getStatus(), equalTo(TaskState.RUNNING.name()));
      }
    }
    catch (java.lang.AssertionError e) {
      throw new RuntimeException(
          "Repo1: " + repo1 + " repo2: " + repo2 + " shouldWait: " + shouldWait + "\n" + msg.toString(), e);
    }
  }

  private ScheduledServiceListResource createTask(String repo)
      throws Exception
  {
    final String taskName = "SleepRepositoryTask_" + repo + "_" + System.nanoTime();
    TaskScheduleUtil.runTask(taskName, "SleepRepositoryTask", 0,
        newProperty("repositoryId", repo),
        newProperty("time", String.valueOf(50)),
        newProperty("cancellable", Boolean.toString(true))
    );

    Thread.sleep(2000);

    ScheduledServiceListResource task = TaskScheduleUtil.getTask(taskName);

    tasks.add(task);

    return task;
  }

}
