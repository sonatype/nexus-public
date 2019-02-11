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
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;
import org.slf4j.Marker;

import static ch.qos.logback.core.spi.FilterReply.DENY;
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.CLUSTER_LOG_ONLY;
import static org.sonatype.nexus.pax.logging.NexusLogFilter.MDC_MARKER_ID;

/**
 * Logback {@link Filter} for cluster_nexus.log
 * - Must have the CLUSTER_LOG_ONLY marker.
 *
 * Note: Pax-logging doesn't support markers :( However, they do store the marker in MDC for us to grab and do the work
 * ourselves
 *
 * @see org.ops4j.pax.logging.slf4j.Slf4jLogger#info(Marker, String)
 * @since 3.next
 */
public class ClusterLogFilter
    extends Filter<ILoggingEvent>
{

  @Override
  public FilterReply decide(final ILoggingEvent event) {
    if (!CLUSTER_LOG_ONLY.getName().equals(MDC.get(MDC_MARKER_ID))) {
      return DENY;
    }
    return NEUTRAL;
  }
}
