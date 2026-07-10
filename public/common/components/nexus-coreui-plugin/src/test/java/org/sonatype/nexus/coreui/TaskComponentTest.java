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
package org.sonatype.nexus.coreui;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Validator;
import jakarta.ws.rs.NotFoundException;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskNotificationCondition;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.TaskUtils;
import org.sonatype.nexus.scheduling.schedule.Cron;
import org.sonatype.nexus.scheduling.schedule.Daily;
import org.sonatype.nexus.scheduling.schedule.Hourly;
import org.sonatype.nexus.scheduling.schedule.Manual;
import org.sonatype.nexus.scheduling.schedule.Monthly;
import org.sonatype.nexus.scheduling.schedule.Monthly.CalendarDay;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Once;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.schedule.Weekly;
import org.sonatype.nexus.scheduling.schedule.Weekly.Weekday;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TaskComponent}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class TaskComponentTest
{
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private TaskScheduler scheduler;

  @Mock
  private Validator validator;

  @Mock
  private TaskInfo taskInfo;

  private final Provider<Validator> validatorProvider = () -> validator;

  private TaskComponent component;

  @Mock
  private TaskScheduler taskScheduler;

  private TaskUtils taskUtils;

  @BeforeEach
  void setup() {
    Provider<TaskScheduler> mockSchedulerProvider = () -> taskScheduler;
    TaskResultStateStore mockResultStore = mock(TaskResultStateStore.class);
    lenient().when(mockResultStore.isSupported()).thenReturn(false);
    taskUtils = new TaskUtils(mockSchedulerProvider, mockResultStore);
    component = new TaskComponent(scheduler, validatorProvider, false, taskUtils);
  }

  @Test
  void testValidateState_running() {
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(extState.getState()).thenReturn(TaskState.RUNNING);
    when(scheduler.toExternalTaskState(taskInfo)).thenReturn(extState);

    assertThrows(IllegalStateException.class, () -> component.validateState("taskId", taskInfo),
        "Task can not be edited while it is being executed or it is in line to be executed");
  }

  @Test
  void testValidateState_notRunning() {
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(scheduler.toExternalTaskState(taskInfo)).thenReturn(extState);

    component.validateState("taskId", taskInfo);
  }

  @Test
  void testValidateScriptUpdate_noSourceChange() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");

    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(Map.of("source", "println 'hello'"));

    component.validateScriptUpdate(taskInfo, taskXO);
  }

  @Test
  void testValidateScriptUpdate_sourceChange_allowCreation() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");

    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(Map.of("source", "println 'hello world'"));

    component = new TaskComponent(scheduler, validatorProvider, true, taskUtils);
    component.validateScriptUpdate(taskInfo, taskXO);
  }

  @Test
  void testValidateScriptUpdate_sourceChange_doNotAllowCreation() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");

    TaskInfo taskInfo = mock(TaskInfo.class);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(Map.of("source", "println 'hello world'"));

    assertThrows(IllegalStateException.class, () -> component.validateScriptUpdate(taskInfo, taskXO),
        "Script source updates are not allowed");
  }

  @Test
  void testNotExposedTaskCannotBeCreated() throws Exception {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");
    taskConfiguration.setExposed(false);

    when(scheduler.getScheduleFactory().manual()).thenReturn(new Manual());

    TaskXO taskXO = new TaskXO();
    taskXO.setName("my-task");
    taskXO.setId("my-id");
    taskXO.setEnabled(true);
    taskXO.setTypeId("task-type-id");
    taskXO.setProperties(Map.of("source", "println 'hello world'"));
    taskXO.setSchedule("manual");

    assertThrows(IllegalStateException.class, () -> component.create(taskXO), "This task is not allowed to be created");
  }

  @Test
  void testAppendPlanReconciliationText_NoDryRun() {
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    setupTask(taskConfiguration);

    List<TaskXO> tasks = component.read();
    assertEquals(1, tasks.size());
    assertEquals(TaskComponent.PLAN_RECONCILIATION_TASK_ID, tasks.get(0).getTypeId());

    assertEquals("Ok [0s]", tasks.get(0).getLastRunResult());
  }

  @Test
  void testAppendPlanReconciliationText_DryRun() {
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    setupTask(taskConfiguration);
    when(taskConfiguration.getBoolean(anyString(), anyBoolean())).thenReturn(true);

    List<TaskXO> tasks = component.read();
    assertEquals(1, tasks.size());
    assertEquals(TaskComponent.PLAN_RECONCILIATION_TASK_ID, tasks.get(0).getTypeId());

    tasks = component.read();
    assertEquals("Ok [0s]" + TaskComponent.PLAN_RECONCILIATION_TASK_OK_TEXT, tasks.get(0).getLastRunResult());
  }

  @Test
  void testGetState_returnsAllowScriptCreation() {
    Map<String, Object> state = component.getState();
    assertNotNull(state);
    assertEquals(false, state.get("allowScriptCreation"));
  }

  @Test
  void testGetState_allowCreationTrue() {
    component = new TaskComponent(scheduler, validatorProvider, true, taskUtils);
    Map<String, Object> state = component.getState();
    assertNotNull(state);
    assertEquals(true, state.get("allowScriptCreation"));
  }

  @Test
  void testValidateState_nullTaskInfo() {
    assertThrows(NotFoundException.class, () -> component.validateState("unknown-id", null));
  }

  @Test
  void testRemove_existingTask() {
    TaskInfo taskInfo = mock(TaskInfo.class);
    when(scheduler.getTaskById("task-id")).thenReturn(taskInfo);

    component.remove("task-id");

    verify(taskInfo).remove();
  }

  @Test
  void testRemove_nonExistentTask() {
    when(scheduler.getTaskById("missing-id")).thenReturn(null);

    assertDoesNotThrow(() -> component.remove("missing-id"));
  }

  @Test
  void testRun_existingTask() throws Exception {
    TaskInfo taskInfo = mock(TaskInfo.class);
    when(scheduler.getTaskById("task-id")).thenReturn(taskInfo);

    component.run("task-id");

    verify(taskInfo).runNow();
  }

  @Test
  void testRun_nonExistentTask() {
    when(scheduler.getTaskById("missing-id")).thenReturn(null);

    assertDoesNotThrow(() -> component.run("missing-id"));
  }

  @Test
  void testStop() {
    component.stop("task-id");

    verify(scheduler).cancel("task-id", false);
  }

  @Test
  void testRead_filtersInvisibleTasks() {
    TaskConfiguration visibleConfig = mock(TaskConfiguration.class);
    when(visibleConfig.isVisible()).thenReturn(true);
    when(visibleConfig.getTypeId()).thenReturn("some-type");
    when(visibleConfig.isEnabled()).thenReturn(true);

    TaskConfiguration invisibleConfig = mock(TaskConfiguration.class);
    when(invisibleConfig.isVisible()).thenReturn(false);

    TaskInfo visibleTask = mock(TaskInfo.class);
    when(visibleTask.getId()).thenReturn("visible-task");
    when(visibleTask.getName()).thenReturn("Visible Task");
    when(visibleTask.getConfiguration()).thenReturn(visibleConfig);

    CurrentState currentState = mock(CurrentState.class);
    when(currentState.getState()).thenReturn(TaskState.WAITING);
    when(visibleTask.getCurrentState()).thenReturn(currentState);
    when(visibleTask.getSchedule()).thenReturn(new Manual());

    TaskInfo invisibleTask = mock(TaskInfo.class);
    when(invisibleTask.getConfiguration()).thenReturn(invisibleConfig);

    when(scheduler.listsTasks()).thenReturn(List.of(visibleTask, invisibleTask));

    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(visibleTask)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(extState.getLastEndState()).thenReturn(null);

    List<TaskXO> tasks = component.read();
    assertEquals(1, tasks.size());
    assertEquals("visible-task", tasks.get(0).getId());
  }

  @Test
  void testRead_onceScheduleSetsStartDate() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Date startDate = new Date();
    Once onceSchedule = new Once(startDate);

    TaskInfo task = mock(TaskInfo.class);
    when(task.getId()).thenReturn("task-1");
    when(task.getName()).thenReturn("Once Task");
    when(task.getConfiguration()).thenReturn(config);
    when(task.getSchedule()).thenReturn(onceSchedule);

    CurrentState currentState = mock(CurrentState.class);
    when(currentState.getState()).thenReturn(TaskState.WAITING);
    when(task.getCurrentState()).thenReturn(currentState);

    when(scheduler.listsTasks()).thenReturn(List.of(task));

    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals(1, tasks.size());
    assertEquals("once", tasks.get(0).getSchedule());
    assertNotNull(tasks.get(0).getStartDate());
  }

  @Test
  void testRead_hourlySchedule() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Hourly hourlySchedule = new Hourly(new Date());
    TaskInfo task = createMockTaskWithSchedule("task-1", "Hourly Task", config, hourlySchedule);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("hourly", tasks.get(0).getSchedule());
  }

  @Test
  void testRead_dailySchedule() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Daily dailySchedule = new Daily(new Date());
    TaskInfo task = createMockTaskWithSchedule("task-1", "Daily Task", config, dailySchedule);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("daily", tasks.get(0).getSchedule());
  }

  @Test
  void testRead_monthlyScheduleWithLastDay() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Monthly monthlySchedule = new Monthly(new Date(), Set.of(CalendarDay.day(15), CalendarDay.lastDay()));
    TaskInfo task = createMockTaskWithSchedule("task-1", "Monthly Task", config, monthlySchedule);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("monthly", tasks.get(0).getSchedule());
    assertNotNull(tasks.get(0).getRecurringDays());
  }

  @Test
  void testRead_cronSchedule() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Cron cronSchedule = new Cron(new Date(), "0 0 12 * * ?");
    TaskInfo task = createMockTaskWithSchedule("task-1", "Cron Task", config, cronSchedule);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("advanced", tasks.get(0).getSchedule());
    assertEquals("0 0 12 * * ?", tasks.get(0).getCronExpression());
  }

  @Test
  void testRead_manualSchedule() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Manual manualSchedule = new Manual();
    TaskInfo task = createMockTaskWithSchedule("task-1", "Manual Task", config, manualSchedule);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("manual", tasks.get(0).getSchedule());
  }

  @Test
  void testRead_nowSchedule() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Now nowSchedule = new Now();
    TaskInfo task = createMockTaskWithSchedule("task-1", "Now Task", config, nowSchedule);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("internal", tasks.get(0).getSchedule());
  }

  @Test
  void testRead_disabledTaskShowsDisabledDescription() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(false);

    TaskInfo task = createMockTaskWithSchedule("task-1", "Disabled Task", config, new Manual());

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("Disabled", tasks.get(0).getStatusDescription());
  }

  @Test
  void testRead_lastRunResult_canceled() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    TaskInfo task = createMockTaskWithSchedule("task-1", "Task", config, new Manual());

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(extState.getLastEndState()).thenReturn(TaskState.CANCELED);
    when(extState.getLastRunStarted()).thenReturn(new Date());
    when(extState.getLastRunDuration()).thenReturn(null);

    List<TaskXO> tasks = component.read();
    assertEquals("Canceled", tasks.get(0).getLastRunResult());
  }

  @Test
  void testRead_lastRunResult_failed() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    TaskInfo task = createMockTaskWithSchedule("task-1", "Task", config, new Manual());

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(extState.getLastEndState()).thenReturn(TaskState.FAILED);
    when(extState.getLastRunStarted()).thenReturn(new Date());
    when(extState.getLastRunDuration()).thenReturn(null);

    List<TaskXO> tasks = component.read();
    assertEquals("Error", tasks.get(0).getLastRunResult());
  }

  @Test
  void testRead_lastRunResult_interrupted() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    TaskInfo task = createMockTaskWithSchedule("task-1", "Task", config, new Manual());

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(extState.getLastEndState()).thenReturn(TaskState.INTERRUPTED);
    when(extState.getLastRunStarted()).thenReturn(new Date());
    when(extState.getLastRunDuration()).thenReturn(null);

    List<TaskXO> tasks = component.read();
    assertEquals("Interrupted", tasks.get(0).getLastRunResult());
  }

  @Test
  void testRead_lastRunResult_withDurationIncludingHoursAndMinutes() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    TaskInfo task = createMockTaskWithSchedule("task-1", "Task", config, new Manual());

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(extState.getLastEndState()).thenReturn(TaskState.OK);
    when(extState.getLastRunStarted()).thenReturn(new Date());
    // 1 hour, 2 minutes, 3 seconds = 3723000 ms
    when(extState.getLastRunDuration()).thenReturn(3723000L);

    List<TaskXO> tasks = component.read();
    assertEquals("Ok [1h2m3s]", tasks.get(0).getLastRunResult());
  }

  @Test
  void testRead_lastRunResult_noEndState() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    TaskInfo task = createMockTaskWithSchedule("task-1", "Task", config, new Manual());

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(extState.getLastEndState()).thenReturn(null);

    List<TaskXO> tasks = component.read();
    assertEquals("", tasks.get(0).getLastRunResult());
  }

  @Test
  void testRead_weeklyScheduleSetsRecurringDays() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);

    Weekly weeklySchedule = new Weekly(new Date(), Set.of(Weekday.MON, Weekday.FRI));
    TaskInfo task = createMockTaskWithSchedule("task-1", "Weekly Task", config, weeklySchedule);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);

    List<TaskXO> tasks = component.read();
    assertEquals("weekly", tasks.get(0).getSchedule());
    assertNotNull(tasks.get(0).getRecurringDays());
    assertTrue(tasks.get(0).getRecurringDays().length > 0);
  }

  @Test
  void testReadTypes() {
    TaskDescriptor descriptor = mock(TaskDescriptor.class);
    when(descriptor.getId()).thenReturn("type-1");
    when(descriptor.getName()).thenReturn("Task Type 1");
    when(descriptor.isExposed()).thenReturn(true);
    when(descriptor.allowConcurrentRun()).thenReturn(false);
    when(descriptor.getFormFields()).thenReturn(null);

    TaskFactory taskFactory = mock(TaskFactory.class);
    when(taskFactory.getDescriptors()).thenReturn(List.of(descriptor));
    when(scheduler.getTaskFactory()).thenReturn(taskFactory);

    List<TaskTypeXO> types = component.readTypes();
    assertEquals(1, types.size());
    assertEquals("type-1", types.get(0).getId());
    assertEquals("Task Type 1", types.get(0).getName());
    assertEquals(true, types.get(0).getExposed());
    assertEquals(false, types.get(0).getConcurrentRun());
    assertThat(types.get(0).getFormFields(), is(nullValue()));
  }

  @Test
  void testReadTypes_withFormFields() {
    TaskDescriptor descriptor = mock(TaskDescriptor.class);
    when(descriptor.getId()).thenReturn("type-2");
    when(descriptor.getName()).thenReturn("Task Type 2");
    when(descriptor.isExposed()).thenReturn(false);
    when(descriptor.allowConcurrentRun()).thenReturn(true);
    FormField formField = new StringTextFormField("field1", "Field 1", "Help text", true);
    when(descriptor.getFormFields()).thenReturn(List.of(formField));

    TaskFactory taskFactory = mock(TaskFactory.class);
    when(taskFactory.getDescriptors()).thenReturn(List.of(descriptor));
    when(scheduler.getTaskFactory()).thenReturn(taskFactory);

    List<TaskTypeXO> types = component.readTypes();
    assertEquals(1, types.size());
    assertNotNull(types.get(0).getFormFields());
    assertEquals(1, types.get(0).getFormFields().size());
  }

  @Test
  void testValidateScriptUpdate_nullOriginalSource() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    // no source set, so originalSource will be null

    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(Map.of("source", "println 'hello'"));

    // When originalSource is null, the condition !allowCreation && originalSource != null is false
    // so no exception is thrown
    component.validateScriptUpdate(taskInfo, taskXO);
  }

  @Test
  void testRead_runningTaskWithProgress() {
    TaskConfiguration config = mock(TaskConfiguration.class);
    when(config.isVisible()).thenReturn(true);
    when(config.getTypeId()).thenReturn("some-type");
    when(config.isEnabled()).thenReturn(true);
    when(config.getProgress()).thenReturn("50% complete");

    TaskInfo task = mock(TaskInfo.class);
    when(task.getId()).thenReturn("task-1");
    when(task.getName()).thenReturn("Running Task");
    when(task.getConfiguration()).thenReturn(config);
    when(task.getSchedule()).thenReturn(new Manual());

    CurrentState runningState = mock(CurrentState.class);
    when(runningState.getState()).thenReturn(TaskState.RUNNING);
    when(task.getCurrentState()).thenReturn(runningState);

    when(scheduler.listsTasks()).thenReturn(List.of(task));
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(task)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.RUNNING);

    List<TaskXO> tasks = component.read();
    assertTrue(tasks.get(0).getStatusDescription().contains("50% complete"));
  }

  @Test
  void testCreate_typeIdInjectionPrevented() throws Exception {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setTypeId("repository.rebuild-index");
    taskConfiguration.setExposed(true);

    when(scheduler.createTaskConfigurationInstance("repository.rebuild-index")).thenReturn(taskConfiguration);
    when(scheduler.getScheduleFactory().manual()).thenReturn(new Manual());

    // Create TaskXO with malicious .typeId property injection
    TaskXO taskXO = new TaskXO();
    taskXO.setName("test-task");
    taskXO.setTypeId("repository.rebuild-index");
    taskXO.setEnabled(true);
    taskXO.setSchedule("manual");
    taskXO.setNotificationCondition(TaskNotificationCondition.FAILURE);
    Map<String, String> properties = new HashMap<>();
    properties.put(".typeId", "script");
    properties.put("source", "malicious code");
    taskXO.setProperties(properties);

    TaskInfo mockTaskInfo = mock(TaskInfo.class);
    when(mockTaskInfo.getId()).thenReturn("task-id");
    CurrentState createCurrentState = mock(CurrentState.class);
    when(createCurrentState.getState()).thenReturn(TaskState.WAITING);
    when(mockTaskInfo.getCurrentState()).thenReturn(createCurrentState);
    when(mockTaskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(mockTaskInfo.getSchedule()).thenReturn(new Manual());

    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(scheduler.toExternalTaskState(mockTaskInfo)).thenReturn(extState);
    when(scheduler.scheduleTask(any(TaskConfiguration.class), any(Schedule.class))).thenReturn(mockTaskInfo);

    component.create(taskXO);

    ArgumentCaptor<TaskConfiguration> configCaptor = ArgumentCaptor.forClass(TaskConfiguration.class);
    verify(scheduler).scheduleTask(configCaptor.capture(), any(Schedule.class));
    assertEquals("repository.rebuild-index", configCaptor.getValue().getTypeId());
  }

  @Test
  void testUpdate_typeIdInjectionPrevented() throws Exception {
    TaskConfiguration existingConfig = new TaskConfiguration();
    existingConfig.setTypeId("repository.rebuild-index");
    existingConfig.setId("existing-task-id");

    TaskInfo existingTask = mock(TaskInfo.class);
    lenient().when(existingTask.getId()).thenReturn("existing-task-id");
    when(existingTask.getTypeId()).thenReturn("repository.rebuild-index");
    when(existingTask.getConfiguration()).thenReturn(existingConfig);

    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(scheduler.toExternalTaskState(existingTask)).thenReturn(extState);
    when(scheduler.getTaskById("existing-task-id")).thenReturn(existingTask);

    TaskConfiguration newConfig = new TaskConfiguration();
    newConfig.setTypeId("repository.rebuild-index");
    newConfig.setExposed(true);
    when(scheduler.createTaskConfigurationInstance("repository.rebuild-index")).thenReturn(newConfig);
    when(scheduler.getScheduleFactory().manual()).thenReturn(new Manual());

    // Create update TaskXO with malicious .typeId injection
    TaskXO taskXO = new TaskXO();
    taskXO.setId("existing-task-id");
    taskXO.setName("test-task");
    taskXO.setTypeId("repository.rebuild-index");
    taskXO.setEnabled(true);
    taskXO.setSchedule("manual");
    taskXO.setNotificationCondition(TaskNotificationCondition.FAILURE);
    Map<String, String> properties = new HashMap<>();
    properties.put(".typeId", "script");
    properties.put("source", "malicious code");
    taskXO.setProperties(properties);

    TaskInfo updatedTask = mock(TaskInfo.class);
    when(updatedTask.getId()).thenReturn("existing-task-id");
    lenient().when(updatedTask.getTypeId()).thenReturn("repository.rebuild-index");
    CurrentState updateCurrentState = mock(CurrentState.class);
    when(updateCurrentState.getState()).thenReturn(TaskState.WAITING);
    when(updatedTask.getCurrentState()).thenReturn(updateCurrentState);
    when(updatedTask.getConfiguration()).thenReturn(newConfig);
    when(updatedTask.getSchedule()).thenReturn(new Manual());
    when(scheduler.toExternalTaskState(updatedTask)).thenReturn(extState);
    when(scheduler.scheduleTask(any(TaskConfiguration.class), any(Schedule.class))).thenReturn(updatedTask);

    component.update(taskXO);

    ArgumentCaptor<TaskConfiguration> configCaptor = ArgumentCaptor.forClass(TaskConfiguration.class);
    verify(scheduler).scheduleTask(configCaptor.capture(), any(Schedule.class));
    assertEquals("repository.rebuild-index", configCaptor.getValue().getTypeId());
  }

  private TaskInfo createMockTaskWithSchedule(
      final String id,
      final String name,
      final TaskConfiguration config,
      final Schedule schedule)
  {
    TaskInfo task = mock(TaskInfo.class);
    when(task.getId()).thenReturn(id);
    when(task.getName()).thenReturn(name);
    lenient().when(task.getTypeId()).thenReturn("some-type");
    when(task.getConfiguration()).thenReturn(config);
    when(task.getSchedule()).thenReturn(schedule);

    CurrentState currentState = mock(CurrentState.class);
    when(currentState.getState()).thenReturn(TaskState.WAITING);
    when(task.getCurrentState()).thenReturn(currentState);

    return task;
  }

  private void setupTask(final TaskConfiguration taskConfiguration) {
    when(taskConfiguration.isVisible()).thenReturn(true);
    when(taskConfiguration.getTypeId()).thenReturn(TaskComponent.PLAN_RECONCILIATION_TASK_ID);

    CurrentState localState = mock(CurrentState.class);
    Schedule schedule = mock(Weekly.class);
    when(localState.getState()).thenReturn(TaskState.WAITING);
    when(taskInfo.getId()).thenReturn("taskId");
    when(taskInfo.getTypeId()).thenReturn(TaskComponent.PLAN_RECONCILIATION_TASK_ID);
    when(taskInfo.getCurrentState()).thenReturn(localState);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskInfo.getSchedule()).thenReturn(schedule);
    when(scheduler.listsTasks()).thenReturn(List.of(taskInfo));

    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(scheduler.toExternalTaskState(taskInfo)).thenReturn(extState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(extState.getLastEndState()).thenReturn(TaskState.OK);
    when(extState.getLastRunStarted()).thenReturn(new Date());
    when(extState.getLastRunDuration()).thenReturn(100L);
  }
}
