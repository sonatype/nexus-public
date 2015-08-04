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

import org.sonatype.nexus.proxy.AbstractNexusTestEnvironment;
import org.sonatype.nexus.proxy.repository.Repository;

import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;

public class RepositoryItemUidTest
    extends AbstractNexusTestEnvironment
{

  @Mock
  protected Repository repository;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    doReturn("dummy").when(repository).getId();
  }

  @Test
  public void testKey() {
    DefaultRepositoryItemUidFactory factory = (DefaultRepositoryItemUidFactory) getRepositoryItemUidFactory();

    RepositoryItemUid uid = factory.createUid(repository, "/a.txt");

    Assert.assertEquals(repository.getId() + ":/a.txt", uid.getKey());
  }

  @Test
  public void testEquality() {
    DefaultRepositoryItemUidFactory factory = (DefaultRepositoryItemUidFactory) getRepositoryItemUidFactory();

    RepositoryItemUid uid1 = factory.createUid(repository, "/a.txt");

    RepositoryItemUid uid2 = factory.createUid(repository, "/a.txt");

    Assert.assertNotSame(uid1, uid2);

    Assert.assertEquals(uid1, uid2);

    Assert.assertEquals(uid1.hashCode(), uid2.hashCode());
  }

    /*
    @Test
    public void testReleaseFromMemory()
        throws Exception
    {
        DefaultRepositoryItemUidFactory factory = (DefaultRepositoryItemUidFactory) getRepositoryItemUidFactory();

        RepositoryItemUid uid = factory.createUid( repository, "/a.txt" );
        RepositoryItemUid uid2 = factory.createUid( repository, "/a.txt" );
        RepositoryItemUid uid3 = factory.createUid( repository, "/b.txt" );

        // Proof that create isn't putting anything in the internal maps
        assertEquals( 2, factory.getUidCount( true ) );

        uid.lock( Action.create );

        // Proof that locking a uid adds it to internal maps
        assertEquals( 2, factory.getUidCount( true ) );

        uid2.lock( Action.create );

        // Proof that locking 2 uids w/ the same item does not increase the internal map count
        assertEquals( 2, factory.getUidCount( true ) );

        uid3.lock( Action.create );

        // Proof that using a different uid creates a new item in internal map
        assertEquals( 2, factory.getUidCount( true ) );

        uid3.unlock();

        // Proof that removing an item updates internal maps
        assertEquals( 2, factory.getUidCount( true ) );

        uid2.unlock();

        // Proof that removing an item that was added twice, doesn't remove the whole list
        assertEquals( 2, factory.getUidCount( true ) );

        uid.unlock();

        uid = null;
        uid2 = null;
        uid3 = null;

        System.gc();

        // Proof that removing the final item (if added more than once) removes whole list
        assertEquals( 0, factory.getUidCount( true ) );
    }

    @Test
    public void testConcurrentLocksOfSameUid()
        throws Exception
    {
        RepositoryItemUidFactory factory = getRepositoryItemUidFactory();

        RepositoryItemUid uidA = factory.createUid( repository, "/a.txt" );

        Thread thread1 = new Thread( new RepositoryItemUidLockProcessLauncher( factory, uidA, 100, 100 ) );
        Thread thread2 = new Thread( new RepositoryItemUidLockProcessLauncher( factory, uidA, 100, 100 ) );

        thread1.start();
        thread2.start();

        Thread.sleep( 50 );

        thread1.join();
        thread2.join();

        uidA = null;
        System.gc();

        assertEquals( 0, ( (DefaultRepositoryItemUidFactory) getRepositoryItemUidFactory() ).getUidCount( true ) );
    }

    private static final class RepositoryItemUidLockProcessLauncher
        implements Runnable
    {
        private RepositoryItemUidFactory repositoryItemUidFactory;

        private RepositoryItemUid uid;

        private int threadCount;

        private long timeout;

        public RepositoryItemUidLockProcessLauncher( RepositoryItemUidFactory repositoryItemUidFactory,
            RepositoryItemUid uid, int threadCount, long timeout )
        {
            this.uid = uid;
            this.threadCount = threadCount;
            this.timeout = timeout;
            this.repositoryItemUidFactory = repositoryItemUidFactory;
        }

        public void run()
        {
            ArrayList<Thread> threads = new ArrayList<Thread>();

            for ( int i = 0; i < threadCount; i++ )
            {
                threads.add( new Thread( new RepositoryItemUidLockProcess( uid, timeout ) ) );
            }

            for ( Iterator<Thread> iter = threads.iterator(); iter.hasNext(); )
            {
                iter.next().start();
            }

            try
            {
                Thread.sleep( 5 );

                for ( Iterator<Thread> iter = threads.iterator(); iter.hasNext(); )
                {
                    iter.next().join();
                }
            }
            catch ( InterruptedException e )
            {
            }
        }
    }

    private static final class RepositoryItemUidLockProcess
        implements Runnable
    {
        private RepositoryItemUid uid;

        private long timeout;

        public RepositoryItemUidLockProcess( RepositoryItemUid uid, long timeout )
        {
            this.uid = uid;
            this.timeout = timeout;
        }

        public void run()
        {
            uid.lock( Action.create );

            try
            {
                Thread.sleep( timeout );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            finally
            {
                uid.unlock();
            }
        }
    }
    */
}
