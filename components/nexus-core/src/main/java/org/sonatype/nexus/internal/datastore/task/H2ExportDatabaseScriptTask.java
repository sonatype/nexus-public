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
package org.sonatype.nexus.internal.datastore.task;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.inject.Inject;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.datastore.api.DataStoreNotFoundException;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * A {@link Task} for exporting the SQL database to a script
 */
public class H2ExportDatabaseScriptTask
    extends TaskSupport
{
  private final DataStoreManager dataStoreManager;

  private final ApplicationDirectories applicationDirectories;

  private final FreezeService freezeService;

  private static final String DEFAULT_LOCATION = "db";

  private static final long MINIMUM_SPACE = 5368709120L;

  @Inject
  public H2ExportDatabaseScriptTask(final DataStoreManager dataStoreManager,
                                    final ApplicationDirectories applicationDirectories,
                                    final FreezeService freezeService) {
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.freezeService = checkNotNull(freezeService);
  }

  @Override
  public String getMessage() {
    return "Export H2 SQL database to the specified location";
  }

  @Override
  protected Object execute() throws Exception {
    Optional<DataStore<?>> dataStore = dataStoreManager.get(DEFAULT_DATASTORE_NAME);

    verifyDataStorePresence(dataStore);

    String locationPath = getLocationPath();

    File scriptFolder = applicationDirectories.getWorkDirectory(locationPath);
    File scriptFile = new File(scriptFolder, getScriptName());

    checkFreeSpace(scriptFolder);

    verifyFileDoesNotAlreadyExist(scriptFile);

    log.info("Starting script generation of {} to {}", DEFAULT_DATASTORE_NAME, scriptFile.getAbsolutePath());

    long start = System.currentTimeMillis();

    freezeService.freezeDuring("System is in read-only mode during script generation", () -> {
      try {
        dataStore.get().generateScript(scriptFile.getAbsolutePath());
      }
      catch (Exception e) {
        throw new SqlScriptGenerationException("Script generation failed", e);
      }
    });

    log.info("Completed script generation of {} in {} ms", DEFAULT_DATASTORE_NAME,
        System.currentTimeMillis() - start);

    return null;
  }

  private void verifyDataStorePresence(Optional<DataStore<?>> dataStore) throws DataStoreNotFoundException {
    if (!dataStore.isPresent()) {
      throw new DataStoreNotFoundException(DEFAULT_DATASTORE_NAME);
    }
  }

  @VisibleForTesting
  String getLocationPath() {
    String locationPath = getConfigurationLocationPath();

    if (Strings.isNullOrEmpty(locationPath)) {
      locationPath = DEFAULT_LOCATION;
    }

    return locationPath;
  }

  @VisibleForTesting
  String getConfigurationLocationPath() {
    return getConfiguration().getString(H2ExportDatabaseScriptTaskDescriptor.LOCATION);
  }

  private String getScriptName() {
    return DEFAULT_DATASTORE_NAME + "-" +
        String.format(TIMESTAMP_FORMAT, LocalDateTime.now()) + ".sql";
  }

  private void checkFreeSpace(File scriptFolder) throws InsufficientStorageException {
    if (scriptFolder.getUsableSpace() < MINIMUM_SPACE) {
      throw new InsufficientStorageException("The script location does not have the recommended 5GB of free space available.");
    }
  }

  private void verifyFileDoesNotAlreadyExist(File scriptFile) throws IOException {
    if (scriptFile.isFile()) {
      throw new IOException("File already exists at script location: " + scriptFile.getAbsolutePath());
    }
  }
}
