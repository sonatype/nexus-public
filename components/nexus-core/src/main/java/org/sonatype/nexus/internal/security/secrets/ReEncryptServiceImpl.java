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
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.EncryptionKeyValidator;
import org.sonatype.nexus.crypto.secrets.KeyAccessValidator;
import org.sonatype.nexus.crypto.secrets.MissingKeyException;
import org.sonatype.nexus.crypto.secrets.ReEncryptService;
import org.sonatype.nexus.crypto.secrets.ReEncryptionNotSupportedException;
import org.sonatype.nexus.crypto.secrets.ActiveKeyChangeEvent;
import org.sonatype.nexus.internal.security.secrets.tasks.ReEncryptTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.UserIdHelper;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.crypto.secrets.SecretsService.SECRETS_MIGRATION_VERSION;

/**
 * Implementation of {@link ReEncryptService}. It schedules a re-encryption task if one is not already running.
 */
@Named
@Singleton
public class ReEncryptServiceImpl
    extends ComponentSupport
    implements ReEncryptService
{
  private static final String KEY_NOT_FOUND_ERR_MSG =
      "Key id '%s' not found. Check secrets configuration, make sure the file and key id exists.";

  private final KeyAccessValidator keyAccessValidator;

  private final EncryptionKeyValidator encryptionKeyValidator;

  private final TaskScheduler taskScheduler;

  private final EventManager eventManager;

  private final DatabaseCheck databaseCheck;

  private final Cooperation2 cooperation;

  @Inject
  public ReEncryptServiceImpl(
      final KeyAccessValidator keyAccessValidator,
      final EncryptionKeyValidator encryptionKeyValidator,
      final TaskScheduler taskScheduler,
      final EventManager eventManager,
      final DatabaseCheck databaseCheck,
      final Cooperation2Factory cooperationFactory,
      @Named("${nexus.secrets.cooperation.enabled:-true}") final boolean cooperationEnabled)
  {
    this.keyAccessValidator = checkNotNull(keyAccessValidator);
    this.encryptionKeyValidator = checkNotNull(encryptionKeyValidator);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.eventManager = checkNotNull(eventManager);
    this.databaseCheck = checkNotNull(databaseCheck);
    this.cooperation = checkNotNull(cooperationFactory).configure()
        .enabled(cooperationEnabled)
        .build("re-encrypt-task");
  }

  private void checkKeyExists(final String secretKeyId) {
    if (!keyAccessValidator.isValidKey(secretKeyId)) {
      throw new MissingKeyException(format(KEY_NOT_FOUND_ERR_MSG, secretKeyId));
    }
  }

  @Override
  public String submitReEncryption(
      final String secretKeyId,
      @Nullable final String notifyEmail) throws MissingKeyException, ReEncryptionNotSupportedException
  {
    return submitTask(secretKeyId, notifyEmail);
  }

  private void checkReEncryptionSupported() {
    if (!databaseCheck.isAtLeast(SECRETS_MIGRATION_VERSION)) {
      throw new ReEncryptionNotSupportedException(
          format("Re-encryption is not supported. Please upgrade DB to version %s", SECRETS_MIGRATION_VERSION));
    }
  }

  private void checkTaskNotSubmitted() {
    if (taskScheduler.getTaskByTypeId(ReEncryptTaskDescriptor.TYPE_ID) != null) {
      throw new IllegalStateException("Re-encryption task is already running");
    }
  }

  /**
   * Schedules re-encrypt task if there is not an existing one
   *
   * @param keyId the key to use for re-encryption
   * @param notifyEmail the email address to notify when the re-encryption task is complete
   * @return the task id
   */
  private String submitTask(final String keyId, final String notifyEmail) {
    checkReEncryptionSupported();
    checkTaskNotSubmitted();
    checkKeyExists(keyId);
    TaskInfo scheduledTask = Optional.ofNullable(maybeScheduleReEncrypt(keyId, notifyEmail))
        .orElseThrow(() -> new RuntimeException("Failed to schedule re-encryption task"));
    String newKeyId = scheduledTask.getConfiguration().getString("keyId");
    String oldKeyId = encryptionKeyValidator.getActiveKeyId().orElse(null);
    eventManager.post(new ActiveKeyChangeEvent(newKeyId, oldKeyId, UserIdHelper.get()));
    return scheduledTask.getId();
  }

  private TaskInfo maybeScheduleReEncrypt(final String keyId, final String notifyEmail) {
    try {
      return cooperation.on(() -> scheduleReEncryptTask(keyId, notifyEmail))
          .checkFunction(this::getReEncryptTask)
          .cooperate("schedule_re-encryption");
    }
    catch (Exception e) {
      return null;
    }
  }

  private TaskInfo scheduleReEncryptTask(final String keyId, final String notifyEmail) {
    return getReEncryptTask().orElseGet(() -> {
      log.debug("Scheduling re-encrypt task");
      TaskConfiguration taskConfiguration =
          taskScheduler.createTaskConfigurationInstance(ReEncryptTaskDescriptor.TYPE_ID);
      taskConfiguration.setString("keyId", keyId);
      Optional.ofNullable(notifyEmail).ifPresent(taskConfiguration::setAlertEmail);
      return taskScheduler.submit(taskConfiguration);
    });
  }

  private Optional<TaskInfo> getReEncryptTask() {
    return taskScheduler.listsTasks()
        .stream()
        .filter(taskInfo -> taskInfo.getTypeId().equals(ReEncryptTaskDescriptor.TYPE_ID))
        .findFirst();
  }
}
