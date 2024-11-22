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
package org.sonatype.nexus.logging.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.NEXUS_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

public class ReplicationTaskLogger
    extends ProgressTaskLogger
{
  public static final String REPLICATION_LOG_LOCATION_PREFIX =
      "running replication task for repository '{}' , replication log : {}";

  public static final String REPLICATION_DISCRIMINATOR_ID = "repositoryName";

  private final TaskLogInfo taskLogInfo;

  private final String repositoryName;

  ReplicationTaskLogger(final Logger log, final TaskLogInfo taskLogInfo) {
    super(log);

    this.taskLogInfo = checkNotNull(taskLogInfo);
    this.repositoryName = taskLogInfo.getString(REPLICATION_DISCRIMINATOR_ID);
    MDC.put(TASK_LOG_ONLY_MDC, "true");
    MDC.put(REPLICATION_DISCRIMINATOR_ID, repositoryName);
  }

  private void logReplicationRunInfo() {
    // show task details on replication log
    log.info(TASK_LOG_ONLY, "Replication run info:");
    log.info(TASK_LOG_ONLY, " Task ID: {}", taskLogInfo.getId());
    log.info(TASK_LOG_ONLY, " Type: {}", taskLogInfo.getTypeId());
    log.info(TASK_LOG_ONLY, " Name: {}", taskLogInfo.getName());
    log.info(TASK_LOG_ONLY, " Description: {}", taskLogInfo.getMessage());

    writeReplicationRunOnNexusLog();
  }

  private void writeReplicationRunOnNexusLog() {
    MDC.remove(TASK_LOG_ONLY_MDC);

    TaskLogHome.getReplicationLogsHome()
        .ifPresent((home) -> {
          String identifier = "replication-" + repositoryName + ".log";
          String filename = format("%s/%s", home, identifier);
          log.info(NEXUS_LOG_ONLY, REPLICATION_LOG_LOCATION_PREFIX, repositoryName, filename);
        });

    MDC.put(TASK_LOG_ONLY_MDC, "true");
  }

  @Override
  public final void start() {
    super.start();
    logReplicationRunInfo();
  }

  @Override
  public final void finish() {
    super.finish();
    log.info(TASK_LOG_ONLY, "Task complete");
    MDC.remove(TASK_LOG_ONLY_MDC);
    MDC.remove(REPLICATION_DISCRIMINATOR_ID);
  }

  public void flush() {
    if (lastProgressEvent != null) {
      Logger logger = Optional.ofNullable(lastProgressEvent.getLogger()).orElse(log);
      logger.info(PROGRESS, lastProgressEvent.getMessage(), lastProgressEvent.getArgumentArray());
    }
    super.flush();
  }
}
