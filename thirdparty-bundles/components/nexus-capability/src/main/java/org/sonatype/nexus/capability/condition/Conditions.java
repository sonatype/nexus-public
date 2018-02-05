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
package org.sonatype.nexus.capability.condition;

import org.sonatype.nexus.capability.Condition;

/**
 * Central access point for built in {@link Condition}s.
 *
 * @since capabilities 2.0
 */
public interface Conditions
{
  /**
   * Access to logical conditions.
   *
   * @return logical conditions factory
   */
  LogicalConditions logical();

  /**
   * Access to capability related conditions.
   *
   * @return capability related conditions factory
   */
  CapabilityConditions capabilities();

  /**
   * Access to nexus specific conditions.
   *
   * @return nexus specific conditions factory
   */
  NexusConditions nexus();

  /**
   * Access to crypto specific conditions.
   *
   * @since 2.7
   */
  CryptoConditions crypto();

  /**
   * Creates a new condition that is always satisfied for the specified reason.
   * 
   * @since 3.1
   */
  Condition always(String reason);

  /**
   * Creates a new condition that is never satisfied for the specified reason.
   * 
   * @since 3.1
   */
  Condition never(String reason);
}
