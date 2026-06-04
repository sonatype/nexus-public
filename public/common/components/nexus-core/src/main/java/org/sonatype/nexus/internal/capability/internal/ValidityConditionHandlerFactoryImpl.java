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
package org.sonatype.nexus.internal.capability.internal;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.condition.Conditions;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.internal.capability.DefaultCapabilityReference;
import org.sonatype.nexus.internal.capability.ValidityConditionHandler;
import org.sonatype.nexus.internal.capability.ValidityConditionHandlerFactory;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ValidityConditionHandlerFactoryImpl
    implements ValidityConditionHandlerFactory
{
  private final EventManager eventManager;

  private final CapabilityRegistry capabilityRegistry;

  private final Conditions conditions;

  @Autowired
  public ValidityConditionHandlerFactoryImpl(
      final EventManager eventManager,
      @Lazy final CapabilityRegistry capabilityRegistry,
      final Conditions conditions)
  {
    this.eventManager = eventManager;
    this.capabilityRegistry = capabilityRegistry;
    this.conditions = conditions;
  }

  @Override
  public ValidityConditionHandler create(final DefaultCapabilityReference reference) {
    return new ValidityConditionHandler(eventManager, capabilityRegistry, conditions, reference);
  }
}
