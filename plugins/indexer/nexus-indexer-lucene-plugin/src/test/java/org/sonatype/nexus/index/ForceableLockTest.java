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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ForceableLockTest
{
  private ForceableReentrantLock subject;

  private TestThread otherThread;

  private static class TestThread
      extends Thread
  {
    public volatile boolean interrupted = false;

    public volatile boolean locked = false;

    public final Semaphore semaphore = new Semaphore(1);

    private final ForceableReentrantLock lock;

    public TestThread(final ForceableReentrantLock lock) {
      this.lock = lock;
    }

    @Override
    public void run() {
      locked = lock.tryLock();
      semaphore.release();
      try {
        try {
          Thread.sleep(60 * 1000L);
        }
        catch (InterruptedException e) {
          interrupted = true;
        }
      }
      finally {
        lock.unlock();
      }
    }
  }

  @Before
  public void setUp() {
    subject = new ForceableReentrantLock();
    otherThread = new TestThread(subject);
  }

  @After
  public void tearDown()
      throws InterruptedException
  {
    otherThread.interrupt();
    otherThread.join();
  }

  @Test
  public void basic()
      throws InterruptedException
  {
    otherThread.semaphore.acquire();
    otherThread.start();
    otherThread.semaphore.acquire();

    Assert.assertTrue(otherThread.locked); // the other thread is holding the lock
    Assert.assertFalse(otherThread.interrupted); // sanity check
    Assert.assertFalse(subject.tryLock()); // this thread is not able to get the lock

    Assert.assertTrue(subject.tryForceLock(5, TimeUnit.SECONDS)); // this thread can force the lock
    Assert.assertTrue(otherThread.interrupted);
  }

  @Test
  public void reentrant() {
    Assert.assertTrue(subject.tryLock());
    Assert.assertTrue(subject.tryLock());

    subject.unlock();
    subject.unlock();
  }

  @Test(expected = IllegalStateException.class)
  public void nonownerUnlock()
      throws InterruptedException
  {
    otherThread.semaphore.acquire();
    otherThread.start();
    otherThread.semaphore.acquire();

    Assert.assertTrue(otherThread.locked); // the other thread is holding the lock
    Assert.assertFalse(otherThread.interrupted); // sanity check

    subject.unlock();
  }

  @Test(expected = IllegalStateException.class)
  public void notlockedUnlock() {
    subject.unlock();
  }
}
