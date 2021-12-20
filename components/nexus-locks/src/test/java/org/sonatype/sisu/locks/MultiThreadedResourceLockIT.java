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
package org.sonatype.sisu.locks;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.testsupport.group.Slow;

import com.google.inject.Provides;
import org.eclipse.sisu.Parameters;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.junit.experimental.categories.Category;

@Category(Slow.class)
public class MultiThreadedResourceLockIT
    extends InjectedTestCase
{
  @Provides
  @Parameters
  Properties systemProperties() {
    return System.getProperties();
  }

  public void testLocalLocks()
      throws InterruptedException
  {
    // test local JVM locks
    System.setProperty("resource-lock-hint", "local");
    launchThreads();
  }

  public void testHazelcastLocks()
      throws InterruptedException
  {
    // test distributed locks
    System.setProperty("resource-lock-hint", "hazelcast");
    launchThreads();
  }

  @Singleton
  static class TestData
  {
    volatile boolean running;

    Thread[] ts;

    int[] sharedDepth;

    int[] exclusiveDepth;

    Throwable[] errors;
  }

  @Inject
  private TestData data;

  private void launchThreads()
      throws InterruptedException
  {
    data.ts = new Thread[128];

    data.sharedDepth = new int[data.ts.length];
    data.exclusiveDepth = new int[data.ts.length];
    data.errors = new Throwable[data.ts.length];

    for (int i = 0; i < data.ts.length; i++) {
      final Locker locker = lookup(Locker.class);
      data.ts[i] = new Thread(locker);
      locker.setIndex(i);
    }

    data.running = true;

    for (final Thread element : data.ts) {
      element.start();
    }

    Thread.sleep(30000);

    data.running = false;

    for (final Thread element : data.ts) {
      element.join(8000);
    }

    boolean failed = false;
    for (final Throwable e : data.errors) {
      if (null != e) {
        e.printStackTrace();
        failed = true;
      }
    }
    assertFalse(failed);
  }

  @Named
  private static class Locker
      implements Runnable
  {
    @Inject
    @Named("resource-lock-hint")
    private ResourceLockFactory locks;

    @Inject
    private TestData data;

    private int index;

    public void setIndex(final int index) {
      this.index = index;
    }

    public void run() {
      try {
        final ResourceLock lk = locks.getResourceLock("TEST");
        final Thread self = Thread.currentThread();

        while (data.running) {
          final double transition = Math.random();
          if (0.0 <= transition && transition < 0.2) {
            if (data.sharedDepth[index] < 8) {
              lk.lockShared(self);
              data.sharedDepth[index]++;
            }
          }
          else if (0.2 <= transition && transition < 0.3) {
            if (data.exclusiveDepth[index] < 8) {
              lk.lockExclusive(self);
              data.exclusiveDepth[index]++;
            }
          }
          else if (0.3 <= transition && transition < 0.6) {
            if (data.exclusiveDepth[index] > 0) {
              data.exclusiveDepth[index]--;
              lk.unlockExclusive(self);
            }
            else {
              try {
                lk.unlockExclusive(self);
                fail("Expected IllegalStateException");
              }
              catch (final IllegalStateException e) {
                // expected
              }
            }
          }
          else {
            if (data.sharedDepth[index] > 0) {
              data.sharedDepth[index]--;
              lk.unlockShared(self);
            }
            else {
              try {
                lk.unlockShared(self);
                fail("Expected IllegalStateException");
              }
              catch (final IllegalStateException e) {
                // expected
              }
            }
          }
        }

        while (data.sharedDepth[index] > 0) {
          lk.unlockShared(self);
          data.sharedDepth[index]--;
        }

        while (data.exclusiveDepth[index] > 0) {
          lk.unlockExclusive(self);
          data.exclusiveDepth[index]--;
        }
      }
      catch (final Throwable e) {
        data.errors[index] = e;
      }
    }
  }
}
