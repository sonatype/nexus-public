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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads and saves performance data as JSON.
 */
public class PerformanceDataIO
{
  private PerformanceDataIO() {
    // empty
  }

  /**
   * Loads performance test data from the specified file if it exists, otherwise returns an empty data set.
   */
  public static PerformanceData loadTestData(final File datafile) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    if (datafile.exists()) {
      return mapper.readValue(datafile, PerformanceData.class);
    }
    return new PerformanceData();
  }

  /**
   * Overwrites the provided datafile with json output representing the suite results.
   */
  public static void saveTestData(final PerformanceData results, final File datafile) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(datafile, results);
  }
}
