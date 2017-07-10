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
import org.slf4j.MDC;
import org.slf4j.Marker;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.logging.task.DefaultTaskLogger.MARK_LINE;
import static org.sonatype.nexus.logging.task.DefaultTaskLogger.PROGRESS_LINE;
import static org.sonatype.nexus.logging.task.TaskLogger.LOGBACK_TASK_DISCRIMINATOR_ID;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.NEXUS_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

public class DefaultTaskLoggerTest
    extends TestSupport
{
  @Mock
  private Logger mockLogger;

  private DefaultTaskLogger underTest;

  @Before
  public void setUp() throws Exception {
    TaskLogInfo taskLogInfo = createTaskLogInfo();

    underTest = new DefaultTaskLogger(mockLogger, taskLogInfo);

    verifyLog(TASK_LOG_ONLY, "Task information:");
    verifyLog(TASK_LOG_ONLY, " ID: {}", taskLogInfo.getId());
    verifyLog(TASK_LOG_ONLY, " Type: {}", taskLogInfo.getTypeId());
    verifyLog(TASK_LOG_ONLY, " Name: {}", taskLogInfo.getName());
    verifyLog(TASK_LOG_ONLY, " Description: {}", taskLogInfo.getMessage());
    verify(mockLogger).debug(TASK_LOG_ONLY, "Task configuration: {}", taskLogInfo.toString());

    // assert the discriminator ID.
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID).matches("typeId-\\d{14}\\b"), is(true));
  }

  @After
  public void tearDown() throws Exception {
    underTest.finish();
  }

  @Test
  public void testProgress() {
    String message = "test message";
    TaskLoggingEvent event = new TaskLoggingEvent(message);
    underTest.progress(event);

    // invoke method normally invoke via thread
    underTest.updateMainLogWithProgress();

    // verify progress logged properly
    verifyLog(NEXUS_LOG_ONLY, format(PROGRESS_LINE, message), (Object[]) null);
  }

  @Test
  public void testProgressMark() {
    // invoke method normally invoke via thread
    underTest.updateMainLogWithProgress();

    // verify progress logged properly
    verifyLog(null, format(PROGRESS_LINE, MARK_LINE), (Object[]) null);
  }

  private TaskLogInfo createTaskLogInfo() {
    return new TaskLogInfo()
    {
      @Override
      public String getId() {
        return "taskid";
      }

      @Override
      public String getTypeId() {
        return "typeId";
      }

      @Override
      public String getName() {
        return "name";
      }

      @Override
      public String getMessage() {
        return "message";
      }

      @Override
      public String toString() {
        return "toString";
      }
    };
  }

  private void verifyLog(Marker m, final String s) {
    verify(mockLogger).info(m, s);
  }

  private void verifyLog(final Marker m, final String s, final Object arg) {
    verify(mockLogger).info(m, s, arg);
  }

  private void verifyLog(final Marker m, final String s, final Object... args) {
    verify(mockLogger).info(m, s, args);
  }

}
