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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.INTERNAL_PROGRESS;

/**
 * {@link TaskLogger} implementation which handles the logic for logging progress within scheduled tasks.
 * Additionally this class has starts a thread which will log regular (1 minute) progress update back to the main
 * nexus.log.
 *
 * @since 3.6
 */
public class ProgressTaskLogger
    implements TaskLogger
{
  static final String PROGRESS_LINE = "---- %s ----";

  private static final long INTERVAL_MINUTES = 10L;

  private static final ScheduledExecutorService executorService = createExecutorService();

  protected final Logger log;

  private Future<?> progressLoggingThread;

  private Map<String, String> mdcMap;

  private final long initialDelay;

  private final long progressInterval;

  private final TimeUnit timeUnit;

  TaskLoggingEvent lastProgressEvent;

  ProgressTaskLogger(final Logger log) {
    this(log, INTERVAL_MINUTES, INTERVAL_MINUTES, MINUTES);
  }

  ProgressTaskLogger(final Logger log,
                     final long initialDelay,
                     final long progressInterval,
                     final TimeUnit timeUnit)
  {
    this.log = checkNotNull(log);
    this.initialDelay = initialDelay;
    checkArgument(progressInterval > 0, "progressInterval must be greater than 0");
    this.progressInterval = progressInterval;
    this.timeUnit = checkNotNull(timeUnit);
  }

  @Override
  public void start() {
    mdcMap = MDC.getCopyOfContextMap();
    startProgressThread();
  }

  @Override
  public void finish() {
    progressLoggingThread.cancel(true);
  }

  @Override
  public void progress(final TaskLoggingEvent event) {
    lastProgressEvent = event;
  }

  @Override
  public void flush() {
    logProgress();
  }

  /**
   * @since 3.16
   */
  public static void shutdown() {
    executorService.shutdown();
  }

  @VisibleForTesting
  void logProgress() {
    if (lastProgressEvent != null) {
      if (mdcMap != null) {
        MDC.setContextMap(mdcMap);
      }

      Logger logger = Optional.ofNullable(lastProgressEvent.getLogger()).orElse(log);
      logger.info(INTERNAL_PROGRESS, format(PROGRESS_LINE, lastProgressEvent.getMessage()),
          lastProgressEvent.getArgumentArray());

      // clear last progress so it does not get logged again
      lastProgressEvent = null;
    }
  }

  private static ScheduledExecutorService createExecutorService() {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
        new ThreadFactoryBuilder().setNameFormat("task-logging-%d").build());
    executor.setRemoveOnCancelPolicy(true);
    return executor;
  }

  private void startProgressThread() {
    progressLoggingThread = executorService
        .scheduleAtFixedRate(this::logProgress, initialDelay, progressInterval, timeUnit);
  }
}
