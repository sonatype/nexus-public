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
package org.sonatype.nexus.test.util;

public class StopWatch
{
  private static final int MS_PER_SEC = 1000;

  private static final int SEC_PER_MIN = 60;

  private long start;

  public StopWatch() {
    reset();
  }

  public void reset() {
    start = System.currentTimeMillis();
  }

  public long elapsedTime() {
    return System.currentTimeMillis() - start;
  }

  public String formattedTime() {
    return formatTime(elapsedTime());
  }

  public static String formatTime(long ms) {
    long secs = ms / MS_PER_SEC;

    long min = secs / SEC_PER_MIN;

    secs = secs % SEC_PER_MIN;

    String msg = "";

    if (min > 1) {
      msg = min + " minutes ";
    }
    else if (min == 1) {
      msg = "1 minute ";
    }

    if (secs > 1) {
      msg += secs + " seconds";
    }
    else if (secs == 1) {
      msg += "1 second";
    }
    else if (min == 0) {
      msg += "< 1 second";
    }
    return msg;
  }

  @Override
  public String toString() {
    return formattedTime();
  }

}
