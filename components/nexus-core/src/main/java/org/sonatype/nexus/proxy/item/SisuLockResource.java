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

import org.sonatype.sisu.locks.ResourceLock;

/**
 * Drop-in replacement for {@link SimpleLockResource} that uses Sisu's semaphore-based {@link ResourceLock}.
 */
final class SisuLockResource
    implements LockResource
{
  private final ResourceLock lock;

  // ==

  SisuLockResource(final ResourceLock lock) {
    this.lock = lock;
  }

  @Override
  public void lockShared() {
    final Thread self = Thread.currentThread();
    if (lock.getExclusiveCount(self) > 0) {
      lock.lockExclusive(self);
    }
    else {
      lock.lockShared(self);
    }
  }

  @Override
  public void lockExclusively() {
    lock.lockExclusive(Thread.currentThread());
  }

  @Override
  public void unlock() {
    final Thread self = Thread.currentThread();
    if (lock.getExclusiveCount(self) > 0) {
      lock.unlockExclusive(self);
    }
    else {
      lock.unlockShared(self);
    }
  }

  @Override
  public boolean hasLocksHeld() {
    final Thread self = Thread.currentThread();
    for (final Thread t : lock.getOwners()) {
      if (self == t) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    int sharedCount = 0, exclusiveCount = 0;
    for (final Thread t : lock.getOwners()) {
      sharedCount += lock.getSharedCount(t);
      exclusiveCount += lock.getExclusiveCount(t);
    }
    return "[Write locks = " + exclusiveCount + ", Read locks = " + sharedCount + "]";
  }
}