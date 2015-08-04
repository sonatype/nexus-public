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

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.remote.AbstractRemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

public class DeleteRepositoryWhileDownloadingIndexIT
    extends AbstractIndexerManagerTest
{
  private Semaphore semaphore = new Semaphore(1);

  private final class StuckRemoteStorage
      extends AbstractRemoteRepositoryStorage
      implements RemoteRepositoryStorage
  {
    public volatile boolean interrupted;

    protected StuckRemoteStorage()
        throws Exception
    {
      super(lookup(ApplicationStatusSource.class), lookup(MimeSupport.class));
    }

    @Override
    public AbstractStorageItem retrieveItem(ProxyRepository repository, ResourceStoreRequest request,
                                            String baseUrl)
        throws ItemNotFoundException, RemoteAccessException, RemoteStorageException
    {
      try {
        semaphore.release();
        Thread.sleep(120 * 1000L);
      }
      catch (InterruptedException e) {
        interrupted = true;
      }
      throw new UnsupportedOperationException();
    }

    @Override
    public String getProviderId() {
      return "test";
    }

    @Override
    public boolean isReachable(ProxyRepository repository, ResourceStoreRequest request)
        throws RemoteAccessException, RemoteStorageException
    {
      return true;
    }

    @Override
    public void validateStorageUrl(String url)
        throws RemoteStorageException
    {
    }

    @Override
    public boolean containsItem(long newerThen, ProxyRepository repository, ResourceStoreRequest request)
        throws RemoteAccessException, RemoteStorageException
    {
      return true;
    }

    @Override
    public void storeItem(ProxyRepository repository, StorageItem item)
        throws UnsupportedStorageOperationException, RemoteAccessException, RemoteStorageException
    {
    }

    @Override
    public void deleteItem(ProxyRepository repository, ResourceStoreRequest request)
        throws ItemNotFoundException, UnsupportedStorageOperationException, RemoteAccessException,
               RemoteStorageException
    {
    }

    @Override
    protected void updateContext(ProxyRepository repository, RemoteStorageContext context)
        throws RemoteStorageException
    {
    }

  }

  @Test
  public void testDeleteRepository()
      throws Exception
  {
        /*
         * The point of this test is to verify that repository indexing context can be removed when there is remote
         * index download in progress for the repository.
         */

    semaphore.acquire();

    central.setDownloadRemoteIndexes(true);
    central.setRemoteUrl("http://localhost:" + 12345);
    central.setRepositoryPolicy(RepositoryPolicy.SNAPSHOT);

    nexusConfiguration().saveConfiguration();

    // central.setRemoteStorage( new StuckRemoteStorage() );
    StuckRemoteStorage stuckRemoteStorage = new StuckRemoteStorage();
    Whitebox.setInternalState(central, "remoteStorage", stuckRemoteStorage);

    Assert.assertTrue(central.getRemoteStorage() instanceof StuckRemoteStorage);

    // wait until download is stuck
    semaphore.acquire();

    // this is expected to interrupt index download thread and remove the indexing context
    indexerManager.removeRepositoryIndexContext(central, true);

    Assert.assertNull(indexerManager.getRepositoryIndexContext(central));
    Assert.assertTrue(stuckRemoteStorage.interrupted);
  }
}
