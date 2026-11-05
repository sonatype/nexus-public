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
package org.sonatype.nexus.blobstore;

import java.util.Arrays;

import com.google.common.base.Ticker;
import org.slf4j.Logger;

/**
 * Logs at most one WARN per interval for a recurring failure. Failures inside the interval are
 * logged at DEBUG with the full exception, and the next WARN reports how many were suppressed.
 */
final class RateLimitedWarn
{
  private final Logger log;

  private final long intervalNanos;

  private final Ticker ticker;

  private boolean warnedBefore;

  private long lastWarnNanos;

  private long suppressed;

  RateLimitedWarn(final Logger log, final long intervalNanos, final Ticker ticker) {
    this.log = log;
    this.intervalNanos = intervalNanos;
    this.ticker = ticker;
  }

  /**
   * @param failure logged with its stack trace only when demoted to DEBUG
   * @param format message format; its placeholders must match {@code args}
   */
  synchronized void warn(final Throwable failure, final String format, final Object... args) {
    long now = ticker.read();
    if (!warnedBefore || now - lastWarnNanos >= intervalNanos) {
      warnedBefore = true;
      lastWarnNanos = now;
      long count = suppressed;
      suppressed = 0;
      if (count == 0) {
        log.warn(format, args);
      }
      else {
        Object[] withCount = Arrays.copyOf(args, args.length + 1);
        withCount[args.length] = count;
        log.warn(format + " ({} similar failures suppressed in the last interval)", withCount);
      }
    }
    else {
      suppressed++;
      Object[] withFailure = Arrays.copyOf(args, args.length + 1);
      withFailure[args.length] = failure;
      log.debug(format, withFailure);
    }
  }
}
