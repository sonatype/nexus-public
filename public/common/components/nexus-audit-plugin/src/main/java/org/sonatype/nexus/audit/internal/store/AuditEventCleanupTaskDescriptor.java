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
package org.sonatype.nexus.audit.internal.store;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.stereotype.Component;

/**
 * Task descriptor for the audit event retention cleanup task.
 * <p>
 * Not exposed and not visible in the UI — the schedule is managed by
 * {@link AuditEventCleanupTaskManager}, which auto-creates it on startup with a daily
 * cron.
 */
@Component
@AvailabilityVersion(from = "1.0")
public class AuditEventCleanupTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "audit.events.cleanup";

  public static final String TASK_NAME = "Admin - Cleanup old audit events";

  public AuditEventCleanupTaskDescriptor() {
    super(TYPE_ID,
        AuditEventCleanupTask.class,
        TASK_NAME,
        NOT_VISIBLE,
        NOT_EXPOSED);
  }
}
