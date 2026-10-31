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

import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.sift.AppenderTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.logging.task.TaskLogger.LOGBACK_TASK_DISCRIMINATOR_ID;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_ONLY_MDC;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_WITH_PROGRESS_MDC;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;
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

    verifyLog("Task information:");
    verifyLog(" ID: {}", taskLogInfo.getId());
    verifyLog(" Type: {}", taskLogInfo.getTypeId());
    verifyLog(" Name: {}", taskLogInfo.getName());
    verifyLog(" Description: {}", taskLogInfo.getMessage());
    verifyLog("Task configuration: {}", taskLogInfo);

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

  @Test
  public void testFinishClearsAllMDCValues() {
    MDC.put(TASK_LOG_ONLY_MDC, "something");
    MDC.put(TASK_LOG_WITH_PROGRESS_MDC, "something-else");
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), notNullValue());
    assertThat(MDC.get(TASK_LOG_WITH_PROGRESS_MDC), notNullValue());
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), notNullValue());
    underTest.finish();
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), nullValue());
    assertThat(MDC.get(TASK_LOG_WITH_PROGRESS_MDC), nullValue());
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), nullValue());
  }

  /**
   * Verifies that finish() stops the nested FileAppender to prevent file handle leaks.
   * Uses mocked Logback components to verify the stop() method is called.
   *
   * @see <a href="https://sonatype.atlassian.net/browse/NEXUS-29099">NEXUS-29099</a>
   */
  @Test
  public void testFinishStopsNestedFileAppender() {
    // Create mocks for the Logback components
    SiftingAppender mockSiftingAppender = mock(SiftingAppender.class);
    @SuppressWarnings("unchecked")
    AppenderTracker<ILoggingEvent> mockAppenderTracker = mock(AppenderTracker.class);
    @SuppressWarnings("unchecked")
    FileAppender<ILoggingEvent> mockFileAppender = mock(FileAppender.class);

    // Configure the mock SiftingAppender to return our mock tracker
    when(mockSiftingAppender.getAppenderTracker()).thenReturn(mockAppenderTracker);

    // Configure the mock tracker to return our mock FileAppender for any task identifier
    when(mockAppenderTracker.find(anyString())).thenReturn(mockFileAppender);

    // Create a testable version that uses our mock SiftingAppender
    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return mockSiftingAppender;
      }
    };

    // Call start to initialize the logger properly
    testableLogger.start();
    testableLogger.finish();

    // Verify that stop() was called on the nested FileAppender
    verify(mockFileAppender).stop();

    // Verify MDC was cleared
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), nullValue());
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), nullValue());
  }

  /**
   * Verifies that finish() handles the case where the nested appender is not a FileAppender.
   *
   * @see <a href="https://sonatype.atlassian.net/browse/NEXUS-29099">NEXUS-29099</a>
   */
  @Test
  public void testFinishHandlesNonFileAppender() {
    // Create mocks
    SiftingAppender mockSiftingAppender = mock(SiftingAppender.class);
    @SuppressWarnings("unchecked")
    AppenderTracker<ILoggingEvent> mockAppenderTracker = mock(AppenderTracker.class);

    // Create a non-FileAppender mock
    @SuppressWarnings("unchecked")
    Appender<ILoggingEvent> mockNonFileAppender = mock(Appender.class);

    when(mockSiftingAppender.getAppenderTracker()).thenReturn(mockAppenderTracker);
    when(mockAppenderTracker.find(anyString())).thenReturn(mockNonFileAppender);

    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return mockSiftingAppender;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    // Verify MDC was still cleared
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), nullValue());
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), nullValue());
  }

  /**
   * Verifies that finish() handles the case where no nested appender is found.
   *
   * @see <a href="https://sonatype.atlassian.net/browse/NEXUS-29099">NEXUS-29099</a>
   */
  @Test
  public void testFinishHandlesNoNestedAppender() {
    // Create mocks
    SiftingAppender mockSiftingAppender = mock(SiftingAppender.class);
    @SuppressWarnings("unchecked")
    AppenderTracker<ILoggingEvent> mockAppenderTracker = mock(AppenderTracker.class);

    when(mockSiftingAppender.getAppenderTracker()).thenReturn(mockAppenderTracker);
    when(mockAppenderTracker.find(anyString())).thenReturn(null);

    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return mockSiftingAppender;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    // Verify MDC was still cleared
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), nullValue());
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), nullValue());
  }

  /**
   * Verifies that finish() handles the case where no SiftingAppender is configured.
   */
  @Test
  public void testFinishHandlesNoSiftingAppender() {
    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return null;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    // Verify MDC was still cleared
    assertThat(MDC.get(LOGBACK_TASK_DISCRIMINATOR_ID), nullValue());
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), nullValue());
  }

  /**
   * Verifies that flush() logs the last progress event when one exists.
   */
  @Test
  public void testFlushWithProgressEvent() {
    Logger progressLogger = mock(Logger.class);
    String message = "Test progress message";
    Object[] args = new Object[]{"arg1", "arg2"};

    TaskLoggingEvent progressEvent = new TaskLoggingEvent(progressLogger, message, args);

    underTest.progress(progressEvent);
    underTest.flush();

    // Verify the progress event was logged by SeparateTaskLogTaskLogger.flush()
    verify(progressLogger).info(PROGRESS, message, args);
  }

  /**
   * Verifies that flush() uses the default logger when logger is set to mocked logger.
   */
  @Test
  public void testFlushWithProgressEventWithMockLogger() {
    String message = "Test progress message";
    Object[] args = new Object[]{"arg1"};

    // Use the mockLogger which is already set up
    TaskLoggingEvent progressEvent = new TaskLoggingEvent(mockLogger, message, args);

    underTest.progress(progressEvent);
    underTest.flush();

    // Verify the progress event was logged using the provided logger
    verify(mockLogger).info(PROGRESS, message, args);
  }

  /**
   * Verifies that finish() logs "Task complete" to task log only.
   */
  @Test
  public void testFinishLogsTaskComplete() {
    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return null;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    verify(mockLogger).info(TASK_LOG_ONLY, "Task complete");
  }

  /**
   * Verifies that finish() calls stopNestedFileAppender even when MDC values are already cleared.
   */
  @Test
  public void testFinishCallsStopNestedFileAppender() {
    SiftingAppender mockSiftingAppender = mock(SiftingAppender.class);
    @SuppressWarnings("unchecked")
    AppenderTracker<ILoggingEvent> mockAppenderTracker = mock(AppenderTracker.class);

    when(mockSiftingAppender.getAppenderTracker()).thenReturn(mockAppenderTracker);
    when(mockAppenderTracker.find(anyString())).thenReturn(null);

    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return mockSiftingAppender;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    // Verify that getAppenderTracker was called, indicating stopNestedFileAppender was invoked
    verify(mockSiftingAppender).getAppenderTracker();
  }

  /**
   * Verifies that stopNestedFileAppender logs debug message when appender is not a FileAppender.
   */
  @Test
  public void testStopNestedFileAppenderLogsDebugForNonFileAppender() {
    SiftingAppender mockSiftingAppender = mock(SiftingAppender.class);
    @SuppressWarnings("unchecked")
    AppenderTracker<ILoggingEvent> mockAppenderTracker = mock(AppenderTracker.class);

    // Create a non-FileAppender mock
    @SuppressWarnings("unchecked")
    Appender<ILoggingEvent> mockNonFileAppender = mock(Appender.class);

    when(mockSiftingAppender.getAppenderTracker()).thenReturn(mockAppenderTracker);
    when(mockAppenderTracker.find(anyString())).thenReturn(mockNonFileAppender);

    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return mockSiftingAppender;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    // Verify debug logging was called for non-FileAppender case
    verify(mockLogger).debug(contains("Nested appender for task"), anyString(), anyString());
  }

  /**
   * Verifies that stopNestedFileAppender logs debug message when no appender is found.
   */
  @Test
  public void testStopNestedFileAppenderLogsDebugForNoAppender() {
    SiftingAppender mockSiftingAppender = mock(SiftingAppender.class);
    @SuppressWarnings("unchecked")
    AppenderTracker<ILoggingEvent> mockAppenderTracker = mock(AppenderTracker.class);

    when(mockSiftingAppender.getAppenderTracker()).thenReturn(mockAppenderTracker);
    when(mockAppenderTracker.find(anyString())).thenReturn(null);

    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return mockSiftingAppender;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    // Verify debug logging was called for no appender case
    verify(mockLogger).debug(contains("No nested appender found"), anyString());
  }

  /**
   * Verifies that stopNestedFileAppender logs debug message when FileAppender is stopped.
   */
  @Test
  public void testStopNestedFileAppenderLogsDebugWhenStopped() {
    SiftingAppender mockSiftingAppender = mock(SiftingAppender.class);
    @SuppressWarnings("unchecked")
    AppenderTracker<ILoggingEvent> mockAppenderTracker = mock(AppenderTracker.class);
    @SuppressWarnings("unchecked")
    FileAppender<ILoggingEvent> mockFileAppender = mock(FileAppender.class);

    when(mockSiftingAppender.getAppenderTracker()).thenReturn(mockAppenderTracker);
    when(mockAppenderTracker.find(anyString())).thenReturn(mockFileAppender);

    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo())
    {
      @Override
      SiftingAppender getTaskLogFileSiftingAppender() {
        return mockSiftingAppender;
      }
    };

    testableLogger.start();
    testableLogger.finish();

    // Verify stop was called
    verify(mockFileAppender).stop();

    // Verify debug logging was called for FileAppender stopped case
    verify(mockLogger).debug(contains("Stopped nested FileAppender"), anyString());
  }

  /**
   * Verifies that getTaskLogFileSiftingAppender returns a SiftingAppender when configured.
   * Note: In the test environment, there is actually a SiftingAppender configured.
   */
  @Test
  public void testGetTaskLogFileSiftingAppenderWithActualConfiguration() {
    SeparateTaskLogTaskLogger testableLogger = new SeparateTaskLogTaskLogger(mockLogger, createTaskLogInfo());
    testableLogger.start();

    // In the test environment with actual Logback configuration, this should return a SiftingAppender
    SiftingAppender appender = testableLogger.getTaskLogFileSiftingAppender();
    assertThat(appender, notNullValue());
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

  private void verifyLog(final String s) {
    verify(mockLogger).info(s);
  }

  private void verifyLog(final String s, final Object arg) {
    verify(mockLogger).info(s, arg);
  }
}
