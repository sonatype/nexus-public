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
package org.sonatype.nexus.cleanup.internal.task;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.cleanup.service.CleanupService;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.logging.task.TaskLogging;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLogType.TASK_LOG_ONLY;

/**
 * Runs cleanup via the cleanup service.
 *
 * @since 3.14
 */
@Named
@TaskLogging(TASK_LOG_ONLY)
public class CleanupTask
    extends TaskSupport
{
  private final CleanupService cleanupService;

  @Inject
  public CleanupTask(final CleanupService cleanupService) {
    this.cleanupService = checkNotNull(cleanupService);
  }
  
  @Override
  protected Object execute() {
    log.info("Starting cleanup");
    
    cleanupService.cleanup(() -> isCanceled());
    
    if (isCanceled()) {
      log.info("Cleanup was cancelled before it could finish");
    }
    else {
      log.info("Cleanup finished");
    }
    
    return null;
  }

  @Override
  public String getMessage() {
    return "Run repository cleanup";
  }
}
