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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * Support for {@link LockResource} tests.
 */
public abstract class LockResourceTestSupport
    extends TestSupport
{
  private ExecutorService executor;

  @Before
  public void prepare() {
    executor = Executors.newFixedThreadPool(5);
  }

  protected abstract RepositoryItemUidLock getLockResource(final String name);

  /**
   * Testing shared access: we lock the resource using shared lock, spawn two shared locking threads and we expect
   * that all of those finish their work before we do.
   */
  @Test
  public void sharedLockIsSharedAccess()
      throws Exception
  {
    final RepositoryItemUidLock slr = getLockResource("foo");

    final LockResourceRunnable r1 = new LockResourceRunnable(null, slr, Action.read);
    final LockResourceRunnable r2 = new LockResourceRunnable(null, slr, Action.read);

    slr.lock(Action.read);

    long unlockTs = -1;

    try {
      executor.execute(r1);
      executor.execute(r2);

      Thread.sleep(100);

      unlockTs = System.currentTimeMillis();
    }
    finally {
      slr.unlock();
    }

    executor.shutdown();
    executor.awaitTermination(1000, TimeUnit.MILLISECONDS);

    assertThat(r1.getDoneTs(), lessThanOrEqualTo(unlockTs));
    assertThat(r2.getDoneTs(), lessThanOrEqualTo(unlockTs));
  }

  /**
   * Testing exclusive access: we lock the resource using exclusive lock, spawn two shared locking threads and we
   * expect that all of those finish their work after we do.
   */
  @Test
  public void exclusiveLockIsNotSharedAccess()
      throws Exception
  {
    final RepositoryItemUidLock slr = getLockResource("foo");

    final LockResourceRunnable r1 = new LockResourceRunnable(null, slr, Action.read);
    final LockResourceRunnable r2 = new LockResourceRunnable(null, slr, Action.read);

    slr.lock(Action.create);

    long unlockTs = -1;

    try {
      executor.execute(r1);
      executor.execute(r2);

      Thread.sleep(100);

      unlockTs = System.currentTimeMillis();
    }
    finally {
      slr.unlock();
    }

    executor.shutdown();
    executor.awaitTermination(1000, TimeUnit.MILLISECONDS);

    assertThat(r1.getDoneTs(), greaterThanOrEqualTo(unlockTs));
    assertThat(r2.getDoneTs(), greaterThanOrEqualTo(unlockTs));
  }

  /**
   * Testing lock downgrading: we lock the resource using shared lock, then using exclusive lock, spawn two shared
   * locking threads (that should block since we have exclusive lock), then we unlock (releasing the exclusive lock)
   * and we expect that all of those finish their work before we do.
   * <p>
   * Note: before downgradeable locks, this UT would fail, since after upgrade, exclusive lock would be released on
   * last unlock invocation. Having this test passing shows that downgrade does happen.
   */
  @Test
  public void downgradeLockEnabledSharedAccess()
      throws Exception
  {
    final RepositoryItemUidLock slr = getLockResource("foo");

    final LockResourceRunnable r1 = new LockResourceRunnable(null, slr, Action.read);
    final LockResourceRunnable r2 = new LockResourceRunnable(null, slr, Action.read);

    slr.lock(Action.read);

    long unlockTs = -1;

    try {
      slr.lock(Action.create);

      try {
        executor.execute(r1);
        executor.execute(r2);
      }
      finally {
        slr.unlock();
      }

      Thread.sleep(100);

      unlockTs = System.currentTimeMillis();
    }
    finally {
      slr.unlock();
    }

    executor.shutdown();
    executor.awaitTermination(1000, TimeUnit.MILLISECONDS);

    assertThat(r1.getDoneTs(), lessThanOrEqualTo(unlockTs));
    assertThat(r2.getDoneTs(), lessThanOrEqualTo(unlockTs));
  }

  /**
   * A simple static class that takes a lock and does something with it.
   *
   * @author cstamas
   */
  public static class LockResourceRunnable
      implements Runnable
  {
    private final Runnable runnable;

    private final RepositoryItemUidLock uidLock;

    private final Action action;

    private long doneTs;

    public LockResourceRunnable(final Runnable runnable, final RepositoryItemUidLock lockResource,
                                final Action action)
    {
      this.runnable = runnable;
      this.uidLock = lockResource;
      this.action = action;
      this.doneTs = -1;
    }

    public long getDoneTs() {
      return doneTs;
    }

    @Override
    public void run() {
      uidLock.lock(action);

      try {
        if (runnable != null) {
          runnable.run();
        }
      }
      finally {
        uidLock.unlock();
      }

      doneTs = System.currentTimeMillis();
    }
  }

}
