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
package org.sonatype.nexus.coreui.internal.log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonatype.nexus.common.log.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Log information object
 *
 * @since 3.39
 */
public class LogXO
{
  private final Logger log = LoggerFactory.getLogger(getClass().getName());

  private String fileName = null;

  private long size = -1;

  private long lastModified = -1;

  public LogXO(Path path) {
    checkNotNull(path);
    try {
      if (LogManager.TASKS_PREFIX.startsWith(path.getParent().getFileName().toString())) {
        this.fileName = LogManager.TASKS_PREFIX + path.getFileName().toString();
      }
      else if (LogManager.REPLICATION_PREFIX.startsWith(path.getParent().getFileName().toString())) {
        this.fileName = LogManager.REPLICATION_PREFIX + path.getFileName().toString();
      }
      else {
        this.fileName = path.getFileName().toString();
      }
      this.size = Files.size(path);
      this.lastModified = Files.getLastModifiedTime(path).toMillis();
    }
    catch (IOException e) {
      log.debug(format("Unable to get information about log file at {%s}", path));
    }
  }

  public String getFileName() {
    return fileName;
  }

  public long getSize() {
    return size;
  }

  public long getLastModified() {
    return lastModified;
  }
}
