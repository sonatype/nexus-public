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
package org.sonatype.nexus.datastore.internal;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Provider;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.DataStoreConfigurationManager;
import org.sonatype.nexus.datastore.DataStoreDescriptor;
import org.sonatype.nexus.datastore.DataStoreRestorer;
import org.sonatype.nexus.datastore.DataStoreSupport;
import org.sonatype.nexus.datastore.DataStoreUsageChecker;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DataStoreManagerImplTest
{
  @Mock
  private DataStoreDescriptor descriptorTest;

  @Mock
  private DataStoreDescriptor descriptorJdbc;

  @Mock
  private Provider<DataStore<?>> prototypeJdbc;

  @Mock
  private DataStoreConfigurationManager configurationManager;

  @Mock
  private DataStoreUsageChecker dataStoreUsageChecker;

  @Mock
  private DataStoreRestorer restorer;

  @Mock
  private EventManager eventManager;

  @Mock
  private ApplicationContext applicationContext;

  private MockedStatic<QualifierUtil> mockedStatic;

  private DataStoreManagerImpl underTest;

  @Before
  public void setup() throws Exception {
    mockedStatic = mockStatic(QualifierUtil.class);
    List<DataStoreDescriptor> descriptorList = mock(List.class);
    when(QualifierUtil.buildQualifierBeanMap(descriptorList))
        .thenReturn(Map.of("jdbc", descriptorJdbc));

    when(descriptorTest.isEnabled()).thenReturn(true);
    when(descriptorJdbc.isEnabled()).thenReturn(true);

    when(prototypeJdbc.get()).thenAnswer(invocation -> mockDataStore());

    when(configurationManager.load()).thenReturn(List.of());

    underTest = new DataStoreManagerImpl(eventManager, descriptorList, prototypeJdbc, configurationManager,
        () -> dataStoreUsageChecker, restorer);
    underTest.start();
    underTest.setApplicationContext(applicationContext);
  }

  @After
  public void tearDown() throws Exception {
    mockedStatic.close();
    underTest.stop();
  }

  @Test
  public void dataStoreNotCreatedForInvalidConfiguration() {
    doThrow(IllegalArgumentException.class).when(descriptorJdbc).validate(any());
    DataStoreConfiguration config = newDataStoreConfiguration("testStore");
    assertThrows(IllegalArgumentException.class, () -> underTest.create(config));
    verify(configurationManager, never()).save(any());
  }

  @Test
  public void testDataStoreNamesMustBeUniqueRegardlessOfCase() throws Exception {
    DataStoreConfiguration config = newDataStoreConfiguration("customStore");
    DataStore<?> store = underTest.create(config);

    assertThat(underTest.exists("custom_store"), is(false));
    assertThat(underTest.exists("customStore"), is(true));
    assertThat(underTest.exists("CUSTOMSTORE"), is(true));

    verify(store).start();
    verify(configurationManager).save(config);
  }

  @Test
  public void testDuplicateDataStoreCannotBeCreated() throws Exception {
    DataStoreConfiguration config = newDataStoreConfiguration("duplicate");
    DataStore<?> store = underTest.create(config);

    DataStoreConfiguration dupe = newDataStoreConfiguration("duplicate");
    assertThrows(IllegalStateException.class, () -> underTest.create(dupe));
    verify(store).start();
    verify(configurationManager).save(config);
  }

  @Test
  public void testInUseDataStoreCannotBeDeleted() throws Exception {
    DataStoreConfiguration usedConfig = newDataStoreConfiguration("used");
    DataStore<?> usedStore = underTest.create(usedConfig);
    DataStoreConfiguration unusedConfig = newDataStoreConfiguration("unused");
    DataStore<?> unusedStore = underTest.create(unusedConfig);

    when(dataStoreUsageChecker.isDataStoreUsed("used")).thenReturn(true);
    when(dataStoreUsageChecker.isDataStoreUsed("unused")).thenReturn(false);

    underTest.delete("unused");

    assertThrows(IllegalStateException.class, () -> underTest.delete("used"));
    verify(usedStore).start();
    verify(configurationManager).save(usedConfig);
    verify(configurationManager, never()).delete(usedConfig);
    verify(usedStore, never()).shutdown();

    verify(unusedStore).start();
    verify(configurationManager).save(unusedConfig);
    verify(configurationManager).delete(unusedConfig);
    verify(unusedStore).shutdown();
  }

  @Test
  public void testDataStoresAreStartedOrShutdownWhenManagerStartsOrShutsDown() throws Exception {
    Optional<DataStore<?>> testStore = underTest.get("test");
    Optional<DataStore<?>> exampleStore = underTest.get("example");

    underTest.stop();

    assertThat(testStore.isPresent(), is(false));
    assertThat(exampleStore.isPresent(), is(false));

    DataStoreConfiguration testConfig = newDataStoreConfiguration("test");
    DataStoreConfiguration exampleConfig = newDataStoreConfiguration("example");

    when(configurationManager.load()).thenReturn(List.of(testConfig, exampleConfig));

    underTest.start();
    testStore = underTest.get("test");
    exampleStore = underTest.get("example");
    underTest.stop();

    verify(testStore.get()).start();
    verify(configurationManager).save(testConfig);
    verify(configurationManager, never()).delete(testConfig);
    verify(testStore.get()).shutdown();

    verify(exampleStore.get()).start();
    verify(configurationManager).save(exampleConfig);
    verify(configurationManager, never()).delete(exampleConfig);
    verify(exampleStore.get()).shutdown();

    when(configurationManager.load()).thenReturn(List.of(exampleConfig));

    underTest.start();
    testStore = underTest.get("test");
    exampleStore = underTest.get("example");
    underTest.stop();

    assertThat(testStore.isPresent(), is(false));

    verify(exampleStore.get()).start();
    verify(configurationManager, times(2)).save(exampleConfig);
    verify(configurationManager, never()).delete(exampleConfig);
    verify(exampleStore.get()).shutdown();

    when(configurationManager.load()).thenReturn(List.of());

    underTest.start();
    testStore = underTest.get("test");
    exampleStore = underTest.get("example");
    underTest.stop();

    assertThat(testStore.isPresent(), is(false));
    assertThat(exampleStore.isPresent(), is(false));

    // leave the manager in a started state so that it can be reused in other tests
    underTest.start();
  }

  @Test
  public void testUpdatingConfigurationRestartsDataStore() throws Exception {
    DataStoreConfiguration firstConfig = newDataStoreConfiguration("test");
    firstConfig.setAttributes(Map.of("version", "first"));
    DataStoreConfiguration secondConfig = newDataStoreConfiguration("test");
    secondConfig.setAttributes(Map.of("version", "second"));

    DataStore<?> store = underTest.create(firstConfig);

    InOrder expected = inOrder(store, configurationManager);

    expected.verify(configurationManager).save(firstConfig);
    expected.verify(store).setConfiguration(firstConfig);
    expected.verify(store).start();
    when(store.isStarted()).thenReturn(true);

    underTest.update(secondConfig);

    expected.verify(configurationManager).save(secondConfig);
    expected.verify(store).stop();
    expected.verify(store).setConfiguration(secondConfig);
    expected.verify(store).start();

    expected.verifyNoMoreInteractions();
  }

  @Test
  public void testInvalidConfigurationUpdateIsNotUsed() throws Exception {
    DataStoreConfiguration goodConfig = newDataStoreConfiguration("test");
    goodConfig.setAttributes(Map.of("version", "first"));
    DataStoreConfiguration invalidConfig = newDataStoreConfiguration("test");
    invalidConfig.setAttributes(Map.of("version", "invalid"));

    doThrow(IllegalArgumentException.class).when(descriptorJdbc).validate(invalidConfig);

    DataStore<?> store = underTest.create(goodConfig);

    InOrder expected = inOrder(store, configurationManager);

    expected.verify(configurationManager).save(goodConfig);
    expected.verify(store).setConfiguration(goodConfig);
    expected.verify(store).start();

    when(store.isStarted()).thenReturn(true);

    assertThrows(IllegalArgumentException.class, () -> underTest.update(invalidConfig));
    expected.verifyNoMoreInteractions();
  }

  @Test
  public void testValidButProblematicConfigurationUpdateIsReverted() throws Exception {
    DataStoreConfiguration goodConfig = newDataStoreConfiguration("test");
    goodConfig.setAttributes(Map.of("version", "first"));
    DataStoreConfiguration badConfig = newDataStoreConfiguration("test");
    badConfig.setAttributes(Map.of("version", "bad"));

    DataStore<?> store = underTest.create(goodConfig);

    InOrder expected = inOrder(store, configurationManager);

    expected.verify(configurationManager).save(goodConfig);
    expected.verify(store).setConfiguration(goodConfig);
    expected.verify(store).start();

    when(store.isStarted()).thenReturn(true);
    doThrow(new IOException()).when(store).start();

    assertThrows(IOException.class, () -> underTest.update(badConfig));

    expected.verify(configurationManager).save(badConfig);
    expected.verify(store).stop();
    expected.verify(store).setConfiguration(badConfig);
    expected.verify(store).start();
    expected.verify(configurationManager).save(goodConfig);
    expected.verify(store).stop();
    expected.verify(store).setConfiguration(goodConfig);
    expected.verify(store).start();

    expected.verifyNoMoreInteractions();
  }

  private static DataStoreSupport<?> mockDataStore() {
    DataStoreSupport<?> dataStoreSupport = mock(DataStoreSupport.class);
    when(dataStoreSupport.getConfiguration()).thenAnswer(CALLS_REAL_METHODS);
    doAnswer(CALLS_REAL_METHODS).when(dataStoreSupport).setConfiguration(any());
    return dataStoreSupport;
  }

  private static DataStoreConfiguration newDataStoreConfiguration(final String name) {
    DataStoreConfiguration config = new DataStoreConfiguration();
    config.setName(name);
    config.setType("jdbc");
    config.setSource("local");
    config.setAttributes(Map.of());
    return config;
  }
}
