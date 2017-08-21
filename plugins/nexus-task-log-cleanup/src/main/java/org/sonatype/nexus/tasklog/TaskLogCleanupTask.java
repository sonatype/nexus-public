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
package org.sonatype.nexus.tasklog;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

/**
 * Background task (hidden from users) that cleans up old log files.
 *
 * @since 3.5
 */
@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class TaskLogCleanupTask
    extends TaskSupport
    implements Cancelable
{
  private final TaskLogCleanup taskLogCleanup;

  @Inject
  public TaskLogCleanupTask(final TaskLogCleanup taskLogCleanup) {
    this.taskLogCleanup = checkNotNull(taskLogCleanup);
  }

  @Override
  protected Void execute() throws Exception {
    taskLogCleanup.cleanup();
    return null;
  }

  @Override
  public String getMessage() {
    return "Remove old task log files";
  }
}
