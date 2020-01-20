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
package org.sonatype.nexus.datastore.internal

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.datastore.DataStoreConfigurationManager
import org.sonatype.nexus.datastore.DataStoreDescriptor
import org.sonatype.nexus.datastore.DataStoreRestorer
import org.sonatype.nexus.datastore.DataStoreSupport
import org.sonatype.nexus.datastore.DataStoreUsageChecker
import org.sonatype.nexus.datastore.api.DataStore
import org.sonatype.nexus.datastore.api.DataStoreConfiguration

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.eclipse.sisu.inject.BeanLocator
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.junit.Assert.fail
import static org.mockito.Mockito.CALLS_REAL_METHODS
import static org.mockito.Mockito.any
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.inOrder
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Tests for {@link DataStoreManagerImpl}.
 */
class DataStoreManagerImplTest
    extends TestSupport
{
  @Mock
  DataStoreDescriptor descriptorTest

  @Mock
  DataStoreDescriptor descriptorJdbc

  @Mock
  Provider<DataStore<?>> prototypeTest

  @Mock
  Provider<DataStore<?>> prototypeJdbc

  @Mock
  DataStoreConfigurationManager configurationManager

  @Mock
  DataStoreUsageChecker dataStoreUsageChecker

  @Mock
  DataStoreRestorer restorer

  @Mock
  BeanLocator beanLocator

  DataStoreManagerImpl underTest

  @Before
  void setup() {
    when(descriptorTest.enabled).thenReturn(true)
    when(descriptorJdbc.enabled).thenReturn(true)

    when(prototypeTest.get()).thenAnswer({ protoDataStore() })
    when(prototypeJdbc.get()).thenAnswer({ protoDataStore() })

    when(configurationManager.load()).thenReturn(ImmutableList.of())

    underTest = new DataStoreManagerImpl(true,
        [test: descriptorTest, jdbc: descriptorJdbc],
        [test: prototypeTest, jdbc: prototypeJdbc],
        configurationManager,
        { -> dataStoreUsageChecker } as Provider,
        restorer,
        beanLocator)

    underTest.start()
  }

  @After
  void teardown() {
    underTest.stop()
  }

  private DataStore protoDataStore() {
    def mock = mock(DataStoreSupport)
    when(mock.getConfiguration()).thenAnswer(CALLS_REAL_METHODS)
    doAnswer(CALLS_REAL_METHODS).when(mock).setConfiguration(any())
    return mock
  }

  private DataStoreConfiguration newDataStoreConfiguration(String name) {
    def config = new DataStoreConfiguration()
    config.setName(name)
    config.setType('test')
    config.setSource('local')
    config.setAttributes([:])
    return config
  }

  void 'Data store not created for invalid configuration'() {
    when(descriptorTest.validate(any())).thenThrow(IllegalArgumentException)

    try {
      underTest.create(newDataStoreConfiguration('customStore'))
      fail('Expected IllegalStateException')
    }
    catch (IllegalStateException e) {
      // expected
    }

    verify(configurationManager, times(0)).save(any())
  }

  @Test
  void 'Data store names must be unique regardless of case'() {
    def config = newDataStoreConfiguration('customStore')
    def store = underTest.create(config)

    assert !underTest.exists('custom_store')
    assert underTest.exists('customStore')
    assert underTest.exists('CUSTOMSTORE')

    verify(store, times(1)).start()

    verify(configurationManager, times(1)).save(config)
  }

  @Test
  void 'Duplicate data store cannot be created'() {
    def config = newDataStoreConfiguration('duplicate')
    def store = underTest.create(config)

    try {
      underTest.create(newDataStoreConfiguration('duplicate'))
      fail('Expected IllegalStateException')
    }
    catch (IllegalStateException e) {
      // expected
    }

    verify(store, times(1)).start()

    verify(configurationManager, times(1)).save(config)
  }

  @Test
  void 'In use data store cannot be deleted'() {
    def usedConfig = newDataStoreConfiguration('used')
    def usedStore = underTest.create(usedConfig)
    def unusedConfig = newDataStoreConfiguration('unused')
    def unusedStore = underTest.create(unusedConfig)

    when(dataStoreUsageChecker.isDataStoreUsed('used')).thenReturn(true)
    when(dataStoreUsageChecker.isDataStoreUsed('unused')).thenReturn(false)

    underTest.delete('unused')
    try {
      underTest.delete('used')
      fail('Expected IllegalStateException')
    }
    catch (IllegalStateException e) {
      // expected
    }

    verify(usedStore, times(1)).start()
    verify(configurationManager, times(1)).save(usedConfig)
    verify(configurationManager, times(0)).delete(usedConfig)
    verify(usedStore, times(0)).shutdown()

    verify(unusedStore, times(1)).start()
    verify(configurationManager, times(1)).save(unusedConfig)
    verify(configurationManager, times(1)).delete(unusedConfig)
    verify(unusedStore, times(1)).shutdown()
  }

  @Test
  void 'Data stores are started/shutdown when manager starts/shutsdown'() {

    def testStore = underTest.get('test')
    def exampleStore = underTest.get('example')

    underTest.stop()

    assert testStore.empty()
    assert exampleStore.empty()

    def testConfig = newDataStoreConfiguration('test')
    def exampleConfig = newDataStoreConfiguration('example')

    when(configurationManager.load()).thenReturn(ImmutableList.of(testConfig, exampleConfig))

    underTest.start()
    testStore = underTest.get('test')
    exampleStore = underTest.get('example')
    underTest.stop()

    verify(testStore.get(), times(1)).start()
    verify(configurationManager, times(1)).save(testConfig)
    verify(configurationManager, times(0)).delete(testConfig)
    verify(testStore.get(), times(1)).shutdown()

    verify(exampleStore.get(), times(1)).start()
    verify(configurationManager, times(1)).save(exampleConfig)
    verify(configurationManager, times(0)).delete(exampleConfig)
    verify(exampleStore.get(), times(1)).shutdown()

    when(configurationManager.load()).thenReturn(ImmutableList.of(exampleConfig))

    underTest.start()
    testStore = underTest.get('test')
    exampleStore = underTest.get('example')
    underTest.stop()

    assert testStore.empty()

    verify(exampleStore.get(), times(1)).start()
    verify(configurationManager, times(2)).save(exampleConfig)
    verify(configurationManager, times(0)).delete(exampleConfig)
    verify(exampleStore.get(), times(1)).shutdown()

    when(configurationManager.load()).thenReturn(ImmutableList.of())

    underTest.start()
    testStore = underTest.get('test')
    exampleStore = underTest.get('example')
    underTest.stop()

    assert testStore.empty()
    assert exampleStore.empty()
  }

  @Test
  void 'Updating configuration restarts data store'() {
    def firstConfig = newDataStoreConfiguration('test')
    firstConfig.attributes = ImmutableMap.of('version', 'first')
    def secondConfig = newDataStoreConfiguration('test')
    secondConfig.attributes = ImmutableMap.of('version', 'second')

    def store = underTest.create(firstConfig)

    def expected = inOrder(store, configurationManager)

    expected.verify(configurationManager, times(1)).save(firstConfig)
    expected.verify(store, times(1)).setConfiguration(firstConfig)
    expected.verify(store, times(1)).start()
    when(store.isStarted()).thenReturn(true)

    underTest.update(secondConfig)

    expected.verify(configurationManager, times(1)).save(secondConfig)
    expected.verify(store, times(1)).stop()
    expected.verify(store, times(1)).setConfiguration(secondConfig)
    expected.verify(store, times(1)).start()

    expected.verifyNoMoreInteractions()
  }

  @Test
  void 'Invalid configuration update is not used'() {
    def goodConfig = newDataStoreConfiguration('test')
    goodConfig.attributes = ImmutableMap.of('version', 'first')
    def invalidConfig = newDataStoreConfiguration('test')
    invalidConfig.attributes = ImmutableMap.of('version', 'invalid')

    when(descriptorTest.validate(invalidConfig)).thenThrow(IllegalArgumentException)

    def store = underTest.create(goodConfig)

    def expected = inOrder(store, configurationManager)

    expected.verify(configurationManager, times(1)).save(goodConfig)
    expected.verify(store, times(1)).setConfiguration(goodConfig)
    expected.verify(store, times(1)).start()
    when(store.isStarted()).thenReturn(true)

    try {
      underTest.update(invalidConfig)
      fail('Expected IllegalArgumentException')
    }
    catch (IllegalArgumentException e) {
      // expected
    }

    expected.verifyNoMoreInteractions()
  }

  @Test
  void 'Valid but problematic configuration update is reverted'() {
    def goodConfig = newDataStoreConfiguration('test')
    goodConfig.attributes = ImmutableMap.of('version', 'first')
    def badConfig = newDataStoreConfiguration('test')
    badConfig.attributes = ImmutableMap.of('version', 'bad')

    def store = underTest.create(goodConfig)

    def expected = inOrder(store, configurationManager)

    expected.verify(configurationManager, times(1)).save(goodConfig)
    expected.verify(store, times(1)).setConfiguration(goodConfig)
    expected.verify(store, times(1)).start()

    when(store.isStarted()).thenReturn(true)
    doThrow(new IOException()).when(store).start()

    try {
      underTest.update(badConfig)
      fail('Expected IOException')
    }
    catch (IOException e) {
      // expected
    }

    expected.verify(configurationManager, times(1)).save(badConfig)
    expected.verify(store, times(1)).stop()
    expected.verify(store, times(1)).setConfiguration(badConfig)
    expected.verify(store, times(1)).start()
    expected.verify(configurationManager, times(1)).save(goodConfig)
    expected.verify(store, times(1)).stop()
    expected.verify(store, times(1)).setConfiguration(goodConfig)
    expected.verify(store, times(1)).start()

    expected.verifyNoMoreInteractions()
  }
}
