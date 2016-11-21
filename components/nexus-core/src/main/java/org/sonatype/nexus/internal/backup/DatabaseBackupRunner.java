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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import org.sonatype.nexus.orient.DatabaseInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;

/**
 * Background thread that creates the database backup
 *
 * @since 3.2
 */
public class DatabaseBackupRunner
    implements Callable<Void>
{

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final DatabaseInstance databaseInstance;

  private final File backupFile;

  private final int compressionLevel;

  private final int bufferSize;

  /**
   * Constructor to instantiate thread for executing database backup
   *
   * @param databaseInstance the database that will be backed up
   * @param backupFile the backup data will be written onto this file
   * @param compressionLevel from {@link com.orientechnologies.orient.core.util.OBackupable} ZIP Compression level between 1 (the minimum) and 9 (maximum). The bigger is the compression, the smaller will be the final backup content, but will consume more CPU and time to execute
   * @param bufferSize from {@link com.orientechnologies.orient.core.util.OBackupable} Buffer size in bytes, the bigger is the buffer, the more efficient will be the compression
   */
  public DatabaseBackupRunner(final DatabaseInstance databaseInstance, final File backupFile,
                              final int compressionLevel, final int bufferSize) {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.backupFile = checkNotNull(backupFile);
    this.compressionLevel = checkNotNull(compressionLevel);
    this.bufferSize = checkNotNull(bufferSize);
  }

  @Override
  public Void call() throws Exception {
    try (final OutputStream backupOutputStream = new FileOutputStream(backupFile)) {
      inTx(() -> databaseInstance).throwing(IOException.class).run(
          db -> {
            db.backup(backupOutputStream, null, null, iText -> {
              // these messages are a bit chatty, so only visible at debug
              log.debug("database backup of {}, received message '{}'", databaseInstance.getName(), iText);
            }, compressionLevel, bufferSize);
            log.info("database backup of {} completed successfully", databaseInstance.getName());
          });
    }
    catch (Throwable e) { // NOSONAR
      throw new RuntimeException(String.format("database backup of %s failed", databaseInstance.getName()), e);
    }
    return null;
  }

}
