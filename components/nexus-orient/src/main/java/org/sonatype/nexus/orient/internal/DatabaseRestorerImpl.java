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
package org.sonatype.nexus.orient.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.DatabaseRestorer;
import org.sonatype.nexus.orient.restore.RestoreFile;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static java.util.stream.Collectors.toList;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Restores orient databases from standard location "sonatype-work/nexus3/restore-from-backup".
 *
 * @since 3.2
 */
@Named
@Singleton
public class DatabaseRestorerImpl
    extends ComponentSupport
    implements DatabaseRestorer
{

  private static final String RESTORE_FROM_LOCATION = "restore-from-backup";

  private final File restoreFromLocation;

  private final NodeAccess nodeAccess;

  @Inject
  public DatabaseRestorerImpl(final ApplicationDirectories applicationDirectories, final NodeAccess nodeAccess) {
    this.restoreFromLocation = applicationDirectories.getWorkDirectory(RESTORE_FROM_LOCATION);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  public RestoreFile getPendingRestore(final String databaseName) throws IOException {
    checkNotNull(databaseName);

    List<Path> backupFiles = Files.list(restoreFromLocation.toPath())
        .filter(path -> isBackupFileForDatabase(path, databaseName))
        .collect(toList());

    if (backupFiles.size() > 1) {
      throw new IllegalStateException("more than 1 backup file found for database " + databaseName + ": " +
          backupFiles);
    }

    return backupFiles.isEmpty() ? null : RestoreFile.newInstance(backupFiles.get(0));
  }

  @Override
  public boolean maybeRestoreDatabase(final ODatabaseDocumentTx db, final String databaseName) throws IOException {
    checkNotNull(db);
    checkNotNull(databaseName);

    log.debug("checking if database {} should be restored", databaseName);
    if (!nodeAccess.isOldestNode()) {
      log.debug("skipping restore of database {} because we're joining an existing cluster", databaseName);
      return false;
    }

    Path path = getRestorePath(databaseName);

    if (path != null) {
      log.info("restoration of database {} from file {} starting", databaseName, path);
      doRestore(path.toFile(), db, databaseName);
      return true;
    }

    return false;
  }

  @Override
  public boolean isRestoreFromLocation(final File location) throws IOException {
    return Files.isSameFile(restoreFromLocation.toPath(), location.toPath());
  }

  /**
   * @param databaseName the name of the database
   * @return the {@link Path} to the 1 backup file if it exists, null otherwise
   * @throws IOException if there was a problem listing files from {@link #RESTORE_FROM_LOCATION}
   * @throws IllegalStateException if more than 1 backup file exists for the database
   */
  protected Path getRestorePath(final String databaseName) throws IOException {
    checkNotNull(databaseName);

    List<Path> backupFiles = Files.list(restoreFromLocation.toPath())
        .filter(path -> isBackupFileForDatabase(path, databaseName))
        .collect(toList());

    if (backupFiles.size() > 1) {
      throw new IllegalStateException("more than 1 backup file found for database " + databaseName + ": " +
          backupFiles);
    }

    return backupFiles.isEmpty() ? null : backupFiles.get(0);
  }

  private void doRestore(final File file, final ODatabaseDocumentTx db, final String databaseName) {
    try (InputStream inputStream = new FileInputStream(file)) {
      db.restore(inputStream, null, null, iText -> {
          log.debug("database restore of {}, received message '{}'", databaseName, iText);
        });
      log.info("database {} restored", databaseName);
    }
    catch (Exception e) {
      throw new RuntimeException(String.format("database restore of %s from %s failed", databaseName, file), e);
    }
  }

  private boolean isBackupFileForDatabase(final Path path, final String databaseName) {
    checkNotNull(path);
    checkNotNull(databaseName);

    log.trace("testing if {} is a backup file for {}", path, databaseName);

    Path pathFile = path.getFileName();
    if (pathFile != null) {
      String filename = pathFile.toString();
      return
          Files.isRegularFile(path) &&
          filename.startsWith(databaseName + '-') &&
          filename.endsWith(".bak");
    }
    else {
      return false;
    }
  }
}
