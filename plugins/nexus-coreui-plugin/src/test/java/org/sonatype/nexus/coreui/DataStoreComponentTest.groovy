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
package org.sonatype.nexus.coreui

import org.sonatype.nexus.datastore.DataStoreDescriptor
import org.sonatype.nexus.datastore.api.DataStore
import org.sonatype.nexus.datastore.api.DataStoreConfiguration
import org.sonatype.nexus.datastore.api.DataStoreManager
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker

import spock.lang.Specification
import spock.lang.Subject

/**
 * Test for {@link DataStoreComponent}
 */
class DataStoreComponentTest
    extends Specification
{

  DataStoreManager dataStoreManager = Mock()

  RepositoryManager repositoryManager = Mock()

  RepositoryPermissionChecker repositoryPermissionChecker = Mock()

  @Subject
  DataStoreComponent dataStoreComponent = new DataStoreComponent(
    dataStoreManager: dataStoreManager,
    repositoryManager: repositoryManager,
    repositoryPermissionChecker: repositoryPermissionChecker)

  def 'Read types returns descriptor data'() {
    given: 'A data store descriptor'
      dataStoreComponent.dataStoreDescriptors =
          [MyType: [getName: { -> 'MyType' }, getFormFields: { -> []}] as DataStoreDescriptor]

    when: 'Reading data store types'
      def types = dataStoreComponent.readTypes()

    then: 'The descriptor information is returned'
      types.collect{[it.id, it.name, it.formFields]} == [['MyType', 'MyType', []]]
  }

  def 'Create data store creates and returns new data store'() {
    given: 'A data store create request'
      DataStoreXO dataStoreXO = new DataStoreXO(name: 'mydata', type: 'jdbc',
          attributes: [url: 'mock:some/datastore/url'])
      DataStoreConfiguration expectedConfig = new DataStoreConfiguration(name: 'mydata', type: 'jdbc',
          attributes: [url: 'mock:some/datastore/url'])
      DataStore dataStore = Mock()

    when: 'The data store is created'
      def createdXO = dataStoreComponent.create(dataStoreXO)

    then: 'The data store is created with the manager and returned'
      _ * dataStore.getConfiguration() >> expectedConfig
      1 * dataStoreManager.create(_) >> dataStore
      [createdXO.name, createdXO.type, createdXO.attributes] ==
        [expectedConfig.name, expectedConfig.type, expectedConfig.attributes]
  }

  def 'Config data store is always in use'() {
    given:
      DataStoreConfiguration configConfig = new DataStoreConfiguration(name: 'config', type: 'jdbc',
        attributes: [url: 'mock:some/datastore/url'])
      DataStoreConfiguration componentConfig = new DataStoreConfiguration(name: 'component', type: 'jdbc',
        attributes: [url: 'mock:some/datastore/url'])
      DataStore configDataStore = Mock()
      DataStore componentDataStore = Mock()

    when: 'Browsing data stores'
      def results = dataStoreComponent.read()

    then: 'Config data store is in use'
      _ * configDataStore.configuration >> configConfig
      _ * componentDataStore.configuration >> componentConfig
      1 * dataStoreManager.browse() >> [configDataStore, componentDataStore]
      results.size == 2
      results[0].inUse == true
      results[1].inUse == false
  }

  def 'Data store reports if in use by a repository'() {
    given:
      DataStoreConfiguration usedConfig = new DataStoreConfiguration(name: 'used', type: 'jdbc',
        attributes: [url: 'mock:some/datastore/url'])
      DataStoreConfiguration unusedConfig = new DataStoreConfiguration(name: 'unused', type: 'jdbc',
        attributes: [url: 'mock:some/datastore/url'])
      DataStore usedDataStore = Mock()
      DataStore unusedDataStore = Mock()

    when: 'Browsing data stores'
      def results = dataStoreComponent.read()

    then: 'Used data store is in use'
      _ * usedDataStore.configuration >> usedConfig
      _ * unusedDataStore.configuration >> unusedConfig
      1 * dataStoreManager.browse() >> [usedDataStore, unusedDataStore]
      1 * repositoryManager.isDataStoreUsed('used') >> true
      1 * repositoryManager.isDataStoreUsed('unused') >> false
      results.size == 2
      results[0].inUse == true
      results[1].inUse == false
  }

}
