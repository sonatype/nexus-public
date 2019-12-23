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
package org.sonatype.nexus.datastore.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.datastore.DataStoreRestorer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link DataStoreRestorer} implementation which restores databases found in zip archives in
 * the {@code restore-from-backup} directory;
 *
 * @since 3.next
 */
@Named("default")
@Singleton
public class DataStoreRestorerLocalImpl
    extends ComponentSupport
    implements DataStoreRestorer
{
  private final ApplicationDirectories applicationDirectories;

  @Inject
  public DataStoreRestorerLocalImpl(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public boolean maybeRestore() {
    File dbDirectory = getDbDirectory();
    if (dbDirectory.exists()) {
      log.debug("Database exists, skipping.");
      return false;
    }

    File restoreDirectory = getRestoreDirectory();
    if (!restoreDirectory.exists()) {
      log.debug("Restore directory does not exist, skipping.");
      return false;
    }

    return doRestore(dbDirectory, restoreDirectory);
  }

  private boolean doRestore(final File dbDirectory, final File restoreDirectory) {
    if (!dbDirectory.exists() && !dbDirectory.mkdirs()) {
      log.error("Unable to restore from backup");
      throw new RuntimeException("Unable to create database directory: " + dbDirectory.getAbsolutePath());
    }

    boolean restored = false;
    Path dbPath = dbDirectory.toPath();
    for (File backup : restoreDirectory.listFiles((p, name) -> name.endsWith(".zip"))) {
      log.info("Restoring from: {}", backup.getAbsolutePath());

      restore(dbPath, backup);
      restored = true;
    }
    return restored;
  }

  private void restore(final Path dbDirectory, final File backupArchive) {
    try (ZipFile zip = new ZipFile(backupArchive)) {
      zip.stream().forEach(entry -> {
        try {
          Files.copy(zip.getInputStream(entry), dbDirectory.resolve(entry.getName()));
          log.info("Restored {}", entry.getName());
        }
        catch (IOException e) {
          throw new RuntimeException(
              "Failed to extract " + entry.getName() + " from archive: " + backupArchive.getAbsolutePath(), e);
        }
      });
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to open archive: " + backupArchive.getAbsolutePath(), e);
    }
  }

  private File getDbDirectory() {
    return applicationDirectories.getWorkDirectory("db");
  }

  private File getRestoreDirectory() {
    return applicationDirectories.getWorkDirectory("restore-from-backup");
  }
}
