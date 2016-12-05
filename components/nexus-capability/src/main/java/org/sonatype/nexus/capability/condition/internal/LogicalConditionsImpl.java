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

import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.condition.LogicalConditions;
import org.sonatype.nexus.common.event.EventManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link LogicalConditions}.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class LogicalConditionsImpl
    implements LogicalConditions
{

  private final EventManager eventManager;

  @Inject
  public LogicalConditionsImpl(final EventManager eventManager) {
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public Condition and(final Condition... conditions) {
    return new ConjunctionCondition(eventManager, conditions);
  }

  @Override
  public Condition or(final Condition... conditions) {
    return new DisjunctionCondition(eventManager, conditions);
  }

  @Override
  public Condition not(final Condition condition) {
    return new InversionCondition(eventManager, condition);
  }
}
