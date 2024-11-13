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
package org.sonatype.nexus.internal.security.secrets;

import java.util.Optional;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.EncryptionKeyValidator;
import org.sonatype.nexus.crypto.secrets.KeyAccessValidator;
import org.sonatype.nexus.crypto.secrets.MissingKeyException;
import org.sonatype.nexus.crypto.secrets.ReEncryptionNotSupportedException;
import org.sonatype.nexus.crypto.secrets.ActiveKeyChangeEvent;
import org.sonatype.nexus.internal.security.secrets.tasks.ReEncryptTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.UserIdHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReEncryptServiceImplTests
    extends TestSupport
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private KeyAccessValidator keyAccessValidator;

  @Mock
  private EncryptionKeyValidator encryptionKeyValidator;

  @Spy
  private TaskConfiguration configuration;

  @Spy
  private TaskInfo taskInfo;

  @Mock
  private EventManager eventManager;

  @Mock
  private DatabaseCheck databaseCheck;

  @Captor
  private ArgumentCaptor<TaskConfiguration> taskConfigurationCaptor;

  private final MockedStatic<UserIdHelper> userIdHelper = mockStatic(UserIdHelper.class);

  private final Cooperation2Factory cooperationFactory = new DefaultCooperation2Factory();

  private ReEncryptServiceImpl underTest;

  @Before
  public void setup() {
    userIdHelper.when(UserIdHelper::get).thenReturn("test");
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(encryptionKeyValidator.getActiveKeyId()).thenReturn(Optional.of("old-key-id"));
    underTest = new ReEncryptServiceImpl(keyAccessValidator, encryptionKeyValidator, taskScheduler, eventManager,
        databaseCheck, cooperationFactory, true);
  }

  @After
  public void teardown() {
    userIdHelper.close();
  }

  @Test
  public void testFailsOnUnsupportedVersion() {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(false);
    assertThrows(ReEncryptionNotSupportedException.class, () -> underTest.submitReEncryption("keyId", null));
  }

  @Test
  public void testFailsIfTaskAlreadySubmitted() {
    when(taskScheduler.getTaskByTypeId(ReEncryptTaskDescriptor.TYPE_ID)).thenReturn(taskInfo);
    assertThrows(IllegalStateException.class, () -> underTest.submitReEncryption("keyId", null));
  }

  @Test
  public void testExceptionThrownOnInvalidKey() {
    when(keyAccessValidator.isValidKey(anyString())).thenReturn(false);
    assertThrows(MissingKeyException.class, () -> underTest.submitReEncryption("invalidKey", null));
    verifyNoInteractions(eventManager);
    verify(taskScheduler).getTaskByTypeId(ReEncryptTaskDescriptor.TYPE_ID);
    verifyNoMoreInteractions(taskScheduler);
  }

  @Test
  public void verifyScheduledTaskOnValidKey() {
    String taskId = UUID.randomUUID().toString();
    when(taskInfo.getId()).thenReturn(taskId);
    when(taskInfo.getConfiguration()).thenReturn(configuration);
    when(taskScheduler.submit(configuration)).thenReturn(taskInfo);
    when(keyAccessValidator.isValidKey(anyString())).thenReturn(true);
    when(taskScheduler.createTaskConfigurationInstance(ReEncryptTaskDescriptor.TYPE_ID)).thenReturn(configuration);
    when(taskScheduler.getTaskById(ReEncryptTaskDescriptor.TYPE_ID)).thenReturn(null).thenReturn(taskInfo);

    underTest.submitReEncryption("validKey", null);
    verify(eventManager).post(any(ActiveKeyChangeEvent.class));
    verify(taskScheduler).submit(taskConfigurationCaptor.capture());

    TaskConfiguration submittedTask = taskConfigurationCaptor.getValue();
    assertThat(submittedTask).isNotNull();
    assertThat(submittedTask.getAlertEmail()).isNull();
    verify(submittedTask).setString("keyId", "validKey");
  }

  @Test
  public void verifyScheduledTaskWithEmailOnValidKey() {
    String notifyEmail = "notify@test.com";
    String keyId = "validKey";

    String taskId = UUID.randomUUID().toString();
    when(taskInfo.getId()).thenReturn(taskId);
    when(taskInfo.getConfiguration()).thenReturn(configuration);
    when(taskScheduler.submit(configuration)).thenReturn(taskInfo);
    when(keyAccessValidator.isValidKey(anyString())).thenReturn(true);
    when(taskScheduler.createTaskConfigurationInstance(ReEncryptTaskDescriptor.TYPE_ID)).thenReturn(configuration);
    when(taskScheduler.getTaskById(ReEncryptTaskDescriptor.TYPE_ID)).thenReturn(null).thenReturn(taskInfo);

    underTest.submitReEncryption(keyId, notifyEmail);
    verify(eventManager).post(any(ActiveKeyChangeEvent.class));
    verify(taskScheduler).submit(taskConfigurationCaptor.capture());

    TaskConfiguration submittedTask = taskConfigurationCaptor.getValue();
    assertThat(submittedTask).isNotNull();
    verify(submittedTask).setAlertEmail(notifyEmail);
    verify(submittedTask).setString("keyId", keyId);
  }
}
