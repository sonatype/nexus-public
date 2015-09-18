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
package org.sonatype.nexus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File locker implementation, inspired by Eclipse Locker. It uses Java NIO {@link FileChannel#tryLock(long, long,
 * boolean)} method to perform locking. As a commodity function, it also writes to a file a payload, making problem
 * diagnosing a bit easier, as reading (ie. from console) of the lock file content might reveal useful information
 * about lock owner.
 * All the limitations mentioned for {@link FileLock} stands.
 *
 * @since 2.7.0
 */
public class LockFile
{
  private static final Logger log = LoggerFactory.getLogger(LockFile.class);

  private static final byte[] DEFAULT_PAYLOAD = ManagementFactory.getRuntimeMXBean().getName().getBytes(
      Charset.forName("UTF-8"));

  private final File lockFile;

  private final byte[] payload;

  private FileLock fileLock;

  private RandomAccessFile randomAccessFile;

  /**
   * Creates a LockFile with default payload (that contains the JVM name, usually {@code PID@hostname}).
   */
  public LockFile(final File lockFile) {
    this(lockFile, DEFAULT_PAYLOAD);
  }

  /**
   * Creates a LockFile with custom payload.
   */
  public LockFile(final File lockFile, final byte[] payload) {
    if (lockFile == null || payload == null) {
      throw new NullPointerException();
    }
    this.lockFile = lockFile;
    this.payload = payload;
  }

  /**
   * Returns the file used by this instance.
   */
  public File getFile() {
    return lockFile;
  }

  /**
   * Returns the payload used by this instance.
   */
  public byte[] getPayload() {
    return payload;
  }

  /**
   * Performs locking. If returns {@code true}, locking was successful and caller holds the lock. Multiple invocations,
   * after lock is acquired, does not have any effect, locking happens only once.
   */
  public synchronized boolean lock() {
    if (fileLock != null) {
      return true;
    }
    try {
      randomAccessFile = new RandomAccessFile(lockFile, "rws");
      fileLock = randomAccessFile.getChannel().tryLock(0L, 1L, false);
      if (fileLock != null) {
        randomAccessFile.setLength(0);
        randomAccessFile.seek(0);
        randomAccessFile.write(payload);
      }
    }
    catch (IOException | OverlappingFileLockException e) {
      log.warn("Failed to write lock file", e);
      // handle it as null result
      fileLock = null;
    }
    finally {
      if (fileLock == null) {
        release();
        return false;
      }
    }
    return true;
  }

  /**
   * Releases the lock. Multiple invocations of this file are possible, release will happen only once.
   */
  public synchronized void release() {
    close(fileLock);
    fileLock = null;
    close(randomAccessFile);
    randomAccessFile = null;
  }

  /**
   * Reads the contents of the lock file for confirmation purposes; only call this method when a lock has been
   * obtained. Package-scoped as this is only used by tests.
   */
  byte[] readBytes() throws IOException {
    if (randomAccessFile == null) {
      throw new IllegalStateException("No lock obtained, cannot read file contents.");
    }

    byte[] buffer = new byte[(int) randomAccessFile.length()];
    randomAccessFile.seek(0);
    randomAccessFile.read(buffer, 0, buffer.length);
    return buffer;
  }

  // ==

  private static void close(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      }
      catch (Exception e) {
        // muted
      }
    }
  }
}
