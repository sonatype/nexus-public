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
package org.sonatype.nexus.audit.internal.store;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Cron;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditEventCleanupTaskManager}. No database or scheduler is invoked;
 * {@link TaskScheduler} is mocked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditEventCleanupTaskManagerTest
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private TaskConfiguration taskConfiguration;

  @Mock
  private ScheduleFactory scheduleFactory;

  @Mock
  private Cron schedule;

  private AuditEventCleanupTaskManager manager;

  @BeforeEach
  void setUp() {
    when(taskScheduler.getScheduleFactory()).thenReturn(scheduleFactory);
    when(taskScheduler.createTaskConfigurationInstance(AuditEventCleanupTaskDescriptor.TYPE_ID))
        .thenReturn(taskConfiguration);
    when(scheduleFactory.cron(any(), any())).thenReturn(schedule);
    when(taskScheduler.listsTasks()).thenReturn(Collections.emptyList());
    manager = new AuditEventCleanupTaskManager(taskScheduler, "0 30 3 * * ?", true);
  }

  @Test
  void createsTaskWhenNoneExists() throws Exception {
    when(taskScheduler.getTaskByTypeId(AuditEventCleanupTaskDescriptor.TYPE_ID)).thenReturn(null);

    manager.doStart();

    verify(taskConfiguration).setName(AuditEventCleanupTaskDescriptor.TASK_NAME);
    verify(taskScheduler).scheduleTask(eq(taskConfiguration), eq(schedule));
  }

  @Test
  void preservesExistingTaskSchedule() throws Exception {
    TaskInfo existing = Mockito.mock(TaskInfo.class);
    when(taskScheduler.getTaskByTypeId(AuditEventCleanupTaskDescriptor.TYPE_ID)).thenReturn(existing);

    manager.doStart();

    verify(taskScheduler, never()).scheduleTask(any(), any());
  }

  @Test
  void doesNotRegisterWhenDisabledByFeatureFlag() throws Exception {
    AuditEventCleanupTaskManager disabled =
        new AuditEventCleanupTaskManager(taskScheduler, "0 30 3 * * ?", false);

    disabled.doStart();

    verify(taskScheduler, never()).scheduleTask(any(), any());
    verify(taskScheduler, never()).getTaskByTypeId(any());
  }

  @Test
  void removesDuplicateTasks() throws Exception {
    TaskInfo t1 = Mockito.mock(TaskInfo.class);
    TaskInfo t2 = Mockito.mock(TaskInfo.class);
    TaskConfiguration cfg = Mockito.mock(TaskConfiguration.class);
    when(cfg.getTypeId()).thenReturn(AuditEventCleanupTaskDescriptor.TYPE_ID);
    when(t1.getConfiguration()).thenReturn(cfg);
    when(t2.getConfiguration()).thenReturn(cfg);
    when(t1.getId()).thenReturn("aaa");
    when(t2.getId()).thenReturn("bbb");
    when(taskScheduler.listsTasks()).thenReturn(List.of(t1, t2));
    when(taskScheduler.getTaskByTypeId(AuditEventCleanupTaskDescriptor.TYPE_ID)).thenReturn(t1);

    manager.doStart();

    verify(t2).remove();
    verify(t1, never()).remove();
  }
}
