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
package org.sonatype.nexus.repository.internal.datastore;

import java.util.Map;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.DataStoreDescriptor;
import org.sonatype.nexus.datastore.StubDataStore;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.MAX_VALUE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.text.Strings2.lower;

/**
 * Default {@link DataStoreManager} implementation.
 *
 * @since 3.next
 */
@Named
@Singleton
@Priority(MAX_VALUE)
@ManagedLifecycle(phase = STORAGE)
@ManagedObject
public class DataStoreManagerImpl
    extends StateGuardLifecycleSupport
    implements DataStoreManager
{
  private final Map<String, DataStore<?>> dataStores = Maps.newConcurrentMap();

  private final Map<String, DataStoreDescriptor> dataStoreDescriptors;

  private final Map<String, Provider<DataStore<?>>> dataStorePrototypes;

  private final BooleanSupplier provisionDefaults;

  private final Provider<RepositoryManager> repositoryManagerProvider;

  @Inject
  public DataStoreManagerImpl(final Map<String, DataStoreDescriptor> dataStoreDescriptors,
                              final Map<String, Provider<DataStore<?>>> dataStorePrototypes,
                              final Provider<RepositoryManager> repositoryManagerProvider,
                              final NodeAccess nodeAccess,
                              @Nullable @Named("${nexus.datastore.provisionDefaults}") final Boolean provisionDefaults)
  {
    this.dataStoreDescriptors = checkNotNull(dataStoreDescriptors);
    this.dataStorePrototypes = checkNotNull(dataStorePrototypes);
    this.repositoryManagerProvider = checkNotNull(repositoryManagerProvider);

    if (provisionDefaults != null) {
      // explicit true/false setting, so honour that
      this.provisionDefaults = provisionDefaults::booleanValue;
    }
    else {
      // otherwise only create in non-clustered mode
      this.provisionDefaults = () -> !nodeAccess.isClustered();
    }
  }

  @Override
  protected void doStart() throws Exception {
    if (provisionDefaults.getAsBoolean()) {
      // TODO: replace stubbed defaults with proper defaults
      dataStores.put(CONFIG_DATASTORE_NAME, new StubDataStore(CONFIG_DATASTORE_NAME));
      dataStores.put(COMPONENT_DATASTORE_NAME, new StubDataStore(COMPONENT_DATASTORE_NAME));
    }

    // TODO: restore stores
  }

  @Override
  protected void doStop() throws Exception {
    // TODO: persist stores
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<DataStore<?>> browse() {
    return dataStores.values();
  }

  @Override
  @Guarded(by = STARTED)
  public DataStore<?> create(final DataStoreConfiguration configuration) throws Exception {
    checkNotNull(configuration);

    String name = configuration.getName();
    String type = configuration.getType();

    DataStoreDescriptor descriptor = dataStoreDescriptors.get(type);
    checkState(descriptor != null, "Missing descriptor for data store type %s", type);
    checkState(descriptor.isEnabled(), "Data store type %s is not enabled", type);
    descriptor.validate(configuration);

    Provider<DataStore<?>> prototype = dataStorePrototypes.get(type);
    checkState(prototype != null, "Missing prototype for data store type %s", type);
    DataStore<?> dataStore = prototype.get();
    dataStore.setConfiguration(configuration);

    dataStore.start();

    if (dataStores.putIfAbsent(lower(name), dataStore) != null) {
      dataStore.stop();
      throw new IllegalStateException("Data store " + name + " already exists");
    }

    return dataStore;
  }

  @Override
  @Guarded(by = STARTED)
  public DataStore<?> update(final DataStoreConfiguration newConfiguration) throws Exception {
    checkNotNull(newConfiguration);

    String name = newConfiguration.getName();
    String type = newConfiguration.getType();

    DataStoreDescriptor descriptor = dataStoreDescriptors.get(type);
    checkState(descriptor != null, "Missing descriptor for data store type %s", type);
    checkState(descriptor.isModifiable(), "Data store type %s cannot be updated", type);
    descriptor.validate(newConfiguration);

    DataStore<?> store = get(name);
    checkState(store != null, "Data store name %s does not exist", name);
    DataStoreConfiguration oldConfiguration = store.getConfiguration();

    if (store.isStarted()) {
      store.stop();
    }

    try {
      store.setConfiguration(newConfiguration);
      store.start();
    } catch (Exception e) {
      log.error("Configuration update failed for '{}' data store", name, e);

      if (store.isStarted()) {
        store.stop();
      }

      store.setConfiguration(oldConfiguration);
      store.start();
      throw new IllegalArgumentException("Configuration update failed for '" + name + "' data store ");
    }

    return store;
  }

  @Override
  @Guarded(by = STARTED)
  @Nullable
  public DataStore<?> get(final String name) {
    checkNotNull(name);
    return dataStores.get(lower(name));
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    checkNotNull(name);

    checkState(!CONFIG_DATASTORE_NAME.equalsIgnoreCase(name),
        "'" + CONFIG_DATASTORE_NAME + "' data store cannot be removed");
    checkState(!repositoryManagerProvider.get().isDataStoreUsed(name),
        "'%s' data store is in use by at least one repository", name);

    DataStore<?> store = dataStores.remove(lower(name));
    if (store != null) {
      store.stop();
    }
  }

  @Override
  public boolean exists(final String name) {
    checkNotNull(name);
    return dataStores.containsKey(lower(name));
  }
}
