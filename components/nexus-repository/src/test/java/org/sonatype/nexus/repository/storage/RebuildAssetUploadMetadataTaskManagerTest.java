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
package org.sonatype.nexus.repository.storage;

import java.util.Date;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo.RunState;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.TaskRemovedException;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RebuildAssetUploadMetadataTaskManagerTest
    extends TestSupport
{
  private RebuildAssetUploadMetadataTaskManager manager;

  @Mock
  private TaskScheduler taskScheduler;

  private RebuildAssetUploadMetadataConfiguration configuration = new RebuildAssetUploadMetadataConfiguration(true, 0);

  @Before
  public void injectMocks() {
    manager = new RebuildAssetUploadMetadataTaskManager(taskScheduler, configuration);
  }

  @Test
  public void doStartSchedulesNewTasks() throws Exception {
    TaskConfiguration configuration = createTaskConfiguration();
    when(taskScheduler.listsTasks()).thenReturn(emptyList());
    when(taskScheduler.createTaskConfigurationInstance(RebuildAssetUploadMetadataTaskDescriptor.TYPE_ID))
        .thenReturn(configuration);

    manager.doStart();

    verify(taskScheduler).submit(eq(configuration));
  }

  @Test
  public void doStartReschedulesExistingTasks() throws Exception {
    TaskInfo existingTask = createMockTaskInfo(RebuildAssetUploadMetadataTaskDescriptor.TYPE_ID);
    when(taskScheduler.listsTasks()).thenReturn(asList(existingTask));
    when(existingTask.getCurrentState()).thenReturn(createCurrentState(State.WAITING));

    manager.doStart();

    verify(taskScheduler, never()).submit(any());
    verify(existingTask).runNow();
  }

  @Test
  public void doStartIgnoresRunningTasks() throws Exception {
    TaskInfo existingTask = createMockTaskInfo(RebuildAssetUploadMetadataTaskDescriptor.TYPE_ID);
    when(taskScheduler.listsTasks()).thenReturn(asList(existingTask));
    when(existingTask.getCurrentState()).thenReturn(createCurrentState(State.RUNNING));

    manager.doStart();

    verify(taskScheduler, never()).submit(any());
    verify(existingTask, never()).runNow();
  }

  @Test
  public void doStartDoesNotScheduleNewTaskWhenExistingFailsToLaunch() throws Exception {
    TaskInfo existingTask = createMockTaskInfo(RebuildAssetUploadMetadataTaskDescriptor.TYPE_ID);
    when(taskScheduler.listsTasks()).thenReturn(asList(existingTask));
    when(existingTask.getCurrentState()).thenReturn(createCurrentState(State.WAITING));
    when(existingTask.runNow()).thenThrow(new TaskRemovedException(""));

    manager.doStart();

    verify(taskScheduler, never()).submit(any());
    verify(existingTask).runNow();
  }

  private TaskConfiguration createTaskConfiguration() {
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setMessage("message");
    return configuration;
  }

  private TaskInfo createMockTaskInfo(String typeId) {
    TaskConfiguration taskConfiguration = createTaskConfiguration();
    taskConfiguration.setTypeId(typeId);

    TaskInfo taskInfo = mock(TaskInfo.class);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    return taskInfo;
  }

  private CurrentState createCurrentState(State state) {
    CurrentState currentState = new CurrentState()
    {
      @Override
      public State getState() {
        return state;
      }

      @Nullable
      @Override
      public Date getNextRun() {
        return null;
      }

      @Nullable
      @Override
      public Date getRunStarted() {
        return null;
      }

      @Nullable
      @Override
      public RunState getRunState() {
        return null;
      }

      @Nullable
      @Override
      public Future<?> getFuture() {
        return null;
      }
    };

    return currentState;
  }
}
