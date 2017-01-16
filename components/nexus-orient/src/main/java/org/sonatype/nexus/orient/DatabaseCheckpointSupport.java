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
package org.sonatype.nexus.orient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.upgrade.Checkpoint;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support for upgrade checkpoint of OrientDB databases.
 * 
 * @since 3.1
 */
public abstract class DatabaseCheckpointSupport
    extends ComponentSupport
    implements Checkpoint
{
  private final String databaseName;

  private final Provider<DatabaseInstance> databaseInstance;

  private final ApplicationDirectories appDirectories;

  private File upgradeDir;

  private File backupZip;

  private File failedZip;

  protected DatabaseCheckpointSupport(final String databaseName,
                                      final Provider<DatabaseInstance> databaseInstance,
                                      final ApplicationDirectories appDirectories)
  {
    this.databaseName = checkNotNull(databaseName);
    this.databaseInstance = checkNotNull(databaseInstance);
    this.appDirectories = checkNotNull(appDirectories);
  }

  @Override
  public void begin(String version) throws Exception {
    upgradeDir = appDirectories.getWorkDirectory("upgrades/" + databaseName);

    String timestampSuffix = String.format("-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS", System.currentTimeMillis());
    backupZip = new File(upgradeDir, databaseName + "-" + version + timestampSuffix + "-backup.zip");
    failedZip = new File(upgradeDir, databaseName + "-failed.zip");

    log.debug("Backing up database to {}", backupZip);
    try (OutputStream out = new FileOutputStream(backupZip)) {
      databaseInstance.get().externalizer().backup(out);
    }
  }

  @Override
  public void commit() throws Exception {
    // no-op
  }

  @Override
  public void rollback() throws Exception {
    checkState(failedZip != null);
    checkState(backupZip != null);
    log.debug("Exporting failed database to {}", failedZip);
    try (OutputStream out = new FileOutputStream(failedZip)) {
      databaseInstance.get().externalizer().backup(out);
    }
    finally {
      log.debug("Restoring original database from {}", backupZip);
      try (InputStream in = new FileInputStream(backupZip)) {
        databaseInstance.get().externalizer().restore(in, true);
      }
    }
  }

  @Override
  public void end() {
    checkState(upgradeDir != null);
    checkState(backupZip != null);
    log.debug("Deleting backup from {}", upgradeDir);
    try {
      Files.delete(backupZip.toPath());
      Files.delete(upgradeDir.toPath());
    }
    catch (IOException e) {  // NOSONAR
      log.warn("Could not delete backup of {} database, please delete {} manually. Error: {}", databaseName, upgradeDir,
          e.toString());
    }
    backupZip = null;
    failedZip = null;
    upgradeDir = null;
  }
}
