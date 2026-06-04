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
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryMonitorTest
{
  private static final long MAX_HEAP = 1_000_000_000L; // 1gb

  @Mock
  private MemoryMXBean memoryMXBean;

  @Mock
  private MemoryPoolMXBean heapPool;

  private ListAppender<ILoggingEvent> logCapture;

  private Logger monitorLogger;

  @BeforeEach
  void setUp() {
    lenient().when(heapPool.getType()).thenReturn(MemoryType.HEAP);
    lenient().when(heapPool.getName()).thenReturn("G1 Old Gen");
    lenient().when(heapPool.getUsage()).thenReturn(new MemoryUsage(0, 100_000_000L, 100_000_000L, MAX_HEAP));

    monitorLogger = (Logger) LoggerFactory.getLogger(MemoryMonitor.class);
    logCapture = new ListAppender<>();
    logCapture.start();
    monitorLogger.addAppender(logCapture);
  }

  @AfterEach
  void tearDown() {
    monitorLogger.detachAppender(logCapture);
  }

  @Test
  void belowThreshold_noLog() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(70);

    for (int i = 0; i < 5; i++) {
      underTest.check();
    }

    assertNoWarnOrError();
  }

  @Test
  void singleSpikeAboveWarning_noLog() {
    MemoryMonitor underTest = monitor(80, 90, 3);

    setHeapPercent(85);
    underTest.check();
    setHeapPercent(70);
    underTest.check();

    assertNoWarnOrError();
  }

  @Test
  void twoConsecutiveAboveWarning_noLog() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(85);

    underTest.check();
    underTest.check();

    assertNoWarnOrError();
  }

  @Test
  void threeConsecutiveAboveWarning_logsWarn() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(85);

    underTest.check();
    underTest.check();
    underTest.check();

    assertThat(warnCount()).isEqualTo(1);
    assertThat(errorCount()).isZero();
  }

  @Test
  void fourConsecutiveAboveWarning_logsWarnOnce() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(85);

    underTest.check();
    underTest.check();
    underTest.check();
    underTest.check();

    assertThat(warnCount()).isEqualTo(1);
  }

  @Test
  void threeConsecutiveAboveCritical_logsError() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(95);

    underTest.check();
    underTest.check();
    underTest.check();

    assertThat(errorCount()).isEqualTo(1);
    assertThat(warnCount()).isZero();
  }

  @Test
  void warningEscalatesToCritical_logsWarnThenError() {
    MemoryMonitor underTest = monitor(80, 90, 3);

    setHeapPercent(85);
    underTest.check();
    underTest.check();
    underTest.check(); // WARN logged here

    setHeapPercent(95);
    underTest.check(); // ERROR logged here

    assertThat(warnCount()).isEqualTo(1);
    assertThat(errorCount()).isEqualTo(1);
  }

  @Test
  void recoveryAfterSustainedWarning_logsInfo() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(85);

    underTest.check();
    underTest.check();
    underTest.check(); // WARN

    setHeapPercent(70);
    underTest.check(); // INFO recovery

    assertThat(infoCount()).isEqualTo(1);
    assertThat(logCapture.list).anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("recovered"));
  }

  @Test
  void noRecoveryLogIfNeverWarned() {
    MemoryMonitor underTest = monitor(80, 90, 3);

    setHeapPercent(85);
    underTest.check(); // only 1 sample — not yet sustained

    setHeapPercent(70);
    underTest.check(); // should NOT log INFO

    assertThat(infoCount()).isZero();
  }

  @Test
  void resetAfterRecovery_warnsAgainIfSustained() {
    MemoryMonitor underTest = monitor(80, 90, 3);

    setHeapPercent(85);
    underTest.check();
    underTest.check();
    underTest.check(); // first WARN

    setHeapPercent(70);
    underTest.check(); // recovery

    setHeapPercent(85);
    underTest.check();
    underTest.check();
    underTest.check(); // second WARN

    assertThat(warnCount()).isEqualTo(2);
  }

  @Test
  void sustainedMessageIncludesElapsedTime() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(85);

    underTest.check();
    underTest.check();
    underTest.check();

    assertThat(logCapture.list)
        .anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("sustained="));
  }

  @Test
  void customSustainedSamples_oneRequiredLogsImmediately() {
    MemoryMonitor underTest = monitor(80, 90, 1);
    setHeapPercent(85);

    underTest.check();

    assertThat(warnCount()).isEqualTo(1);
  }

  @Test
  void warningDoesNotRepeatAfterFirstLog() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(85);

    for (int i = 0; i < 20; i++) {
      underTest.check();
    }

    assertThat(warnCount()).isEqualTo(1);
  }

  @Test
  void twoWarnSamplesThenOneCritical_logsError() {
    // Debounce counts consecutive samples above ANY threshold; 2 warn + 1 critical = 3 total,
    // which meets the sustained requirement and correctly triggers ERROR.
    MemoryMonitor underTest = monitor(80, 90, 3);

    setHeapPercent(85);
    underTest.check();
    underTest.check();

    setHeapPercent(95);
    underTest.check();

    assertThat(errorCount()).isEqualTo(1);
    assertThat(warnCount()).isZero();
  }

  @Test
  void maxHeapZero_noException() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    when(memoryMXBean.getHeapMemoryUsage()).thenReturn(new MemoryUsage(0, 0, 0, 0));

    underTest.check();

    assertNoWarnOrError();
  }

  @Test
  void initializeResetsDebounceState() {
    MemoryMonitor underTest = monitor(80, 90, 3);
    setHeapPercent(85);

    underTest.check();
    underTest.check(); // 2 samples in, not yet sustained

    underTest.initialize(); // reset — counter goes back to 0

    underTest.check();
    underTest.check(); // only 2 samples since reset — still no WARN

    assertNoWarnOrError();
  }

  // --- helpers ---

  private MemoryMonitor monitor(final int warn, final int critical, final int sustained) {
    try (MockedStatic<ManagementFactory> mf = Mockito.mockStatic(ManagementFactory.class)) {
      mf.when(ManagementFactory::getMemoryMXBean).thenReturn(memoryMXBean);
      mf.when(ManagementFactory::getMemoryPoolMXBeans).thenReturn(List.of(heapPool));
      return new MemoryMonitor(warn, critical, sustained);
    }
  }

  private void setHeapPercent(final int percent) {
    long used = MAX_HEAP * percent / 100;
    when(memoryMXBean.getHeapMemoryUsage()).thenReturn(new MemoryUsage(0, used, used, MAX_HEAP));
  }

  private long warnCount() {
    return logCapture.list.stream().filter(e -> e.getLevel() == Level.WARN).count();
  }

  private long errorCount() {
    return logCapture.list.stream().filter(e -> e.getLevel() == Level.ERROR).count();
  }

  private long infoCount() {
    return logCapture.list.stream().filter(e -> e.getLevel() == Level.INFO).count();
  }

  private void assertNoWarnOrError() {
    assertThat(warnCount()).isZero();
    assertThat(errorCount()).isZero();
  }
}
