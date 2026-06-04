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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors garbage collection activity and logs warnings when GC pauses exceed configured thresholds.
 */
@Component
public class GcMonitor
    implements JvmMonitor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String SYSTEM_MARKER = "*SYSTEM";

  private final long gcInfoThresholdMs;

  private final long gcWarningThresholdMs;

  private final long gcCriticalThresholdMs;

  private final MemoryMXBean memoryMXBean;

  private final List<GarbageCollectorMXBean> gcMXBeans;

  private final List<MemoryPoolMXBean> memoryPoolMXBeans;

  private final Map<String, GcStats> previousGcStats = new ConcurrentHashMap<>();

  @Autowired
  public GcMonitor(
      @Value("${nexus.jvm.gc.info.threshold:1000}") final long gcInfoThresholdMs,
      @Value("${nexus.jvm.gc.warning.threshold:5000}") final long gcWarningThresholdMs,
      @Value("${nexus.jvm.gc.critical.threshold:10000}") final long gcCriticalThresholdMs)
  {
    this.gcInfoThresholdMs = gcInfoThresholdMs;
    this.gcWarningThresholdMs = gcWarningThresholdMs;
    this.gcCriticalThresholdMs = gcCriticalThresholdMs;
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    this.memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
  }

  @Override
  public void initialize() {
    // Initialize baseline GC stats
    for (GarbageCollectorMXBean gcBean : gcMXBeans) {
      previousGcStats.put(gcBean.getName(), new GcStats(gcBean.getCollectionCount(), gcBean.getCollectionTime()));
    }
  }

  @Override
  public void check() {
    for (GarbageCollectorMXBean gcBean : gcMXBeans) {
      String gcName = gcBean.getName();
      long currentCount = gcBean.getCollectionCount();
      long currentTime = gcBean.getCollectionTime();

      GcStats previous = previousGcStats.get(gcName);
      if (previous != null) {
        long countDelta = currentCount - previous.count;
        long timeDelta = currentTime - previous.time;

        if (countDelta > 0) {
          long avgDuration = timeDelta / countDelta;
          logGcEvent(gcName, countDelta, timeDelta, avgDuration);
        }
      }

      previousGcStats.put(gcName, new GcStats(currentCount, currentTime));
    }
  }

  private void logGcEvent(final String gcName, final long countDelta, final long timeDelta, final long avgDuration) {
    if (avgDuration >= gcCriticalThresholdMs && log.isErrorEnabled()) {
      logGcEventAtLevel(gcName, countDelta, timeDelta, avgDuration, "error");
    }
    else if (avgDuration >= gcWarningThresholdMs && log.isWarnEnabled()) {
      logGcEventAtLevel(gcName, countDelta, timeDelta, avgDuration, "warn");
    }
    else if (avgDuration >= gcInfoThresholdMs && log.isInfoEnabled()) {
      logGcEventAtLevel(gcName, countDelta, timeDelta, avgDuration, "info");
    }
    else if (log.isDebugEnabled()) {
      logGcEventAtLevel(gcName, countDelta, timeDelta, avgDuration, "debug");
    }
  }

  private void logGcEventAtLevel(
      final String gcName,
      final long countDelta,
      final long timeDelta,
      final long avgDuration,
      final String level)
  {
    String gcType = getGcType(gcName);
    MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
    Map<String, String> poolInfo = getMemoryPoolInfo();

    String message = String.format(
        "%s [jvm monitor] [gc][%s] collector=\"%s\", collections=%d, total_time=%dms, avg_duration=%dms, " +
            "heap_used=%s, heap_committed=%s, heap_max=%s, pools=%s",
        SYSTEM_MARKER, gcType, gcName, countDelta, timeDelta, avgDuration,
        formatBytes(heapUsage.getUsed()), formatBytes(heapUsage.getCommitted()),
        formatBytes(heapUsage.getMax()), poolInfo);

    switch (level) {
      case "error":
        log.error(message);
        break;
      case "warn":
        log.warn(message);
        break;
      case "info":
        log.info(message);
        break;
      case "debug":
        log.debug(message);
        break;
      default:
        // Should never happen
        break;
    }
  }

  @Override
  public void cleanup() {
    previousGcStats.clear();
  }

  private String getGcType(final String gcName) {
    String lowerName = gcName.toLowerCase();
    if (lowerName.contains("old") || lowerName.contains("tenured") || lowerName.contains("cms") ||
        lowerName.contains("mark")) {
      return "old";
    }
    else if (lowerName.contains("young") || lowerName.contains("new") || lowerName.contains("scavenge") ||
        lowerName.contains("copy")) {
      return "young";
    }
    return "unknown";
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

  /**
   * Holds GC statistics for comparison between check intervals.
   */
  private static class GcStats
  {
    final long count;

    final long time;

    GcStats(final long count, final long time) {
      this.count = count;
      this.time = time;
    }
  }
}
