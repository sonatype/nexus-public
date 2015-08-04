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
package org.sonatype.nexus.index;

import java.util.concurrent.TimeUnit;

/**
 * Simple high-contention low-traffic reentrant lock implementation. In addition to regular #tryLock/#unlock methods,
 * provides #tryForceLock that attempts to force lock acquisition by periodically calling #interrupt on the thread
 * holding the lock.
 * <p>
 * This class is not meant as general purpose lock implementation. It is specifically tailored for synchronizing
 * repository index download, reindex, publish and delete operations.
 */
class ForceableReentrantLock
{
  private final Object lock = new Object();

  private Thread owner;

  private int count;

  public boolean tryLock() {
    synchronized (lock) {
      if (owner != null && owner != Thread.currentThread()) {
        return false;
      }
      owner = Thread.currentThread();
      count++;
      return true;
    }
  }

  public void unlock() {
    synchronized (lock) {
      if (owner != Thread.currentThread()) {
        throw new IllegalStateException();
      }
      if (--count == 0) {
        owner = null;
        lock.notifyAll();
      }
    }
  }

  public boolean tryForceLock(long time, TimeUnit unit) {
    final long start = System.currentTimeMillis();
    synchronized (lock) {
      while (owner != null && owner != Thread.currentThread()) {
        if (timeout(start, time, unit)) {
          return false;
        }
        owner.interrupt();
        try {
          lock.wait(1000L);
        }
        catch (InterruptedException e) {
          return false;
        }
      }
      owner = Thread.currentThread();
      count++;
      return true;
    }
  }

  private boolean timeout(long start, long duration, TimeUnit unit) {
    return System.currentTimeMillis() - start > unit.toMillis(duration);
  }
}
