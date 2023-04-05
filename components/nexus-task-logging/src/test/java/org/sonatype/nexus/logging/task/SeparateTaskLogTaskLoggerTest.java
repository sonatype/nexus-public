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

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.logging.task.TaskLogger.LOGBACK_TASK_DISCRIMINATOR_ID;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_ONLY_MDC;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

public class SeparateTaskLogTaskLoggerTest
    extends ProgressTaskLoggerTest
{

  @Mock
  private Logger mockLogger;

  private SeparateTaskLogTaskLogger underTest;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    TaskLogInfo taskLogInfo = createTaskLogInfo();

    underTest = new SeparateTaskLogTaskLogger(mockLogger, taskLogInfo);
    underTest.start();

    verifyLog(TASK_LOG_ONLY, "Task information:");
    verifyLog(TASK_LOG_ONLY, " ID: {}", taskLogInfo.getId());
    verifyLog(TASK_LOG_ONLY, " Type: {}", taskLogInfo.getTypeId());
    verifyLog(TASK_LOG_ONLY, " Name: {}", taskLogInfo.getName());
    verifyLog(TASK_LOG_ONLY, " Description: {}", taskLogInfo.getMessage());
    verify(mockLogger).debug(TASK_LOG_ONLY, "Task configuration: {}", taskLogInfo);

    // assert the discriminator ID.
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID).matches("typeId-\\d{17}\\b"), is(true));
  }

  @After
  public void tearDown() throws Exception {
    underTest.finish();
  }

  @Test
  public void testFinishClearsMDCValues() {
    MDC.put(TASK_LOG_ONLY_MDC, "something");
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), notNullValue());
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), notNullValue());
    underTest.finish();
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), nullValue());
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), nullValue());
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

      @Nullable
      @Override
      public String getString(final String key) {
        return null;
      }

      @Override
      public boolean getBoolean(final String key, final boolean defaultValue) {
        return false;
      }

      @Override
      public int getInteger(final String key, final int defaultValue) {
        return 0;
      }
    };
  }

  private void verifyLog(Marker m, final String s) {
    verify(mockLogger).info(m, s);
  }

  private void verifyLog(final Marker m, final String s, final Object arg) {
    verify(mockLogger).info(m, s, arg);
  }
}
