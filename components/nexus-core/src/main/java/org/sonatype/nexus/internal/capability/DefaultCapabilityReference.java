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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityEvent.CallbackFailure;
import org.sonatype.nexus.capability.CapabilityEvent.CallbackFailureCleared;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.event.EventManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Default {@link CapabilityReference} implementation.
 *
 * @since capabilities 2.0
 */
public class DefaultCapabilityReference
    extends ComponentSupport
    implements CapabilityReference, CapabilityContext
{

  private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

  private final CapabilityIdentity id;

  private final CapabilityType type;

  private final CapabilityDescriptor descriptor;

  private final Capability capability;

  private String notes;

  private final CapabilityRegistry capabilityRegistry;

  private final EventManager eventManager;

  private final ActivationConditionHandler activationHandler;

  private final ValidityConditionHandler validityHandler;

  private final ReentrantReadWriteLock stateLock;

  private Map<String, String> encryptedProperties;

  private Map<String, String> capabilityProperties;

  private State state;

  private Exception failure;

  private String failingAction;

  DefaultCapabilityReference(final CapabilityRegistry capabilityRegistry,
                             final EventManager eventManager,
                             final ActivationConditionHandlerFactory activationListenerFactory,
                             final ValidityConditionHandlerFactory validityConditionHandlerFactory,
                             final CapabilityIdentity id,
                             final CapabilityType type,
                             final CapabilityDescriptor descriptor,
                             final Capability capability)
  {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.eventManager = checkNotNull(eventManager);

    this.id = checkNotNull(id);
    this.type = checkNotNull(type);
    this.descriptor = checkNotNull(descriptor);
    this.capability = checkNotNull(capability);
    capabilityProperties = EMPTY_MAP;

    state = new NewState();
    stateLock = new ReentrantReadWriteLock();
    activationHandler = checkNotNull(activationListenerFactory).create(this);
    validityHandler = checkNotNull(validityConditionHandlerFactory).create(this);

    capability.init(this);
  }

  @Override
  public Capability capability() {
    return capability;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Capability> T capabilityAs(final Class<T> type) {
    return (T) capability;
  }

  @Override
  public CapabilityIdentity id() {
    return id;
  }

  @Override
  public CapabilityType type() {
    return type;
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return descriptor;
  }

  @Override
  public String notes() {
    return notes;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
  }

  @Override
  public boolean isEnabled() {
    try {
      stateLock.readLock().lock();
      return state.isEnabled();
    }
    finally {
      stateLock.readLock().unlock();
    }
  }

  /**
   * Enables the referenced capability.
   */
  public void enable() {
    try {
      stateLock.writeLock().lock();
      state.enable();
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Disables the referenced capability.
   */
  public void disable() {
    try {
      stateLock.writeLock().lock();
      state.disable();
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  @Override
  public boolean isActive() {
    try {
      stateLock.readLock().lock();
      return state.isActive();
    }
    finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public boolean hasFailure() {
    return failure() != null;
  }

  @Override
  public Exception failure() {
    try {
      stateLock.readLock().lock();
      return failure;
    }
    finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public String failingAction() {
    try {
      stateLock.readLock().lock();
      return failingAction;
    }
    finally {
      stateLock.readLock().unlock();
    }
  }

  /**
   * Activates the referenced capability.
   */
  public void activate() {
    try {
      stateLock.writeLock().lock();
      state.activate();
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Passivate the referenced capability.
   */
  public void passivate() {
    try {
      stateLock.writeLock().lock();
      state.passivate();
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  @Override
  public CapabilityContext context() {
    return this;
  }

  /**
   * Callback when a new capability is created.
   *
   * @param properties capability configuration
   */
  public void create(final Map<String, String> properties, final Map<String, String> encryptedProperties) {
    try {
      stateLock.writeLock().lock();
      state.create(properties, encryptedProperties);
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Callback when a capability configuration is loaded from persisted store (configuration file).
   *
   * @param properties capability configuration
   */
  public void load(final Map<String, String> properties, final Map<String, String> encryptedProperties) {
    try {
      stateLock.writeLock().lock();
      state.load(properties, encryptedProperties);
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Callback when a capability configuration is updated.
   *
   * @param properties         capability configuration
   * @param previousProperties previous capability configuration
   * @param encryptedProperties capability configuration with encrypted properties
   */
  public void update(
      final Map<String, String> properties,
      final Map<String, String> previousProperties,
      final Map<String, String> encryptedProperties)
  {
    update(properties, previousProperties, encryptedProperties, false);
  }

  /**
   * Updates encrypted properties.
   *
   * @param properties          capability configuration
   * @param encryptedProperties encrypted capability configuration
   */
  public void updateEncrypted(
      final Map<String, String> properties,
      final Map<String, String> encryptedProperties)
  {
    update(properties, properties, encryptedProperties, true);
  }

  private void update(
      final Map<String, String> properties,
      final Map<String, String> previousProperties,
      final Map<String, String> encryptedProperties,
      final boolean force)
  {
    if (force || !sameProperties(previousProperties, properties)) {
      try {
        stateLock.writeLock().lock();
        state.update(properties, previousProperties, encryptedProperties);
      }
      finally {
        stateLock.writeLock().unlock();
      }
    }
  }

  /**
   * Callback when a capability configuration is removed.
   */
  public void remove() {
    try {
      stateLock.writeLock().lock();
      state.remove();
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  @Override
  public Map<String, String> properties() {
    try {
      stateLock.readLock().lock();
      return capabilityProperties;
    }
    finally {
      stateLock.readLock().unlock();
    }
  }

  public Map<String, String> encryptedProperties() {
    try {
      stateLock.readLock().lock();
      return encryptedProperties;
    }
    finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public String stateDescription() {
    try {
      stateLock.readLock().lock();
      return state.stateDescription();
    }
    finally {
      stateLock.readLock().unlock();
    }
  }

  @Override
  public String toString() {
    return String.format("capability %s (enabled=%s, active=%s)", capability, isEnabled(), isActive());
  }

  // @TestAccessible //
  static boolean sameProperties(final Map<String, String> p1, final Map<String, String> p2) {
    if (p1 == null) {
      return p2 == null;
    }
    else if (p2 == null) {
      return false;
    }
    return p1.size() == p2.size() && p1.equals(p2);
  }

  private void resetFailure() {
    try {
      stateLock.writeLock().lock();
      if (failure != null) {
        failure = null;
        failingAction = null;
        eventManager.post(new CallbackFailureCleared(capabilityRegistry, this));
      }
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  void setFailure(final String action, final Exception e) {
    try {
      stateLock.writeLock().lock();
      failure = checkNotNull(e);
      failingAction = checkNotNull(action);
      log.error("Could not {} capability {} ({})", action.toLowerCase(), capability, id, e);
      eventManager.post(new CallbackFailure(capabilityRegistry, this, action, e));
    }
    finally {
      stateLock.writeLock().unlock();
    }
  }

  private class State
  {

    State() {
      log.debug("Capability {} ({}) state changed to {}", capability, id, this);
    }

    public boolean isEnabled() {
      return false;
    }

    public void enable() {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'enable' operation");
    }

    public void disable() {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'disable' operation");
    }

    public boolean isActive() {
      return false;
    }

    public void activate() {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'activate' operation");
    }

    public void passivate() {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'passivate' operation");
    }

    public void create(final Map<String, String> properties, final Map<String, String> encryptedProperties) {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'create' operation");
    }

    public void load(final Map<String, String> properties, final Map<String, String> encryptedProperties) {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'load' operation");
    }

    public void update(
        final Map<String, String> properties,
        final Map<String, String> previousProperties,
        final Map<String, String> encryptedProperties)
    {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'update' operation");
    }

    public void remove() {
      throw new IllegalStateException("State '" + toString() + "' does not permit 'remove' operation");
    }

    public String stateDescription() {
      return "Undefined";
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }

  }

  private class NewState
      extends State
  {

    @Override
    public void create(final Map<String, String> properties, final Map<String, String> encryptedProperties) {
      try {
        log.debug("Creating capability {} ({})", capability, id);
        capabilityProperties = properties == null ? EMPTY_MAP : unmodifiableMap(newHashMap(properties));
        DefaultCapabilityReference.this.encryptedProperties = encryptedProperties == null ? EMPTY_MAP : unmodifiableMap(encryptedProperties);
        eventManager.post(new CapabilityEvent.Created(capabilityRegistry, DefaultCapabilityReference.this));
        capability.onCreate();
        resetFailure();
        log.debug("Created capability {} ({})", capability, id);
      }
      catch (Exception e) {
        setFailure("Create", e);
      }
      finally {
        validityHandler.bind();
        state = new DisabledState();
      }
    }

    @Override
    public void load(final Map<String, String> properties, final Map<String, String> encryptedProperties) {
      try {
        log.debug("Loading capability {} ({})", capability, id);
        capabilityProperties = properties == null ? EMPTY_MAP : unmodifiableMap(newHashMap(properties));
        DefaultCapabilityReference.this.encryptedProperties = encryptedProperties == null ? EMPTY_MAP : unmodifiableMap(encryptedProperties);
        eventManager.post(new CapabilityEvent.Created(capabilityRegistry, DefaultCapabilityReference.this));
        capability.onLoad();
        resetFailure();
        log.debug("Loaded capability {} ({})", capability, id);
      }
      catch (Exception e) {
        setFailure("Load", e);
      }
      finally {
        validityHandler.bind();
        state = new DisabledState();
      }
    }

    @Override
    public String stateDescription() {
      return "New";
    }

    @Override
    public String toString() {
      return "NEW";
    }

  }

  private class DisabledState
      extends State
  {

    @Override
    public void enable() {
      log.debug("Enabling capability {} ({})", capability, id);
      state = new EnabledState();
      activationHandler.bind();
    }

    @Override
    public void disable() {
      // do nothing (not yet enabled)
    }

    @Override
    public void passivate() {
      // do nothing (not yet activated)
    }

    @Override
    public void update(
        final Map<String, String> properties,
        final Map<String, String> previousProperties,
        final Map<String, String> encryptedProperties)
    {
      try {
        log.debug("Updating capability {} ({})", capability, id);
        eventManager.post(
            new CapabilityEvent.BeforeUpdate(
                capabilityRegistry, DefaultCapabilityReference.this, properties, previousProperties
            )
        );
        capabilityProperties = properties == null ? EMPTY_MAP : unmodifiableMap(newHashMap(properties));
        DefaultCapabilityReference.this.encryptedProperties = encryptedProperties == null ? EMPTY_MAP : unmodifiableMap(encryptedProperties);
        capability.onUpdate();
        resetFailure();
        log.debug("Updated capability {} ({})", capability, id);
      }
      catch (Exception e) {
        setFailure("Update", e);
      }
      finally {
        eventManager.post(
            new CapabilityEvent.AfterUpdate(
                capabilityRegistry, DefaultCapabilityReference.this, properties, previousProperties
            )
        );
      }
    }

    @Override
    public void remove() {
      try {
        log.debug("Removing capability {} ({})", capability, id);
        DefaultCapabilityReference.this.disable();
        validityHandler.release();
        capability.onRemove();
        resetFailure();
        log.debug("Removed capability {} ({})", capability, id);
      }
      catch (Exception e) {
        setFailure("Remove", e);
      }
      finally {
        state = new RemovedState();
        eventManager.post(
            new CapabilityEvent.AfterRemove(capabilityRegistry, DefaultCapabilityReference.this)
        );
      }
    }

    @Override
    public String stateDescription() {
      return "Disabled";
    }

    @Override
    public String toString() {
      return "DISABLED";
    }

  }

  private class EnabledState
      extends DisabledState
  {

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public void enable() {
      // do nothing (already enabled)
    }

    @Override
    public void disable() {
      log.debug("Disabling capability {} ({})", capability, id);
      activationHandler.release();
      DefaultCapabilityReference.this.passivate();
      state = new DisabledState();
    }

    @Override
    public void activate() {
      if (activationHandler.isConditionSatisfied()) {
        log.debug("Activating capability {} ({})", capability, id);
        try {
          capability.onActivate();
          resetFailure();
          log.debug("Activated capability {} ({})", capability, id);
          state = new ActiveState();
          eventManager.post(
              new CapabilityEvent.AfterActivated(capabilityRegistry, DefaultCapabilityReference.this)
          );
        }
        catch (Exception e) {
          setFailure("Activate", e);
        }
      }
      else {
        log.debug("Capability {} ({}) is not yet activatable", capability, id);
      }
    }

    @Override
    public void passivate() {
      // do nothing (not yet activated)
    }

    @Override
    public String stateDescription() {
      return activationHandler.isConditionSatisfied() ? "Enabled" : activationHandler.explainWhyNotSatisfied();
    }

    @Override
    public String toString() {
      return "ENABLED";
    }

  }

  private class ActiveState
      extends EnabledState
  {

    @Override
    public boolean isActive() {
      return true;
    }

    @Override
    public void activate() {
      // do nothing (already active)
    }

    @Override
    public void passivate() {
      log.debug("Passivating capability {} ({})", capability, id);
      try {
        state = new EnabledState();
        eventManager.post(
            new CapabilityEvent.BeforePassivated(capabilityRegistry, DefaultCapabilityReference.this)
        );
        capability.onPassivate();
        log.debug("Passivated capability {} ({})", capability, id);
      }
      catch (Exception e) {
        setFailure("Passivate", e);
      }
    }

    @Override
    public String stateDescription() {
      return "Active";
    }

    @Override
    public String toString() {
      return "ACTIVE";
    }

  }

  public class RemovedState
      extends State
  {

    @Override
    public String stateDescription() {
      return "Removed";
    }

    @Override
    public String toString() {
      return "REMOVED";
    }

  }

}
