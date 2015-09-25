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
package org.sonatype.nexus.karaf;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileLock;

/**
 * This is a copy of LockFile from nexus-bootstrap, adapted to run in the limited environment of Karaf's launcher.
 *
 * @since 3.0
 */
public class LockFile
{
  private static final byte[] DEFAULT_PAYLOAD;

  static {
    try {
      DEFAULT_PAYLOAD = ManagementFactory.getRuntimeMXBean().getName().getBytes("UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new Error(e);
    }
  }

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
   * Performs locking.
   *
   * If returns {@code true}, locking was successful and caller holds the lock. Multiple invocations,
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
    catch (Exception e) {
      // logging is not configured yet, so use console
      System.err.println("Failed to write lock file");
      e.printStackTrace();
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
   * Releases the lock.
   *
   * Multiple invocations of this file are possible, release will happen only once.
   */
  public synchronized void release() {
    if (fileLock != null) {
      try {
        fileLock.close();
      }
      catch (Exception e) {
        // ignore
      }
      fileLock = null;
    }

    if (randomAccessFile != null) {
      try {
        randomAccessFile.close();
      }
      catch (Exception e) {
        // ignore
      }
      randomAccessFile = null;
    }
  }
}
