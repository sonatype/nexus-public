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
package org.sonatype.nexus.internal.scheduling;

import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskNotificationCondition;
import org.sonatype.nexus.scheduling.TaskNotificationMessageGenerator;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.*;

/**
 * Tests {@link NexusTaskNotificationEmailSender}
 */
public class NexusTaskNotificationEmailSenderTest
    extends TestSupport
{

  @Mock
  private EmailManager emailManager;

  @Mock
  private TaskNotificationMessageGenerator defaultTaskNotificationMessageGenerator;

  @Mock
  private TaskNotificationMessageGenerator customTaskNotificationMessageGenerator;

  private NexusTaskNotificationEmailSender underTest;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Map<String, TaskNotificationMessageGenerator> taskNotificationMessageGenerators = new HashMap<>();
    taskNotificationMessageGenerators.put("DEFAULT", defaultTaskNotificationMessageGenerator);
    taskNotificationMessageGenerators.put("CUSTOM", customTaskNotificationMessageGenerator);

    when(defaultTaskNotificationMessageGenerator.completed(isNotNull())).thenReturn("completed message");
    when(defaultTaskNotificationMessageGenerator.failed(isNotNull(), isNotNull())).thenReturn("failure message");
    when(emailManager.constructMessage("completed message")).thenReturn("completed message");
    when(emailManager.constructMessage("failure message")).thenReturn("failure message");

    underTest = new NexusTaskNotificationEmailSender(() -> emailManager, taskNotificationMessageGenerators);
  }

  @Test
  public void generatesEmailIfTaskFailed() throws EmailException {
    TaskInfo taskInfo = mock(TaskInfo.class);
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    when(taskInfo.getId()).thenReturn("taskId");
    when(taskInfo.getName()).thenReturn("test task");
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskConfiguration.getAlertEmail()).thenReturn("foo@example.com");
    TaskEventStoppedFailed event = new TaskEventStoppedFailed(taskInfo, new RuntimeException());

    underTest.on(event);

    verify(emailManager).send(isNotNull());
  }

  @Test
  public void generatesNoEmailIfTaskCompletesAndConfigurationConditionIsFailedOnly() throws EmailException {
    TaskInfo taskInfo = mock(TaskInfo.class);
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    when(taskInfo.getId()).thenReturn("taskId");
    when(taskInfo.getName()).thenReturn("test task");
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskConfiguration.getAlertEmail()).thenReturn("foo@example.com");
    when(taskConfiguration.getNotificationCondition()).thenReturn(TaskNotificationCondition.FAILURE);
    TaskEventStoppedDone event = new TaskEventStoppedDone(taskInfo);

    underTest.on(event);

    verify(emailManager, never()).send(isNotNull());
  }

  @Test
  public void generatesEmailIfTaskCompletesAndConfigurationConditionIsCompleted() throws EmailException {
    TaskInfo taskInfo = mock(TaskInfo.class);
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    when(taskInfo.getId()).thenReturn("taskId");
    when(taskInfo.getName()).thenReturn("test task");
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskConfiguration.getAlertEmail()).thenReturn("foo@example.com");
    when(taskConfiguration.getNotificationCondition()).thenReturn(TaskNotificationCondition.SUCCESS_FAILURE);
    TaskEventStoppedDone event = new TaskEventStoppedDone(taskInfo);

    underTest.on(event);

    verify(emailManager).send(isNotNull());
  }

  @Test
  public void usesCustomMessageGeneratorIfAvailableForTaskType() {
    TaskInfo taskInfo = mock(TaskInfo.class);
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    when(taskInfo.getId()).thenReturn("taskId");
    when(taskInfo.getTypeId()).thenReturn("CUSTOM");
    when(taskInfo.getName()).thenReturn("test task");
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskConfiguration.getAlertEmail()).thenReturn("foo@example.com");
    when(taskConfiguration.getNotificationCondition()).thenReturn(TaskNotificationCondition.SUCCESS_FAILURE);
    when(customTaskNotificationMessageGenerator.completed(isNotNull())).thenReturn("body");
    TaskEventStoppedDone event = new TaskEventStoppedDone(taskInfo);

    underTest.on(event);

    verify(customTaskNotificationMessageGenerator).completed(isNotNull());
  }
}
