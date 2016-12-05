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
package org.sonatype.nexus.capability.condition.internal;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.condition.ConditionSupport;
import org.sonatype.nexus.common.event.EventManager;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A condition that is satisfied when a capability of a specified type exists.
 *
 * @since capabilities 2.0
 */
public class CapabilityOfTypeExistsCondition
    extends ConditionSupport
{

  private final CapabilityRegistry capabilityRegistry;

  private final ReentrantReadWriteLock bindLock;

  final CapabilityType type;

  final String typeName;

  public CapabilityOfTypeExistsCondition(final EventManager eventManager,
                                         final CapabilityDescriptorRegistry descriptorRegistry,
                                         final CapabilityRegistry capabilityRegistry,
                                         final CapabilityType type)
  {
    super(eventManager);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.type = checkNotNull(type);
    final CapabilityDescriptor descriptor = checkNotNull(descriptorRegistry).get(type);
    typeName = descriptor == null ? type.toString() : descriptor.name();
    bindLock = new ReentrantReadWriteLock();
  }

  @Override
  protected void doBind() {
    try {
      bindLock.writeLock().lock();
      for (final CapabilityReference reference : capabilityRegistry.getAll()) {
        handle(new CapabilityEvent.Created(capabilityRegistry, reference));
      }
    }
    finally {
      bindLock.writeLock().unlock();
    }
    getEventManager().register(this);
  }

  @Override
  public void doRelease() {
    getEventManager().unregister(this);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.Created event) {
    if (!isSatisfied() && type.equals(event.getReference().context().type())) {
      checkAllCapabilities();
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.AfterRemove event) {
    if (isSatisfied() && type.equals(event.getReference().context().type())) {
      checkAllCapabilities();
    }
  }

  void checkAllCapabilities() {
    for (final CapabilityReference ref : capabilityRegistry.getAll()) {
      if (isSatisfiedBy(ref)) {
        setSatisfied(true);
        return;
      }
    }
    setSatisfied(false);
  }

  boolean isSatisfiedBy(final CapabilityReference reference) {
    return type.equals(reference.context().type());
  }

  @Override
  protected void setSatisfied(final boolean satisfied) {
    try {
      bindLock.readLock().lock();
      super.setSatisfied(satisfied);
    }
    finally {
      bindLock.readLock().unlock();
    }
  }

  @Override
  public String toString() {
    return type + " exists";
  }

  @Override
  public String explainSatisfied() {
    return typeName + " exists";
  }

  @Override
  public String explainUnsatisfied() {
    return typeName + " does not exist";
  }

}
