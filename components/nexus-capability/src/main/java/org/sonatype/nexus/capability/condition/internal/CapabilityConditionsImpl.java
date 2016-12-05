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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Evaluable;
import org.sonatype.nexus.capability.condition.CapabilityConditions;
import org.sonatype.nexus.common.event.EventManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link CapabilityConditions}.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class CapabilityConditionsImpl
    implements CapabilityConditions
{

  private final CapabilityRegistry capabilityRegistry;

  private final EventManager eventManager;

  private final CapabilityDescriptorRegistry descriptorRegistry;

  @Inject
  public CapabilityConditionsImpl(final EventManager eventManager,
                                  final CapabilityDescriptorRegistry descriptorRegistry,
                                  final CapabilityRegistry capabilityRegistry)
  {
    this.descriptorRegistry = checkNotNull(descriptorRegistry);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public Condition capabilityOfTypeExists(final CapabilityType type) {
    return new CapabilityOfTypeExistsCondition(eventManager, descriptorRegistry, capabilityRegistry, type);
  }

  @Override
  public Condition capabilityOfTypeActive(final CapabilityType type) {
    return new CapabilityOfTypeActiveCondition(eventManager, descriptorRegistry, capabilityRegistry, type);
  }

  @Override
  public Condition passivateCapabilityDuringUpdate() {
    return new PassivateCapabilityDuringUpdateCondition(eventManager);
  }

  @Override
  public Condition passivateCapabilityWhenPropertyChanged(final String... propertyNames) {
    return new PassivateCapabilityDuringUpdateCondition(eventManager, propertyNames);
  }

  @Override
  public Condition capabilityHasNoFailures() {
    return new CapabilityHasNoFailuresCondition(eventManager);
  }

  @Override
  public Condition evaluable(final Evaluable condition) {
    return new EvaluableCondition(eventManager, condition);
  }
}
