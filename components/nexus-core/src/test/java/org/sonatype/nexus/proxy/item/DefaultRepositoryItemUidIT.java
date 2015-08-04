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

import java.util.Random;

import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.test.NexusTestSupport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

// This is an IT just because it runs longer then 15 seconds
public class DefaultRepositoryItemUidIT
    extends NexusTestSupport
{

  private RepositoryItemUidFactory factory;

  private Random random;

  @Before
  public void setUp()
      throws Exception
  {
    this.factory = lookup(RepositoryItemUidFactory.class);
    this.random = new Random(System.currentTimeMillis());
  }

  @Test
  public void testLocking() {
    final Repository repository = Mockito.mock(Repository.class);
    Mockito.when(repository.getId()).thenReturn("dummy");

    DefaultRepositoryItemUid uid = (DefaultRepositoryItemUid) factory.createUid(repository, "/some/path.txt");

    lockingSteps1(uid, null);

    verifyUidIsNotLocked(uid);
  }

  // does not stand anymore and proves nothing
  // @Test
  // public void testIllegalMonitorStateException()
  // {
  // // see NXCM-2905: this test produces the very same stack trace to be found there.
  // // While this is the actual symptom, it is not the actual problem sadly.
  // // The problem is in Factory, that hands out in some circumstances NEW
  // // instances of UIDs, while it should not do that.
  //
  // // dummy repo
  // final Repository repository = new DummyRepository( "dummy" );
  // OldDefaultRepositoryItemUid uid;
  //
  // // "simulate" we get an UID from factory
  // uid = new OldDefaultRepositoryItemUid( factory, repository, "/some/path.txt" );
  //
  // // do some locking
  // uid.lock( Action.read );
  //
  // // now tricky part, we "simulate" how factory hands us a _new_ instance instead of the existing one
  // uid = new OldDefaultRepositoryItemUid( factory, repository, "/some/path.txt" );
  //
  // try
  // {
  // // continue using it and kaboom, IllegalMonitorStateException
  // uid.lock( Action.create );
  //
  // Assert.fail( "We expect IllegalMonitorState exception!" );
  // }
  // catch ( IllegalMonitorStateException e )
  // {
  // // good
  // }
  // }

  @Test
  public void testMultiThreadedLocking()
      throws InterruptedException
  {
    final Repository repository = Mockito.mock(Repository.class);
    Mockito.when(repository.getId()).thenReturn("dummy");

    Sleeper sleeper1 = new Sleeper()
    {
      public void sleep() {
        try {
          Thread.sleep(Math.abs(random.nextLong() % 100));
        }
        catch (InterruptedException e) {
          // noop
        }
      }
    };

    Sleeper sleeper2 = new Sleeper()
    {
      public void sleep() {
        try {
          Thread.sleep(Math.abs(random.nextLong() % 75));
        }
        catch (InterruptedException e) {
          // noop
        }
      }
    };

    LockingThreadSteps1 t1 =
        new LockingThreadSteps1(factory.createUid(repository, "/some/path.txt"), 100, sleeper1);
    LockingThreadSteps2 t2 =
        new LockingThreadSteps2(factory.createUid(repository, "/some/path.txt"), 100, sleeper1);
    LockingThreadSteps3 t3 =
        new LockingThreadSteps3(factory.createUid(repository, "/some/path.txt"), 100, sleeper2);
    LockingThreadSteps1 t4 =
        new LockingThreadSteps1(factory.createUid(repository, "/some/path.txt"), 100, sleeper2);
    LockingThreadSteps1 t5 =
        new LockingThreadSteps1(factory.createUid(repository, "/some/path.txt"), 100, null);

    t1.start();
    t2.start();
    t3.start();
    t4.start();
    t5.start();

    Thread.sleep(1000);

    t1.join();
    t2.join();
    t3.join();
    t4.join();
    t5.join();

    System.out.println("done");

    // we have to have "clean" UID (not locked)
    // since they cover same key (repo + path), the lock under the hood is same, so
    // checking only one of them is actaully checking all of them
    verifyUidIsNotLocked(t1.getUid());
  }

  @Test
  public void testMultiThreadedLockingSharedUid()
      throws InterruptedException
  {
    final Repository repository = Mockito.mock(Repository.class);
    Mockito.when(repository.getId()).thenReturn("dummy");

    Sleeper sleeper1 = new Sleeper()
    {
      public void sleep() {
        try {
          Thread.sleep(Math.abs(random.nextLong() % 100));
        }
        catch (InterruptedException e) {
          // noop
        }
      }
    };

    Sleeper sleeper2 = new Sleeper()
    {
      public void sleep() {
        try {
          Thread.sleep(Math.abs(random.nextLong() % 75));
        }
        catch (InterruptedException e) {
          // noop
        }
      }
    };

    final RepositoryItemUid uid = factory.createUid(repository, "/some/path.txt");

    LockingThreadSteps1 t1 = new LockingThreadSteps1(uid, 100, sleeper1);
    LockingThreadSteps2 t2 = new LockingThreadSteps2(uid, 100, sleeper1);
    LockingThreadSteps3 t3 = new LockingThreadSteps3(uid, 100, sleeper2);
    LockingThreadSteps1 t4 = new LockingThreadSteps1(uid, 100, sleeper2);
    LockingThreadSteps1 t5 = new LockingThreadSteps1(uid, 100, null);

    t1.start();
    t2.start();
    t3.start();
    t4.start();
    t5.start();

    Thread.sleep(1000);

    t1.join();
    t2.join();
    t3.join();
    t4.join();
    t5.join();

    System.out.println("done");

    // we have to have "clean" UID (not locked)
    // since they cover same key (repo + path), the lock under the hood is same, so
    // checking only one of them is actaully checking all of them
    verifyUidIsNotLocked(t1.getUid());
  }

  // verification steps

  public void verifyUidIsNotLocked(RepositoryItemUid uid) {
    verifyUidLockCounts(uid, 0, 0);
  }

  public void verifyUidLockCounts(RepositoryItemUid uid, int wc, int rc) {
    // Warning: this trick is to present overall lock counts, and not owned by the current thread (before anyone
    // says "there is an API for this")

    String lockToString = ((DefaultRepositoryItemUidLock) uid.getLock()).getContentLock().toString();

    Assert.assertTrue("We expect " + wc + " write locks but have " + lockToString,
        lockToString.contains("[Write locks = " + wc));
    Assert.assertTrue("We expect " + rc + " read locks but have " + lockToString,
        lockToString.contains(", Read locks = " + rc + "]"));
  }

  // test steps

  /**
   * Performs read, write, read locking on passed in UID, causing one upgrade to happen. Cleanup/unlock is done also.
   * This call returns "clean" (non locked UID).
   */
  public static void lockingSteps1(RepositoryItemUid uid, Sleeper sleeper) {
    RepositoryItemUidLock uidLock = uid.getLock();

    // 1st lock is READ
    uidLock.lock(Action.read);
    sleep(sleeper);

    // 2nd lock is UPGRADE
    uidLock.lock(Action.create);
    sleep(sleeper);

    // 3rd lock is NOOP, since we already have WRITE lock but READ is requested (write is "stronger")
    uidLock.lock(Action.read);
    sleep(sleeper);

    // 3rd level unlock
    uidLock.unlock();
    sleep(sleeper);

    // 2nd level unlock
    uidLock.unlock();
    sleep(sleeper);

    // 1st level unlock
    uidLock.unlock();
    sleep(sleeper);
  }

  /**
   * Performs read locking on passed in UID, just to interfere with others wanting write. Cleanup/unlock is done
   * also.
   * This call returns "clean" (non locked UID).
   */
  public static void lockingSteps2(RepositoryItemUid uid, Sleeper sleeper) {
    RepositoryItemUidLock uidLock = uid.getLock();

    // 1st lock is READ
    uidLock.lock(Action.read);
    sleep(sleeper);

    // 1st level unlock
    uidLock.unlock();
    sleep(sleeper);
  }

  /**
   * Performs write locking on passed in UID, just to block others wanting anything. Cleanup/unlock is done also.
   * This
   * call returns "clean" (non locked UID).
   */
  public static void lockingSteps3(RepositoryItemUid uid, Sleeper sleeper) {
    RepositoryItemUidLock uidLock = uid.getLock();

    // 1st lock is WRITE
    uidLock.lock(Action.create);
    sleep(sleeper);

    // 1st level unlock
    uidLock.unlock();
    sleep(sleeper);
  }

  public static void sleep(Sleeper sleeper) {
    if (sleeper != null) {
      sleeper.sleep();
    }
  }

  // threads

  public interface Sleeper
  {

    public void sleep();
  }

  /**
   * A thread using step1 count times, hence after this thread runs, no UID lock should exist.
   */
  public static abstract class LockingThreadSteps
      extends Thread
  {

    private final RepositoryItemUid uid;

    private final int count;

    private final Sleeper sleeper;

    public LockingThreadSteps(RepositoryItemUid uid, int count, Sleeper sleeper) {
      super();

      this.uid = uid;

      this.count = count;

      this.sleeper = sleeper;
    }

    public RepositoryItemUid getUid() {
      return uid;
    }

    public void run() {
      for (int i = 0; i < count; i++) {
        doIt(i, uid, sleeper);

        if (i % 20 == 0) {
          System.out.print(".");
        }
      }
    }

    protected abstract void doIt(int count, RepositoryItemUid uid, Sleeper sleeper);
  }

  public static class LockingThreadSteps1
      extends LockingThreadSteps
  {

    public LockingThreadSteps1(RepositoryItemUid uid, int count, Sleeper sleeper) {
      super(uid, count, sleeper);
    }

    @Override
    protected void doIt(int count, RepositoryItemUid uid, Sleeper sleeper) {
      lockingSteps1(uid, sleeper);
    }
  }

  public static class LockingThreadSteps2
      extends LockingThreadSteps
  {

    public LockingThreadSteps2(RepositoryItemUid uid, int count, Sleeper sleeper) {
      super(uid, count, sleeper);
    }

    @Override
    protected void doIt(int count, RepositoryItemUid uid, Sleeper sleeper) {
      lockingSteps2(uid, sleeper);
    }
  }

  public static class LockingThreadSteps3
      extends LockingThreadSteps
  {

    public LockingThreadSteps3(RepositoryItemUid uid, int count, Sleeper sleeper) {
      super(uid, count, sleeper);
    }

    @Override
    protected void doIt(int count, RepositoryItemUid uid, Sleeper sleeper) {
      lockingSteps3(uid, sleeper);
    }
  }
}
