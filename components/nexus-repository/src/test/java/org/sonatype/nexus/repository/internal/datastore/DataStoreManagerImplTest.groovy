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
package org.sonatype.nexus.repository.internal.datastore

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.datastore.DataStoreDescriptor
import org.sonatype.nexus.datastore.api.DataStore
import org.sonatype.nexus.datastore.api.DataStoreConfiguration
import org.sonatype.nexus.repository.manager.RepositoryManager

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.junit.Assert.fail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.when

/**
 * Tests for {@link DataStoreManagerImpl}.
 */
class DataStoreManagerImplTest
    extends TestSupport
{
  @Mock
  DataStoreDescriptor descriptor

  @Mock
  Provider<DataStore<?>> prototype

  @Mock
  RepositoryManager repositoryManager

  @Mock
  NodeAccess nodeAccess

  DataStoreManagerImpl underTest

  @Before
  void setup() {
    when(descriptor.enabled).thenReturn(true)
    when(prototype.get()).thenReturn(mock(DataStore))
    underTest = newDataStoreManager()
    underTest.start()
  }

  @After
  void teardown() {
    underTest.stop()
  }

  private DataStoreManagerImpl newDataStoreManager(Boolean provisionDefaults = null) {
    spy(new DataStoreManagerImpl([test: descriptor, jdbc: descriptor], [test: prototype, jdbc: prototype],
        { -> repositoryManager } as Provider, nodeAccess, provisionDefaults))
  }

  private DataStoreConfiguration newDataStoreConfiguration(String name) {
    DataStoreConfiguration config = new DataStoreConfiguration()
    config.setName(name)
    config.setType('test')
    return config
  }

  void 'Data store not created for invalid configuration'() {
    when(descriptor.validate(any())).thenThrow(IllegalArgumentException)

    try {
      underTest.create(newDataStoreConfiguration('customStore'))
      fail('Expected IllegalStateException')
    }
    catch (IllegalStateException e) {
      // expected
    }

  }

  @Test
  void 'Data store names must be unique regardless of case'() {
    underTest.create(newDataStoreConfiguration('customStore'))

    assert !underTest.exists('custom_store')
    assert underTest.exists('customStore')
    assert underTest.exists('CUSTOMSTORE')
  }

  void 'In use data store cannot be deleted'() {
    underTest.create(newDataStoreConfiguration('used'))
    underTest.create(newDataStoreConfiguration('unused'))

    when(repositoryManager.isDataStoreUsed('used')).thenReturn(true)
    when(repositoryManager.isDataStoreUsed('unused')).thenReturn(false)

    underTest.delete('unused')
    try {
      underTest.delete('used')
      fail('Expected IllegalStateException')
    }
    catch (IllegalStateException e) {
      // expected
    }
  }
}
