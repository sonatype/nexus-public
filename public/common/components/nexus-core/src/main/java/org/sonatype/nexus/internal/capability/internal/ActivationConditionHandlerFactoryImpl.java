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

import org.sonatype.nexus.capability.condition.Conditions;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.internal.capability.ActivationConditionHandler;
import org.sonatype.nexus.internal.capability.ActivationConditionHandlerFactory;
import org.sonatype.nexus.internal.capability.DefaultCapabilityReference;

import org.springframework.stereotype.Component;

@Component
public class ActivationConditionHandlerFactoryImpl
    implements ActivationConditionHandlerFactory
{
  private final EventManager eventManager;

  private final Conditions conditions;

  @Autowired
  public ActivationConditionHandlerFactoryImpl(final EventManager eventManager, final Conditions conditions) {
    this.eventManager = eventManager;
    this.conditions = conditions;
  }

  @Override
  public ActivationConditionHandler create(final DefaultCapabilityReference reference) {
    return new ActivationConditionHandler(eventManager, conditions, reference);
  }
}
