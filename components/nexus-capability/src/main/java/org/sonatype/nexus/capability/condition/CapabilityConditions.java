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

import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Evaluable;

/**
 * Factory of {@link Condition}s related to capabilities.
 *
 * @since capabilities 2.0
 */
public interface CapabilityConditions
{
  /**
   * Creates a new condition that is satisfied when a capability of a specified type exists.
   *
   * @param type class of capability that should exist
   * @return created condition
   */
  Condition capabilityOfTypeExists(CapabilityType type);

  /**
   * Creates a new condition that is satisfied when a capability of a specified type exists and is in an active
   * state.
   *
   * @param type class of capability that should exist and be active
   * @return created condition
   */
  Condition capabilityOfTypeActive(CapabilityType type);

  /**
   * Creates a new condition that is becoming unsatisfied before an capability is updated and becomes satisfied after
   * capability was updated.
   *
   * @return created condition
   */
  Condition passivateCapabilityDuringUpdate();

  /**
   * Creates a new condition that is becoming unsatisfied before an capability is updated and becomes satisfied after
   * capability was updated when one of the enumerated properties changes.
   *
   * @param propertyNames list of properties names that should be checked if updated
   * @return created condition
   */
  Condition passivateCapabilityWhenPropertyChanged(String... propertyNames);

  /**
   * Creates a new condition that is satisfied as long as capability has no failures.
   *
   * @return created condition
   * @since 2.7
   */
  Condition capabilityHasNoFailures();

  /**
   * Creates a new condition that delegates to provided {@link Evaluable} for checking if the condition is satisfied.
   * {@link Evaluable#isSatisfied()} is reevaluated after each update of capability the condition is used for.
   *
   * @param condition delegate
   * @return created condition
   */
  Condition evaluable(Evaluable condition);

  /**
   * Creates a new condition that is satisfied when the capability has no duplicates.
   *
   * @return created condition
   * @since 3.13
   */
  Condition capabilityHasNoDuplicates();
}
