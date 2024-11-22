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

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;

/**
 * Helper for logging progress messages, one per defined interval.
 */
public class ProgressLogIntervalHelper
    implements AutoCloseable
{
  private final Stopwatch elapsed;

  private final Stopwatch progress;

  private final Logger logger;

  private final int internal;

  private static final long SECONDS_PER_MINUTE = 60;

  private static final long SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;

  private static final long SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;

  public ProgressLogIntervalHelper(final Logger logger, int intervalInSeconds) {
    this.logger = checkNotNull(logger);
    this.internal = intervalInSeconds;

    this.elapsed = Stopwatch.createStarted();
    this.progress = Stopwatch.createStarted();
  }

  /**
   * Get elapsed time as a string so it can be included in logs
   */
  public String getElapsed() {
    return formatDuration(elapsed.elapsed().getSeconds());
  }

  private String formatDuration(final long durationSeconds) {
    StringBuilder builder = new StringBuilder();
    long seconds = durationSeconds;

    long days = seconds / SECONDS_PER_DAY;
    seconds = seconds - (days * SECONDS_PER_DAY);
    if (days > 0) {
      builder.append(days).append("d ");
    }
    long hours = seconds / SECONDS_PER_HOUR;
    seconds = seconds - (hours * SECONDS_PER_HOUR);
    if (hours > 0 || builder.length() > 0) {
      builder.append(hours).append("h ");
    }
    long minutes = seconds / SECONDS_PER_MINUTE;
    seconds = seconds - (minutes * SECONDS_PER_MINUTE);
    if (minutes > 0 || builder.length() > 0) {
      builder.append(minutes).append("m ");
    }
    builder.append(seconds).append("s");

    return builder.toString();
  }

  /**
   * Log the message using the PROGRESS marker. Will only send the log message to logback once per interval, otherwise
   * will store the message in the task logger context
   */
  public void info(String message, Object... args) {
    if (hasIntervalElapsed()) {
      logger.info(PROGRESS, message, args);
    }
    else {
      TaskLoggerHelper.progress(logger, message, args);
    }
  }

  /**
   * Flush any pending progress messages so they are logged immediately. This should be called when the section of code
   * receiving progress logging is complete, otherwise a stale progress message might get logged out of sequence.
   *
   * @see TaskLogger#flush()
   */
  public void flush() {
    TaskLoggerHelper.flush();
  }

  private boolean hasIntervalElapsed() {
    boolean logProgress = progress.elapsed(TimeUnit.SECONDS) >= internal;
    if (logProgress) {
      progress.reset().start();
    }
    return logProgress;
  }

  @Override
  public void close() {
    this.flush();
  }
}
