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
package org.sonatype.nexus.self.hosted.datastore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.datastore.DataStoreRestorer;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link DataStoreRestorer} implementation which restores databases found in zip archives in
 * the {@code restore-from-backup} directory;
 *
 * @since 3.21
 */
@Primary
@Component
@Qualifier("default")
public class DataStoreRestorerLocalImpl
    implements DataStoreRestorer
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ApplicationDirectories applicationDirectories;

  @Autowired
  public DataStoreRestorerLocalImpl(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public boolean maybeRestore(final DataStoreConfiguration dataStoreConfiguration) {
    File dbDirectory = getDbDirectory();
    File restoreDirectory = getRestoreDirectory();

    if (!restoreDirectory.exists()) {
      log.debug("Restore directory does not exist, skipping.");
      return false;
    }

    if (!isH2Database(dataStoreConfiguration)) {
      log.debug("Data store '{}' is not H2, skipping restore from backup.", dataStoreConfiguration.getName());
      return false;
    }

    return doRestore(dbDirectory, restoreDirectory, dataStoreConfiguration);
  }

  /**
   * Determines if the data store is configured to use H2 database.
   * Only H2 databases use .mv.db files that can be restored from the backup directory.
   *
   * @param dataStoreConfiguration the data store configuration to check
   * @return true if this is an H2 database, false otherwise (e.g., PostgreSQL)
   */
  private boolean isH2Database(final DataStoreConfiguration dataStoreConfiguration) {
    Map<String, String> attributes = dataStoreConfiguration.getAttributes();
    if (attributes == null) {
      return true; // Default to H2 if no attributes are configured
    }

    String jdbcUrl = attributes.get("jdbcUrl");
    if (jdbcUrl == null) {
      return true; // Default to H2 if no JDBC URL is configured
    }

    // Check if the JDBC URL indicates H2 database
    return jdbcUrl.toLowerCase().contains("jdbc:h2:");
  }

  private boolean doRestore(
      final File dbDirectory,
      final File restoreDirectory,
      final DataStoreConfiguration dataStoreConfiguration)
  {

    String dataStoreFileName = dataStoreConfiguration.getName().concat(".mv.db");
    Path dbPath = dbDirectory.toPath();
    if (dbPath.resolve(dataStoreFileName).toFile().exists()) {
      log.debug("Data store '{}' exists, skipping.", dataStoreFileName);
      return false;
    }

    File[] files = restoreDirectory.listFiles();
    if (files == null) {
      log.debug("Could not list files in restore directory '{}', skipping.", restoreDirectory.getAbsolutePath());
      return false;
    }

    if (files.length > 0 && !dbDirectory.exists() && !dbDirectory.mkdirs()) {
      log.error("Unable to restore from backup");
      throw new RuntimeException("Unable to create database directory: " + dbDirectory.getAbsolutePath());
    }

    for (File backup : files) {
      log.info("Checking for backup of '{}' in '{}'", dataStoreFileName, backup.getAbsolutePath());

      if (restore(dbPath, backup, dataStoreFileName)) {
        return true;
      }
    }

    return false;
  }

  private boolean restore(final Path dbDirectory, final File backupArchive, final String dataStoreFileName) {
    try (ZipFile zip = new ZipFile(backupArchive)) {
      return zip.stream().filter(entry -> entry.getName().equals(dataStoreFileName)).findFirst().map(entry -> {
        try {
          Files.copy(zip.getInputStream(entry), dbDirectory.resolve(entry.getName()));
          log.info("Restored {}", entry.getName());
          return true;
        }
        catch (IOException e) {
          throw new RuntimeException(
              "Failed to extract " + entry.getName() + " from archive: " + backupArchive.getAbsolutePath(), e);
        }
      }).orElse(false);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to open archive: " + backupArchive.getAbsolutePath(), e);
    }
  }

  private File getDbDirectory() {
    return applicationDirectories.getWorkDirectory("db", false);
  }

  private File getRestoreDirectory() {
    return applicationDirectories.getWorkDirectory("restore-from-backup");
  }
}
