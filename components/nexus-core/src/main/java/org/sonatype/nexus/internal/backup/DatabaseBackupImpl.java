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
package org.sonatype.nexus.internal.backup;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseServer;

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

  private final int compressionLevel;

  private final int bufferSize;

  private final DatabaseServer databaseServer;

  private final DatabaseManager databaseManager;

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public DatabaseBackupImpl(final DatabaseServer databaseServer, final DatabaseManager databaseManager,
                            @Named("${nexus.backup.compressionLevel:-9}") final int compressionLevel,
                            @Named("${nexus.backup.bufferSize:-1024}") final int bufferSize,
                            final ApplicationDirectories applicationDirectories) {
    this.databaseServer = checkNotNull(databaseServer);
    this.databaseManager = checkNotNull(databaseManager);
    this.compressionLevel = compressionLevel;
    this.bufferSize = bufferSize;
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public Callable<Void> fullBackup(final String backupFolder, final String dbName) throws IOException {
    File backupFile = checkTarget(backupFolder, dbName);
    return new DatabaseBackupRunner(databaseManager.instance(dbName), backupFile, compressionLevel, bufferSize);
  }

  @VisibleForTesting
  File checkTarget(final String backupFolder, final String dbName) throws IOException {
    String filename = dbName + String.format("-%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS.bak", Calendar.getInstance());
    File parentDir = applicationDirectories.getWorkDirectory(backupFolder);
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
