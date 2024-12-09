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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobInputStreamException;
import org.sonatype.nexus.blobstore.api.BlobSession;
import org.sonatype.nexus.blobstore.api.BlobSessionSupplier;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreCreatedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreDeletedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreNotFoundException;
import org.sonatype.nexus.blobstore.api.BlobStoreStartedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreStoppedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreUpdatedEvent;
import org.sonatype.nexus.blobstore.api.DefaultBlobStoreProvider;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventConsumer;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.distributed.event.service.api.EventType;
import org.sonatype.nexus.distributed.event.service.api.common.BlobStoreDistributedConfigurationEvent;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.replication.ReplicationBlobStoreStatusManager;
import org.sonatype.nexus.security.UserIdHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.distributed.event.service.api.EventType.DELETED;
import static org.sonatype.nexus.distributed.event.service.api.EventType.UPDATED;

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
    implements BlobStoreManager, BlobSessionSupplier, EventAware
{
  protected static final String BLOBSTORE_CONFIG = "blobstore-config";

  private final EventManager eventManager;

  private final Map<String, BlobStore> stores = Maps.newConcurrentMap();

  private final BlobStoreConfigurationStore store;

  private final Map<String, BlobStoreDescriptor> blobStoreDescriptors;

  private final Map<String, Provider<BlobStore>> blobStorePrototypes;

  private final FreezeService freezeService;

  private final BooleanSupplier provisionDefaults;

  private final Provider<RepositoryManager> repositoryManagerProvider;

  private final BlobStoreTaskService blobStoreTaskService;

  private final Provider<BlobStoreOverride> blobStoreOverrideProvider;

  private final ReplicationBlobStoreStatusManager replicationBlobStoreStatusManager;

  private final DefaultBlobStoreProvider defaultBlobstoreProvider;

  private final SecretsService secretService;

  @Inject
  public BlobStoreManagerImpl(
      final EventManager eventManager, // NOSONAR
      final BlobStoreConfigurationStore store,
      final Map<String, BlobStoreDescriptor> blobStoreDescriptors,
      final Map<String, Provider<BlobStore>> blobStorePrototypes,
      final FreezeService freezeService,
      final Provider<RepositoryManager> repositoryManagerProvider,
      final NodeAccess nodeAccess,
      @Nullable @Named("${nexus.blobstore.provisionDefaults}") final Boolean provisionDefaults,
      final DefaultBlobStoreProvider defaultBlobstoreProvider,
      final BlobStoreTaskService blobStoreTaskService,
      final Provider<BlobStoreOverride> blobStoreOverrideProvider,
      final ReplicationBlobStoreStatusManager replicationBlobStoreStatusManager,
      final SecretsService secretService)
  {
    this.eventManager = checkNotNull(eventManager);
    this.store = checkNotNull(store);
    this.blobStoreDescriptors = checkNotNull(blobStoreDescriptors);
    this.blobStorePrototypes = checkNotNull(blobStorePrototypes);
    this.freezeService = checkNotNull(freezeService);
    this.repositoryManagerProvider = checkNotNull(repositoryManagerProvider);
    this.blobStoreTaskService = checkNotNull(blobStoreTaskService);
    this.blobStoreOverrideProvider = blobStoreOverrideProvider;
    this.replicationBlobStoreStatusManager = checkNotNull(replicationBlobStoreStatusManager);
    this.defaultBlobstoreProvider = checkNotNull(defaultBlobstoreProvider);
    this.secretService = checkNotNull(secretService);

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
    Optional.ofNullable(blobStoreOverrideProvider.get()).ifPresent(BlobStoreOverride::apply);
    List<BlobStoreConfiguration> configurations = store.list();

    if (configurations.isEmpty() && provisionDefaults.getAsBoolean()) {
      log.debug("No BlobStores configured; provisioning default BlobStore");
      BlobStoreConfiguration defaultBlobStoreConfiguration = defaultBlobstoreProvider.get(this::newConfiguration);
      encryptSensitiveAttributes(newConfiguration(), defaultBlobStoreConfiguration);
      store.create(defaultBlobStoreConfiguration);
      configurations = store.list();
    }

    log.debug("Restoring {} BlobStores", configurations.size());
    for (BlobStoreConfiguration configuration : configurations) {
      log.debug("Restoring BlobStore: {}", configuration);
      BlobStore blobStore = null;

      try {
        blobStore = blobStorePrototypes.get(configuration.getType()).get();
        blobStore.init(configuration);
      }
      catch (Exception e) {
        log.error("Unable to restore BlobStore {}", configuration.getName(), e);
      }
      finally {
        if (blobStore != null) {
          track(configuration.getName(), blobStore);
        }
      }

      // TODO - event publishing
    }

    log.debug("Starting {} BlobStores", stores.size());
    for (Map.Entry<String, BlobStore> entry : stores.entrySet()) {
      String name = entry.getKey();
      BlobStore blobStore = entry.getValue();
      log.debug("Starting BlobStore: {}", name);
      try {
        blobStore.start();
        eventManager.post(new BlobStoreStartedEvent(blobStore));

      }
      catch (Exception e) {
        log.error("Unable to start BlobStore {}", name, e);
      }

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
      eventManager.post(new BlobStoreStoppedEvent(store));

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
    log.debug("Creating BlobStore: {} with attributes: {}", configuration.getName(), configuration);
    validateConfiguration(configuration, true);
    List<Secret> encryptedAttributes = encryptSensitiveAttributes(newConfiguration(), configuration);
    try {
      BlobStore blobStore = getBlobStoreForCreate(configuration);
      if (!EventHelper.isReplicating()) {
        try {
          store.create(configuration);
          log.debug("BlobStore: {} created.", configuration.getName());
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
      }

      doCreate(blobStore, configuration);
      log.debug("BlobStore: {} saved into local cache", configuration.getName());
      eventManager.post(new BlobStoreDistributedConfigurationEvent(configuration.getName(), EventType.CREATED));
      return blobStore;
    }
    catch (Exception e) {
      encryptedAttributes.forEach(this::removeSecret);
      throw e;
    }
  }

  private BlobStore getBlobStoreForCreate(final BlobStoreConfiguration configuration) throws Exception {
    BlobStore blobStore = blobStorePrototypes.get(configuration.getType()).get();
    blobStore.init(configuration);
    replicationBlobStoreStatusManager.initializeReplicationStatus(configuration);

    blobStore.validateCanCreateAndUpdate();

    return blobStore;
  }

  private void doCreate(final BlobStore blobStore, final BlobStoreConfiguration configuration) throws Exception {
    checkNotNull(blobStore, "blobStore param is NULL");
    checkNotNull(configuration, "configuration param is NULL");
    final String blobStoreName = configuration.getName();
    if (!stores.containsKey(blobStoreName)) {
      track(blobStoreName, blobStore);
      blobStore.start();
      eventManager.post(new BlobStoreCreatedEvent(blobStore));
      eventManager.post(new BlobStoreStartedEvent(blobStore));
    }
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStore update(final BlobStoreConfiguration configuration) throws Exception {
    checkNotNull(configuration);
    validateConfiguration(configuration, false);
    BlobStore blobStore = getBlobStoreForUpdate(configuration);

    if (!EventHelper.isReplicating()) {
      List<Secret> encryptedAttributes = new ArrayList<>();
      try {
        encryptedAttributes = encryptSensitiveAttributes(blobStore.getBlobStoreConfiguration(), configuration);
        store.update(configuration);
      }
      catch (Exception e) {
        encryptedAttributes.forEach(this::removeSecret);
        throw e;
      }
    }

    doUpdate(blobStore, configuration);

    eventManager.post(new BlobStoreDistributedConfigurationEvent(configuration.getName(), UPDATED));

    return blobStore;
  }

  private BlobStore getBlobStoreForUpdate(final BlobStoreConfiguration configuration) throws Exception {
    BlobStore blobStore = get(configuration.getName());
    checkNotNull(blobStore);
    blobStore.validateCanCreateAndUpdate();
    log.debug("Updating BlobStore: {} with attributes: {}", configuration.getName(),
        configuration);

    return blobStore;
  }

  private void doUpdate(final BlobStore blobStore, final BlobStoreConfiguration configuration) throws Exception {
    BlobStoreConfiguration currentBlobStoreConfiguration = blobStore.getBlobStoreConfiguration();

    if (blobStore.isStarted()) {
      blobStore.stop();
      eventManager.post(new BlobStoreStoppedEvent(blobStore));
    }

    try {
      blobStore.init(configuration);
      blobStore.start();
      executeIfNotReplicating(() -> removeEncryptedAttributes(currentBlobStoreConfiguration, configuration));
      eventManager.post(new BlobStoreUpdatedEvent(blobStore));
      eventManager.post(new BlobStoreStartedEvent(blobStore));
    }
    catch (BlobStoreException e) {
      startWithConfig(blobStore, currentBlobStoreConfiguration);
      executeIfNotReplicating(restoreOldConfiguration(currentBlobStoreConfiguration, configuration));
      throw e;
    }
    catch (Exception e) {
      log.error("Failed to update configuration", e);
      startWithConfig(blobStore, currentBlobStoreConfiguration);
      executeIfNotReplicating(restoreOldConfiguration(currentBlobStoreConfiguration, configuration));
      throw new BlobStoreException("Failed to start blob store with new configuration.", null);
    }
  }

  private Runnable restoreOldConfiguration(
      final BlobStoreConfiguration oldConfig,
      final BlobStoreConfiguration newConfig)
  {
    return () -> {
      log.debug("Restoring old configuration for blob store name: '{}'", oldConfig.getName());
      store.update(oldConfig);
      removeEncryptedAttributes(newConfig, oldConfig);
    };
  }

  private void executeIfNotReplicating(Runnable action) {
    if (!EventHelper.isReplicating()) {
      action.run();
    }
  }

  private void startWithConfig(final BlobStore blobStore, final BlobStoreConfiguration config) throws Exception {
    if (blobStore.isStarted()) {
      blobStore.stop();
      eventManager.post(new BlobStoreStoppedEvent(blobStore));
    }
    blobStore.init(config);
    blobStore.start();
    eventManager.post(new BlobStoreStartedEvent(blobStore));
  }

  @Override
  @Guarded(by = STARTED)
  @Nullable
  public BlobStore get(final String name) {
    checkNotNull(name);

    BlobStore blobStore = stores.get(name);
    if (blobStore == null) {
      final BlobStoreConfiguration configuration = store.read(name);
      if (Objects.isNull(configuration)) {
        return null;
      }
      try {
        BlobStore validBS = getBlobStoreForCreate(configuration);
        doCreate(validBS, configuration);
        blobStore = stores.get(name);
      }
      catch (Exception e) {
        throw new BlobStoreException("Could not start blobstore: " + name + ", reason: " + e.getMessage(), e, null);
      }
    }

    return blobStore;
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    checkNotNull(name);
    if (hasConflictingTasks(name)) {
      throw new IllegalStateException("BlobStore " + name + " is in use by a Change Repository Blob Store task");
    }
    else if (!repositoryManagerProvider.get().isBlobstoreUsed(name)) {
      forceDelete(name);
    }
    else {
      throw new BlobStoreException("BlobStore " + name + " is in use and cannot be deleted", null);
    }
  }

  @Override
  public boolean hasConflictingTasks(final String blobStoreName) {
    return blobStoreTaskService.isAnyTaskInUseForBlobStore(blobStoreName);
  }

  @Override
  @Guarded(by = STARTED)
  public void forceDelete(final String name) throws Exception {
    checkNotNull(name);
    freezeService.checkWritable("Unable to delete a BlobStore while database is frozen.");
    log.debug("Deleting BlobStore: {}", name);
    BlobStore blobStore = doForceDelete(name);

    if (!EventHelper.isReplicating()) {
      BlobStoreConfiguration blobStoreConfiguration = blobStore.getBlobStoreConfiguration();
      store.delete(blobStoreConfiguration);
      removeEncryptedAttributes(blobStoreConfiguration, newConfiguration());
    }
    eventManager.post(new BlobStoreDeletedEvent(blobStore));
    eventManager.post(new BlobStoreDistributedConfigurationEvent(name, DELETED));
  }

  private BlobStore doForceDelete(final String blobStoreName) {
    BlobStore blobStore = blobStore(blobStoreName);

    try {
      blobStore.shutdown();
      blobStore.remove();
    }
    catch (Exception e) {
      log.error("Error cleaning up blobStore {} while attempting to delete", blobStoreName, e);
    }
    untrack(blobStoreName);

    return blobStore;
  }

  @Override
  public boolean exists(final String name) {
    return stores.keySet().stream().anyMatch(key -> key.equalsIgnoreCase(name));
  }

  @VisibleForTesting
  BlobStore blobStore(final String name) {
    BlobStore blobStore = stores.get(name);
    checkState(blobStore != null, "Missing BlobStore: %s", name);
    return blobStore;
  }

  @VisibleForTesting
  void track(final String name, final BlobStore blobStore) {
    log.debug("Tracking: {}", name);
    stores.put(name, blobStore);
  }

  private void untrack(final String name) {
    log.debug("Untracking: {}", name);
    stores.remove(name);
  }

  @Subscribe
  public void on(final BlobStoreDistributedConfigurationEvent event) throws Exception {
    if (!EventHelper.isReplicating()) {
      return;
    }
    final BlobStoreConfiguration configuration = store.read(event.getBlobStoreName());
    String blobStoreName = event.getBlobStoreName();
    EventType eventType = event.getEventType();
    log.debug("Received {} event for the {} blob store", eventType, blobStoreName);

    switch (eventType) {
      case CREATED:
        validateConfiguration(configuration, true);
        doCreate(getBlobStoreForCreate(configuration), configuration);
        break;
      case UPDATED:
        doUpdate(getBlobStoreForUpdate(configuration), configuration);
        break;
      case DELETED:
        BlobStore deletedBlobStore = doForceDelete(blobStoreName);
        eventManager.post(new BlobStoreDeletedEvent(deletedBlobStore));
        break;
      default:
        log.error("Unknown event type {}", eventType);
    }
  }

  @Subscribe
  public void on(final BlobStoreConfigurationCreatedEvent event) {
    handleReplication(event, e -> create(e.getConfiguration()));
  }

  @Subscribe
  public void on(final BlobStoreConfigurationDeletedEvent event) {
    handleReplication(event, e -> forceDelete(e.getName()));
  }

  @Subscribe
  public void on(final BlobStoreConfigurationUpdatedEvent event) {
    handleReplication(event, e -> update(e.getConfiguration()));
  }

  private void handleReplication(
      final BlobStoreConfigurationEvent event,
      final EventConsumer<BlobStoreConfigurationEvent> consumer)
  {
    if (!event.isLocal()) {
      try {
        consumer.accept(event);
      }
      catch (Exception e) {
        log.error("Failed to replicate: {}", event, e);
      }
    }
  }

  @Override
  public long blobStoreUsageCount(final String blobStoreName) {
    long count = 0;
    for (BlobStore otherBlobStore : stores.values()) {
      BlobStoreConfiguration otherBlobStoreConfig = otherBlobStore.getBlobStoreConfiguration();
      BlobStoreDescriptor otherBlobStoreDescriptor = blobStoreDescriptors.get(otherBlobStoreConfig.getType());
      if (otherBlobStoreDescriptor.configHasDependencyOn(otherBlobStoreConfig, blobStoreName)) {
        count += 1;
      }
    }
    return count;
  }

  @Override
  public boolean isConvertable(final String blobStoreName) {
    BlobStore blobStore = get(blobStoreName);
    return blobStore != null && blobStore.isGroupable() && blobStore.isWritable() &&
        !store.findParent(blobStore.getBlobStoreConfiguration().getName()).isPresent() &&
        !hasConflictingTasks(blobStoreName);
  }

  @Override
  public Optional<String> getParent(final String blobStoreName) {
    BlobStore blobStore = get(blobStoreName);
    return blobStore == null ? empty() : store.findParent(blobStoreName).map(BlobStoreConfiguration::getName);
  }

  @Override
  public BlobStoreConfiguration newConfiguration() {
    return store.newConfiguration();
  }

  @Override
  public void validateConfiguration(final BlobStoreConfiguration configuration, final boolean sanitize) {
    checkNotNull(configuration, "configuration param is NULL");
    BlobStoreDescriptor blobStoreDescriptor = blobStoreDescriptors.get(configuration.getType());
    if (sanitize) {
      blobStoreDescriptor.sanitizeConfig(configuration);
    }
    blobStoreDescriptor.validateConfig(configuration);
  }

  @Override
  public BlobSession<?> openSession(final String storeName) {
    return ofNullable(get(storeName)).orElseThrow(() -> new BlobStoreNotFoundException(storeName)).openSession();
  }

  @Override
  public boolean existBlobFile(final BlobId blobId, final BlobStore blobStore) {
    return blobStore.exists(blobId);
  }

  @Override
  public Blob moveBlob(final BlobId blobId, final BlobStore srcBlobStore, final BlobStore destBlobStore) {
    checkNotNull(srcBlobStore);
    checkNotNull(destBlobStore);

    BlobAttributes srcBlobAttributes = srcBlobStore.getBlobAttributes(blobId);
    boolean isSrcDeleted = srcBlobAttributes.isDeleted();

    Map<String, String> headers = srcBlobAttributes.getHeaders();
    InputStream srcInputStream = inputStreamOfBlob(srcBlobStore, blobId);
    Blob newBlob = destBlobStore.create(srcInputStream, headers, blobId);
    destBlobStore.setBlobAttributes(blobId, srcBlobAttributes);

    ensureDeletedStateTransferred(blobId, srcBlobStore, destBlobStore, isSrcDeleted);
    log.debug("Created blobId {} in blob store '{}'", blobId, destBlobStore.getBlobStoreConfiguration().getName());

    try {
      srcBlobStore.deleteHard(blobId);
      log.debug("Removed blobId {} from blob store '{}'", blobId, srcBlobStore.getBlobStoreConfiguration().getName());
    }
    catch (BlobStoreException e) {
      log.warn("Failed to remove blobId {} from blob store '{}'", blobId,
          srcBlobStore.getBlobStoreConfiguration().getName(), e);
    }
    return newBlob;
  }

  /**
   * A blob may be deleted or un-deleted while it is being copied. To ensure that this is captured we need to
   * propagate the change to the destination blob store. Once the copy is complete all updates should be routed to
   * the blob in its new member blob store so it can be assumed that it will no longer change in the source and can be
   * safely deleted.
   */
  private void ensureDeletedStateTransferred(
      final BlobId blobId,
      final BlobStore srcBlobStore,
      final BlobStore destBlobStore,
      final boolean isSrcDeleted)
  {
    BlobAttributes currentSrcAttributes = srcBlobStore.getBlobAttributes(blobId);
    if (currentSrcAttributes != null && currentSrcAttributes.isDeleted() != isSrcDeleted) {
      BlobAttributes outOfDateAttributes = destBlobStore.getBlobAttributes(blobId);
      outOfDateAttributes.setDeleted(!isSrcDeleted);
      destBlobStore.setBlobAttributes(blobId, outOfDateAttributes);
    }
  }

  private InputStream inputStreamOfBlob(final BlobStore blobStore, final BlobId blobId) {
    try {
      return Optional.of(blobId)
          .map(r -> blobStore.get(r, true))
          .map(Blob::getInputStream)
          .orElseThrow(
              () -> new IllegalStateException(format("Unable to get input stream from source %S with blobId: %s",
                  blobStore.getBlobStoreConfiguration().getName(),
                  blobId)));
    }
    catch (BlobStoreException ex) {
      throw new BlobInputStreamException(ex, ex.getBlobId());
    }
  }

  private List<Secret> encryptSensitiveAttributes(
      BlobStoreConfiguration oldBlobStoreConfiguration,
      BlobStoreConfiguration newblobStoreConfiguration)
  {
    String typeKey = newblobStoreConfiguration.getType();
    NestedAttributesMap newBlobStoreTypeData = newblobStoreConfiguration.attributes(typeKey.toLowerCase());
    NestedAttributesMap oldBlobStoreTypeData = oldBlobStoreConfiguration.attributes(typeKey.toLowerCase());
    List<String> sensitiveAttributes = blobStoreDescriptors.get(typeKey).getSensitiveConfigurationFields();
    List<Secret> secrets = new ArrayList<>();
    for (String sensitiveAttrKey : sensitiveAttributes) {
      String value =
          Objects.equals(newBlobStoreTypeData.get(sensitiveAttrKey), oldBlobStoreTypeData.get(sensitiveAttrKey))
              ? oldBlobStoreTypeData.get(sensitiveAttrKey, String.class)
              : newBlobStoreTypeData.get(sensitiveAttrKey, String.class);

      if (value != null) {
        Secret newSecret =
            Objects.equals(newBlobStoreTypeData.get(sensitiveAttrKey), oldBlobStoreTypeData.get(sensitiveAttrKey))
                ? secretService.encryptMaven(BLOBSTORE_CONFIG, secretService.from(value).decrypt(), UserIdHelper.get())
                : secretService.encryptMaven(BLOBSTORE_CONFIG, value.toCharArray(), UserIdHelper.get());

        newBlobStoreTypeData.set(sensitiveAttrKey, newSecret.getId());
        secrets.add(newSecret);
      }
    }
    return secrets;
  }

  private void removeEncryptedAttributes(
      BlobStoreConfiguration oldBlobStoreConfiguration,
      BlobStoreConfiguration newblobStoreConfiguration)
  {
    String typeKey = oldBlobStoreConfiguration.getType();
    NestedAttributesMap newBlobStoreTypeData = newblobStoreConfiguration.attributes(typeKey.toLowerCase());
    NestedAttributesMap oldBlobStoreTypeData = oldBlobStoreConfiguration.attributes(typeKey.toLowerCase());
    List<String> sensitiveAttributes = blobStoreDescriptors.get(typeKey).getSensitiveConfigurationFields();
    log.debug("Removing sensitive attributes from old store configuration. Blob store name: '{}'",
        oldBlobStoreConfiguration.getName());
    for (String sensitiveAttrKey : sensitiveAttributes) {
      if (oldBlobStoreTypeData.get(sensitiveAttrKey) != null &&
          !Objects.equals(newBlobStoreTypeData.get(sensitiveAttrKey), oldBlobStoreTypeData.get(sensitiveAttrKey))) {
        String value = (String) oldBlobStoreTypeData.get(sensitiveAttrKey);
        removeSecret(secretService.from(value));
      }
    }
  }

  private void removeSecret(final Secret secret) {
    try {
      secretService.remove(secret);
    }
    catch (Exception e) {
      log.error("Failed to cleanup secret {} cause {}", secret.getId(), e.getMessage(),
          log.isDebugEnabled() ? e : null);
    }
  }

  @Override
  public Map<String, BlobStore> getByName() {
    return Collections.unmodifiableMap(stores);
  }
}
