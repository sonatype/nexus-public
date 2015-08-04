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
package org.sonatype.nexus.proxy.repository;

import java.lang.reflect.Field;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.DefaultRepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.fs.FSPeer;
import org.sonatype.nexus.proxy.storage.remote.RemoteItemNotFoundException;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.locks.LocalResourceLockFactory;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for cleanup detection. See NEXUS-8080.
 */
public class AnAbstractProxyRepositoryRetrieveRemoteItemMethodTest
    extends TestSupport
{
  @Mock
  private RepositoryRegistry repositoryRegistry;

  @Mock
  private AbstractProxyRepository abstractRepository;

  @Mock
  private LocalRepositoryStorage localRepositoryStorage;

  @Mock
  private RemoteRepositoryStorage remoteRepositoryStorage;

  private RepositoryItemUidFactory repositoryItemUidFactory;

  private DefaultStorageFileItem file;

  private DefaultStorageCollectionItem coll;

  private RemoteItemNotFoundException rinf;

  private ResourceStoreRequest resourceStoreRequest;

  @Before
  public void prepare() throws Exception {
    // FIXME: HACK Warning, ComponentSupport uses method call on itself to init log, so in mock the log variable is null
    // FIXME: maybe move this code into own "helper" class a la Spring's ReflectionTestUtils
    Field logField = AbstractRepository.class.getSuperclass().getSuperclass().getSuperclass().getSuperclass()
        .getDeclaredField("log");
    logField.setAccessible(true);
    logField.set(abstractRepository, LoggerFactory.getLogger(AbstractRepository.class));

    doCallRealMethod().when(abstractRepository).doRetrieveRemoteItem(any(ResourceStoreRequest.class));
    doReturn("test").when(abstractRepository).getId();
    doReturn("test").when(abstractRepository).getName();
    doReturn(repositoryItemUidFactory).when(abstractRepository).getRepositoryItemUidFactory();
    doReturn(localRepositoryStorage).when(abstractRepository).getLocalStorage();
    doReturn(remoteRepositoryStorage).when(abstractRepository).getRemoteStorage();
    doReturn(ImmutableList.of("http://repo1.maven.org/maven2/")).when(abstractRepository)
        .getRemoteUrls(any(ResourceStoreRequest.class));

    when(repositoryRegistry.getRepository(anyString())).thenReturn(abstractRepository);

    this.repositoryItemUidFactory = new DefaultRepositoryItemUidFactory(mock(EventBus.class), repositoryRegistry,
        new LocalResourceLockFactory());
    when(abstractRepository.createUid(anyString())).thenAnswer(new Answer<RepositoryItemUid>()
    {
      @Override
      public RepositoryItemUid answer(final InvocationOnMock invocationOnMock) throws Throwable {
        return repositoryItemUidFactory.createUid(abstractRepository, (String) invocationOnMock.getArguments()[0]);
      }
    });

    this.file = new DefaultStorageFileItem(abstractRepository, new ResourceStoreRequest("/foo"), true, true,
        new StringContentLocator("irrelevant"));
    this.coll = new DefaultStorageCollectionItem(abstractRepository, new ResourceStoreRequest("/foo"), true, true);

    this.rinf = new RemoteItemNotFoundException(new ResourceStoreRequest("/foo"), abstractRepository, "nan", "nan");
    this.resourceStoreRequest = new ResourceStoreRequest("/foo");
  }

  @Test
  public void fileRequestedFoundRemotelyButFailsCaching() throws Exception {
    when(remoteRepositoryStorage
        .retrieveItem(any(ProxyRepository.class), any(ResourceStoreRequest.class), anyString())).thenReturn(
        file);
    doThrow(new LocalStorageException("unimportant")).when(localRepositoryStorage).storeItem(any(Repository.class),
        any(StorageItem.class));
    when(localRepositoryStorage.retrieveItem(any(Repository.class), any(ResourceStoreRequest.class))).thenReturn(file);
    try {
      abstractRepository.doRetrieveRemoteItem(resourceStoreRequest);
    } catch (ItemNotFoundException e) {
      // we expect this
    }
    // we need cleanup: file was returned and caching was attempted
    verify(localRepositoryStorage, times(1)).deleteItem(any(Repository.class), any(ResourceStoreRequest.class));
  }

  @Test
  public void directoryRequestedFoundRemotelyButFailsCaching() throws Exception {
    when(remoteRepositoryStorage.retrieveItem(any(ProxyRepository.class), any(ResourceStoreRequest.class), anyString())).thenReturn(
        file);
    doThrow(new LocalStorageException("unimportant")).when(localRepositoryStorage).storeItem(any(Repository.class),
        any(StorageItem.class));
    when(localRepositoryStorage.retrieveItem(any(Repository.class), any(ResourceStoreRequest.class))).thenReturn(coll);
    try {
      abstractRepository.doRetrieveRemoteItem(resourceStoreRequest);
    } catch (ItemNotFoundException e) {
      // we expect this
    }
    // we do not need cleanup: file was returned and caching was attempted but LS has coll
    verify(localRepositoryStorage, times(0)).deleteItem(any(Repository.class), any(ResourceStoreRequest.class));
  }

  @Test
  public void fileRequestedNotFoundRemotely() throws Exception {
    // remote throws RemoteItemNotFoundEx
    when(remoteRepositoryStorage.retrieveItem(any(ProxyRepository.class), any(ResourceStoreRequest.class), anyString()))
        .thenThrow(rinf);

    when(localRepositoryStorage.retrieveItem(any(Repository.class), any(ResourceStoreRequest.class))).thenReturn(file);
    try {
      abstractRepository.doRetrieveRemoteItem(resourceStoreRequest);
    } catch (RemoteItemNotFoundException e) {
      // we expect this
    }
    // we do not need cleanup: no caching was attempted
    verify(localRepositoryStorage, times(0)).deleteItem(any(Repository.class), any(ResourceStoreRequest.class));
  }

  @Test
  public void directoryRequestedNotFoundRemotely() throws Exception {
    // remote throws RemoteItemNotFoundEx
    when(remoteRepositoryStorage.retrieveItem(any(ProxyRepository.class), any(ResourceStoreRequest.class), anyString()))
        .thenThrow(rinf);

    when(localRepositoryStorage.retrieveItem(any(Repository.class), any(ResourceStoreRequest.class))).thenReturn(coll);
    try {
      abstractRepository.doRetrieveRemoteItem(resourceStoreRequest);
    } catch (RemoteItemNotFoundException e) {
      // we expect this
    }
    // we do not need cleanup: no caching was attempted
    verify(localRepositoryStorage, times(0)).deleteItem(any(Repository.class), any(ResourceStoreRequest.class));
  }

  @Test
  public void fileRequestedForceRemoteNotFoundRemotely() throws Exception {
    // remote throws RemoteItemNotFoundEx
    when(remoteRepositoryStorage.retrieveItem(any(ProxyRepository.class), any(ResourceStoreRequest.class), anyString()))
        .thenThrow(rinf);

    when(localRepositoryStorage.retrieveItem(any(Repository.class), any(ResourceStoreRequest.class))).thenReturn(file);
    try {
      resourceStoreRequest.setRequestRemoteOnly(true);
      abstractRepository.doRetrieveRemoteItem(resourceStoreRequest);
    } catch (RemoteItemNotFoundException e) {
      // we expect this
    }
    // we do need cleanup
    verify(localRepositoryStorage, times(1)).deleteItem(any(Repository.class), any(ResourceStoreRequest.class));
  }

  @Test
  public void directoryRequestedForceRemoteNotFoundRemotely() throws Exception {
    // remote throws RemoteItemNotFoundEx
    when(remoteRepositoryStorage.retrieveItem(any(ProxyRepository.class), any(ResourceStoreRequest.class), anyString()))
        .thenThrow(rinf);

    when(localRepositoryStorage.retrieveItem(any(Repository.class), any(ResourceStoreRequest.class))).thenReturn(coll);
    try {
      resourceStoreRequest.setRequestRemoteOnly(true);
      abstractRepository.doRetrieveRemoteItem(resourceStoreRequest);
    } catch (RemoteItemNotFoundException e) {
      // we expect this
    }
    // we do not need cleanup: no caching was attempted, and LS has coll
    verify(localRepositoryStorage, times(0)).deleteItem(any(Repository.class), any(ResourceStoreRequest.class));
  }

}
