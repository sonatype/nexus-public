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
package org.sonatype.nexus.coreui.internal.metrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.internal.upgrade.datastore.UpgradeTaskData;
import org.sonatype.nexus.scheduling.internal.upgrade.datastore.UpgradeTaskStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.coreui.internal.metrics.BlobStoreMetricsStateContributor.BLOBSTORE_METRICS_CALCULATING;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BlobStoreMetricsStateContributorTest
{
  @Mock
  private UpgradeTaskStore upgradeTaskStore;

  @Mock
  private TaskScheduler taskScheduler;

  private BlobStoreMetricsStateContributor underTest;

  @Before
  public void setup() {
    underTest = new BlobStoreMetricsStateContributor(upgradeTaskStore, taskScheduler);
  }

  @Test
  public void testGetState_noTasksRunning() {
    when(upgradeTaskStore.browse()).thenReturn(Stream.empty());
    when(taskScheduler.listsTasks()).thenReturn(Collections.emptyList());

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(BLOBSTORE_METRICS_CALCULATING), is(false));
  }

  @Test
  public void testGetState_upgradeTaskRunning() {
    UpgradeTaskData upgradeTask = mock(UpgradeTaskData.class);
    when(upgradeTask.getTaskId()).thenReturn("nexus.blobstore.metrics.migration.task");

    when(upgradeTaskStore.browse()).thenReturn(Stream.of(upgradeTask));
    when(taskScheduler.listsTasks()).thenReturn(Collections.emptyList());

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(BLOBSTORE_METRICS_CALCULATING), is(true));
  }

  @Test
  public void testGetState_componentNormalizeTaskRunning() {
    UpgradeTaskData upgradeTask = mock(UpgradeTaskData.class);
    when(upgradeTask.getTaskId()).thenReturn("component.normalize.version");

    when(upgradeTaskStore.browse()).thenReturn(Stream.of(upgradeTask));
    when(taskScheduler.listsTasks()).thenReturn(Collections.emptyList());

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(BLOBSTORE_METRICS_CALCULATING), is(true));
  }

  @Test
  public void testGetState_recalculateTaskRunning() {
    TaskInfo taskInfo = mock(TaskInfo.class);
    CurrentState currentState = mock(CurrentState.class);

    when(taskInfo.getTypeId()).thenReturn("blobstore.metrics.reconcile");
    when(taskInfo.getCurrentState()).thenReturn(currentState);
    when(currentState.getState()).thenReturn(TaskState.RUNNING);

    when(upgradeTaskStore.browse()).thenReturn(Stream.empty());
    when(taskScheduler.listsTasks()).thenReturn(List.of(taskInfo));

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(BLOBSTORE_METRICS_CALCULATING), is(true));
  }

  @Test
  public void testGetState_recalculateTaskNotRunning() {
    TaskInfo taskInfo = mock(TaskInfo.class);
    CurrentState currentState = mock(CurrentState.class);

    when(taskInfo.getTypeId()).thenReturn("blobstore.metrics.reconcile");
    when(taskInfo.getCurrentState()).thenReturn(currentState);
    when(currentState.getState()).thenReturn(TaskState.WAITING);

    when(upgradeTaskStore.browse()).thenReturn(Stream.empty());
    when(taskScheduler.listsTasks()).thenReturn(List.of(taskInfo));

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(BLOBSTORE_METRICS_CALCULATING), is(false));
  }

  @Test
  public void testGetState_otherTaskRunning() {
    TaskInfo taskInfo = mock(TaskInfo.class);
    CurrentState currentState = mock(CurrentState.class);

    when(taskInfo.getTypeId()).thenReturn("some.other.task");
    when(taskInfo.getCurrentState()).thenReturn(currentState);
    when(currentState.getState()).thenReturn(TaskState.RUNNING);

    when(upgradeTaskStore.browse()).thenReturn(Stream.empty());
    when(taskScheduler.listsTasks()).thenReturn(List.of(taskInfo));

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(BLOBSTORE_METRICS_CALCULATING), is(false));
  }

  @Test
  public void testGetState_bothTasksRunning() {
    UpgradeTaskData upgradeTask = mock(UpgradeTaskData.class);
    when(upgradeTask.getTaskId()).thenReturn("nexus.blobstore.metrics.migration.task");

    TaskInfo taskInfo = mock(TaskInfo.class);
    CurrentState currentState = mock(CurrentState.class);

    when(taskInfo.getTypeId()).thenReturn("blobstore.metrics.reconcile");
    when(taskInfo.getCurrentState()).thenReturn(currentState);
    when(currentState.getState()).thenReturn(TaskState.RUNNING);

    when(upgradeTaskStore.browse()).thenReturn(Stream.of(upgradeTask));
    when(taskScheduler.listsTasks()).thenReturn(List.of(taskInfo));

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(BLOBSTORE_METRICS_CALCULATING), is(true));
  }
}
