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
package org.sonatype.nexus.quartz.internal.task;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Provider;

import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link QuartzTaskJob} focusing on the isTrulyBlocking method
 * which enables concurrent execution of blob store compact tasks for different blob stores.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class QuartzTaskJobTest
{
  @Mock
  private Task task;

  @Mock
  private TaskConfiguration currentTaskConfig;

  @Mock
  private EventManager eventManager;

  @Mock
  private Provider<QuartzSchedulerSPI> schedulerProvider;

  @Mock
  private QuartzSchedulerSPI quartzSchedulerSPI;

  @Mock
  private TaskFactory taskFactory;

  @Mock
  private BaseUrlManager baseUrlManager;

  @Mock
  private TaskInfo otherTask;

  @Mock
  private TaskConfiguration otherTaskConfig;

  @Mock
  private CurrentState currentState;

  private QuartzTaskJob quartzTaskJob;

  @Before
  public void setup() throws Exception {
    when(schedulerProvider.get()).thenReturn(quartzSchedulerSPI);
    when(task.taskConfiguration()).thenReturn(currentTaskConfig);
    when(task.getId()).thenReturn("current-task-id");
    when(currentTaskConfig.getTypeId()).thenReturn("blobstore.compact");

    when(otherTask.getId()).thenReturn("other-task-id");
    when(otherTask.getConfiguration()).thenReturn(otherTaskConfig);
    when(otherTask.getCurrentState()).thenReturn(currentState);
    when(currentState.getState()).thenReturn(TaskState.RUNNING);
    when(currentState.getRunState()).thenReturn(TaskState.RUNNING);
    when(otherTaskConfig.getTypeId()).thenReturn("blobstore.compact");

    quartzTaskJob = new QuartzTaskJob(eventManager, schedulerProvider, taskFactory, baseUrlManager);
    setPrivateField(quartzTaskJob, "task", task);
  }

  @Test
  public void testIsTrulyBlocking_sameBlobStore_shouldBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(otherTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Same blob store should block execution", blockedTasks.size(), is(1));
  }

  @Test
  public void testIsTrulyBlocking_differentBlobStores_shouldNotBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(otherTaskConfig.getString("blobstoreName")).thenReturn("my-file-blob");
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Different blob stores should NOT block execution", blockedTasks.size(), is(0));
  }

  @Test
  public void testIsTrulyBlocking_currentTaskNoBlobStore_shouldBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn(null);
    when(otherTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Non-blob-store task should block all same-type tasks", blockedTasks.size(), is(1));
  }

  @Test
  public void testIsTrulyBlocking_otherTaskNoBlobStore_shouldNotBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(otherTaskConfig.getString("blobstoreName")).thenReturn(null);
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Task with blob store should not block task without blob store", blockedTasks.size(), is(0));
  }

  @Test
  public void testIsTrulyBlocking_bothNoBlobStore_shouldBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn(null);
    when(otherTaskConfig.getString("blobstoreName")).thenReturn(null);
    when(currentTaskConfig.getTypeId()).thenReturn("repository.cleanup");
    when(otherTaskConfig.getTypeId()).thenReturn("repository.cleanup");
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Non-blob-store tasks should block each other (default behavior)", blockedTasks.size(), is(1));
  }

  @Test
  public void testIsTrulyBlocking_emptyBlobStoreName_shouldBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn("");
    when(otherTaskConfig.getString("blobstoreName")).thenReturn("");
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Empty blob store names should match", blockedTasks.size(), is(1));
  }

  @Test
  public void testIsTrulyBlocking_emptyVsPopulated_shouldNotBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn("");
    when(otherTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Empty vs populated blob store name should not match", blockedTasks.size(), is(0));
  }

  @Test
  public void testIsTrulyBlocking_differentTaskTypes_shouldNotBlock() throws Exception {
    when(currentTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(otherTaskConfig.getString("blobstoreName")).thenReturn("my-s3-blob");
    when(otherTaskConfig.getTypeId()).thenReturn("repository.cleanup");
    when(quartzSchedulerSPI.listsTasks()).thenReturn(Collections.singletonList(otherTask));

    List<TaskInfo> blockedTasks = invokeBlockedBy();

    assertThat("Different task types should not block even with same blob store", blockedTasks.size(), is(0));
  }

  private List<TaskInfo> invokeBlockedBy() throws Exception {
    Method blockedByMethod = QuartzTaskJob.class.getDeclaredMethod("blockedBy");
    blockedByMethod.setAccessible(true);
    return (List<TaskInfo>) blockedByMethod.invoke(quartzTaskJob);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = QuartzTaskJob.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
