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
package org.sonatype.nexus.supportzip.datastore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.supportzip.ImportTaskData;

import org.apache.commons.io.FileUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.supportzip.datastore.RestoreHelper.FILE_SUFFIX;

/**
 * Restore Task's related data from JSON file(s).
 *
 * @since 3.30
 */
@Named
@Singleton
@Priority(0) // allow all default tasks to be loaded at first.
@ManagedLifecycle(phase = TASKS)
public class TaskRestorer
    extends StateGuardLifecycleSupport
{
  private final RestoreHelper restoreHelper;

  private final Map<String, ImportTaskData> importTaskByName;

  @Inject
  public TaskRestorer(
      final RestoreHelper restoreHelper,
      final Map<String, ImportTaskData> importTaskByName)
  {
    this.restoreHelper = checkNotNull(restoreHelper);
    this.importTaskByName = checkNotNull(importTaskByName);
  }

  @Override
  protected void doStart() throws Exception {
    maybeRestore();
  }

  private void maybeRestore() throws IOException {
    Path dbDir = restoreHelper.getDbPath();
    for (Entry<String, ImportTaskData> exporterEntry : importTaskByName.entrySet()) {
      String fileName = exporterEntry.getKey() + FILE_SUFFIX;
      File file = dbDir.resolve(fileName).toFile();
      ImportTaskData importTask = exporterEntry.getValue();
      if (file.exists()) {
        importTask.restore(file);
        FileUtils.deleteQuietly(file);
      }
      else {
        log.debug("Can't find {} file to restore data", file);
      }
    }
  }
}
