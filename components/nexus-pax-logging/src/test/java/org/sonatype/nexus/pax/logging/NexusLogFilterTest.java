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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static ch.qos.logback.core.spi.FilterReply.DENY;
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_ONLY_MDC;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_WITH_PROGRESS_MDC;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.AUDIT_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.INTERNAL_PROGRESS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

public class NexusLogFilterTest
{
  private NexusLogFilter excludeProgressLogsFilter;

  private ILoggingEvent event;

  @Before
  public void setup() {
    excludeProgressLogsFilter = new NexusLogFilter();
    event = new LoggingEvent();
  }

  @After
  public void tearDown() {
    MDC.remove(TASK_LOG_ONLY_MDC);
    MDC.remove(TASK_LOG_WITH_PROGRESS_MDC);
  }

  @Test
  public void testNothingInMDC() {
    assertThat(excludeProgressLogsFilter.decide(event), equalTo(NEUTRAL));
  }

  @Test
  public void testOtherMarkerInMDC() {
    Marker fooMarker = MarkerFactory.getMarker("foo");
    assertThat(excludeProgressLogsFilter.decide(eventWithMarkerOf(fooMarker)), equalTo(NEUTRAL));
  }

  @Test
  public void testTaskLogWithProgressMarkerInMDC() {
    MDC.put(TASK_LOG_WITH_PROGRESS_MDC, "true");

    // as the method also returns NEUTRAL by default, we also test that TASK_LOG_ONLY_MDC being set has no affect
    MDC.put(TASK_LOG_ONLY_MDC, "anything");

    assertThat(excludeProgressLogsFilter.decide(eventWithMarkerOf(INTERNAL_PROGRESS)), equalTo(NEUTRAL));
  }

  @Test
  public void testProgressMarkerInMDC() {
    assertThat(excludeProgressLogsFilter.decide(eventWithMarkerOf(PROGRESS)), equalTo(DENY));
  }

  @Test
  public void testTaskMarkerInMDC() {
    assertThat(excludeProgressLogsFilter.decide(eventWithMarkerOf(TASK_LOG_ONLY)), equalTo(DENY));
  }

  @Test
  public void testTaskOnlyMDCConstant_InMDC() {
    MDC.put(TASK_LOG_ONLY_MDC, "anything");
    assertThat(excludeProgressLogsFilter.decide(event), equalTo(DENY));
  }

  @Test
  public void testAuditLogNotWrittenToNexusLog() {
    assertThat(excludeProgressLogsFilter.decide(eventWithMarkerOf(AUDIT_LOG_ONLY)), equalTo(DENY));
  }

  private ILoggingEvent eventWithMarkerOf(final Marker marker) {
    LoggingEvent event = new LoggingEvent();
    event.setMessage("Test Message");
    event.addMarker(marker);
    return event;
  }
}
