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

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Background task (hidden from users) that cleans up old log files.
 *
 * @since 3.5
 */
@Component
@TaskLogging(NEXUS_LOG_ONLY)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TaskLogCleanupTask
    extends TaskSupport
    implements Cancelable
{
  private final TaskLogCleanup taskLogCleanup;

  @Autowired
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
