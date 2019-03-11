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
package org.sonatype.nexus.coreui.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo.RunState;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.ALL_REPOSITORIES;

public class BrowseStateContributorTest
    extends TestSupport
{
  @Mock
  private BrowseNodeConfiguration browseNodeConfiguration;

  @Mock
  private TaskScheduler taskScheduler;

  private BrowseStateContributor underTest;

  @Before
  public void setup() {
    when(browseNodeConfiguration.getMaxNodes()).thenReturn(10);
    underTest = new BrowseStateContributor(browseNodeConfiguration, taskScheduler);
  }

  @Test
  public void testGetState() {
    List<TaskInfo> tasks = Arrays
        .asList(createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, ALL_REPOSITORIES));
    when(taskScheduler.listsTasks()).thenReturn(tasks);

    Map<String, Object> state = underTest.getState();

    assertThat(state.size(), is(2));
    assertThat(state.get("rebuildingRepositories"), is(Collections.singleton(ALL_REPOSITORIES)));
    assertThat(state.get("browseTreeMaxNodes"), is(10));
  }

  @Test
  public void testGetState_multipleTasks() {
    List<TaskInfo> tasks = Arrays
        .asList(createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, "repo1"),
            createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, "repo2"),
            createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, "repo3"));
    when(taskScheduler.listsTasks()).thenReturn(tasks);

    Map<String, Object> state = underTest.getState();

    assertThat(state.size(), is(2));
    assertThat(state.get("rebuildingRepositories"), is(Sets.newHashSet("repo1", "repo2", "repo3")));
    assertThat(state.get("browseTreeMaxNodes"), is(10));
  }

  @Test
  public void testGetState_firstTaskAllReposOthersIgnored() {
    List<TaskInfo> tasks = Arrays
        .asList(createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, ALL_REPOSITORIES),
            createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, "repo2"),
            createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, "repo3"));
    when(taskScheduler.listsTasks()).thenReturn(tasks);

    Map<String, Object> state = underTest.getState();

    assertThat(state.size(), is(2));
    assertThat(state.get("rebuildingRepositories"), is(Collections.singleton(ALL_REPOSITORIES)));
    assertThat(state.get("browseTreeMaxNodes"), is(10));

    verifyZeroInteractions(tasks.get(1));
    verifyZeroInteractions(tasks.get(2));
  }

  @Test
  public void testGetState_mixedTaskTypes() {
    List<TaskInfo> tasks = Arrays
        .asList(createTaskInfo(RebuildBrowseNodesTaskDescriptor.TYPE_ID, RunState.RUNNING, "repo1"),
            createTaskInfo("typeId", RunState.RUNNING, "repo2"));
    when(taskScheduler.listsTasks()).thenReturn(tasks);

    Map<String, Object> state = underTest.getState();

    assertThat(state.size(), is(2));
    assertThat(state.get("rebuildingRepositories"), is(Collections.singleton("repo1")));
    assertThat(state.get("browseTreeMaxNodes"), is(10));
  }

  private TaskInfo createTaskInfo(String typeId, RunState runState, String repositoryName) {
    CurrentState currentState = mock(CurrentState.class);
    when(currentState.getRunState()).thenReturn(runState);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("repositoryName", repositoryName);

    TaskInfo taskInfo = mock(TaskInfo.class);
    when(taskInfo.getTypeId()).thenReturn(typeId);
    when(taskInfo.getCurrentState()).thenReturn(currentState);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    return taskInfo;
  }
}
