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
package org.sonatype.nexus.common.hash;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When parallel is enabled (default on) and the {@link ForkJoinPool#commonPool()} isn't saturated provides
 * {@link ParallelMultiHashingInputStream} when disabled a {@link MultiHashingInputStream}
 */
public final class MultiHashingInputStreamFactory
{
  public static final Logger log = LoggerFactory.getLogger(MultiHashingInputStreamFactory.class);

  private static final String ENABLED_ENV_VAR = "NEXUS_HASHING_PARALLELISM";

  private static final String ENABLED_SYS_PROP = "nexus.hashing.parallism";

  private static final String THRESHOLD_ENV_VAR = "NEXUS_HASHING_THRESHOLD";

  private static final String THRESHOLD_SYS_PROP = "nexus.hashing.threshold";

  /*
   * See belowThreshold()
   */
  private static int threshold;

  private static boolean enabled;

  static {
    enabled = Boolean.valueOf(Optional.ofNullable(System.getenv(ENABLED_ENV_VAR))
        .orElseGet(() -> System.getProperty(ENABLED_SYS_PROP, Boolean.TRUE.toString())));

    threshold = Integer.valueOf(Optional.ofNullable(System.getenv(THRESHOLD_ENV_VAR))
        .orElseGet(() -> System.getProperty(THRESHOLD_SYS_PROP, "-1")));

    if (!enabled || threshold != -1) {
      // log only for non-default settings
      log.info("Configured with enabled={} threshold={}", enabled, threshold);
    }
  }

  private MultiHashingInputStreamFactory() {
    // private
  }

  /*
   * Exists for use by Groovy scripting if necessary
   */
  @VisibleForTesting
  public static void enableParallel() {
    log.info("Enabling parallel input stream hashing. Threshold {}", threshold);

    enabled = true;
  }

  /*
   * Exists for use by Groovy scripting if necessary
   */
  @VisibleForTesting
  public static void disableParallel() {
    log.info("Disabling parallel input stream hashing");

    enabled = false;
  }

  /*
   * Exists for use by Groovy scripting if necessary
   */
  @VisibleForTesting
  public static void setThreshold(final int threshold) {
    log.info("Setting threshold to {}. Parallel input stream hashing enabled={}", threshold, enabled);

    MultiHashingInputStreamFactory.threshold = threshold;
  }

  public static MultiHashingInputStream input(final Iterable<HashAlgorithm> algorithms, final InputStream inputStream) {
    if (enabled && belowThreshold()) {
      return new ParallelMultiHashingInputStream(algorithms, inputStream);
    }
    return new MultiHashingInputStream(algorithms, inputStream);
  }

  /*
   * We only use parallelism if the commonPool does not currently have a large backlog of queued tasks. This is
   * determined by the queued tasks exceeding more than threshold tasks queued per worker (as defined by parallelism).
   *
   * Values below zero
   */
  private static boolean belowThreshold() {
    if (threshold < 0) {
      // negative values disable limits
      return true;
    }

    ForkJoinPool commonPool = ForkJoinPool.commonPool();
    int queued = commonPool.getQueuedSubmissionCount();
    int maxQueue = commonPool.getParallelism() * threshold;
    boolean belowMax = queued < maxQueue;

    if (log.isTraceEnabled()) {
      log.trace("Threshold {}. Queued {}. Max Queue {}. Below max {}", threshold, queued, maxQueue, belowMax);
    }

    return belowMax;
  }
}
