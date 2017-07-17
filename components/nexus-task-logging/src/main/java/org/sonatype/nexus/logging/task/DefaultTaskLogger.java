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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Future;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

/**
 * Handle logic for logging progress within scheduled tasks. Logback handles most of the work (see logback.xml,
 * TaskLogsFilter, and NexusLogFilter in nexus-pax-logging).
 * Additionally this class has starts a thread which will log regular (1 minute) progress update back to the main
 * nexus.log.
 *
 * @since 3.4.1
 */
public class DefaultTaskLogger
    implements TaskLogger
{
  static final String PROGRESS_LINE = "---- %s ----";

  static final String MARK_LINE = "Mark";

  private static final long DELAY = 60L;

  private static final long INTERVAL = 60L;

  private static final TaskLoggingEvent MARK_LOG_MESSAGE = new TaskLoggingEvent(MARK_LINE);

  private final Logger log;

  private final TaskLogInfo taskLogInfo;

  private Future<?> loggingThread;

  private TaskLoggingEvent lastProgressEvent = MARK_LOG_MESSAGE;

  DefaultTaskLogger(final Logger log, final TaskLogInfo taskLogInfo) {
    this.log = checkNotNull(log);
    this.taskLogInfo = checkNotNull(taskLogInfo);

    // Set per-thread logback property via MDC (see logback.xml)
    MDC.put(LOGBACK_TASK_DISCRIMINATOR_ID, getTaskLogIdentifier());
  }

  private String getTaskLogIdentifier() {
    return String.format("%s-%s", taskLogInfo.getTypeId(),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
  }

  private void logTaskInfo() {
    // dump task details to task log
    log.info(TASK_LOG_ONLY, "Task information:");
    log.info(TASK_LOG_ONLY, " ID: {}", taskLogInfo.getId());
    log.info(TASK_LOG_ONLY, " Type: {}", taskLogInfo.getTypeId());
    log.info(TASK_LOG_ONLY, " Name: {}", taskLogInfo.getName());
    log.info(TASK_LOG_ONLY, " Description: {}", taskLogInfo.getMessage());
    log.debug(TASK_LOG_ONLY, "Task configuration: {}", taskLogInfo.toString());
  }

  private void startLogThread() {
    loggingThread = newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(this::updateMainLogWithProgress, DELAY, INTERVAL, SECONDS);
  }

  @VisibleForTesting
  void updateMainLogWithProgress() {
    // if the lastProgressEvent is the 'mark' (i.e. there has been no progress), we want to log to both logs
    Marker marker = lastProgressEvent == MARK_LOG_MESSAGE ? null : TaskLoggingMarkers.NEXUS_LOG_ONLY;

    log.info(marker, format(PROGRESS_LINE, lastProgressEvent.getMessage()),
        lastProgressEvent.getArgumentArray());

    // reset last progress message to the mark
    lastProgressEvent = MARK_LOG_MESSAGE;
  }

  @Override
  public final void start() {
    startLogThread();
    logTaskInfo();
  }

  @Override
  public final void finish() {
    log.info(TASK_LOG_ONLY, "Task complete");
    MDC.remove(LOGBACK_TASK_DISCRIMINATOR_ID);
    MDC.remove(TASK_LOG_ONLY_MDC);
    loggingThread.cancel(true);
  }

  @Override
  public final void progress(final TaskLoggingEvent event) {
    lastProgressEvent = event;
  }
}
