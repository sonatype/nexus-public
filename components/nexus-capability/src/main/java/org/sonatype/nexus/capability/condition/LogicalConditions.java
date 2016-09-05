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
 * Factory of logical {@link Condition}s.
 *
 * @since capabilities 2.0
 */
public interface LogicalConditions
{
  /**
   * Creates a new condition that is satisfied when both conditions are satisfied (logical AND).
   *
   * @param conditions to be AND-ed
   * @return created condition
   */
  Condition and(Condition... conditions);

  /**
   * Creates a new condition that is satisfied when at least one condition is satisfied (logical OR).
   *
   * @param conditions to be OR-ed
   * @return created condition
   */
  Condition or(Condition... conditions);

  /**
   * Creates a new condition that is satisfied when at another condition is not satisfied (logical NOT).
   *
   * @param condition negated condition
   * @return created condition
   */
  Condition not(Condition condition);
}
