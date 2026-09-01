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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.NEXUS_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

/**
 * {@link TaskLogger} implementation which handles the logic for creating separate task log files per task execution.
 * Extends {@link ProgressTaskLogger} to also include progress functionality.
 * Note logback handles most of the work (see logback.xml, TaskLogsFilter, and NexusLogFilter in nexus-logging).
 *
 * @since 3.5
 */
public class SeparateTaskLogTaskLogger
    extends ProgressTaskLogger
{
  protected static final String TASK_LOG_LOCATION_PREFIX = "Task log: ";

  private final TaskLogInfo taskLogInfo;

  private final String taskLogIdentifier;

  SeparateTaskLogTaskLogger(final Logger log, final TaskLogInfo taskLogInfo) {
    super(log);
    this.taskLogInfo = checkNotNull(taskLogInfo);

    // Set per-thread logback property via MDC (see logback.xml)
    taskLogIdentifier = format("%s-%s", taskLogInfo.getTypeId(),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
    MDC.put(LOGBACK_TASK_DISCRIMINATOR_ID, taskLogIdentifier);
  }

  private void logTaskInfo() {
    // dump task details to task log and nexus log
    log.info("Task information:");
    log.info(" ID: {}", taskLogInfo.getId());
    log.info(" Type: {}", taskLogInfo.getTypeId());
    log.info(" Name: {}", taskLogInfo.getName());
    log.info(" Description: {}", taskLogInfo.getMessage());
    log.info("Task configuration: {}", taskLogInfo);

    writeLogFileNameToNexusLog();
  }

  protected void writeLogFileNameToNexusLog() {
    String taskLogsHome = TaskLogHome.getTaskLogsHome();
    if (taskLogsHome != null) {
      String filename = format("%s/%s", taskLogsHome, getTaskLogIdentifier());
      log.info(NEXUS_LOG_ONLY, TASK_LOG_LOCATION_PREFIX + filename);
    }
  }

  private String getTaskLogIdentifier() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Appender<ILoggingEvent> appender = loggerContext.getLogger(ROOT_LOGGER_NAME).getAppender("tasklogfile");
    if (appender instanceof RollingFileAppender) {
      File file = new File(((RollingFileAppender<ILoggingEvent>) appender).getFile());
      return file.getName();
    }
    return taskLogIdentifier + ".log";
  }

  @Override
  public final void start() {
    super.start();
    logTaskInfo();
  }

  @Override
  public final void finish() {
    super.finish();
    log.info(TASK_LOG_ONLY, "Task complete");
    MDC.remove(LOGBACK_TASK_DISCRIMINATOR_ID);
    MDC.remove(TASK_LOG_ONLY_MDC);
    MDC.remove(TASK_LOG_WITH_PROGRESS_MDC);
    stopNestedFileAppender();
  }

  /**
   * Stops the nested FileAppender created by the SiftingAppender for this task.
   * This releases the file handle that would otherwise remain open, preventing file handle leaks.
   *
   * <p>
   * Note: The AppenderTracker may still hold a reference to this appender after stopping it,
   * but Logback's UnsynchronizedAppenderBase checks the 'started' flag before appending,
   * so late-arriving log events are safely ignored. The tracker will eventually evict the
   * stopped appender during its normal stale-component cleanup cycle (default: 30 minutes
   * when no {@code <timeout>} is configured in the SiftingAppender's sift block).
   *
   * @see <a href="https://sonatype.atlassian.net/browse/NEXUS-29099">NEXUS-29099</a>
   */
  void stopNestedFileAppender() {
    SiftingAppender siftingAppender = getTaskLogFileSiftingAppender();
    if (siftingAppender == null) {
      return;
    }

    Appender<ILoggingEvent> nestedAppender = siftingAppender.getAppenderTracker().find(taskLogIdentifier);

    if (nestedAppender instanceof FileAppender<ILoggingEvent> fileAppender) {
      fileAppender.stop();
      log.debug("Stopped nested FileAppender for task log: {}", taskLogIdentifier);
    }
    else if (nestedAppender != null) {
      log.debug("Nested appender for task {} is not a FileAppender (type: {}), skipping stop",
          taskLogIdentifier, nestedAppender.getClass().getSimpleName());
    }
    else {
      log.debug("No nested appender found for task: {}", taskLogIdentifier);
    }
  }

  /**
   * Returns the SiftingAppender used for task log files, or null if not configured.
   * This method is package-private for testability.
   */
  SiftingAppender getTaskLogFileSiftingAppender() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Appender<ILoggingEvent> appender = loggerContext.getLogger(ROOT_LOGGER_NAME).getAppender("tasklogfile");

    if (appender instanceof SiftingAppender siftingAppender) {
      return siftingAppender;
    }

    if (appender != null) {
      log.debug("Task log appender is not a SiftingAppender (type: {}), skipping nested appender stop",
          appender.getClass().getSimpleName());
    }

    return null;
  }

  @Override
  public void flush() {
    if (lastProgressEvent != null) {
      Logger logger = Optional.ofNullable(lastProgressEvent.getLogger()).orElse(log);
      logger.info(PROGRESS, lastProgressEvent.getMessage(), lastProgressEvent.getArgumentArray());
    }
    super.flush();
  }
}
