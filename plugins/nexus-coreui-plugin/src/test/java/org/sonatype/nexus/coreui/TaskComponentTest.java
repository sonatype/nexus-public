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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Manual;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TaskComponent}.
 */
public class TaskComponentTest
    extends TestSupport
{
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private TaskComponent component;
  private TaskScheduler scheduler;

  @Before
  public void setUp() {
    scheduler = mock(TaskScheduler.class, Mockito.RETURNS_DEEP_STUBS);
    component = new TaskComponent();
    component.setScheduler(scheduler);
  }

  @Test
  public void testValidateState_running() {
    TaskInfo taskInfo = mock(TaskInfo.class);
    CurrentState localState = mock(CurrentState.class);
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(localState.getState()).thenReturn(TaskState.RUNNING);
    when(taskInfo.getId()).thenReturn("taskId");
    when(taskInfo.getCurrentState()).thenReturn(localState);
    when(extState.getState()).thenReturn(TaskState.RUNNING);
    when(component.getScheduler().toExternalTaskState(taskInfo)).thenReturn(extState);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Task can not be edited while it is being executed or it is in line to be executed");
    component.validateState(taskInfo);
  }

  @Test
  public void testValidateState_notRunning() {
    TaskInfo taskInfo = mock(TaskInfo.class);
    CurrentState localState = mock(CurrentState.class);
    ExternalTaskState extState = mock(ExternalTaskState.class);
    when(localState.getState()).thenReturn(TaskState.WAITING);
    when(taskInfo.getId()).thenReturn("taskId");
    when(taskInfo.getCurrentState()).thenReturn(localState);
    when(extState.getState()).thenReturn(TaskState.WAITING);
    when(component.getScheduler().toExternalTaskState(taskInfo)).thenReturn(extState);

    component.validateState(taskInfo);
  }

  @Test
  public void testValidateScriptUpdate_noSourceChange() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");

    TaskInfo taskInfo = mock(TaskInfo.class);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(ImmutableMap.of("source", "println 'hello'"));

    component.validateScriptUpdate(taskInfo, taskXO);
  }

  @Test
  public void testValidateScriptUpdate_sourceChange_allowCreation() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");

    TaskInfo taskInfo = mock(TaskInfo.class);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(ImmutableMap.of("source", "println 'hello world'"));

    component.setAllowCreation(true);
    component.validateScriptUpdate(taskInfo, taskXO);
  }

  @Test
  public void testValidateScriptUpdate_sourceChange_doNotAllowCreation() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");

    TaskInfo taskInfo = mock(TaskInfo.class);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(ImmutableMap.of("source", "println 'hello world'"));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Script source updates are not allowed");

    component.setAllowCreation(false);
    component.validateScriptUpdate(taskInfo, taskXO);
  }

  @Test
  public void testNotExposedTaskCannotBeCreated() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString("source", "println 'hello'");
    taskConfiguration.setExposed(false);

    TaskInfo taskInfo = mock(TaskInfo.class);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(scheduler.getScheduleFactory().manual()).thenReturn(new Manual());

    TaskXO taskXO = new TaskXO();
    taskXO.setProperties(ImmutableMap.of("source", "println 'hello world'"));
    taskXO.setSchedule("manual");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("This task is not allowed to be created");

    component.create(taskXO);
  }
}
