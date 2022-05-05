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

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.Marker;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.logging.task.ProgressTaskLogger.PROGRESS_LINE;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.INTERNAL_PROGRESS;

public class ProgressTaskLoggerTest
    extends TestSupport
{
  @Mock
  private Logger mockLogger;

  private ProgressTaskLogger underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ProgressTaskLogger(mockLogger);
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    underTest.finish();
  }

  @Test
  public void testProgress() {
    String message = "test message";
    TaskLoggingEvent event = new TaskLoggingEvent(mockLogger, message);
    underTest.progress(event);

    // invoke method normally invoke via thread
    underTest.logProgress();

    // verify progress logged properly
    verifyLog(INTERNAL_PROGRESS, format(PROGRESS_LINE, message), (Object[]) null);
  }

  @Test
  public void testNoProgressLogged() {
    // invoke method normally invoke via thread
    underTest.logProgress();

    // verify no progress was logged as no progress message was set
    verify(mockLogger, never()).info(any(Marker.class), anyString(), anyCollection());
  }

  private void verifyLog(final Marker m, final String s, final Object... args) {
    verify(mockLogger).info(m, s, args);
  }
}
