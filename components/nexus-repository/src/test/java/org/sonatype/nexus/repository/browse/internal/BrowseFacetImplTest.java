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
package org.sonatype.nexus.repository.browse.internal;

import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID;

public class BrowseFacetImplTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repositoryName";

  private BrowseFacetImpl browseFacet;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private Repository repository;

  @Mock
  private TaskInfo taskInfo;

  @Mock
  private CurrentState currentState;

  @Mock
  private TaskConfiguration taskConfiguration;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this.getClass());
    browseFacet = new BrowseFacetImpl(taskScheduler);
    browseFacet.attach(repository);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(taskInfo.getCurrentState()).thenReturn(currentState);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskScheduler.listsTasks()).thenReturn(Collections.singletonList(taskInfo));
  }

  @Test
  public void isRebuildingIsTrueForAllRepositories() {
    when(taskInfo.getTypeId()).thenReturn(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    when(taskInfo.getCurrentState().getState()).thenReturn(TaskInfo.State.RUNNING);
    when(taskInfo.getConfiguration().getString(REPOSITORY_NAME_FIELD_ID)).thenReturn(RepositorySelector.ALL);

    assertThat(browseFacet.isRebuilding(), is(true));
  }

  @Test
  public void isRebuildingIsTrueForMatchingRepositoryName() {
    when(taskInfo.getTypeId()).thenReturn(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    when(taskInfo.getCurrentState().getState()).thenReturn(TaskInfo.State.RUNNING);
    when(taskInfo.getConfiguration().getString(REPOSITORY_NAME_FIELD_ID)).thenReturn(REPOSITORY_NAME);

    assertThat(browseFacet.isRebuilding(), is(true));
  }

  @Test
  public void isRebuildingIsFalseForNonMatchingTaskTypes() {
    when(taskInfo.getTypeId()).thenReturn("some other type id");
    when(taskInfo.getCurrentState().getState()).thenReturn(TaskInfo.State.RUNNING);
    when(taskInfo.getConfiguration().getString(REPOSITORY_NAME_FIELD_ID)).thenReturn(REPOSITORY_NAME);

    assertThat(browseFacet.isRebuilding(), is(false));
  }

  @Test
  public void isRebuildingIsFalseForNonRunningTasks() {
    when(taskInfo.getTypeId()).thenReturn(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    when(taskInfo.getCurrentState().getState()).thenReturn(State.WAITING);
    when(taskInfo.getConfiguration().getString(REPOSITORY_NAME_FIELD_ID)).thenReturn(REPOSITORY_NAME);

    assertThat(browseFacet.isRebuilding(), is(false));
  }

  @Test
  public void isRebuildingIsFalseForNonMatchingRepositoryName() {
    when(taskInfo.getTypeId()).thenReturn(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    when(taskInfo.getCurrentState().getState()).thenReturn(TaskInfo.State.RUNNING);
    when(taskInfo.getConfiguration().getString(REPOSITORY_NAME_FIELD_ID)).thenReturn("some non matching repo name");

    assertThat(browseFacet.isRebuilding(), is(false));
  }
}
