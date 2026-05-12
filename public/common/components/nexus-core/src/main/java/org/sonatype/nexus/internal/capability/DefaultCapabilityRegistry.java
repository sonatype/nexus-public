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
package org.sonatype.nexus.internal.capability;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.validation.ValidationException;
import javax.validation.Validator;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptor.ValidationMode;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityEventQueue;
import org.sonatype.nexus.capability.CapabilityFactory;
import org.sonatype.nexus.capability.CapabilityFactoryRegistry;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityNotFoundException;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityRegistryEvent.AfterLoad;
import org.sonatype.nexus.capability.CapabilityRegistryEvent.Ready;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.sonatype.nexus.formfields.Encrypted;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorage;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemCreatedEvent;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemDeletedEvent;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemUpdatedEvent;
import org.sonatype.nexus.security.UserIdHelper;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.sonatype.nexus.capability.CapabilityDescriptor.ValidationMode.CREATE;
import static org.sonatype.nexus.capability.CapabilityDescriptor.ValidationMode.CREATE_NON_EXPOSED;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.CAPABILITIES;

/**
 * Default {@link CapabilityRegistry} implementation.
 */
@Primary
@Singleton
@Component
@ManagedLifecycle(phase = CAPABILITIES)
public class DefaultCapabilityRegistry
    extends StateGuardLifecycleSupport
    implements CapabilityRegistry, EventAware, EventAware.Asynchronous
{

  private final CapabilityStorage capabilityStorage;

  private final CapabilityFactoryRegistry capabilityFactoryRegistry;

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  private final EventManager eventManager;

  private final CapabilityEventQueue capabilityEventQueue;

  private final ActivationConditionHandlerFactory activationConditionHandlerFactory;

  private final ValidityConditionHandlerFactory validityConditionHandlerFactory;

  private final Provider<Validator> validatorProvider;

  @VisibleForTesting
  final Map<CapabilityIdentity, DefaultCapabilityReference> references;

  private final ReentrantLock lock;

  private final SecretsService secretsService;

  private final SecretsStore secretsStore;

  /**
   * Single-threaded executor for processing remote capability events sequentially.
   * Event handlers submit tasks to this executor for async processing.
   * This eliminates deadlock risk by ensuring event handlers never hold locks.
   */
  private ExecutorService capabilitySyncExecutor;

  @Inject
  DefaultCapabilityRegistry(
      final CapabilityStorage capabilityStorage,
      final CapabilityFactoryRegistry capabilityFactoryRegistry,
      final CapabilityDescriptorRegistry capabilityDescriptorRegistry,
      final EventManager eventManager,
      final CapabilityEventQueue capabilityEventQueue,
      final ActivationConditionHandlerFactory activationConditionHandlerFactory,
      final ValidityConditionHandlerFactory validityConditionHandlerFactory,
      final SecretsService secretsService,
      final SecretsStore secretsStore,
      final Provider<Validator> validatorProvider)
  {
    this.capabilityStorage = checkNotNull(capabilityStorage);
    this.capabilityFactoryRegistry = checkNotNull(capabilityFactoryRegistry);
    this.capabilityDescriptorRegistry = checkNotNull(capabilityDescriptorRegistry);
    this.eventManager = checkNotNull(eventManager);
    this.capabilityEventQueue = checkNotNull(capabilityEventQueue);
    this.activationConditionHandlerFactory = checkNotNull(activationConditionHandlerFactory);
    this.validityConditionHandlerFactory = checkNotNull(validityConditionHandlerFactory);
    this.secretsService = checkNotNull(secretsService);
    this.secretsStore = checkNotNull(secretsStore);
    this.validatorProvider = checkNotNull(validatorProvider);

    references = new ConcurrentHashMap<>();
    lock = new ReentrantLock();
  }

  @Override
  protected void doStart() throws Exception {
    load();

    // Start single-threaded executor to process remote capability events
    capabilitySyncExecutor = Executors.newSingleThreadExecutor(
        new NexusThreadFactory("capability-sync", "capability-sync"));

    // fire event when the registry is loaded and ready for use
    eventManager.post(new Ready(this));
  }

  @Override
  protected void doStop() throws Exception {
    if (capabilitySyncExecutor != null) {
      capabilitySyncExecutor.shutdown();
      try {
        if (!capabilitySyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
          capabilitySyncExecutor.shutdownNow();
        }
      }
      catch (InterruptedException e) {
        capabilitySyncExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
      capabilitySyncExecutor = null;
    }
    super.doStop();
  }

  @Override
  public CapabilityReference add(
      final CapabilityType type,
      final boolean enabled,
      @Nullable final String notes,
      @Nullable final Map<String, String> properties)
  {
    return validateAndAdd(type, enabled, notes, properties, CREATE);
  }

  @Override
  public CapabilityReference addNonExposed(
      final CapabilityType type,
      final boolean enabled,
      @Nullable final String notes,
      @Nullable final Map<String, String> properties)
  {
    return validateAndAdd(type, enabled, notes, properties, CREATE_NON_EXPOSED);
  }

  private CapabilityReference validateAndAdd(
      final CapabilityType type,
      final boolean enabled,
      @Nullable final String notes,
      @Nullable final Map<String, String> properties,
      final ValidationMode validationMode)
  {
    checkNotNull(type);

    try {
      lock.lock();

      final Map<String, String> props = properties == null ? Maps.<String, String>newHashMap() : properties;

      validatorProvider.get().validate(type);

      final CapabilityDescriptor descriptor = capabilityDescriptorRegistry.get(type);

      descriptor.validate(null, props, validationMode);

      final Map<String, String> encryptedProps = encryptValuesIfNeeded(descriptor, props, Collections.emptyMap());

      final CapabilityStorageItem item = capabilityStorage.newStorageItem(
          descriptor.version(), type.toString(), enabled, notes, encryptedProps);

      final CapabilityIdentity generatedId;
      try {
        generatedId = capabilityStorage.add(item);
      }
      catch (Exception e) {
        pruneSecretsIfNeeded(descriptor, Collections.emptyMap(), encryptedProps);
        throw e;
      }

      return doAdd(generatedId, type, descriptor, item, props);
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Remote capability creation event handler.
   * DEADLOCK PREVENTION: Event handler never holds locks.
   * Submits to single-threaded executor for sequential processing.
   */
  @Subscribe
  public void on(final CapabilityStorageItemCreatedEvent event) {
    if (!event.isLocal()) {
      CapabilityIdentity id = event.getCapabilityId();
      log.debug("Received remote capability creation event for {}", id);

      // Submit to executor for sequential processing
      capabilitySyncExecutor.submit(() -> processRemoteCapabilityEvent(id));
    }
  }

  @VisibleForTesting
  boolean capabilityAlreadyRegistered(final CapabilityStorageItem capability) {
    return references.values()
        .stream()
        .anyMatch(f -> Objects.equals(f.type().toString(), capability.getType()) &&
            Objects.equals(f.properties(), capability.getProperties()));
  }

  @VisibleForTesting
  boolean capabilityAlreadyUpToDate(final CapabilityIdentity id, final CapabilityStorageItem item) {
    DefaultCapabilityReference reference = references.get(id);
    return reference != null &&
        Objects.equals(reference.properties(), item.getProperties()) &&
        reference.isEnabled() == item.isEnabled();
  }

  private CapabilityReference doAdd(
      final CapabilityIdentity id,
      final CapabilityType type,
      final CapabilityDescriptor descriptor,
      final CapabilityStorageItem item,
      @Nullable final Map<String, String> attributes)
  {
    log.debug("Added capability '{}' of type '{}' with properties '{}'", id, type, item.getProperties());

    DefaultCapabilityReference reference = create(id, type, descriptor);

    reference.setNotes(item.getNotes());
    reference.create(attributes, item.getProperties());
    if (item.isEnabled()) {
      reference.enable();
      reference.activate();
    }

    return reference;
  }

  @Override
  public CapabilityReference update(
      final CapabilityIdentity id,
      final boolean enabled,
      @Nullable final String notes,
      @Nullable final Map<String, String> properties)
  {
    final DefaultCapabilityReference reference;
    final Map<String, String> encryptedProps;
    try {
      lock.lock();

      final Map<String, String> props = properties == null ? Maps.<String, String>newHashMap() : properties;

      reference = require(id);

      reference.descriptor().validate(id, props, ValidationMode.UPDATE);

      encryptedProps = encryptValuesIfNeeded(reference.descriptor(), props, reference.encryptedProperties());

      final CapabilityStorageItem item = capabilityStorage.newStorageItem(
          reference.descriptor().version(), reference.type().toString(), enabled, notes, encryptedProps);

      try {
        capabilityStorage.update(id, item);
      }
      catch (Exception e) {
        pruneSecretsIfNeeded(reference.descriptor(), reference.encryptedProperties(), encryptedProps);
        throw e;
      }
      pruneSecretsIfNeeded(reference.descriptor(), encryptedProps, reference.encryptedProperties());

      return doUpdate(reference, item, props);
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Remote capability update event handler.
   * DEADLOCK PREVENTION: Event handler never holds locks.
   * Submits to single-threaded executor for sequential processing.
   */
  @Subscribe
  public void on(final CapabilityStorageItemUpdatedEvent event) {
    if (!event.isLocal()) {
      CapabilityIdentity id = event.getCapabilityId();
      log.debug("Received remote capability update event for {}", id);

      // Submit to executor for sequential processing
      capabilitySyncExecutor.submit(() -> processRemoteCapabilityEvent(id));
    }
  }

  private CapabilityReference doUpdate(
      final DefaultCapabilityReference reference,
      final CapabilityStorageItem item,
      @Nullable final Map<String, String> attributes)
  {
    log.debug("Updated capability '{}' of type '{}' with properties '{}'",
        reference.id(), reference.type(), item.getProperties());

    if (reference.isEnabled() && !item.isEnabled()) {
      reference.disable();
      log.debug("Disabled capability '{}' for type '{}'", reference.id(), reference.type());
    }
    reference.setNotes(item.getNotes());
    reference.update(attributes, reference.properties(), item.getProperties());
    if (!reference.isEnabled() && item.isEnabled()) {
      reference.enable();
      reference.activate();
      log.debug("Enabled and activated capability '{}' for type '{}'", reference.id(), reference.type());
    }

    return reference;
  }

  @Override
  public CapabilityReference remove(final CapabilityIdentity id) {
    try {
      lock.lock();

      DefaultCapabilityReference reference = require(id);

      capabilityStorage.remove(id);

      pruneSecretsIfNeeded(reference.descriptor(), Collections.emptyMap(), reference.encryptedProperties());

      return doRemove(id);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public CapabilityReference removeNonExposed(final CapabilityIdentity id) {
    try {
      lock.lock();

      DefaultCapabilityReference reference = require(id);

      final Map<String, String> props = reference.properties();

      final CapabilityDescriptor descriptor = capabilityDescriptorRegistry.get(reference.type());

      descriptor.validate(null, props, ValidationMode.DELETE_NON_EXPOSED);

      capabilityStorage.remove(id);

      pruneSecretsIfNeeded(reference.descriptor(), Collections.emptyMap(), reference.encryptedProperties());

      return doRemove(id);
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Remote capability delete event handler.
   * DEADLOCK PREVENTION: Event handler never holds locks.
   * Submits to single-threaded executor for sequential processing.
   */
  @Subscribe
  public void on(final CapabilityStorageItemDeletedEvent event) {
    if (!event.isLocal()) {
      CapabilityIdentity id = event.getCapabilityId();
      log.debug("Received remote capability delete event for {}", id);

      // Submit to executor for sequential processing
      capabilitySyncExecutor.submit(() -> processRemoteCapabilityEvent(id));
    }
  }

  private CapabilityReference doRemove(final CapabilityIdentity id) {
    log.debug("Removed capability '{}'", id);

    DefaultCapabilityReference reference = references.remove(id);
    if (reference != null) {
      reference.remove();
    }

    return reference;
  }

  @Override
  public CapabilityReference enable(final CapabilityIdentity id) {
    final DefaultCapabilityReference reference = require(id);
    return update(reference.context().id(), true, reference.notes(), reference.properties());
  }

  @Override
  public CapabilityReference disable(final CapabilityIdentity id) {
    final DefaultCapabilityReference reference = require(id);
    return update(reference.context().id(), false, reference.notes(), reference.properties());
  }

  @Override
  public DefaultCapabilityReference get(final CapabilityIdentity id) {
    return references.get(id);
  }

  @Override
  public Collection<DefaultCapabilityReference> get(final Predicate<CapabilityReference> filter) {
    return getAll().stream()
        .filter(filter)
        .toList();
  }

  @Override
  public Collection<DefaultCapabilityReference> getAll() {
    return Set.copyOf(references.values());
  }

  public void load() {
    final Map<CapabilityIdentity, CapabilityStorageItem> items = capabilityStorage.getAll();
    for (final Map.Entry<CapabilityIdentity, CapabilityStorageItem> entry : items.entrySet()) {
      CapabilityIdentity id = entry.getKey();
      CapabilityStorageItem item = entry.getValue();

      log.debug(
          "Loading capability '{}' of type '{}' with properties '{}'",
          id, item.getType(), item.getProperties());

      final CapabilityDescriptor descriptor = capabilityDescriptorRegistry.get(capabilityType(item.getType()));

      if (descriptor == null) {
        log.info(
            "Capabilities persistent storage contains a capability of unknown type {} with"
                + " id {}. This capability will not be loaded",
            item.getType(), id);
        continue;
      }

      // Do NOT decrypt secrets automatically - capabilities must decrypt on-demand
      Map<String, String> properties = item.getProperties();
      if (descriptor.version() != item.getVersion()) {
        log.debug(
            "Converting capability '{}' properties from version '{}' to version '{}'",
            id, item.getVersion(), descriptor.version());
        try {
          properties = descriptor.convert(properties, item.getVersion());
          if (properties == null) {
            properties = Collections.emptyMap();
          }
          if (log.isDebugEnabled()) {
            log.debug(
                "Converted capability '{}' properties '{}' (version '{}') to '{}' (version '{}')",
                id, item.getProperties(), item.getVersion(),
                properties, descriptor.version());
          }
        }
        catch (Exception e) {
          log.error(
              "Failed converting capability '{}' properties '{}' from version '{}' to version '{}'."
                  + " Capability will not be loaded",
              id, item.getProperties(), item.getVersion(), descriptor.version(), e);
          continue;
        }
        capabilityStorage.update(id, capabilityStorage.newStorageItem(
            descriptor.version(), item.getType(), item.isEnabled(), item.getNotes(), properties));
      }

      DefaultCapabilityReference reference = references.get(id);
      if (reference != null) {
        // already loaded, update instead...
        doUpdate(reference, item, properties);
        continue;
      }

      reference = create(id, capabilityType(item.getType()), descriptor);

      reference.setNotes(item.getNotes());
      reference.load(properties, item.getProperties());

      try {
        // validate after initial load, so properties are filled in for fixing
        reference.descriptor().validate(id, properties, ValidationMode.LOAD);
      }
      catch (ValidationException e) {
        log.warn("Capability '{}' of type '{}' with properties '{}' is invalid",
            id, item.getType(), item.getProperties(), e);

        reference.setFailure("Load", e); // flag validation issues in the UI
      }

      if (item.isEnabled()) {
        reference.enable();
        reference.activate();
      }
    }
    eventManager.post(new AfterLoad(this));
  }

  @Override
  public void pullAndRefreshReferencesFromDB() {
    Map<CapabilityIdentity, CapabilityStorageItem> refreshedCapabilities = capabilityStorage.getAll();
    references.forEach(
        (capabilityIdentity, capabilityReference) -> Optional.ofNullable(refreshedCapabilities.get(capabilityIdentity)) // When
                                                                                                                        // working
                                                                                                                        // in
                                                                                                                        // HA
                                                                                                                        // mode
                                                                                                                        // it
                                                                                                                        // could
                                                                                                                        // be
                                                                                                                        // null
            .ifPresent(value -> {
              // Do NOT decrypt secrets automatically - capabilities must decrypt on-demand
              doUpdate(capabilityReference, value, value.getProperties());
            }));
  }

  @Override
  public void migrateSecrets(final CapabilityReference capabilityReference, final Predicate<Secret> shouldMigrate) {
    DefaultCapabilityReference reference = (DefaultCapabilityReference) capabilityReference;

    try {
      lock.lock();

      Map<String, String> reEncryptedProps =
          migrateValues(reference.descriptor(), reference.encryptedProperties(), shouldMigrate);

      if (reEncryptedProps.equals(reference.encryptedProperties())) {
        return;
      }

      final CapabilityStorageItem item = capabilityStorage.newStorageItem(
          reference.descriptor().version(), reference.type().toString(), reference.isEnabled(), reference.notes(),
          reEncryptedProps);

      try {
        capabilityStorage.update(reference.id(), item);
      }
      catch (Exception e) {
        pruneSecretsIfNeeded(reference.descriptor(), reference.encryptedProperties(), reEncryptedProps);
        throw e;
      }
      reference.updateEncrypted(reEncryptedProps, reEncryptedProps);
      pruneSecretsIfNeeded(reference.descriptor(), reEncryptedProps, reference.encryptedProperties());
    }
    finally {
      lock.unlock();
    }
  }

  private DefaultCapabilityReference create(
      final CapabilityIdentity id,
      final CapabilityType type,
      final CapabilityDescriptor descriptor)
  {
    final CapabilityFactory factory = capabilityFactoryRegistry.get(type);
    if (factory == null) {
      throw new RuntimeException(format("No factory found for a capability of type %s", type));
    }

    final Capability capability = factory.create();

    final DefaultCapabilityReference reference = createReference(id, type, descriptor, capability);

    references.put(id, reference);

    return reference;
  }

  @VisibleForTesting
  DefaultCapabilityReference createReference(
      final CapabilityIdentity id,
      final CapabilityType type,
      final CapabilityDescriptor descriptor,
      final Capability capability)
  {
    return new DefaultCapabilityReference(
        this,
        capabilityEventQueue,
        activationConditionHandlerFactory,
        validityConditionHandlerFactory,
        id,
        type,
        descriptor,
        capability,
        secretsService,
        secretsStore);
  }

  private DefaultCapabilityReference require(final CapabilityIdentity id) {
    return Optional.ofNullable(get(id)).orElseThrow(() -> new CapabilityNotFoundException(id));
  }

  /**
   * Processes a remote capability event by reading current state from database and syncing.
   * Called by the single-threaded executor for sequential async processing.
   * Uses blocking lock since we're in a dedicated thread.
   */
  private void processRemoteCapabilityEvent(final CapabilityIdentity id) {
    lock.lock();
    try {
      EventHelper.asReplicating(() -> {
        CapabilityStorageItem item = capabilityStorage.read(entityId(id)).orElse(null);

        if (item == null) {
          // Capability was deleted from database - remove from memory if present
          log.debug("Capability {} no longer in storage, removing from memory", id);
          if (references.containsKey(id)) {
            doRemove(id);
          }
          return;
        }

        // Check if already up-to-date before doing work
        if (capabilityAlreadyUpToDate(id, item)) {
          log.debug("Capability {} already up-to-date, skipping sync", id);
          return;
        }

        CapabilityType type = capabilityType(item.getType());
        CapabilityDescriptor descriptor = capabilityDescriptorRegistry.get(type);

        if (descriptor == null) {
          log.warn("Cannot sync capability {} - unknown type {}", id, item.getType());
          return;
        }

        syncCapabilityReference(id, item, descriptor, type);
      });
    }
    finally {
      lock.unlock();
    }
  }

  private void syncCapabilityReference(
      final CapabilityIdentity id,
      final CapabilityStorageItem item,
      final CapabilityDescriptor descriptor,
      final CapabilityType type)
  {
    Map<String, String> decryptedProps = decryptValuesIfNeeded(descriptor, item.getProperties());
    DefaultCapabilityReference existingRef = references.get(id);

    if (existingRef == null) {
      // Create new capability
      log.info("Syncing new capability {} from pending queue", id);
      doAdd(id, type, descriptor, item, decryptedProps);
    }
    else {
      // Update existing capability
      log.info("Syncing updated capability {} from pending queue", id);
      doUpdate(existingRef, item, decryptedProps);
    }
  }

  /**
   * Re encrypts the secrets of the capability (executed by the migration task).
   *
   * @param descriptor capability descriptor
   * @param props capability already encrypted properties
   * @param shouldMigrate predicate to determine if the secret should be re-encrypted
   * @return the re-encrypted properties
   */
  private Map<String, String> migrateValues(
      final CapabilityDescriptor descriptor,
      final Map<String, String> props,
      final Predicate<Secret> shouldMigrate)
  {
    if (props == null || props.isEmpty()) {
      return props;
    }

    Map<String, String> encrypted = Maps.newHashMap(props);
    List<FormField> formFields = descriptor.formFields();

    if (formFields != null) {
      for (FormField formField : formFields) {
        if (formField instanceof Encrypted) {
          String value = encrypted.get(formField.getId());
          if (value != null) {
            Secret oldSecret = secretsService.from(value);
            if (shouldMigrate.apply(oldSecret)) {
              encrypted.put(formField.getId(),
                  secretsService.encryptMaven("capabilities", oldSecret.decrypt(), UserIdHelper.get()).getId());
            }
          }
        }
      }
    }

    return encrypted;
  }

  private boolean isExistingSecretId(final String value) {
    return value.matches("_\\d+") && secretsStore.read(Integer.parseInt(value.substring(1))).isPresent();
  }

  /**
   * Encrypts value of properties marked to be stored encrypted.
   */
  private Map<String, String> encryptValuesIfNeeded(
      final CapabilityDescriptor descriptor,
      final Map<String, String> props,
      final Map<String, String> oldProperties)
  {
    if (props == null || props.isEmpty()) {
      return props;
    }

    Map<String, String> encrypted = Maps.newHashMap(props);
    List<FormField> formFields = descriptor.formFields();

    if (formFields != null) {
      for (FormField formField : formFields) {
        if (formField instanceof Encrypted) {
          String value = encrypted.get(formField.getId());

          if (value != null) {
            String oldSecretId = oldProperties.get(formField.getId());

            if (Objects.equals(oldSecretId, value)) {
              log.debug("Reusing existing secret for field {}", formField.getId());
              // existing secret matches
              encrypted.put(formField.getId(), oldSecretId);
            }
            else if (isExistingSecretId(value)) {
              log.debug("Value for field {} is already a Secret ID, skipping re-encryption",
                  formField.getId());
              encrypted.put(formField.getId(), value);
            }
            else {
              log.debug("Encrypting new value for field {}", formField.getId());
              String newSecretId =
                  secretsService.encryptMaven("capabilities", value.toCharArray(), UserIdHelper.get()).getId();
              encrypted.put(formField.getId(), newSecretId);
            }
          }
        }
      }
    }

    return encrypted;
  }

  /*
   * Attempts to remove secrets which are not used by the persisted capability
   */
  private void pruneSecretsIfNeeded(
      final CapabilityDescriptor descriptor,
      final Map<String, String> persisted,
      final Map<String, String> toBePruned)
  {
    List<FormField> formFields = descriptor.formFields();
    if (formFields != null) {
      for (FormField formField : formFields) {
        if (formField instanceof Encrypted) {
          String pruneCandidate = toBePruned.get(formField.getId());
          String persistedSecret = Optional.ofNullable(persisted)
              .map(m -> m.get(formField.getId()))
              .orElse(null);

          if (pruneCandidate != null && !pruneCandidate.equals(persistedSecret)) {
            try {
              secretsService.remove(secretsService.from(pruneCandidate));
            }
            catch (Exception e) {
              log.warn("Failed to cleanup secret for {} field {}.", descriptor.type(), formField.getId(), e);
            }
          }
        }
      }
    }
  }

  /**
   * Decrypts value of properties marked to be stored encrypted.
   *
   * @since 2.7
   */
  private Map<String, String> decryptValuesIfNeeded( // NOSONAR
      final CapabilityDescriptor descriptor,
      final Map<String, String> props)
  {
    if (props == null || props.isEmpty()) {
      return props;
    }
    Map<String, String> decrypted = Maps.newHashMap(props);
    List<FormField> formFields = descriptor.formFields();
    if (formFields != null) {
      for (FormField formField : formFields) {
        if (formField instanceof Encrypted) {
          String value = decrypted.get(formField.getId());
          if (value != null) {
            try {
              decrypted.put(formField.getId(), String.valueOf(secretsService.from(value).decrypt()));
            }
            catch (Exception e) {
              throw new RuntimeException(
                  "Could not decrypt value of '" + formField.getType() + "' due to " + e.getMessage(), e);
            }
          }
        }
      }
    }
    return decrypted;
  }

  private static EntityId entityId(final CapabilityIdentity id) {
    return new EntityUUID(fromString(id.toString()));
  }

}
