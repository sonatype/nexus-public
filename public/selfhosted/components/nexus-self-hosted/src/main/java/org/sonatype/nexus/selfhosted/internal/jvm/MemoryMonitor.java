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
package org.sonatype.nexus.selfhosted.internal.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors memory usage and logs warnings when memory thresholds are exceeded for a sustained period.
 *
 * Requires {@code nexus.jvm.memory.sustained.samples} consecutive samples above a threshold before
 * logging, preventing false alarms from transient G1GC pre-collection heap peaks.
 */
@Component
public class MemoryMonitor
    implements JvmMonitor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String SYSTEM_MARKER = "*SYSTEM";

  // Log level constants: 0=none logged, 1=warn logged, 2=error logged
  private static final int LEVEL_NONE = 0;

  private static final int LEVEL_WARN = 1;

  private static final int LEVEL_ERROR = 2;

  private final int memoryWarningThresholdPercent;

  private final int memoryCriticalThresholdPercent;

  private final int memorySustainedSamples;

  private final MemoryMXBean memoryMXBean;

  private final List<MemoryPoolMXBean> memoryPoolMXBeans;

  // Debounce state — written by the lifecycle thread (initialize) before the scheduler starts,
  // and thereafter only by the single periodic thread. volatile provides the cross-thread
  // visibility guarantee for the initialize() → scheduler handoff.
  private volatile int consecutiveSamplesAboveThreshold = 0;

  private volatile long thresholdExceededSinceMs = -1L;

  private volatile int lastLoggedLevel = LEVEL_NONE;

  @Autowired
  public MemoryMonitor(
      @Value("${nexus.jvm.memory.warning.threshold:80}") final int memoryWarningThresholdPercent,
      @Value("${nexus.jvm.memory.critical.threshold:90}") final int memoryCriticalThresholdPercent,
      @Value("${nexus.jvm.memory.sustained.samples:3}") final int memorySustainedSamples)
  {
    this.memoryWarningThresholdPercent = memoryWarningThresholdPercent;
    this.memoryCriticalThresholdPercent = memoryCriticalThresholdPercent;
    this.memorySustainedSamples = memorySustainedSamples;
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    this.memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
  }

  @Override
  public void initialize() {
    resetDebounceState();
  }

  @Override
  public void check() {
    MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
    long used = heapUsage.getUsed();
    long max = heapUsage.getMax();

    if (max <= 0) {
      return;
    }

    int usagePercent = (int) ((used * 100) / max);

    if (usagePercent >= memoryCriticalThresholdPercent) {
      trackThresholdBreach(usagePercent, used, max, true);
    }
    else if (usagePercent >= memoryWarningThresholdPercent) {
      trackThresholdBreach(usagePercent, used, max, false);
    }
    else {
      if (lastLoggedLevel > LEVEL_NONE && log.isInfoEnabled()) {
        Map<String, String> poolInfo = getMemoryPoolInfo();
        log.info("{} [jvm monitor] [memory] Heap recovered: {}% ({}/{}), pools={}",
            SYSTEM_MARKER,
            usagePercent,
            formatBytes(used),
            formatBytes(max),
            poolInfo);
      }
      resetDebounceState();
      if (log.isDebugEnabled()) {
        log.debug("{} [jvm monitor] [memory] Heap usage: {}% ({}/{})",
            SYSTEM_MARKER,
            usagePercent,
            formatBytes(used),
            formatBytes(max));
      }
    }
  }

  private void trackThresholdBreach(
      final int usagePercent,
      final long used,
      final long max,
      final boolean critical)
  {
    // Cap at memorySustainedSamples to prevent integer overflow on long-running instances
    consecutiveSamplesAboveThreshold = Math.min(consecutiveSamplesAboveThreshold + 1, memorySustainedSamples);
    if (thresholdExceededSinceMs < 0) {
      thresholdExceededSinceMs = System.currentTimeMillis();
    }

    if (consecutiveSamplesAboveThreshold < memorySustainedSamples) {
      return;
    }

    // Time since the first sample crossed the threshold (includes the pre-debounce window),
    // i.e. the real-world duration the heap has been elevated, not just since the log fired.
    long sustainedMs = System.currentTimeMillis() - thresholdExceededSinceMs;

    if (critical && lastLoggedLevel < LEVEL_ERROR && log.isErrorEnabled()) {
      Map<String, String> poolInfo = getMemoryPoolInfo();
      log.error("{} [jvm monitor] [memory] CRITICAL heap usage: {}% ({}/{}), sustained={}ms, pools={}",
          SYSTEM_MARKER,
          usagePercent,
          formatBytes(used),
          formatBytes(max),
          sustainedMs,
          poolInfo);
      lastLoggedLevel = LEVEL_ERROR;
    }
    else if (!critical && lastLoggedLevel < LEVEL_WARN && log.isWarnEnabled()) {
      Map<String, String> poolInfo = getMemoryPoolInfo();
      log.warn("{} [jvm monitor] [memory] High heap usage: {}% ({}/{}), sustained={}ms, pools={}",
          SYSTEM_MARKER,
          usagePercent,
          formatBytes(used),
          formatBytes(max),
          sustainedMs,
          poolInfo);
      lastLoggedLevel = LEVEL_WARN;
    }
  }

  private void resetDebounceState() {
    consecutiveSamplesAboveThreshold = 0;
    thresholdExceededSinceMs = -1L;
    lastLoggedLevel = LEVEL_NONE;
  }

  private Map<String, String> getMemoryPoolInfo() {
    Map<String, String> poolInfo = new HashMap<>();
    for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
      if (pool.getType() == MemoryType.HEAP) {
        MemoryUsage usage = pool.getUsage();
        poolInfo.put(pool.getName(), formatBytes(usage.getUsed()) + "/" + formatBytes(usage.getMax()));
      }
    }
    return poolInfo;
  }

  private String formatBytes(final long bytes) {
    if (bytes < 0) {
      return "unknown";
    }
    if (bytes < 1024) {
      return bytes + "b";
    }
    if (bytes < 1024 * 1024) {
      return String.format("%.1fkb", bytes / 1024.0);
    }
    if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1fmb", bytes / (1024.0 * 1024));
    }
    return String.format("%.1fgb", bytes / (1024.0 * 1024 * 1024));
  }
}
