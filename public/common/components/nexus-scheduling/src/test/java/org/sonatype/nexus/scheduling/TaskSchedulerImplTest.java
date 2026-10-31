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
package org.sonatype.nexus.scheduling;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSchedulerImplTest
{
  @Mock
  private TaskFactory taskFactory;

  @Mock
  private SchedulerSPI schedulerSPI;

  @Mock
  private EventManager eventManager;

  private TaskSchedulerImpl underTest;

  @BeforeEach
  void setUp() {
    underTest = new TaskSchedulerImpl(eventManager, taskFactory, () -> schedulerSPI, true);
  }

  @Test
  void scheduleTask_singletonType_throwsWhenDuplicateExists() {
    TaskDescriptor descriptor = mock(TaskDescriptor.class);
    when(descriptor.isSingletonTaskType()).thenReturn(true);
    when(taskFactory.findDescriptor("my.singleton")).thenReturn(descriptor);

    TaskInfo existingTask = mock(TaskInfo.class);
    when(schedulerSPI.getTaskByTypeId("my.singleton")).thenReturn(existingTask);

    when(existingTask.getId()).thenReturn("existing-uuid");

    TaskConfiguration config = new TaskConfiguration();
    config.setTypeId("my.singleton");
    config.setId("new-uuid");
    Schedule schedule = mock(Schedule.class);

    ValidationErrorsException ex =
        assertThrows(ValidationErrorsException.class, () -> underTest.scheduleTask(config, schedule));
    assertTrue(ex.getMessage().contains("my.singleton"));
    verify(schedulerSPI, never()).scheduleTask(any(), any());
  }

  @Test
  void scheduleTask_singletonType_proceedsWhenNoExistingTask() {
    TaskDescriptor descriptor = mock(TaskDescriptor.class);
    when(descriptor.isSingletonTaskType()).thenReturn(true);
    when(taskFactory.findDescriptor("my.singleton")).thenReturn(descriptor);
    when(schedulerSPI.getTaskByTypeId("my.singleton")).thenReturn(null);

    TaskConfiguration config = new TaskConfiguration();
    config.setTypeId("my.singleton");
    config.setId("some-uuid");
    Schedule schedule = mock(Schedule.class);

    TaskInfo result = mock(TaskInfo.class);
    when(schedulerSPI.scheduleTask(any(), any())).thenReturn(result);
    when(result.getConfiguration()).thenReturn(config);
    when(result.getSchedule()).thenReturn(schedule);
    when(schedule.getType()).thenReturn("now");

    underTest.scheduleTask(config, schedule);

    verify(schedulerSPI).scheduleTask(config, schedule);
  }

  @Test
  void scheduleTask_nonSingletonType_allowsDuplicates() {
    TaskDescriptor descriptor = mock(TaskDescriptor.class);
    when(descriptor.isSingletonTaskType()).thenReturn(false);
    when(taskFactory.findDescriptor("my.regular")).thenReturn(descriptor);

    TaskConfiguration config = new TaskConfiguration();
    config.setTypeId("my.regular");
    config.setId("some-uuid");
    Schedule schedule = mock(Schedule.class);

    TaskInfo result = mock(TaskInfo.class);
    when(schedulerSPI.scheduleTask(any(), any())).thenReturn(result);
    when(result.getConfiguration()).thenReturn(config);
    when(result.getSchedule()).thenReturn(schedule);
    when(schedule.getType()).thenReturn("now");

    underTest.scheduleTask(config, schedule);

    // no duplicate check, no exception — getTaskByTypeId never called
    verify(schedulerSPI, never()).getTaskByTypeId(any());
    verify(schedulerSPI).scheduleTask(config, schedule);
  }

  @Test
  void scheduleTask_singletonType_allowsUpdateOfSameTask() {
    TaskDescriptor descriptor = mock(TaskDescriptor.class);
    when(descriptor.isSingletonTaskType()).thenReturn(true);
    when(taskFactory.findDescriptor("repository.search.update")).thenReturn(descriptor);

    String taskId = "task-123";
    TaskInfo existingTask = mock(TaskInfo.class);
    when(existingTask.getId()).thenReturn(taskId);
    when(schedulerSPI.getTaskByTypeId("repository.search.update")).thenReturn(existingTask);

    TaskConfiguration config = new TaskConfiguration();
    config.setTypeId("repository.search.update");
    config.setId(taskId); // same ID as existing — this is an update, not a new task
    Schedule schedule = mock(Schedule.class);

    TaskInfo result = mock(TaskInfo.class);
    when(schedulerSPI.scheduleTask(any(), any())).thenReturn(result);
    when(result.getConfiguration()).thenReturn(config);
    when(result.getSchedule()).thenReturn(schedule);
    when(schedule.getType()).thenReturn("cron");

    assertDoesNotThrow(() -> underTest.scheduleTask(config, schedule));
    verify(schedulerSPI).scheduleTask(config, schedule);
  }
}
