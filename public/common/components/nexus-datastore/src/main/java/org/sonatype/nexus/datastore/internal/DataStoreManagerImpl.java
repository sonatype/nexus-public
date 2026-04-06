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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.DataStoreConfigurationManager;
import org.sonatype.nexus.datastore.DataStoreDescriptor;
import org.sonatype.nexus.datastore.DataStoreRestorer;
import org.sonatype.nexus.datastore.DataStoreUsageChecker;
import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.datastore.api.DataStoreNotFoundException;
import org.sonatype.nexus.distributed.event.service.api.common.DataStoreConfigurationEvent;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.transaction.TransactionIsolation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.text.Strings2.lower;

/**
 * Default {@link DataStoreManager} implementation.
 *
 * @since 3.19
 */
@Component
@Singleton
@Priority(MAX_VALUE)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ManagedLifecycle(phase = STORAGE)
@ManagedObject
public class DataStoreManagerImpl
    extends StateGuardLifecycleSupport
    implements DataStoreManager, DataSessionSupplier, ApplicationContextAware
{
  private final boolean enabled;

  private final Map<String, DataStoreDescriptor> dataStoreDescriptors;

  private final Map<String, Provider<DataStore<?>>> dataStorePrototypes;

  private final DataStoreConfigurationManager configurationManager;

  private final Provider<DataStoreUsageChecker> usageChecker;

  private final Map<String, DataStore<?>> dataStores = new ConcurrentHashMap<>();

  private final DataStoreRestorer restorer;

  private final EventManager eventManager;

  private ApplicationContext applicationContext;

  @Inject
  public DataStoreManagerImpl(
      final EventManager eventManager,
      final List<DataStoreDescriptor> dataStoreDescriptorsList,
      @Qualifier("jdbc") final Provider<DataStore<?>> jdbcPrototype,
      final DataStoreConfigurationManager configurationManager,
      final Provider<DataStoreUsageChecker> usageChecker,
      @Nullable final DataStoreRestorer restorer)
  {
    this.enabled = true;

    this.eventManager = checkNotNull(eventManager);
    this.dataStoreDescriptors = QualifierUtil.buildQualifierBeanMap(checkNotNull(dataStoreDescriptorsList));
    this.dataStorePrototypes = Map.of("jdbc", jdbcPrototype);
    this.configurationManager = checkNotNull(configurationManager);
    this.usageChecker = checkNotNull(usageChecker);
    this.restorer = restorer;
  }

  @Override
  protected void doStart() throws Exception {
    if (enabled) {
      configurationManager.load().forEach(this::tryRestore);
    }
  }

  @Override
  protected void doStop() throws Exception {
    for (DataStore<?> store : browse()) {
      try {
        log.debug("Shutting down {}", store);
        store.shutdown();
        log.debug("Shut down {}", store);
      }
      catch (Exception e) {
        log.warn("Problem shutting down {}", store, e);
      }
    }
    dataStores.clear();
  }

  @Override
  public DataSession<?> openSession(final String storeName) {
    return get(storeName).orElseThrow(() -> new DataStoreNotFoundException(storeName)).openSession();
  }

  @Override
  public DataSession<?> openSerializableTransactionSession(final String storeName) {
    return get(storeName).orElseThrow(() -> new DataStoreNotFoundException(storeName))
        .openSession(TransactionIsolation.SERIALIZABLE);
  }

  @Override
  public Connection openConnection(final String storeName) throws SQLException {
    return get(storeName).orElseThrow(() -> new DataStoreNotFoundException(storeName)).openConnection();
  }

  @Override
  public Iterable<DataStore<?>> browse() {
    return dataStores.values();
  }

  @Override
  @Guarded(by = STARTED)
  public DataStore<?> create(final DataStoreConfiguration configuration) throws Exception {
    checkState(enabled, "Datastore feature is not enabled");

    return doCreate(configuration);
  }

  private void tryRestore(final DataStoreConfiguration configuration) {
    try {
      if (restorer != null) {
        restorer.maybeRestore(configuration);
      }
      doCreate(configuration);
    }
    catch (Exception e) {
      log.warn("Problem restoring {}", configuration, e);
    }
  }

  private DataStore<?> doCreate(final DataStoreConfiguration configuration) throws Exception {
    checkNotNull(configuration);

    String storeName = configuration.getName();

    checkState(!exists(storeName), "%s data store already exists", storeName);

    validateConfiguration(configuration);
    configurationManager.save(configuration);

    DataStore<?> store = createDataStore(configuration);
    log.debug("Starting {}", store);
    store.start();

    register(applicationContext, store);

    // check someone hasn't just created the same store; if our store is a duplicate then stop it
    if (dataStores.putIfAbsent(lower(storeName), store) != null) {
      log.debug("Stopping duplicate {}", store);
      store.stop();
      throw new IllegalStateException("Duplicate request to create " + storeName + " data store");
    }

    log.debug("Started {}", store);

    return store;
  }

  @Override
  @Guarded(by = STARTED)
  public DataStore<?> update(final DataStoreConfiguration newConfiguration) throws Exception {
    checkNotNull(newConfiguration);

    String storeName = newConfiguration.getName();

    checkState(exists(storeName), "%s data store does not exist", storeName);

    validateConfiguration(newConfiguration);
    configurationManager.save(newConfiguration);

    DataStore<?> store = get(storeName).get();
    checkState(store != null, "%s data store has been removed", storeName);
    DataStoreConfiguration oldConfiguration = store.getConfiguration();

    if (store.isStarted()) {
      log.debug("Stopping {} for reconfiguration", store);
      store.stop();
    }

    Exception updateFailure = null;
    try {
      store.setConfiguration(newConfiguration);
      log.debug("Restarting {}", store);
      store.start();
    }
    catch (Exception e) {
      updateFailure = e;

      // roll back to known 'good' configuration
      log.warn("Problem restarting {}", store, e);
      configurationManager.save(oldConfiguration);

      if (store.isStarted()) {
        log.debug("Stopping {} to revert changes", store);
        store.stop();
      }

      store.setConfiguration(oldConfiguration);
      log.debug("Restarting {}", store);
      store.start();
    }

    log.debug("Restarted {}", store);

    if (updateFailure != null) {
      throw new IllegalArgumentException("Configuration update failed for " + storeName, updateFailure);
    }

    if (!EventHelper.isReplicating()) {
      eventManager.post(new DataStoreConfigurationEvent(newConfiguration.getName(), newConfiguration.getType(),
          newConfiguration.getSource(), newConfiguration.getAttributes()));
    }

    return store;
  }

  @Override
  @Guarded(by = STARTED)
  public Optional<DataStore<?>> get(final String storeName) {
    checkNotNull(storeName);

    return ofNullable(dataStores.get(lower(storeName)));
  }

  @Override
  @Guarded(by = STARTED)
  public boolean delete(final String storeName) throws Exception {
    checkNotNull(storeName);

    checkState(!usageChecker.get().isDataStoreUsed(storeName),
        "%s data store is in use by at least one repository", storeName);

    DataStore<?> store = dataStores.remove(lower(storeName));
    if (store != null) {
      try {
        log.debug("Shutting down {} for deletion", store);
        store.shutdown();
        log.debug("Shut down {}", store);
      }
      finally {
        configurationManager.delete(store.getConfiguration());
      }
    }

    return store != null;
  }

  @Override
  public boolean exists(final String storeName) {
    checkNotNull(storeName);

    return dataStores.containsKey(lower(storeName));
  }

  @Override
  public void freeze() {
    // no-op: freeze mechanism removed
  }

  @Override
  public void unfreeze() {
    // no-op: freeze mechanism removed
  }

  @Override
  public boolean isFrozen() {
    return false;
  }

  /**
   * Validate the given configuration is consistent according to the declared type.
   */
  private void validateConfiguration(final DataStoreConfiguration configuration) {
    String storeType = configuration.getType();

    DataStoreDescriptor descriptor = dataStoreDescriptors.get(storeType);
    checkState(descriptor != null, "Missing data store descriptor '%s'", storeType);
    checkState(descriptor.isEnabled(), "Data store type '%s' is not enabled", storeType);
    descriptor.validate(configuration);
  }

  /**
   * Create a {@link DataStore} of the declared type with the given configuration.
   */
  private DataStore<?> createDataStore(final DataStoreConfiguration configuration) {
    String storeType = configuration.getType();

    Provider<DataStore<?>> prototype = dataStorePrototypes.get(storeType);
    checkState(prototype != null, "Missing data store prototype '%s'", storeType);
    DataStore<?> store = prototype.get();
    store.setConfiguration(configuration);

    return store;
  }

  /**
   * Dynamically registers {@link DataAccess} types with their associated stores.
   */
  @EventListener
  public void on(final ContextRefreshedEvent event) {
    dataStores.values().forEach(store -> register(event.getApplicationContext(), store));
  }

  private void register(final ApplicationContext applicationContext, final DataStore<?> store) {
    applicationContext.getBeansOfType(DataAccess.class)
        .values()
        .stream()
        .map(DataAccess::getClass)
        .forEach(store::register);
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
