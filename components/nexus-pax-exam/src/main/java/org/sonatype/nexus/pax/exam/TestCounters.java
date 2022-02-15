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
package org.sonatype.nexus.pax.exam;

import java.io.File;

import static java.lang.Math.max;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

/**
 * Tracks test counters for Pax-Exam based ITs.
 *
 * @since 3.14
 */
public class TestCounters
{
  private static final File DATA_DIR = TestBaseDir.resolve("target/it-data");

  private static final File REPORTS_DIR = TestBaseDir.resolve("target/it-reports");

  private TestCounters() {
    // static helper class
  }

  /**
   * Returns the next install directory candidate.
   */
  public static File nextInstallDirectory() {
    return new File(DATA_DIR, Integer.toString(1 + lastInstallCounter()));
  }

  /**
   * Returns the last known install directory.
   */
  public static File lastInstallDirectory() {
    return new File(DATA_DIR, Integer.toString(lastInstallCounter()));
  }

  /**
   * Returns the counter associated with the last known IT installation.
   */
  public static int lastInstallCounter() {
    return max(lastDataCounter(), lastIndexCounter());
  }

  private static int lastDataCounter() {
    return ofNullable(DATA_DIR.list()).map(TestCounters::lastNumberedFile).orElse(0);
  }

  private static int lastIndexCounter() {
    return ofNullable(REPORTS_DIR.list()).map(TestCounters::lastNumberedFile).orElse(0);
  }

  /**
   * Returns the largest number that exactly matches one of the listed filenames.
   */
  private static int lastNumberedFile(final String[] list) {
    return stream(list).mapToInt(TestCounters::tryParseInt).max().orElse(0);
  }

  /**
   * Tries to parse the string as an integer; returns 0 for non-numeric strings.
   */
  private static int tryParseInt(final String value) {
    try {
      return Integer.parseInt(value);
    }
    catch (NumberFormatException e) { // NOSONAR
      return 0;
    }
  }
}
