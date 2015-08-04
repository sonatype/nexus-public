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
package org.sonatype.nexus.proxy.item;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Before you continue using this class, stop and read carefully its description. This "smart" lock is NOT an improved
 * implementation of Java's ReentrantReadWriteLock in any way. It does NOT implement the unsupported atomic
 * lock-upgrade. All this class allows is following pattern:
 *
 * <pre>
 *   ...
 *   lock.lockShared();
 *   try {
 *     ...
 *     lock.lockExclusively();
 *     try {
 *       ...
 *     } finally {
 *       lock.unlock();
 *     }
 *     ...
 *   } finally {
 *     lock.unlock();
 *   }
 * </pre>
 *
 * So, it is all about being able to "box" the locks.
 * <p>
 * Caveat No 1: When a lock is "upgraded" from shared to exclusive, it remains exclusive until the matching unlock.
 * <p>
 * Previous behaviour: <s>Once you "upgrade" from shared to exclusive lock, you will possess exclusive lock as long as
 * your last {@link #unlock()} is invoked! While this is not totally correct or natural with "expectations", it does
 * fit
 * it's purpose for use in Nexus.</s>
 * <p>
 * Caveat No 2: The "upgrade" is not atomic, and it is actually not even meant to be atomic. If you take a peek at
 * implementation, by acquiring an exclusive lock, you actually give away all your shared locks, and just then tries to
 * acquire the exclusive lock! By releasing shared lock, you might actually let some other thread, that was waiting for
 * exclusive lock do something. Thus, only after acquiring exclusive lock you are assured that the path content state
 * is
 * unchanged (ie. you should check for it's existence if you want to append to it).
 *
 * @author cstamas
 */
final class SimpleLockResource
    implements LockResource
{
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  @Override
  public void lockShared() {
    if (lock.isWriteLockedByCurrentThread()) {
      lock.writeLock().lock();
    }
    else {
      lock.readLock().lock();
    }
  }

  @Override
  public void lockExclusively() {
    // if we already have exclusive lock then we don't need to upgrade...
    final int readHoldCount = lock.isWriteLockedByCurrentThread() ? 0 : lock.getReadHoldCount();

    if (readHoldCount > 0) {
      // upgrading: must release all read locks
      for (int i = 0; i < readHoldCount; i++) {
        lock.readLock().unlock();
      }

      Thread.yield(); // fairness
    }

    lock.writeLock().lock();

    if (readHoldCount > 0) {
      // re-acquire so downgrade works later on
      for (int i = 0; i < readHoldCount; i++) {
        lock.readLock().lock();
      }
    }
  }

  @Override
  public void unlock() {
    if (lock.isWriteLockedByCurrentThread()) {
      lock.writeLock().unlock();
    }
    else {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean hasLocksHeld() {
    return lock.isWriteLockedByCurrentThread() || lock.getReadHoldCount() > 0;
  }

  // ==

  /**
   * Mainly for debug purposes, see DefaultRepositoryItemUidTest UT how is this used to verify conditions.
   */
  @Override
  public String toString() {
    return lock.toString();
  }
}