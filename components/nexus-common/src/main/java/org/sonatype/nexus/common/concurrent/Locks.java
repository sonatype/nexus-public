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
package org.sonatype.nexus.common.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Lock helpers.
 *
 * @since 3.0
 */
public class Locks
{
  /**
   * Returns locked lock.
   *
   * Uses {@link Lock#tryLock} with timeout of 60 seconds to avoid potential deadlocks.
   */
  public static Lock lock(final Lock lock) {
    checkNotNull(lock);
    try {
      lock.tryLock(60, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
    return lock;
  }

  /**
   * Returns locked read-lock.
   */
  public static Lock read(final ReadWriteLock readWriteLock) {
    checkNotNull(readWriteLock);
    return lock(readWriteLock.readLock());
  }

  /**
   * Returns locked write-lock.
   */
  public static Lock write(final ReadWriteLock readWriteLock) {
    checkNotNull(readWriteLock);
    return lock(readWriteLock.writeLock());
  }
}
