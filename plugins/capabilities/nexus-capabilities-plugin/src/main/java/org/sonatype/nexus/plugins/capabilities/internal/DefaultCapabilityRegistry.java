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
package org.sonatype.nexus.plugins.capabilities.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.PasswordHelper;
import org.sonatype.nexus.formfields.Encrypted;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.plugins.capabilities.Capability;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptor;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptorRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityEvent;
import org.sonatype.nexus.plugins.capabilities.CapabilityFactory;
import org.sonatype.nexus.plugins.capabilities.CapabilityFactoryRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.CapabilityNotFoundException;
import org.sonatype.nexus.plugins.capabilities.CapabilityReference;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistryEvent;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.ValidatorRegistry;
import org.sonatype.nexus.plugins.capabilities.internal.storage.CapabilityStorage;
import org.sonatype.nexus.plugins.capabilities.internal.storage.CapabilityStorageItem;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static org.sonatype.nexus.plugins.capabilities.CapabilityType.capabilityType;

/**
 * Default {@link CapabilityRegistry} implementation.
 */
@Singleton
@Named
public class DefaultCapabilityRegistry
    extends ComponentSupport
    implements CapabilityRegistry
{

  private final CapabilityStorage capabilityStorage;

  private final Provider<ValidatorRegistry> validatorRegistryProvider;

  private final CapabilityFactoryRegistry capabilityFactoryRegistry;

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  private final EventBus eventBus;

  private final ActivationConditionHandlerFactory activationConditionHandlerFactory;

  private final ValidityConditionHandlerFactory validityConditionHandlerFactory;

  private final PasswordHelper passwordHelper;

  private final Map<CapabilityIdentity, DefaultCapabilityReference> references;

  private final ReentrantReadWriteLock lock;

  @Inject
  DefaultCapabilityRegistry(final CapabilityStorage capabilityStorage,
                            final Provider<ValidatorRegistry> validatorRegistryProvider,
                            final CapabilityFactoryRegistry capabilityFactoryRegistry,
                            final CapabilityDescriptorRegistry capabilityDescriptorRegistry,
                            final EventBus eventBus,
                            final ActivationConditionHandlerFactory activationConditionHandlerFactory,
                            final ValidityConditionHandlerFactory validityConditionHandlerFactory,
                            final PasswordHelper passwordHelper)
  {
    this.capabilityStorage = checkNotNull(capabilityStorage);
    this.validatorRegistryProvider = checkNotNull(validatorRegistryProvider);
    this.capabilityFactoryRegistry = checkNotNull(capabilityFactoryRegistry);
    this.capabilityDescriptorRegistry = checkNotNull(capabilityDescriptorRegistry);
    this.eventBus = checkNotNull(eventBus);
    this.activationConditionHandlerFactory = checkNotNull(activationConditionHandlerFactory);
    this.validityConditionHandlerFactory = checkNotNull(validityConditionHandlerFactory);
    this.passwordHelper = checkNotNull(passwordHelper);

    references = new HashMap<CapabilityIdentity, DefaultCapabilityReference>();
    lock = new ReentrantReadWriteLock();
  }

  @Override
  public CapabilityReference add(final CapabilityType type,
                                 final boolean enabled,
                                 final String notes,
                                 final Map<String, String> properties)
      throws InvalidConfigurationException, IOException
  {
    try {
      lock.writeLock().lock();

      final Map<String, String> props = properties == null ? Maps.<String, String>newHashMap() : properties;

      validateType(type);

      validate(checkNotNull(validatorRegistryProvider.get()).get(type), props);

      final CapabilityDescriptor descriptor = capabilityDescriptorRegistry.get(type);

      final Map<String, String> encryptedProps = encryptValuesIfNeeded(descriptor, props);

      final CapabilityIdentity generatedId = capabilityStorage.add(new CapabilityStorageItem(
          descriptor.version(), type.toString(), enabled, notes, encryptedProps
      ));

      log.debug("Added capability '{}' of type '{}' with properties '{}'", generatedId, type, encryptedProps);

      final DefaultCapabilityReference reference = create(generatedId, type, descriptor);

      reference.setNotes(notes);
      reference.create(props);
      if (enabled) {
        reference.enable();
        reference.activate();
      }

      return reference;
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public CapabilityReference update(final CapabilityIdentity id,
                                    final boolean enabled,
                                    final String notes,
                                    final Map<String, String> properties)
      throws InvalidConfigurationException, IOException
  {
    try {
      lock.writeLock().lock();

      final Map<String, String> props = properties == null ? Maps.<String, String>newHashMap() : properties;

      validateId(id);

      validate(checkNotNull(validatorRegistryProvider.get()).get(id), props);

      final DefaultCapabilityReference reference = get(id);

      final Map<String, String> encryptedProps = encryptValuesIfNeeded(reference.descriptor(), props);

      capabilityStorage.update(id, new CapabilityStorageItem(
          reference.descriptor().version(), reference.type().toString(), enabled, notes, encryptedProps)
      );

      log.debug(
          "Updated capability '{}' of type '{}' with properties '{}'", id, reference.type(), encryptedProps
      );

      if (reference.isEnabled() && !enabled) {
        reference.disable();
      }
      reference.setNotes(notes);
      reference.update(props, reference.properties());
      if (!reference.isEnabled() && enabled) {
        reference.enable();
        reference.activate();
      }

      return reference;
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public CapabilityReference remove(final CapabilityIdentity id)
      throws IOException
  {
    try {
      lock.writeLock().lock();

      validateId(id);

      capabilityStorage.remove(id);
      log.debug("Removed capability with '{}'", id);

      final DefaultCapabilityReference reference = references.remove(id);
      if (reference != null) {
        reference.remove();
      }
      return reference;
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public CapabilityReference enable(final CapabilityIdentity id)
      throws IOException
  {
    try {
      lock.writeLock().lock();

      validateId(id);

      final DefaultCapabilityReference reference = get(id);

      try {
        return update(reference.context().id(), true, reference.notes(), reference.properties());
      }
      catch (InvalidConfigurationException e) {
        throw new RuntimeException("Unexpected", e);
      }
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public CapabilityReference disable(final CapabilityIdentity id)
      throws IOException
  {
    try {
      lock.writeLock().lock();

      validateId(id);

      final DefaultCapabilityReference reference = get(id);

      try {
        return update(reference.context().id(), false, reference.notes(), reference.properties());
      }
      catch (InvalidConfigurationException e) {
        throw new RuntimeException("Unexpected", e);
      }
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public DefaultCapabilityReference get(final CapabilityIdentity id) {
    try {
      lock.readLock().lock();

      return references.get(id);
    }
    finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Collection<DefaultCapabilityReference> get(final Predicate<CapabilityReference> filter) {
    return unmodifiableCollection(Collections2.filter(getAll(), filter));
  }

  @Override
  public Collection<DefaultCapabilityReference> getAll() {
    try {
      lock.readLock().lock();

      return ImmutableList.copyOf(references.values());
    }
    finally {
      lock.readLock().unlock();
    }
  }

  public void load()
      throws IOException
  {
    final Map<CapabilityIdentity, CapabilityStorageItem> items = capabilityStorage.getAll();
    for (final Map.Entry<CapabilityIdentity, CapabilityStorageItem> entry : items.entrySet()) {
      CapabilityIdentity id = entry.getKey();
      CapabilityStorageItem item = entry.getValue();

      log.debug(
          "Loading capability '{}' of type '{}' with properties '{}'",
          id, item.getType(), item.getProperties()
      );

      final CapabilityDescriptor descriptor = capabilityDescriptorRegistry.get(capabilityType(item.getType()));

      if (descriptor == null) {
        log.warn(
            "Capabilities persistent storage (capabilities.xml?) contains an capability of unknown type {} with"
                + " id {}. This capability will not be loaded", item.getType(), id
        );
        continue;
      }

      Map<String, String> properties = decryptValuesIfNeeded(descriptor, item.getProperties());
      if (descriptor.version() != item.getVersion()) {
        log.debug(
            "Converting capability '{}' properties from version '{}' to version '{}'",
            id, item.getVersion(), descriptor.version()
        );
        try {
          properties = descriptor.convert(properties, item.getVersion());
          if (properties == null) {
            properties = Collections.emptyMap();
          }
          if (log.isDebugEnabled()) {
            log.debug(
                "Converted capability '{}' properties '{}' (version '{}') to '{}' (version '{}')",
                id, item.getProperties(), item.getVersion(),
                encryptValuesIfNeeded(descriptor, properties), descriptor.version()
            );
          }
        }
        catch (Exception e) {
          log.error(
              "Failed converting capability '{}' properties '{}' from version '{}' to version '{}'."
                  + " Capability will not be loaded",
              id, item.getProperties(), item.getVersion(), descriptor.version(), e
          );
          continue;
        }
        capabilityStorage.update(id, new CapabilityStorageItem(
            descriptor.version(), item.getType(), item.isEnabled(), item.getNotes(), properties)
        );
      }

      final DefaultCapabilityReference reference = create(id, capabilityType(item.getType()), descriptor);

      reference.setNotes(item.getNotes());
      reference.load(properties);
      if (item.isEnabled()) {
        reference.enable();
        reference.activate();
      }
    }
    eventBus.post(new CapabilityRegistryEvent.AfterLoad(this));
  }

  private DefaultCapabilityReference create(final CapabilityIdentity id,
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

    log.debug("Created capability '{}'", capability);

    eventBus.post(new CapabilityEvent.Created(this, reference));

    return reference;
  }

  @VisibleForTesting
  DefaultCapabilityReference createReference(final CapabilityIdentity id,
                                             final CapabilityType type,
                                             final CapabilityDescriptor descriptor,
                                             final Capability capability)
  {
    return new DefaultCapabilityReference(
        this,
        eventBus,
        activationConditionHandlerFactory,
        validityConditionHandlerFactory,
        id,
        type,
        descriptor,
        capability
    );
  }

  private void validateType(final CapabilityType type)
      throws InvalidConfigurationException
  {
    final ValidationResponse vr = new ValidationResponse();

    if (type == null) {
      vr.addValidationError(new ValidationMessage("typeId", "Type must be provided"));
    }
    else {
      if (capabilityFactoryRegistry.get(type) == null || capabilityDescriptorRegistry.get(type) == null) {
        vr.addValidationError(new ValidationMessage("typeId", "Type '" + type + "' is not supported"));
      }
    }

    if (vr.getValidationErrors().size() > 0) {
      throw new InvalidConfigurationException(vr);
    }
  }

  private void validateId(final CapabilityIdentity id)
      throws CapabilityNotFoundException
  {
    if (get(id) == null) {
      throw new CapabilityNotFoundException(id);
    }
  }

  private void validate(final Collection<Validator> validators, final Map<String, String> properties)
      throws InvalidConfigurationException
  {
    if (validators != null && !validators.isEmpty()) {
      final ValidationResponse vr = new ValidationResponse();

      for (final Validator validator : validators) {
        final ValidationResult validationResult = validator.validate(properties);
        if (!validationResult.isValid()) {
          for (final ValidationResult.Violation violation : validationResult.violations()) {
            vr.addValidationError(new ValidationMessage(
                violation.key(),
                violation.message()
            ));
          }
        }
      }

      if (vr.getValidationErrors().size() > 0) {
        throw new InvalidConfigurationException(vr);
      }
    }
  }

  /**
   * Encrypts value of properties marked to be stored encrypted.
   *
   * @since 2.7
   */
  private Map<String, String> encryptValuesIfNeeded(final CapabilityDescriptor descriptor,
                                                    final Map<String, String> props) throws IOException
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
            try {
              encrypted.put(formField.getId(), passwordHelper.encrypt(value));
            }
            catch (PlexusCipherException e) {
              throw new IOException(
                  "Could not encrypt value of '" + formField.getType() + "' due to " + e.getMessage(), e
              );
            }
          }
        }
      }
    }
    return encrypted;
  }

  /**
   * Decrypts value of properties marked to be stored encrypted.
   *
   * @since 2.7
   */
  private Map<String, String> decryptValuesIfNeeded(final CapabilityDescriptor descriptor,
                                                    final Map<String, String> props) throws IOException
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
              decrypted.put(formField.getId(), passwordHelper.decrypt(value));
            }
            catch (PlexusCipherException e) {
              throw new IOException(
                  "Could not decrypt value of '" + formField.getType() + "' due to " + e.getMessage(), e
              );
            }
          }
        }
      }
    }
    return decrypted;
  }

}
