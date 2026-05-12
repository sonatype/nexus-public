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

import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors memory usage and logs warnings when memory thresholds are exceeded.
 */
@Component
public class MemoryMonitor
    implements JvmMonitor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String SYSTEM_MARKER = "*SYSTEM";

  private final int memoryWarningThresholdPercent;

  private final int memoryCriticalThresholdPercent;

  private final MemoryMXBean memoryMXBean;

  private final List<MemoryPoolMXBean> memoryPoolMXBeans;

  @Inject
  public MemoryMonitor(
      @Value("${nexus.jvm.memory.warning.threshold:80}") final int memoryWarningThresholdPercent,
      @Value("${nexus.jvm.memory.critical.threshold:90}") final int memoryCriticalThresholdPercent)
  {
    this.memoryWarningThresholdPercent = memoryWarningThresholdPercent;
    this.memoryCriticalThresholdPercent = memoryCriticalThresholdPercent;
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    this.memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
  }

  @Override
  public void check() {
    MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
    long used = heapUsage.getUsed();
    long max = heapUsage.getMax();

    if (max > 0) {
      int usagePercent = (int) ((used * 100) / max);

      if (usagePercent >= memoryCriticalThresholdPercent && log.isErrorEnabled()) {
        Map<String, String> poolInfo = getMemoryPoolInfo();
        log.error("{} [jvm monitor] [memory] CRITICAL heap usage: {}% ({}/{}), pools={}",
            SYSTEM_MARKER,
            usagePercent,
            formatBytes(used),
            formatBytes(max),
            poolInfo);
      }
      else if (usagePercent >= memoryWarningThresholdPercent && log.isWarnEnabled()) {
        Map<String, String> poolInfo = getMemoryPoolInfo();
        log.warn("{} [jvm monitor] [memory] High heap usage: {}% ({}/{}), pools={}",
            SYSTEM_MARKER,
            usagePercent,
            formatBytes(used),
            formatBytes(max),
            poolInfo);
      }
      else if (log.isDebugEnabled()) {
        log.debug("{} [jvm monitor] [memory] Heap usage: {}% ({}/{})",
            SYSTEM_MARKER,
            usagePercent,
            formatBytes(used),
            formatBytes(max));
      }
    }
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
