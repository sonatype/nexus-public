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

import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;
import org.slf4j.Marker;

import static ch.qos.logback.core.spi.FilterReply.DENY;
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_ONLY_MDC;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_WITH_PROGRESS_MDC;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.CLUSTER_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.INTERNAL_PROGRESS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.PROGRESS;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

/**
 * Logback {@link Filter} for the main nexus.log
 * - Must NOT have the PROGRESS_LOG marker. These are full progress events for the task log.
 * - Must NOT have the TASK_LOG_ONLY marker. These are task log only events.
 * - Must NOT have TASK_LOG_ONLY_MDC in MDC. These are task log only events.
 *
 * Note: Pax-logging doesn't support markers :( However, they do store the marker in MDC for us to grab and do the work
 * ourselves
 *
 * @see org.ops4j.pax.logging.slf4j.Slf4jLogger#info(Marker, String)
 * @since 3.5
 */
public class NexusLogFilter
    extends Filter<ILoggingEvent>
{
  static final String MDC_MARKER_ID = "slf4j.marker";

  private static final List<Marker> DENY_MARKERS = Arrays.asList(PROGRESS, TASK_LOG_ONLY, CLUSTER_LOG_ONLY);

  @Override
  public FilterReply decide(final ILoggingEvent event) {
    String marker = MDC.get(MDC_MARKER_ID);

    if (MDC.get(TASK_LOG_WITH_PROGRESS_MDC) != null && INTERNAL_PROGRESS.getName().equals(marker)) {
      // internal progress logs for TaskLogType.TASK_LOG_WITH_PROGRESS are wanted
      return NEUTRAL;
    }

    if (DENY_MARKERS.stream().anyMatch(m -> m.getName().equals(marker)) || MDC.get(TASK_LOG_ONLY_MDC) != null) {
      return DENY;
    }

    return NEUTRAL;
  }
}
