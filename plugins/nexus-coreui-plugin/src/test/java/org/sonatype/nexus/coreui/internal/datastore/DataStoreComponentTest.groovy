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
package org.sonatype.nexus.coreui.internal.datastore

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

    def 'Reading databases'() {
      given:
        DataStoreConfiguration contentConfig = new DataStoreConfiguration(name: 'content', type: 'jdbc',
          source: 'local', attributes: [jdbcUrl: 'jdbc:h2:some/datastore/url'])
        DataStoreConfiguration configConfig = new DataStoreConfiguration(name: 'config', type: 'jdbc',
          source: 'local', attributes: [jdbcUrl: 'jdbc:postgresql:some/datastore/url'])
        DataStore contentDataStore = Mock()
        DataStore configDataStore = Mock()
  
      when: 'Browsing data stores'
        def results = dataStoreComponent.read()
  
      then: 'The known datastores are returned'
        _ * contentDataStore.configuration >> contentConfig
        _ * configDataStore.configuration >> configConfig
        1 * dataStoreManager.browse() >> [contentDataStore, configDataStore]
        results.size() == 2
        results[0].name == 'content'
        results[1].name == 'config'
    }

  def 'Reading H2 databases'() {
    given:
      DataStoreConfiguration usedConfig = new DataStoreConfiguration(name: 'valid', type: 'jdbc',
        source: 'local', attributes: [jdbcUrl: 'jdbc:h2:some/datastore/url'])
      DataStoreConfiguration unusedConfig = new DataStoreConfiguration(name: 'invalid', type: 'jdbc',
        source: 'local', attributes: [jdbcUrl: 'jdbc:postgresql:some/datastore/url'])
      DataStore usedDataStore = Mock()
      DataStore unusedDataStore = Mock()

    when: 'Browsing data stores'
      def results = dataStoreComponent.readH2()

    then: 'Only the datastore with h2 is returned'
      _ * usedDataStore.configuration >> usedConfig
      _ * unusedDataStore.configuration >> unusedConfig
      1 * dataStoreManager.browse() >> [usedDataStore, unusedDataStore]
      results.size() == 1
      results[0].name == 'valid'
  }
}
