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

import static ch.qos.logback.core.spi.FilterReply.ACCEPT;
import static ch.qos.logback.core.spi.FilterReply.DENY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.AUDIT_LOG_ONLY;
import static org.sonatype.nexus.pax.logging.NexusLogFilter.MDC_MARKER_ID;

/**
 * Logback {@link Filter} for audit logs. Ensures that the audit logs get the appropriate entries.
 *
 * @since 3.16
 */
public class AuditLogFilter
    extends Filter<ILoggingEvent>
{
  @Override
  public FilterReply decide(final ILoggingEvent event) {
    return AUDIT_LOG_ONLY.getName().equals(MDC.get(MDC_MARKER_ID)) ? ACCEPT : DENY;
  }
}
