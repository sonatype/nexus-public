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
import org.sonatype.nexus.capability.condition.NexusConditions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link NexusConditions}.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class NexusConditionsImpl
    implements NexusConditions
{
  private final NexusIsActiveCondition nexusIsActiveCondition;

  @Inject
  public NexusConditionsImpl(final NexusIsActiveCondition nexusIsActiveCondition) {
    this.nexusIsActiveCondition = checkNotNull(nexusIsActiveCondition);
  }

  @Override
  public Condition active() {
    return nexusIsActiveCondition;
  }
}
