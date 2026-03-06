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
package org.sonatype.nexus.datastore.h2.task;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A {@link Task} for backing up an embedded H2 datastore.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class H2BackupTask
    extends TaskSupport
{
  private final DataStoreManager dataStoreManager;

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public H2BackupTask(final DataStoreManager dataStoreManager, final ApplicationDirectories applicationDirectories) {
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public String getMessage() {
    return "Backup embedded h2 database to the specified location";
  }

  @Override
  protected Object execute() throws Exception {
    DataStore<?> dataStore = dataStoreManager.get(DEFAULT_DATASTORE_NAME)
        .orElseThrow(() -> new IllegalStateException(
            "Unable to locate datastore with name " + DEFAULT_DATASTORE_NAME));

    // Trim whitespace from location to prevent silent failures
    String configuredLocation = checkNotNull(
        getConfiguration().getString(H2BackupTaskDescriptor.LOCATION),
        "Backup location not configured");
    String trimmedLocation = configuredLocation.trim();

    if (trimmedLocation.isEmpty()) {
      throw new IllegalArgumentException(
          "Backup location cannot be empty or whitespace: '" + configuredLocation + "'");
    }

    // Resolve backup location (supports both relative and absolute paths)
    File backupFolder = applicationDirectories.getWorkDirectory(trimmedLocation);
    File backupFile = new File(backupFolder, getBackupFileName());

    // Prepare backup location and verify preconditions
    prepareBackupLocation(backupFolder, backupFile);

    log.info("Starting backup of {} to {}", DEFAULT_DATASTORE_NAME, backupFile.getAbsolutePath());

    long start = System.currentTimeMillis();

    dataStore.backup(backupFile.getAbsolutePath());

    // Verify backup succeeded
    verifyBackupCreated(backupFile);

    long backupSize = backupFile.length();
    long duration = System.currentTimeMillis() - start;
    log.info("Completed backup of {} in {} ms ({} bytes)", DEFAULT_DATASTORE_NAME, duration, backupSize);

    return null;
  }

  /**
   * Prepares the backup location by ensuring the directory exists and the target file doesn't exist yet.
   * This prevents overwriting existing backups and ensures we have a valid location to write to.
   *
   * @param backupFolder the directory where backup will be stored
   * @param backupFile the target backup file
   * @throws IOException if directory cannot be created or file already exists
   */
  private void prepareBackupLocation(final File backupFolder, final File backupFile) throws IOException {
    // Create backup directory if needed - check exists() to distinguish "already exists" from "creation failed"
    if (!backupFolder.mkdirs() && !backupFolder.exists()) {
      throw new IOException("Failed to create backup directory: " + backupFolder.getAbsolutePath());
    }

    // Verify backup folder is actually a directory, not a file
    if (!backupFolder.isDirectory()) {
      throw new IOException("Backup path exists but is not a directory: " + backupFolder.getAbsolutePath());
    }

    // Ensure backup file doesn't already exist (prevents accidental overwrite)
    if (backupFile.exists()) {
      throw new IOException("File already exists at backup file location: " + backupFile.getAbsolutePath());
    }
  }

  /**
   * Verifies that the backup operation successfully created a valid backup file.
   * This prevents silent failures where the backup operation returns without error but doesn't create a file.
   *
   * @param backupFile the backup file to verify
   * @throws IOException if file doesn't exist or is empty
   */
  private void verifyBackupCreated(final File backupFile) throws IOException {
    if (!backupFile.exists()) {
      throw new IOException("Backup file was not created: " + backupFile.getAbsolutePath());
    }
    if (backupFile.length() == 0) {
      throw new IOException("Backup file is empty (0 bytes): " + backupFile.getAbsolutePath());
    }
  }

  private String getBackupFileName() {
    return DEFAULT_DATASTORE_NAME + "-" +
        String.format(TIMESTAMP_FORMAT, LocalDateTime.now()) + ".zip";
  }
}
