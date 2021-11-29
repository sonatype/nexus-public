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
package org.sonatype.nexus.yum.internal.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.internal.support.YumNexusTestSupport;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.yum.internal.task.MergeMetadataTask.ID;
import static org.sonatype.scheduling.TaskState.RUNNING;

public class MergeMetadataTaskTest
    extends YumNexusTestSupport
{

  private static final String GROUP_ID_1 = "group-repo-id-1";

  private static final String GROUP_ID_2 = "group-repo-id-2";

  @Test
  public void shouldNotAllowConcurrentExecutionForSameRepo()
      throws Exception
  {
    final MergeMetadataTask task = new MergeMetadataTask(
        mock(EventBus.class), mock(YumRegistry.class), mock(CommandLineExecutor.class)
    );
    final GroupRepository group = mock(GroupRepository.class);
    when(group.getId()).thenReturn(GROUP_ID_1);
    task.setGroupRepository(group);
    assertThat(task.allowConcurrentExecution(createRunningTaskForGroups(group)), is(false));
  }

  @Test
  public void shouldAllowConcurrentExecutionIfAnotherTaskIsRunning()
      throws Exception
  {
    final MergeMetadataTask task = new MergeMetadataTask(
        mock(EventBus.class), mock(YumRegistry.class), mock(CommandLineExecutor.class)
    );
    final GroupRepository group1 = mock(GroupRepository.class);
    when(group1.getId()).thenReturn(GROUP_ID_1);
    final GroupRepository group2 = mock(GroupRepository.class);
    when(group2.getId()).thenReturn(GROUP_ID_2);
    task.setGroupRepository(group1);
    assertThat(task.allowConcurrentExecution(createRunningTaskForGroups(group2)), is(true));
  }

  private Map<String, List<ScheduledTask<?>>> createRunningTaskForGroups(final GroupRepository... groups) {
    final Map<String, List<ScheduledTask<?>>> map = new HashMap<String, List<ScheduledTask<?>>>();
    final List<ScheduledTask<?>> taskList = new ArrayList<ScheduledTask<?>>();
    for (final GroupRepository group : groups) {
      taskList.add(runningTask(group));
    }
    map.put(ID, taskList);
    return map;
  }

  @SuppressWarnings({"unchecked"})
  private ScheduledTask<?> runningTask(final GroupRepository group) {
    final ScheduledTask<?> task = mock(ScheduledTask.class);
    final MergeMetadataTask otherGenerationTask = mock(MergeMetadataTask.class);
    when(otherGenerationTask.getGroupRepository()).thenReturn(group);
    when(task.getTaskState()).thenReturn(RUNNING);
    when(task.getTask()).thenReturn((Callable) otherGenerationTask);
    return task;
  }

}
