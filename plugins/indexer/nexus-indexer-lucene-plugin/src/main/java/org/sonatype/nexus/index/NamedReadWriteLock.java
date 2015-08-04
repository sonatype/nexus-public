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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReadWriteLock implementation that logs lock acquisition/release to slf4j.
 */
class NamedReadWriteLock
    implements ReadWriteLock
{
  private static final Logger log = LoggerFactory.getLogger(NamedReadWriteLock.class);

  private final Lock readLock;

  private final Lock writeLock;

  private NamedReadWriteLock(ReadWriteLock lock, String name) {
    this.readLock = new NamedLock(lock.readLock(), name, "S");
    this.writeLock = new NamedLock(lock.writeLock(), name, "X");
  }

  private static class NamedLock
      implements Lock
  {

    private final Lock lock;

    private final String mode;

    private final String name;

    public NamedLock(Lock lock, String name, String mode) {
      this.lock = lock;
      this.name = name;
      this.mode = mode;
    }

    @Override
    public void lock() {
      log.debug("requesting lock() {} lock on {}", mode, name);
      lock.lock();
      log.debug("acquired lock() {} lock on {}", mode, name);
    }

    @Override
    public void lockInterruptibly()
        throws InterruptedException
    {
      log.debug("requesting lockInterruptibly() {} lock on {}", mode, name);
      lock.lockInterruptibly();
      log.debug("acquired lockInterruptibly() {} lock on {}", mode, name);
    }

    @Override
    public boolean tryLock() {
      boolean locked = lock.tryLock();
      log.debug("tryLock() {} lock on {} returned {}", mode, name, locked);
      return locked;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit)
        throws InterruptedException
    {
      boolean locked = lock.tryLock(time, unit);
      log.debug("tryLock() {} lock on {} returned {}", mode, name, locked);
      return locked;
    }

    @Override
    public void unlock() {
      lock.unlock();
      log.debug("unlock() {} lock on {}", mode, name);
    }

    @Override
    public Condition newCondition() {
      // TODO need to implement if we decide to use conditions
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Lock readLock() {
    return readLock;
  }

  @Override
  public Lock writeLock() {
    return writeLock;
  }

  public static ReadWriteLock decorate(ReadWriteLock lock, String name) {
    if (log.isDebugEnabled()) {
      return new NamedReadWriteLock(lock, name);
    }
    return lock;
  }
}
