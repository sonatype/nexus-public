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
package org.sonatype.nexus.internal.security.secrets.tasks;

import java.time.Duration;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogType;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

@Named(ReEncryptTaskDescriptor.TYPE_ID)
@TaskLogging(TaskLogType.TASK_LOG_ONLY)
public class ReEncryptTask
    extends TaskSupport
    implements Cancelable
{
  private final SecretsService secretsService;

  private final SecretsStore secretsStore;

  private final long delayTimeMs;

  @Inject
  public ReEncryptTask(
      final SecretsService secretsService,
      final SecretsStore secretsStore,
      @Named("${nexus.distributed.events.fetch.interval.seconds:-5}") int pollInterval)
  {
    this.secretsService = checkNotNull(secretsService);
    this.secretsStore = checkNotNull(secretsStore);
    this.delayTimeMs = Duration.ofSeconds(pollInterval).toMillis() * 2;
  }

  @Override
  public String getMessage() {
    return "Re-encrypting secrets with specified key id";
  }

  @Override
  protected Object execute() throws Exception {
    waitActiveKeyProcessing();
    log.info("Started re-encrypting secrets with provided keyId");
    String secretKeyId = taskConfiguration().getString("keyId");
    int processed = reEncrypt(secretKeyId);
    log.info("Completed re-encryption of secrets with keyId '{}'. Processed {} secrets", secretKeyId, processed);
    return processed;
  }

  private int reEncrypt(String keyId) {
    int processedCount = 0;
    List<SecretData> page = secretsStore.fetchWithDifferentKeyId(keyId, Continuations.BROWSE_LIMIT);
    try (ProgressLogIntervalHelper progress = new ProgressLogIntervalHelper(log, 60)) {
      while (!page.isEmpty()) {
        page.forEach(secret -> {
          checkCancellation();
          secretsService.reEncrypt(secret, keyId);
        });
        processedCount += page.size();
        progress.info("Processed {} secrets.", processedCount);
        page = secretsStore.fetchWithDifferentKeyId(keyId, Continuations.BROWSE_LIMIT);
      }
    }
    return processedCount;
  }

  /**
   * Delay to wait for other nodes to process the event that sets the new active key
   */
  private void waitActiveKeyProcessing() {
    try {
      Thread.sleep(delayTimeMs);
    }
    catch (InterruptedException e) {
      // ignore
    }
  }

}
