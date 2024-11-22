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

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.logging.task.TaskLogHome;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.filefilter.AgeFileFilter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.iterateFiles;

/**
 * Cleanup the task log files based on how old in days they are
 * 
 * @since 3.5
 */
@Named
@Singleton
public class TaskLogCleanup
    extends ComponentSupport
{
  private final Integer numberOfDays;

  private String taskLogHome;

  @Inject
  public TaskLogCleanup(@Named("${nexus.tasks.log.cleanup.numberOfDays:-30}") final Integer numberOfDays) {
    this.numberOfDays = checkNotNull(numberOfDays);
  }

  void cleanup() {
    String taskLogsHome = getTaskLogHome();

    if (taskLogsHome == null) {
      // we are forgiving if the task logs home is not defined. Just log a message with a call to action.
      log.warn("Unable to cleanup task log files. Please check that the 'tasklogfile' appender exists in logback.xml");
      return;
    }

    File logFilesHome = new File(taskLogsHome);

    log.info("Cleaning up log files in {} older than {} days", logFilesHome.getAbsolutePath(), numberOfDays);

    LocalDate now = LocalDate.now().minusDays(numberOfDays);
    Date thresholdDate = Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant());
    AgeFileFilter ageFileFilter = new AgeFileFilter(thresholdDate);
    Iterator<File> filesToDelete = iterateFiles(logFilesHome, ageFileFilter, ageFileFilter);
    filesToDelete.forEachRemaining(f -> {
      try {
        forceDelete(f);
        log.info("Removed task log file {}", f.toString());
      }
      catch (IOException e) { // NOSONAR
        log.error("Unable to delete task file {}. Message was {}.", f.toString(), e.getMessage());
      }
    });
  }

  @VisibleForTesting
  String getTaskLogHome() {
    if (taskLogHome == null) {
      taskLogHome = TaskLogHome.getTaskLogsHome();
    }
    return taskLogHome;
  }
}
