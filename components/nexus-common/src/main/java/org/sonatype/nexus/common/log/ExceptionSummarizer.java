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
package org.sonatype.nexus.common.log;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Summarizes logging of continually repeated exceptions to avoid flooding logs.
 *
 * When an exception repeats the stack trace is shown for the first occurrence,
 * with a summary line logged every 5 seconds the exception continues to repeat.
 * The full stack is logged again each minute, assuming it's still repeating.
 *
 * @since 3.6
 */
public class ExceptionSummarizer
{
  private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1L);

  private static final long ONE_SECOND = TimeUnit.SECONDS.toMillis(1L);

  private static final long FIVE_SECONDS = 5 * ONE_SECOND;

  private final BiPredicate<Exception, Exception> matcher;

  private final BiConsumer<String, Exception> logger;

  @Nullable
  private Exception failureCause;

  private long firstFailureMillis;

  private long lastSummaryMillis;

  private int count;

  /**
   * Summarizes exceptions using the matcher to detect repeats; summaries are written to the logger.
   */
  public static ExceptionSummarizer summarize(
      final BiPredicate<Exception, Exception> matcher,
      final BiConsumer<String, Exception> logger)
  {
    return new ExceptionSummarizer(matcher, logger);
  }

  /**
   * Summarize exceptions of the same type.
   */
  public static BiPredicate<Exception, Exception> sameType() {
    return (lastFailure, failure) -> lastFailure != null && lastFailure.getClass().equals(failure.getClass());
  }

  /**
   * Summarize exceptions with the same text.
   */
  public static BiPredicate<Exception, Exception> sameText() {
    return (lastFailure, failure) -> lastFailure != null && lastFailure.toString().equals(failure.toString());
  }

  /**
   * Write summaries to the WARN logger.
   */
  public static BiConsumer<String, Exception> warn(final Logger logger) {
    return (message, failure) -> logger.warn(message, failure);
  }

  /**
   * Write summaries to the INFO logger.
   */
  public static BiConsumer<String, Exception> info(final Logger logger) {
    return (message, failure) -> logger.info(message, failure);
  }

  @VisibleForTesting
  ExceptionSummarizer(
      final BiPredicate<Exception, Exception> matcher,
      final BiConsumer<String, Exception> logger)
  {
    this.matcher = checkNotNull(matcher);
    this.logger = checkNotNull(logger);
  }

  /**
   * Logs the given failure, summarizing repeated exceptions to minimize log-spam.
   */
  public synchronized void log(final String message, final Exception cause) {
    count++;
    long now = currentTimeMillis();
    if (!matcher.test(failureCause, cause) || now - firstFailureMillis >= ONE_MINUTE) {

      // new exception or its been over a minute since the first failure
      logger.accept(message, cause);

      failureCause = cause;
      firstFailureMillis = now;
      lastSummaryMillis = now;
      count = 0;
    }
    else if (now - lastSummaryMillis >= FIVE_SECONDS) {

      // repeating exception, log summary without stack at most every 5 seconds
      String summary = String.format("%s: %s - occurred %d times in last %d seconds",
          message, cause, count, (now - lastSummaryMillis) / ONE_SECOND);
      logger.accept(summary, null);

      lastSummaryMillis = now;
      count = 0;
    }
  }

  @VisibleForTesting
  long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
