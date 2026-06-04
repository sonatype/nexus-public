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
package org.sonatype.nexus.common.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to facilitate writing to a temporary file and replacing target file with optional backup.
 *
 */
public class FileReplacer
{
  private static final AtomicInteger counter = new AtomicInteger(0);

  private static final Logger log = LoggerFactory.getLogger(FileReplacer.class);

  private final File file;

  private final String filePrefix;

  private final File tempFile;

  private final File backupFile;

  private boolean deleteBackupFile;

  public FileReplacer(final File file) throws IOException {
    this.file = checkNotNull(file);

    // not using File.createTempFile() here so tmp + backup can share same timestamp-id
    // and delay creation of file until needed in the case of backup file
    // counter here just to ensure that sub-mills usage will not conflict

    this.filePrefix = file.getName() + "-" + UUID.randomUUID() + "-" + counter.getAndIncrement();
    this.tempFile = new File(file.getParentFile(), filePrefix + ".tmp");
    this.backupFile = new File(file.getParentFile(), filePrefix + ".bak");

    file.getParentFile().mkdirs();

    if (tempFile.exists()) {
      log.warn("Temporary file already exists; removing: {}", tempFile);
      delete(tempFile);
    }

    tempFile.createNewFile();
  }

  public FileReplacer(final String fileName) throws IOException {
    this(new File(checkNotNull(fileName)));
  }

  private void delete(final File file) throws IOException {
    boolean deleted = file.delete();
    if (!deleted) {
      throw new IOException("Failed to delete file: " + file);
    }
  }

  public File getFile() {
    return file;
  }

  public File getTempFile() {
    return tempFile;
  }

  public File getBackupFile() {
    return backupFile;
  }

  public boolean isDeleteBackupFile() {
    return deleteBackupFile;
  }

  public void setDeleteBackupFile(final boolean deleteBackupFile) {
    this.deleteBackupFile = deleteBackupFile;
  }

  public static interface ContentWriter
  {
    void write(final BufferedOutputStream output) throws IOException;
  }

  public void replace(final ContentWriter writer) throws IOException {
    checkNotNull(writer);

    // setup buffering, as almost certainly anywhere using this class is going to want this
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(tempFile));

    // delegate to do the write operation
    try {
      try {
        writer.write(output);
      }
      finally {
        // always close after success or failure
        output.close();
      }
    }
    catch (IOException e) {
      // complain with details about temp file and propagate exception
      log.warn("Failed to write temporary file: {}", tempFile, e);
      throw e;
    }

    // replace the file only if operation succeeded
    replaceFile();
  }

  private void replaceFile() throws IOException {
    checkState(tempFile.exists(), "Temporary file missing");

    // backup target file if it exists
    if (file.exists()) {
      log.trace("Backing up target file: {} -> {}", file, backupFile);

      if (backupFile.exists()) {
        log.warn("Backup file already exists; removing: {}", backupFile);
        delete(backupFile);
      }

      Files.move(file, backupFile);
    }

    // move tmp file into place
    log.trace("Replacing file: {} -> {}", tempFile, file);
    Files.move(tempFile, file);

    // delete the backup file if requested
    if (backupFile.exists() && deleteBackupFile) {
      log.trace("Deleting backup file: {}", backupFile);
      delete(backupFile);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "file=" + file +
        ", filePrefix='" + filePrefix + '\'' +
        '}';
  }
}
