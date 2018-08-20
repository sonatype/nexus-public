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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreCreatedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreDeletedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStoreConfigurationBuilder;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default {@link BlobStoreManager} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedObject
public class BlobStoreManagerImpl
    extends StateGuardLifecycleSupport
    implements BlobStoreManager, EventAware
{
  private final EventManager eventManager;

  private final Map<String, BlobStore> stores = Maps.newConcurrentMap();

  private final BlobStoreConfigurationStore store;

  private final Map<String, BlobStoreDescriptor> blobStoreDescriptors;

  private final Map<String, Provider<BlobStore>> blobStorePrototypes;

  private final DatabaseFreezeService databaseFreezeService;

  private final BooleanSupplier provisionDefaults;

  @Inject
  public BlobStoreManagerImpl(final EventManager eventManager,
                              final BlobStoreConfigurationStore store,
                              final Map<String, BlobStoreDescriptor> blobStoreDescriptors,
                              final Map<String, Provider<BlobStore>> blobStorePrototypes,
                              final DatabaseFreezeService databaseFreezeService,
                              final NodeAccess nodeAccess,
                              @Nullable @Named("${nexus.blobstore.provisionDefaults}") final Boolean provisionDefaults)
  {
    this.eventManager = checkNotNull(eventManager);
    this.store = checkNotNull(store);
    this.blobStoreDescriptors = checkNotNull(blobStoreDescriptors);
    this.blobStorePrototypes = checkNotNull(blobStorePrototypes);
    this.databaseFreezeService = checkNotNull(databaseFreezeService);

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
    List<BlobStoreConfiguration> configurations = store.list();
    if (configurations.isEmpty() && provisionDefaults.getAsBoolean()) {
      log.debug("No BlobStores configured; provisioning default BlobStore");
      store.create(new FileBlobStoreConfigurationBuilder(DEFAULT_BLOBSTORE_NAME).build());
      configurations = store.list();
    }

    log.debug("Restoring {} BlobStores", configurations.size());
    for (BlobStoreConfiguration configuration : configurations) {
      log.debug("Restoring BlobStore: {}", configuration);
      BlobStore blobStore = newBlobStore(configuration);
      track(configuration.getName(), blobStore);

      // TODO - event publishing
    }

    log.debug("Starting {} BlobStores", stores.size());
    for (BlobStore blobStore : stores.values()) {
      log.debug("Starting BlobStore: {}", blobStore);
      blobStore.start();

      // TODO - event publishing
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (stores.isEmpty()) {
      log.debug("No BlobStores defined");
      return;
    }

    log.debug("Stopping {} BlobStores", stores.size());
    for (Map.Entry<String, BlobStore> entry : stores.entrySet()) {
      String name = entry.getKey();
      BlobStore store = entry.getValue();
      log.debug("Stopping blob-store: {}", name);
      store.stop();

      // TODO - event publishing
    }

    stores.clear();
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<BlobStore> browse() {
    return ImmutableList.copyOf(stores.values());
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStore create(final BlobStoreConfiguration configuration) throws Exception {
    checkNotNull(configuration);
    log.debug("Creating BlobStore: {} with attributes: {}", configuration.getName(),
        configuration.getAttributes());
    BlobStoreDescriptor blobStoreDescriptor = blobStoreDescriptors.get(configuration.getType());
    blobStoreDescriptor.validateConfig(configuration);

    BlobStore blobStore = newBlobStore(configuration);

    try {
      store.create(configuration);
    }
    catch (Exception e) {
      try {
        blobStore.remove();
      }
      catch (Exception removeException) {
        // if an error occurs on remove log and rethrow original to avoid losing the root cause
        log.error("Error removing BlobStore {} after create failed", configuration.getName(), removeException);
      }
      throw e;
    }

    track(configuration.getName(), blobStore);

    blobStore.start();

    eventManager.post(new BlobStoreCreatedEvent(blobStore));

    return blobStore;
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStore update(final BlobStoreConfiguration configuration) throws Exception {
    checkNotNull(configuration);
    log.debug("Updating BlobStore: {} with attributes: {}", configuration.getName(),
        configuration.getAttributes());
    BlobStoreDescriptor blobStoreDescriptor = blobStoreDescriptors.get(configuration.getType());
    blobStoreDescriptor.validateConfig(configuration);

    BlobStore blobStore = get(configuration.getName());
    store.update(configuration);
    blobStore.init(configuration);

    return blobStore;
  }

  @Override
  @Guarded(by = STARTED)
  @Nullable
  public BlobStore get(final String name) {
    checkNotNull(name);

    return stores.get(name);
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    checkNotNull(name);
    databaseFreezeService.checkUnfrozen("Unable to delete a BlobStore while database is frozen.");

    BlobStore blobStore = blobStore(name);
    log.debug("Deleting BlobStore: {}", name);
    blobStore.stop();
    blobStore.remove();
    untrack(name);
    store.delete(blobStore.getBlobStoreConfiguration());

    eventManager.post(new BlobStoreDeletedEvent(blobStore));
  }

  @Override
  public boolean exists(final String name) {
    return stores.keySet().stream().anyMatch(key -> key.equalsIgnoreCase(name));
  }

  private BlobStore newBlobStore(final BlobStoreConfiguration blobStoreConfiguration) throws Exception {
    BlobStore blobStore = blobStorePrototypes.get(blobStoreConfiguration.getType()).get();
    blobStore.init(blobStoreConfiguration);
    return blobStore;
  }

  @VisibleForTesting
  BlobStore blobStore(final String name) {
    BlobStore blobStore = stores.get(name);
    checkState(blobStore != null, "Missing BlobStore: %s", name);
    return blobStore;
  }

  private void track(final String name, final BlobStore blobStore) {
    log.debug("Tracking: {}", name);
    stores.put(name, blobStore);
  }

  private void untrack(final String name) {
    log.debug("Untracking: {}", name);
    stores.remove(name);
  }

  @Subscribe
  public void on(final BlobStoreConfigurationCreatedEvent event) {
    handleRemoteOnly(event, evt -> {
      // only create if not tracked
      String name = evt.getName();
      if (!stores.containsKey(name)) {
        store.list().stream()
            .filter(c -> c.getName().equals(name))
            .findFirst()
            .ifPresent(c -> {
              try {
                BlobStore blobStore = newBlobStore(c);
                track(name, blobStore);
                blobStore.start();
              }
              catch (Exception e) {
                log.warn("create blob store from remote event failed: {}", name, e);
              }
            });
      }
    });
  }

  @Subscribe
  public void on(final BlobStoreConfigurationDeletedEvent event) {
    handleRemoteOnly(event, evt -> {
      try {
        // only delete if tracked
        String name = evt.getName();
        if (stores.containsKey(name)) {
          BlobStore blobStore = blobStore(name);
          blobStore.stop();
          blobStore.remove();
          untrack(name);
        }
      }
      catch (Exception e) {
        log.warn("delete blob store from remote event failed: {}", evt.getName(), e);
      }
    });
  }

  @Subscribe
  public void on(final BlobStoreConfigurationUpdatedEvent event) {
    handleRemoteOnly(event, evt -> {
      try {
        // only update if tracked
        String name = evt.getName();
        if (stores.containsKey(name)) {
          BlobStore blobStore = blobStore(name);
          blobStore.init(event.getEntity());
        }
      }
      catch (Exception e) {
        log.warn("update blob store from remote event failed: {}", evt.getName(), e);
      }
    });
  }

  private void handleRemoteOnly(final BlobStoreConfigurationEvent event,
                                final Consumer<BlobStoreConfigurationEvent> consumer)
  {
    log.trace("handling: {}", event);
    // skip local events
    if (!event.isLocal()) {
      consumer.accept(event);
    }
  }
}
