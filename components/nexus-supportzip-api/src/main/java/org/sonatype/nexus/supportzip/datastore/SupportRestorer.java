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

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.supportzip.ExportData;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.MAX_VALUE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.RESTORE;

/**
 * Restore {@code DataStoreManager#CONFIG_DATASTORE_NAME} data from JSON files.
 *
 * @since 3.next
 */
@Named
@Singleton
@Priority(MAX_VALUE)
@ManagedLifecycle(phase = RESTORE)
public class SupportRestorer
    extends StateGuardLifecycleSupport
{
  private static final String FILE_SUFFIX = ".json";

  private final ApplicationDirectories applicationDirectories;

  private final Map<String, ExportData> exportDataByName;

  @Inject
  public SupportRestorer(
      final ApplicationDirectories applicationDirectories,
      final Map<String, ExportData> exportDataByName)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.exportDataByName = checkNotNull(exportDataByName);
  }

  @Override
  protected void doStart() throws Exception {
    maybeRestore();
  }

  private void maybeRestore() throws IOException {
    Path dbDir = applicationDirectories.getWorkDirectory("db").toPath();
    for (Entry<String, ExportData> exporterEntry : exportDataByName.entrySet()) {
      String fileName = exporterEntry.getKey() + FILE_SUFFIX;
      File file = dbDir.resolve(fileName).toFile();
      ExportData exportData = exporterEntry.getValue();
      if (file.exists()) {
        exportData.restore(file);
      }
      else {
        log.warn("Can't find {} file to restore data", file);
      }
    }
  }
}
