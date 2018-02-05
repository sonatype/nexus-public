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

import org.junit.Test;
import org.slf4j.Logger;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;

public class ProgressLogIntervalHelperTest
{

  @Test
  public void intervalElapsed() throws InterruptedException {
    Logger logger = mock(Logger.class);
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

}