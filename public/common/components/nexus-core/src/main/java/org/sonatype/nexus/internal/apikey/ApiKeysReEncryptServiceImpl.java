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
package org.sonatype.nexus.internal.apikey;

import java.util.Optional;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.crypto.apikey.ApiKeysReEncryptService;
import org.sonatype.nexus.crypto.secrets.MissingKeyException;
import org.sonatype.nexus.crypto.secrets.ReEncryptionNotSupportedException;
import org.sonatype.nexus.internal.security.apikey.task.ReEncryptPrincipalsTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.crypto.secrets.SecretsService.SECRETS_MIGRATION_VERSION;

/**
 * Implementation of {@link ApiKeysReEncryptService}. It schedules a re-encryption principals task if one is not already
 * running.
 */
@Component
@Singleton
public class ApiKeysReEncryptServiceImpl
    implements ApiKeysReEncryptService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final TaskScheduler taskScheduler;

  private final DatabaseCheck databaseCheck;

  private final Cooperation2 cooperation;

  @Inject
  public ApiKeysReEncryptServiceImpl(
      final TaskScheduler taskScheduler,
      final DatabaseCheck databaseCheck,
      final Cooperation2Factory cooperationFactory,
      @Value("${nexus.secrets.cooperation.enabled:true}") final boolean cooperationEnabled)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.databaseCheck = checkNotNull(databaseCheck);
    this.cooperation = checkNotNull(cooperationFactory).configure()
        .enabled(cooperationEnabled)
        .build(getClass()); // before re-encrypt-task
  }

  @Override
  public String submitReEncryption(
      final String algorithmForDecryption,
      final Integer iterationsForDecryption,
      final String notifyEmail) throws MissingKeyException, ReEncryptionNotSupportedException
  {
    return submitTask(algorithmForDecryption, iterationsForDecryption, notifyEmail);
  }

  private void checkReEncryptionSupported() {
    if (!databaseCheck.isAtLeast(SECRETS_MIGRATION_VERSION)) {
      throw new ReEncryptionNotSupportedException(
          format("Re-encryption api key principals is not supported. Please upgrade DB to version %s",
              SECRETS_MIGRATION_VERSION));
    }
  }

  private void checkTaskNotSubmitted() {
    if (taskScheduler.getTaskByTypeId(ReEncryptPrincipalsTaskDescriptor.TYPE_ID) != null) {
      throw new IllegalStateException("Re-encryption principals task is already running");
    }
  }

  /**
   * Schedules re-encrypt principals task if there is not an existing one
   *
   * @param algorithmForDecryption the algorithm to be used for decrypting the principals
   * @param notifyEmail the email address to notify when the re-encryption principals task is complete
   * @return the task id
   */
  private String submitTask(
      final String algorithmForDecryption,
      final Integer iterationsForDecryption,
      final String notifyEmail)
  {
    checkReEncryptionSupported();
    checkTaskNotSubmitted();
    TaskInfo scheduledTask =
        Optional.ofNullable(maybeScheduleReEncrypt(algorithmForDecryption, iterationsForDecryption, notifyEmail))
            .orElseThrow(() -> new RuntimeException("Failed to schedule re-encryption task"));
    return scheduledTask.getId();
  }

  private TaskInfo maybeScheduleReEncrypt(
      final String algorithmForDecryption,
      final Integer iterationsForDecryption,
      final String notifyEmail)
  {
    try {
      return cooperation.on(() -> scheduleReEncryptTask(algorithmForDecryption, iterationsForDecryption, notifyEmail))
          .checkFunction(this::getTaskInfo)
          .cooperate("schedule_re-encryption-api-keys-principal");
    }
    catch (Exception e) {
      log.warn("Failed to schedule re-encryption principals task", e);
      return null;
    }
  }

  private TaskInfo scheduleReEncryptTask(
      final String algorithmForDecryption,
      final Integer iterationsForDecryption,
      final String notifyEmail)
  {
    return getTaskInfo().orElseGet(() -> {
      log.debug("Scheduling re-encrypt principals task");
      TaskConfiguration taskConfiguration =
          taskScheduler.createTaskConfigurationInstance(ReEncryptPrincipalsTaskDescriptor.TYPE_ID);
      taskConfiguration.setString("algorithmForDecryption", algorithmForDecryption);
      if (iterationsForDecryption != null) {
        taskConfiguration.setInteger("iterationsForDecryption", iterationsForDecryption);
      }

      Optional.ofNullable(notifyEmail).ifPresent(taskConfiguration::setAlertEmail);
      return taskScheduler.submit(taskConfiguration);
    });
  }

  private Optional<TaskInfo> getTaskInfo() {
    return taskScheduler.listsTasks()
        .stream()
        .filter(taskInfo -> taskInfo.getTypeId().equals(ReEncryptPrincipalsTaskDescriptor.TYPE_ID))
        .findFirst();
  }
}
