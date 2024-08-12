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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.logging.task.TaskLogHome;
import org.sonatype.nexus.supportzip.FileContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import com.google.common.annotations.VisibleForTesting;

import static java.time.Instant.ofEpochMilli;
import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.TASKLOG;

/**
 * Adds log files to support bundle.
 *
 * @since 3.5
 */
@Named
@Singleton
public class TaskLogCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private static final String[] extensions = new String[]{"log"};

  @Override
  public void customize(final SupportBundle supportBundle) {
    Instant cutoff = ZonedDateTime.now().minusHours(24).toInstant();
    String taskLogHome = getTaskLogHome();
    if (taskLogHome != null) {
      iterateFiles(new File(taskLogHome), extensions, false).forEachRemaining(file -> {
        if (ofEpochMilli(file.lastModified()).isAfter(cutoff)) {
          supportBundle.add(
              new FileContentSourceSupport(TASKLOG, String.format("log/tasks/%s", file.getName()), file, DEFAULT));
        }
        else {
          log.debug("Skipping file [past 24 hours]: {}", file);
        }
      });
    }
  }

  @VisibleForTesting
  protected String getTaskLogHome() {
    return TaskLogHome.getTaskLogsHome();
  }
}
