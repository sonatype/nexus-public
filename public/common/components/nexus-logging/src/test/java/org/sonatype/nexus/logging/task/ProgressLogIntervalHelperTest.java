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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.test.util.Whitebox;

import com.google.common.base.Stopwatch;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;

@RunWith(JUnitParamsRunner.class)
public class ProgressLogIntervalHelperTest
{
  Logger logger;

  @Before
  public void setUp() {
    logger = mock(Logger.class);
  }

  @Test
  public void intervalElapsed() throws InterruptedException {
    String arg = "arg";
    Object[] argArray = {arg};

    ProgressLogIntervalHelper underTest = new ProgressLogIntervalHelper(logger, 1);

    // on immediate call interval will not have elapased so the logger should not be hit
    underTest.info("Test 1", arg);
    verify(logger, never()).info(PROGRESS, "Test 1", argArray);

    // sleep for 1 second
    sleep(1100);

    // invoke after interval elapsed and now logger should have been hit
    underTest.info("Test 2", arg);
    verify(logger).info(PROGRESS, "Test 2", argArray);
  }

  @Test
  @Parameters({
      "0, 0s",
      "1, 1s",
      "60, 1m 0s",
      "61, 1m 1s",
      "3599, 59m 59s",
      "3600, 1h 0m 0s",
      "3601, 1h 0m 1s",
      "86400, 1d 0h 0m 0s",
      "1296000, 15d 0h 0m 0s",
      "2161045, 25d 0h 17m 25s",
  })
  public void getElapsedTest(long seconds, String expected) {
    Stopwatch elapsedStopwatch = mock(Stopwatch.class);
    when(elapsedStopwatch.elapsed()).thenReturn(Duration.ofSeconds(seconds));

    ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(logger, 1);
    Whitebox.setInternalState(progressLogger, "elapsed", elapsedStopwatch);
    assertEquals(expected, progressLogger.getElapsed());
  }

  /**
   * Test that hasIntervalElapsed is thread-safe when called concurrently. This test verifies the fix for NEXUS-51191
   * where concurrent calls to hasIntervalElapsed could cause "This stopwatch is already running" exception.
   */
  @Test
  public void hasIntervalElapsed_isThreadSafe() throws Exception {
    Stopwatch progressStopwatch = spy(Stopwatch.createStarted());

    ProgressLogIntervalHelper underTest = new ProgressLogIntervalHelper(logger, 1);
    Whitebox.setInternalState(underTest, "progress", progressStopwatch);

    doReturn(100L).when(progressStopwatch).elapsed(TimeUnit.SECONDS);

    int threadCount = 10;
    int iterationsPerThread = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    try {
      CountDownLatch startLatch = new CountDownLatch(1);
      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger errorCount = new AtomicInteger(0);
      List<Future<?>> futures = new ArrayList<>();

      // Submit tasks that will all start at the same time
      for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(() -> {
          try {
            startLatch.await(); // Wait for all threads to be ready
            for (int j = 0; j < iterationsPerThread; j++) {
              underTest.info("message");
              successCount.incrementAndGet();
            }
          }
          catch (IllegalStateException e) {
            // This is the error we're testing for - "This stopwatch is already running"
            errorCount.incrementAndGet();
          }
          catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }));
      }

      // Start all threads simultaneously
      startLatch.countDown();

      // Wait for all tasks to complete
      for (Future<?> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }

      executor.shutdown();
      assertTrue("Executor should terminate", executor.awaitTermination(10, TimeUnit.SECONDS));

      // Verify no errors occurred
      assertEquals("Should have no IllegalStateException errors", 0, errorCount.get());
      assertEquals("All iterations should complete successfully",
          threadCount * iterationsPerThread, successCount.get());
    }
    finally {
      executor.shutdownNow();
    }
  }
}
