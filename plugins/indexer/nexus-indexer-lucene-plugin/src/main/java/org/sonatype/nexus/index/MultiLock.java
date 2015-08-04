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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Composite lock implementation that locks/unlocks member locks.
 */
class MultiLock
    implements Lock
{
  private List<Lock> locks;

  public MultiLock(List<Lock> locks) {
    this.locks = locks;
  }

  @Override
  public void lock() {
    for (int i = 0; i < locks.size(); i++) {
      locks.get(i).lock();
    }
  }

  @Override
  public void lockInterruptibly()
      throws InterruptedException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean tryLock() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit)
      throws InterruptedException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlock() {
    for (int i = locks.size() - 1; i >= 0; i--) {
      locks.get(i).unlock();
    }
  }

  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException();
  }
}
