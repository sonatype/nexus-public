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
package org.sonatype.nexus.repository.internal.blobstore

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService

import com.google.common.collect.Lists
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.fail
import static org.mockito.ArgumentCaptor.forClass
import static org.mockito.Matchers.any
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME

/**
 * Tests for {@link BlobStoreManagerImpl}.
 */
class BlobStoreManagerImplTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder()

  @Mock
  EventManager eventManager

  @Mock
  BlobStoreConfigurationStore store
  
  @Mock
  Provider<BlobStore> provider

  @Mock
  DatabaseFreezeService databaseFreezeService

  @Mock
  NodeAccess nodeAccess

  BlobStoreManagerImpl underTest

  @Before
  void setup() {
    underTest = newBlobStoreManager()
  }

  private BlobStoreManagerImpl newBlobStoreManager(boolean provisionDefaults = false) {
    spy(new BlobStoreManagerImpl(eventManager, store, [test: provider, File: provider],
        databaseFreezeService, nodeAccess, provisionDefaults))
  }

  @Test
  void 'Can start with nothing configured'() {
    ArgumentCaptor<BlobStoreConfiguration> configurationArgumentCaptor = forClass(BlobStoreConfiguration.class)
    when(store.list()).thenReturn(Lists.newArrayList())
    underTest.doStart()
    assert !underTest.browse()

    verify(store).create(configurationArgumentCaptor.capture())
    assertThat(configurationArgumentCaptor.getValue().name, Matchers.is(DEFAULT_BLOBSTORE_NAME))
  }

  @Test
  void 'Can start with nothing configured and does not create default when clustered'() {

    when(nodeAccess.isClustered()).thenReturn(true)
    when(store.list()).thenReturn(Lists.newArrayList())
    underTest.doStart()

    verify(store, times(0)).create(any(BlobStoreConfiguration.class))
  }

  @Test
  void 'Can start with nothing configured and does create default when clustered if provisionDefaults is true'() {
    underTest = newBlobStoreManager(true)

    ArgumentCaptor<BlobStoreConfiguration> configurationArgumentCaptor = forClass(BlobStoreConfiguration.class)
    when(nodeAccess.isClustered()).thenReturn(true)

    when(store.list()).thenReturn(Lists.newArrayList())
    underTest.doStart()

    verify(store).create(configurationArgumentCaptor.capture())
    assertThat(configurationArgumentCaptor.getValue().name, Matchers.is(DEFAULT_BLOBSTORE_NAME))
  }

  @Test
  void 'Can start with existing configuration'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    when(store.list()).thenReturn(Lists.newArrayList(createConfig('test')))

    underTest.doStart()

    assert underTest.browse().toList() == [blobStore]
  }

  @Test
  void 'Name can be duplicate regardless of case'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    when(store.list()).thenReturn(Lists.newArrayList(createConfig('test')))

    underTest.doStart()

    assert !underTest.exists('unique')
    assert underTest.exists('test')
    assert underTest.exists('TEST')
  }

  @Test
  void 'Can create a BlobStore'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    BlobStoreConfiguration configuration = createConfig('test')
    
    BlobStore createdBlobStore = underTest.create(configuration)

    assert createdBlobStore == blobStore
    verify(store).create(configuration)
    verify(blobStore).start()
    assert underTest.browse().toList() == [blobStore]
    assert underTest.get('test') == blobStore
  }

  @Test
  void 'Can delete an existing BlobStore'() {
    BlobStoreConfiguration configuration = createConfig('test')
    BlobStore blobStore = mock(BlobStore)
    doReturn(blobStore).when(underTest).blobStore('test')
    when(store.list()).thenReturn([configuration])
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration)
    
    underTest.delete(configuration.getName())
    
    verify(blobStore).stop()
    verify(store).delete(configuration)
    verify(databaseFreezeService).checkUnfrozen("Unable to delete a BlobStore while database is frozen.")
  }

  @Test
  void 'All BlobStores are stopped with the manager is stopped'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    BlobStoreConfiguration configuration = createConfig('test')
    underTest.create(configuration)
    
    underTest.stop()
    
    verify(blobStore).stop()
  }

  @Test
  void 'Blob store not created for invalid configuration'() {
    when(provider.get()).thenThrow(new IllegalArgumentException())

    BlobStoreConfiguration configuration = createConfig('test')

    try {
      underTest.create(configuration)
      fail()
    }
    catch (Exception e) {
      // expected
    }

    assert underTest.browse().toList() == []
  }

  @Test
  void 'Can successfullly create new blob stores concurrently'() {
    // avoid newBlobStoreManager method because it returns a spy that throws NPE accessing the stores field
    underTest = new BlobStoreManagerImpl(eventManager, store, [test: provider, File: provider],
        databaseFreezeService, nodeAccess, true)

    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)

    underTest.create(createConfig(name: 'concurrency-test-1'))
    underTest.create(createConfig(name: 'concurrency-test-2'))

    // simulate concurrent access by opening an iterator on the internal stores
    def storesIterator = underTest.stores.entrySet().iterator()
    storesIterator.next()

    underTest.create(createConfig(name: 'concurrency-test-3'))
    // this method will throw ConcurrentModificationException if the internal store isn't thread-safe
    storesIterator.next()
  }

  private BlobStoreConfiguration createConfig(name = 'foo', type = 'test', attributes = [file:[path:'baz']]) {
    def entity = new BlobStoreConfiguration(
        name: name,
        type: type,
        attributes: attributes
    )
    return entity
  }
}
