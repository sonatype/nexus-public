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

import java.util.concurrent.Future;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
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

  protected final Logger log;

  private Future<?> progressLoggingThread;

  private TaskLoggingEvent lastProgressEvent;

  ProgressTaskLogger(final Logger log) {
    this.log = checkNotNull(log);
  }

  @Override
  public void start() {
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

  @VisibleForTesting
  void logProgress() {
    if (lastProgressEvent != null) {
      log.info(INTERNAL_PROGRESS, format(PROGRESS_LINE, lastProgressEvent.getMessage()),
          lastProgressEvent.getArgumentArray());

      // clear last progress so it does not get logged again
      lastProgressEvent = null;
    }
  }

  private void startProgressThread() {
    progressLoggingThread = newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(this::logProgress, INTERVAL_MINUTES, INTERVAL_MINUTES, MINUTES);
  }
}
