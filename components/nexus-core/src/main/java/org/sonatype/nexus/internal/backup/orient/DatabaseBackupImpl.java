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
package org.sonatype.nexus.internal.backup.orient;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseRestorer;
import org.sonatype.nexus.orient.DatabaseServer;
import org.sonatype.nexus.orient.restore.RestoreFile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * basic implementation of {@link DatabaseBackup}
 *
 * @since 3.2
 */
@Named
@Singleton
public class DatabaseBackupImpl
    extends ComponentSupport
    implements DatabaseBackup
{

  private final DatabaseServer databaseServer;

  private final DatabaseManager databaseManager;

  private final DatabaseRestorer databaseRestorer;

  private final ApplicationDirectories applicationDirectories;

  private final ApplicationVersion applicationVersion;

  @Inject
  public DatabaseBackupImpl(final DatabaseServer databaseServer, final DatabaseManager databaseManager,
                            final DatabaseRestorer databaseRestorer,
                            final ApplicationDirectories applicationDirectories,
                            final ApplicationVersion applicationVersion) {
    this.databaseServer = checkNotNull(databaseServer);
    this.databaseManager = checkNotNull(databaseManager);
    this.databaseRestorer = checkNotNull(databaseRestorer);
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.applicationVersion = checkNotNull(applicationVersion);
  }

  @Override
  public Callable<Void> fullBackup(final String backupFolder, final String dbName, final LocalDateTime timestamp) throws IOException {
    File backupFile = checkTarget(backupFolder, dbName, timestamp);
    return new DatabaseBackupRunner(databaseManager.instance(dbName), backupFile,
        databaseManager.getBackupCompressionLevel(), databaseManager.getBackupBufferSize());
  }

  @VisibleForTesting
  File checkTarget(final String backupFolder, final String dbName, final LocalDateTime timestamp) throws IOException {
    String filename = RestoreFile.formatFilename(dbName, timestamp, applicationVersion.getVersion());
    File parentDir = applicationDirectories.getWorkDirectory(backupFolder);
    if (databaseRestorer.isRestoreFromLocation(parentDir)) {
      throw new IllegalArgumentException("Backup to " + parentDir + " is not allowed.");
    }

    File output = new File(parentDir, filename);
    if (output.createNewFile()) {
      return output;
    }
    else {
      throw new IOException("file creation failed for file: " + output.getAbsolutePath());
    }
  }

  @Override
  public List<String> dbNames() {
    return ImmutableList.copyOf(databaseServer.databases());
  }

}
