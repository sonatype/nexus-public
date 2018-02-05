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
import org.sonatype.nexus.capability.condition.CapabilityConditions;
import org.sonatype.nexus.capability.condition.Conditions;
import org.sonatype.nexus.capability.condition.CryptoConditions;
import org.sonatype.nexus.capability.condition.LogicalConditions;
import org.sonatype.nexus.capability.condition.NexusConditions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link Conditions}.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class ConditionsImpl
    implements Conditions
{

  private final LogicalConditions logicalConditions;

  private final CapabilityConditions capabilityConditions;

  private final NexusConditions nexusConditions;

  private final CryptoConditions cryptoConditions;

  @Inject
  public ConditionsImpl(final LogicalConditions logicalConditions,
                        final CapabilityConditions capabilityConditions,
                        final NexusConditions nexusConditions,
                        final CryptoConditions cryptoConditions)
  {
    this.logicalConditions = checkNotNull(logicalConditions);
    this.capabilityConditions = checkNotNull(capabilityConditions);
    this.nexusConditions = checkNotNull(nexusConditions);
    this.cryptoConditions = checkNotNull(cryptoConditions);
  }

  @Override
  public LogicalConditions logical() {
    return logicalConditions;
  }

  @Override
  public CapabilityConditions capabilities() {
    return capabilityConditions;
  }

  @Override
  public NexusConditions nexus() {
    return nexusConditions;
  }

  @Override
  public CryptoConditions crypto() {
    return cryptoConditions;
  }

  @Override
  public Condition always(final String reason) {
    return new SatisfiedCondition(reason);
  }

  @Override
  public Condition never(final String reason) {
    return new UnsatisfiedCondition(reason);
  }
}
