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
package org.sonatype.nexus.testsuite.testsupport.performance;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.testsupport.TestIndex;
import org.sonatype.nexus.common.io.DirectoryHelper;
import org.sonatype.nexus.testsuite.testsupport.performance.PerformanceData.PerformanceRunResult;
import org.sonatype.nexus.testsuite.testsupport.performance.PerformanceData.PerformanceTestSeries;

import com.google.common.util.concurrent.Runnables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Conducts a performance test with a variable number of clients, contributing a data series to the format performance
 * chart.
 *
 * @since 3.0
 */
public class EscalatingClientLoadExecutor
    extends ComponentSupport
{
  /**
   * The numbers of threads used to conduct the performance tests.
   *
   * TODO: Consider a format-specific cap, if certain formats (e.g. docker?) are simply unable to handle the client
   * loads at which we need to measure more performant formats.
   */
  private static final int[] THREAD_COUNTS = new int[]{1, 10, 25, 50};

  /**
   * How long should the run be for each data point?
   */
  public static final int DURATION_SECONDS = 60;

  private final TestIndex testIndex;

  private final Runnable before;

  /**
   * Accepts an optional callable to be invoked between loads with different numbers of clients.
   */
  public EscalatingClientLoadExecutor(final TestIndex testIndex, @Nullable final Runnable before) {
    this.testIndex = checkNotNull(testIndex);
    this.before = before != null ? before : Runnables.doNothing();
  }

  public void calculateAndGraphPerformance(final String dataSeriesName,
                                           final List<Callable<?>> tasks,
                                           final File reportDir)
      throws Exception
  {
    checkNotNull(reportDir);
    if (!reportDir.exists()) {
      DirectoryHelper.mkdir(reportDir);
    }

    final File dataFile = new File(reportDir, "performance-data.json");

    final PerformanceData results = PerformanceDataIO.loadTestData(dataFile);

    PerformanceTestSeries testResults = results.findTestResult(dataSeriesName);

    // Now carry out the tests, with an escalating number of threads
    for (int clientThreads : THREAD_COUNTS) {

      // Do whatever preparatory step the test requires
      before.run();

      final LoadExecutor loadExec = new LoadExecutor(tasks, clientThreads, DURATION_SECONDS);

      boolean exceptionThrown = false;
      try {
        loadExec.callTasks();
      }
      catch (Exception e) {
        log.warn("Performance run for {} with {} threads aborted with exception", dataSeriesName, clientThreads, e);
        exceptionThrown = true;
      }
      catch (AssertionError e) {
        log.warn("Performance run for {} with {} threads failed assertion", dataSeriesName, clientThreads, e);
        exceptionThrown = true;
      }

      // Record the results
      testResults.addResults(clientThreads, new PerformanceRunResult(
          loadExec.getRequestsProcessed(),
          loadExec.getRequestsStarted() - loadExec.getRequestsProcessed(),
          DURATION_SECONDS,
          exceptionThrown));

      PerformanceDataIO.saveTestData(results, dataFile);
      testIndex.recordLink("performance-data", dataFile);
    }

    final File htmlReport = new File(reportDir, "performance-report.html");
    PerformanceChart.writePerformanceReport(results, htmlReport);

    testIndex.recordLink("performance-report", htmlReport);
  }
}
