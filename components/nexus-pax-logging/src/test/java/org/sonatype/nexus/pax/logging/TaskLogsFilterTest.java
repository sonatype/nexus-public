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
package org.sonatype.nexus.pax.logging;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.logging.task.TaskLogger;
import org.sonatype.nexus.logging.task.TaskLoggerHelper;
import org.sonatype.nexus.logging.task.TaskLoggingEvent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.slf4j.Marker;

import static ch.qos.logback.core.spi.FilterReply.DENY;
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.logging.task.TaskLogger.LOGBACK_TASK_DISCRIMINATOR_ID;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.INTERNAL_PROGRESS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.NEXUS_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;

public class TaskLogsFilterTest
    extends TestSupport
{
  private static final String TEST_MESSAGE = "test message";

  private static final Object[] TEST_ARGS = new Object[]{"x"};

  @Mock
  private TaskLogger taskLogger;

  private TaskLogsFilter taskLogsFilter;

  @Before
  public void setUp() {
    taskLogsFilter = new TaskLogsFilter();
  }

  @After
  public void tearDown() {
    MDC.remove(LOGBACK_TASK_DISCRIMINATOR_ID);
    if (TaskLoggerHelper.get() != null) {
      TaskLoggerHelper.finish();
    }
  }

  @Test
  public void testNotATask() {
    // not a task
    MDC.remove(LOGBACK_TASK_DISCRIMINATOR_ID);
    assertThat(taskLogsFilter.decide(eventWithMarkerOf(null)), equalTo(DENY));
  }

  @Test
  public void testIsANexusLog() {
    startTask();
    assertThat(taskLogsFilter.decide(eventWithMarkerOf(NEXUS_LOG_ONLY)), equalTo(DENY));
  }

  @Test
  public void testIsInternalProgress() {
    startTask();
    assertThat(taskLogsFilter.decide(eventWithMarkerOf(INTERNAL_PROGRESS)), equalTo(DENY));
  }

  @Test
  public void testIsProgress() {
    startTask();
    assertThat(taskLogsFilter.decide(eventWithMarkerOf(PROGRESS)), equalTo(NEUTRAL));
    assertNotNull(TaskLoggerHelper.get());

    ArgumentCaptor<TaskLoggingEvent> argumentCaptor = ArgumentCaptor.forClass(TaskLoggingEvent.class);
    verify(taskLogger).progress(argumentCaptor.capture());
    TaskLoggingEvent taskLoggingEvent = argumentCaptor.getValue();
    assertNotNull(taskLoggingEvent);
    assertThat(taskLoggingEvent.getMessage(), equalTo(TEST_MESSAGE));
    assertThat(taskLoggingEvent.getArgumentArray(), equalTo(TEST_ARGS));
  }

  @Test
  public void testNotProgress() {
    startTask();
    assertThat(taskLogsFilter.decide(eventWithMarkerOf(null)), equalTo(NEUTRAL));
    assertNotNull(TaskLoggerHelper.get());
  }

  private void startTask() {
    // default is a task
    MDC.put(LOGBACK_TASK_DISCRIMINATOR_ID, "taskId");

    TaskLoggerHelper.start(taskLogger);
  }

  private ILoggingEvent eventWithMarkerOf(final Marker marker) {
    LoggingEvent event = new LoggingEvent();
    event.setMessage(TEST_MESSAGE);
    event.addMarker(marker);
    event.setArgumentArray(TEST_ARGS);
    return event;
  }
}
