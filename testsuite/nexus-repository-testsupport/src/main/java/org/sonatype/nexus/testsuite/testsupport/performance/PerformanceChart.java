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
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sonatype.nexus.testsuite.testsupport.performance.PerformanceData.PerformanceRunResult;
import org.sonatype.nexus.testsuite.testsupport.performance.PerformanceData.PerformanceTestSeries;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import static com.google.common.base.Preconditions.checkState;

/**
 * Utilities for writing performance results to an HTML chart.
 *
 * @since 3.0
 */
public class PerformanceChart
{
  private static final Logger log = LoggerFactory.getLogger(PerformanceChart.class);

  private PerformanceChart() {
    // empty
  }

  public static void writePerformanceReport(final PerformanceData results, final File outputFile)
      throws IOException
  {
    String chartData = buildChartData(results);

    final String reportTemplate = loadReportTemplate();

    String reportHtml = reportTemplate.replace("/*ROW-DATA*/", chartData);
    reportHtml = reportHtml.replace("<!-- SYSTEM-INFO -->", buildSystemInfo());

    log.info("Writing performance chart to {}", outputFile);
    Files.asCharSink(outputFile, StandardCharsets.UTF_8).write(reportHtml);
  }

  private static String loadReportTemplate() throws IOException {
    final URL reportTemplateResource = PerformanceChart.class.getResource("performanceReport.html");
    checkState(reportTemplateResource != null);
    return Resources.toString(reportTemplateResource, StandardCharsets.UTF_8);
  }

  /**
   * Builds a Javascript array to be inserted into the Google Charts API definition.
   */
  private static String buildChartData(final PerformanceData results) {
    final DecimalFormat format = new DecimalFormat("#.00");

    StringBuilder s = new StringBuilder();

    SortedSet<String> testNames = new TreeSet<>(results.getTests().keySet());

    // Create the header row
    s.append("['# of Client Threads'");
    for (String testName : testNames) {
      s.append(",'").append(testName).append("'");
    }
    s.append("]");

    for (int threadCount : results.getThreadCounts()) {
      s.append(",[").append(threadCount);

      for (String testName : testNames) {
        final PerformanceTestSeries series = results.findTestResult(testName);

        final PerformanceRunResult test = series.getResultsByThreadCount().get(threadCount);
        final int requestsCompleted = test.getRequestsCompleted();

        final int duration = test.getTestDurationSeconds();

        final double requestPerSecond = ((double) requestsCompleted) / duration;

        s.append(",").append(format.format(requestPerSecond));
      }

      s.append("]");
    }
    return s.toString();
  }

  private static String buildSystemInfo() {

    StringBuilder systemSummary = new StringBuilder();

    final SystemInfo systemInfo = new SystemInfo();
    final HardwareAbstractionLayer hardware = systemInfo.getHardware();

    final CentralProcessor processor = hardware.getProcessor();

    systemSummary.append("Processor: ").append(processor.getProcessorIdentifier()).append("\n");

    final GlobalMemory memory = hardware.getMemory();
    systemSummary.append(String.format("Memory: %,d Mb%n", memory.getTotal() / (1024 * 1024)));

    final OperatingSystem os = systemInfo.getOperatingSystem();
    systemSummary.append(String.format("OS: %s %s %s%n", os.getManufacturer(), os.getFamily(), os.getVersionInfo()));

    return systemSummary.toString();
  }
}
