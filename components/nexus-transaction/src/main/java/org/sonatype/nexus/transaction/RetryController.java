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
package org.sonatype.nexus.transaction;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Time;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.union;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static org.sonatype.goodies.common.Time.millis;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getInteger;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getString;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getTime;

/**
 * Default controller of {@link Transaction} retries.
 *
 * @since 3.next
 */
public class RetryController
    extends ComponentSupport
{
  private static final int DEFAULT_RETRY_LIMIT = 8;

  private static final int DEFAULT_MIN_SLOTS = 2;

  private static final int DEFAULT_MAX_SLOTS = 256;

  private static final Time DEFAULT_MINOR_DELAY_MILLIS = millis(10);

  private static final Time DEFAULT_MAJOR_DELAY_MILLIS = millis(100);

  public static final RetryController INSTANCE = new RetryController();

  private final RollingStats excessiveRetriesHourlyStats = new RollingStats(60, MINUTES);

  private int retryLimit = getInteger("nexus.tx.retry.limit", DEFAULT_RETRY_LIMIT);

  private int minSlots = getInteger("nexus.tx.retry.minSlots", DEFAULT_MIN_SLOTS);

  private int maxSlots = getInteger("nexus.tx.retry.maxSlots", DEFAULT_MAX_SLOTS);

  private int minorDelayMillis = getTime("nexus.tx.retry.minorDelay", DEFAULT_MINOR_DELAY_MILLIS).toMillisI();

  private int majorDelayMillis = getTime("nexus.tx.retry.majorDelay", DEFAULT_MAJOR_DELAY_MILLIS).toMillisI();

  private Set<String> majorExceptionFilter = parseFilter(
      getString("nexus.tx.retry.majorExceptionFilter", IOException.class.getName()));

  /**
   * Point at which we declare the transaction as having excessive retries.
   */
  private int excessiveRetriesThreshold;

  public RetryController() {
    updateExcessiveRetriesThreshold();
  }

  /**
   * Gets maximum number of retries allowed.
   */
  public int getRetryLimit() {
    return retryLimit;
  }

  /**
   * Sets maximum number of retries allowed.
   */
  public void setRetryLimit(final int retryLimit) {
    this.retryLimit = retryLimit;
    updateExcessiveRetriesThreshold();
  }

  /**
   * Gets minimum number of slots in the binary exponential backoff.
   */
  public int getMinSlots() {
    return minSlots;
  }

  /**
   * Sets minimum number of slots in the binary exponential backoff.
   */
  public void setMinSlots(final int minSlots) {
    checkArgument(minSlots >= 0);
    this.minSlots = minSlots;
  }

  /**
   * Gets maximum number of slots in the binary exponential backoff.
   */
  public int getMaxSlots() {
    return maxSlots;
  }

  /**
   * Sets maximum number of slots in the binary exponential backoff.
   */
  public void setMaxSlots(final int maxSlots) {
    checkArgument(maxSlots >= 0);
    this.maxSlots = maxSlots;
  }

  /**
   * Gets the initial backoff delay to be used for minor exceptions.
   */
  public int getMinorDelayMillis() {
    return minorDelayMillis;
  }

  /**
   * Sets the initial backoff delay to be used for minor exceptions.
   */
  public void setMinorDelayMillis(final int minorDelayMillis) {
    checkArgument(minorDelayMillis >= 0);
    this.minorDelayMillis = minorDelayMillis;
  }

  /**
   * Gets the initial backoff delay to be used for major exceptions.
   */
  public int getMajorDelayMillis() {
    return majorDelayMillis;
  }

  /**
   * Sets the initial backoff delay to be used for major exceptions.
   */
  public void setMajorDelayMillis(final int majorDelayMillis) {
    checkArgument(majorDelayMillis >= 0);
    this.majorDelayMillis = majorDelayMillis;
  }

  /**
   * Gets the comma-separated list of types to be considered as major exceptions.
   */
  public String getMajorExceptionFilter() {
    return majorExceptionFilter.stream().collect(joining(","));
  }

  /**
   * Sets the comma-separated list of types to be considered as major exceptions.
   */
  public void setMajorExceptionFilter(final String majorExceptionFilter) {
    this.majorExceptionFilter = parseFilter(majorExceptionFilter);
  }

  /**
   * Treats the given exception type as a major exception.
   */
  public void addAsMajorException(final Class<? extends Exception> type) {
    this.majorExceptionFilter = union(majorExceptionFilter, ImmutableSet.of(type.getName())).immutableCopy();
  }

  /**
   * Treats the given exception type as a minor exception.
   */
  public void removeAsMajorException(final Class<? extends Exception> type) {
    this.majorExceptionFilter = difference(majorExceptionFilter, ImmutableSet.of(type.getName())).immutableCopy();
  }

  /**
   * Immediately returns {@code false} if we've exceeded the maximum number of retries; otherwise waits,
   * using binary exponential backoff to hopefully avoid further collisions, before returning {@code true}.
   *
   * @param retriesSoFar the number of retries so far
   * @param cause the exception that caused the retry
   */
  public boolean allowRetry(final int retriesSoFar, final Exception cause) {
    int nextRetry = retriesSoFar + 1;
    if (nextRetry > retryLimit) {
      if (log.isTraceEnabled()) {
        log.warn("Exceeded retry limit: {}/{}", retriesSoFar, retryLimit, cause);
      }
      else {
        log.warn("Exceeded retry limit: {}/{} ({})", retriesSoFar, retryLimit, cause.toString());
      }

      return false;
    }

    /**
     * Once we reach this threshold declare the transaction as having excessive retries.
     *
     * Note: this is only done once as we cross the threshold, not for every retry.
     */
    if (nextRetry == excessiveRetriesThreshold) {
      excessiveRetriesHourlyStats.mark();
    }

    long delay = randomDelay(nextRetry, cause);

    if (log.isTraceEnabled()) {
      log.debug("Allowing retry: {}/{} in {}ms", nextRetry, retryLimit, delay, cause);
    }
    else {
      log.debug("Allowing retry: {}/{} in {}ms ({})", nextRetry, retryLimit, delay, cause.toString());
    }

    backoff(delay);
    return true;
  }

  /**
   * Return the number of transactions with excessive retries in the last hour.
   */
  public long excessiveRetriesInLastHour() {
    return excessiveRetriesHourlyStats.sum();
  }

  /**
   * Updates the excessive retries threshold to be just above the mid-point of the retry limit.
   */
  private void updateExcessiveRetriesThreshold() {
    excessiveRetriesThreshold = (retryLimit >> 1) + 1;
  }

  /**
   * Applies backoff by waiting for the given delay before allowing the retry.
   */
  @VisibleForTesting
  protected void backoff(final long delay) {
    try {
      Thread.sleep(delay);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Picks a random delay using a binary exponential backoff approach.
   *
   * @see https://en.wikipedia.org/wiki/Exponential_backoff
   */
  private long randomDelay(final int nextRetry, final Exception cause) {
    int slots = min(max(1 << nextRetry, minSlots), maxSlots);
    int randomSlot = ThreadLocalRandom.current().nextInt(slots);
    if (isMajorException(cause)) {
      // avoid zero wait if it's a major exception
      return majorDelayMillis * (randomSlot + 1);
    }
    else {
      return minorDelayMillis * randomSlot;
    }
  }

  /**
   * Parses a comma-separated list into a {@link Set}.
   */
  private static Set<String> parseFilter(final String filter) {
    return ImmutableSet.copyOf(filter.split("\\s*,\\s*"));
  }

  /**
   * Determines whether this is a major exception (like an I/O issue) resulting in a longer delay.
   */
  private boolean isMajorException(final Exception cause) {
    return majorExceptionFilter.contains(cause.getClass().getName());
  }
}
