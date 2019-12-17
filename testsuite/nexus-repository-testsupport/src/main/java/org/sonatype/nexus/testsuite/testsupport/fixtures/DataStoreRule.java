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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.api.DataStoreManager;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.20
 */
public class DataStoreRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(TrustStoreRule.class);

  private final Provider<DataStoreManager> dataStoreManagerProvider;

  private Set<String> managedDataStores = new HashSet<>();

  public DataStoreRule(final Provider<DataStoreManager> dataStoreManagerProvider) {
    this.dataStoreManagerProvider = dataStoreManagerProvider;
  }

  @Override
  protected void after() {
    managedDataStores.forEach(storeName -> {
      try {
        dataStoreManagerProvider.get().delete(storeName);
        log.debug("Removed data store: {}", storeName);
      }
      catch (Exception e) { // NOSONAR
        log.info("Unable to clean up data store {}", storeName, e);
      }
    });
  }

  public DataStore<?> createDataStore(final DataStoreConfiguration configuration) {
    try {
      DataStore<?> dataStore = dataStoreManagerProvider.get().create(configuration);
      managedDataStores.add(configuration.getName());
      return dataStore;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public DataStore<?> createDataStore(final String storeName) {
    return createDataStore(storeName, null, null, "jdbc:h2:file:${karaf.data}/db/${storeName}");
  }

  public DataStore<?> createDataStore(
      final String storeName,
      final String username,
      final String password,
      final String jdbcUrl)
  {
    try {
      DataStoreConfiguration configuration = new DataStoreConfiguration();

      configuration.setName(storeName);
      configuration.setSource("local");
      configuration.setType("jdbc");

      Map<String, String> attributes = new HashMap<>();
      attributes.put("jdbcUrl", jdbcUrl);
      if (username != null) {
        attributes.put("username", username);
      }
      if (password != null) {
        attributes.put("password", password);
      }
      configuration.setAttributes(attributes);

      DataStore<?> dataStore = dataStoreManagerProvider.get().create(configuration);
      managedDataStores.add(configuration.getName());
      return dataStore;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add a data store to automatically cleanup upon test failure
   */
  public void manageDataStore(final String storeName) {
    managedDataStores.add(storeName);
  }

  /**
   * Remove a data store from automatic cleanup
   */
  public void unmanageAlias(final String storeName) {
    managedDataStores.remove(storeName);
  }

}
