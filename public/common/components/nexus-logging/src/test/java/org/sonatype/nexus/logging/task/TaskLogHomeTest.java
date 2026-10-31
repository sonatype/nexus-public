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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

public class TaskLogHomeTest

{
  private Appender<ILoggingEvent> detachedTaskLogFileAppender;

  @After
  public void restoreTaskLogFileAppender() {
    if (detachedTaskLogFileAppender != null) {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      loggerContext.getLogger(ROOT_LOGGER_NAME).addAppender(detachedTaskLogFileAppender);
      detachedTaskLogFileAppender = null;
    }
  }

  @Test
  public void getTaskLogsHome() {
    Path taskLogHome = Paths.get(TaskLogHome.getTaskLogsHome());
    assertTrue(taskLogHome.endsWith(Paths.get("test", "log", "tasks")));

    Path file = taskLogHome.resolve("temp.log");
    assertFalse("temp file was not deleted", Files.exists(file));
  }

  @Test
  public void getReplicationLogsHome() {
    Path taskLogHome = Paths.get(TaskLogHome.getReplicationLogsHome().get());
    assertTrue(taskLogHome.endsWith(Paths.get("test", "log", "replication")));

    Path file = taskLogHome.resolve("temp.log");
    assertFalse("temp file was not deleted", Files.exists(file));
  }

  /**
   * When the 'tasklogfile' appender is absent, getTaskLogsHome() should log the
   * "could not find appender" message at most once, not once per call.
   */
  @Test
  public void missingAppenderIsLoggedOnlyOnce() {
    TaskLogHome.resetMissingAppenderLoggedForTests();

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
    detachedTaskLogFileAppender = rootLogger.getAppender("tasklogfile");
    rootLogger.detachAppender("tasklogfile");

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.setContext(loggerContext);
    listAppender.start();
    ch.qos.logback.classic.Logger taskLogHomeLogger = loggerContext.getLogger(TaskLogHome.class);
    taskLogHomeLogger.addAppender(listAppender);

    try {
      assertNull(TaskLogHome.getTaskLogsHome());
      assertNull(TaskLogHome.getTaskLogsHome());
      assertNull(TaskLogHome.getTaskLogsHome());

      List<ILoggingEvent> missingAppenderEvents = listAppender.list.stream()
          .filter(event -> event.getFormattedMessage().contains("Could not find a Logback SiftingAppender"))
          .toList();
      assertEquals(1, missingAppenderEvents.size());
    }
    finally {
      taskLogHomeLogger.detachAppender(listAppender);
    }
  }

  /**
   * Tests that the task log appender is active after starting a task logger and using a temp appender to determine
   * the log path via {@link TaskLogHome#getTaskLogsHome()}
   *
   * Edge case test stemming from NEXUS-13587 and NEXUS-14052
   *
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-13587">NEXUS-13587</a>
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-14052">NEXUS-14052</a>
   */
  @Test
  public void testAppenderActiveAfterGetTaskLogHome() throws Exception {
    String timeMillis = String.valueOf(System.currentTimeMillis());
    String taskTypeId = "appendTaskTest".concat(timeMillis);
    String infoLogMsg = "info".concat(timeMillis);
    String errorLogMsg = "error".concat(timeMillis);
    Logger logger = LoggerFactory.getLogger(SeparateTaskLogTaskLogger.class);
    SeparateTaskLogTaskLogger taskLogger = new SeparateTaskLogTaskLogger(logger, createTaskInfo(taskTypeId));

    // log a few messages before getTaskLogHome()
    logger.info("logger initialized");
    logger.error("initialize error");

    // explicitly call target method, start task logger and log a few messages
    TaskLogHome.getTaskLogsHome();
    taskLogger.start();
    logger.info(infoLogMsg);
    logger.error(errorLogMsg, new RuntimeException("runtimeException"));
    taskLogger.finish();

    // validate all messages were written to log file
    String logFileContents = getLogFileContents(taskTypeId);
    assertThat(logFileContents, StringContains.containsString("logger initialized"));
    assertThat(logFileContents, StringContains.containsString("initialize error"));
    assertThat(logFileContents, StringContains.containsString(infoLogMsg));
    assertThat(logFileContents, StringContains.containsString(errorLogMsg));
    assertThat(logFileContents, StringContains.containsString("runtimeException"));
  }

  private TaskLogInfo createTaskInfo(final String typeId) {
    return new TaskLogInfo()
    {
      @Override
      public String getId() {
        return "id";
      }

      @Override
      public String getTypeId() {
        return typeId;
      }

      @Override
      public String getName() {
        return "testAppenderTask";
      }

      @Override
      public String getMessage() {
        return "appender test";
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

  /**
   * Get the contents of the log file for the specified task. Assumes a unique task ID, and will return the contents
   * of the first matching file
   */
  private String getLogFileContents(String typeId) throws IOException {
    Path logDirectory = Paths.get(TaskLogHome.getTaskLogsHome());

    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(logDirectory, String.format("%s-*.log", typeId))) {
      for (Path file : dirStream) {
        if (Files.isRegularFile(file)) {
          return new String(Files.readAllBytes(file));
        }
      }
    }

    return "";
  }
}
