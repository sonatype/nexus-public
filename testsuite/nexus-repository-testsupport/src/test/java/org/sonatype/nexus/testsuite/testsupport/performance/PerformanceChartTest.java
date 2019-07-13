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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.testsuite.testsupport.performance.PerformanceData.PerformanceRunResult;
import org.sonatype.nexus.testsuite.testsupport.performance.PerformanceData.PerformanceTestSeries;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Tests {@link PerformanceChart}
 */
public class PerformanceChartTest
    extends TestSupport
{
  @Rule
  public TestName name = new TestName();

  private File chartFile;

  @Before
  public void setJsonFileLocation() throws IOException {
    final File chartDir = util.resolveFile("target/test-tmp/" + getClass().getSimpleName());
    chartDir.mkdirs();
    chartFile = new File(chartDir, name.getMethodName() + ".html");
  }

  @Test
  public void writeChartSmokeTest() throws Exception {
    final PerformanceData perfData =  new PerformanceData();
    final PerformanceTestSeries data = perfData.findTestResult("sample");
    data.addResults(1, new PerformanceRunResult(1, 0, 60, true));
    data.addResults(2, new PerformanceRunResult(10, 0, 60, true));
    data.addResults(3, new PerformanceRunResult(100, 0, 60, true));

    PerformanceChart.writePerformanceReport(perfData, chartFile);
  }
}
