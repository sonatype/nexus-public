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

import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.common.event.EventManager;

/**
 * A condition that applies a logical OR between conditions.
 *
 * @since capabilities 2.0
 */
public class DisjunctionCondition
    extends CompositeConditionSupport
    implements Condition
{

  private Condition lastSatisfied;

  public DisjunctionCondition(final EventManager eventManager,
                              final Condition... conditions)
  {
    super(eventManager, conditions);
  }

  @Override
  protected boolean reevaluate(final Condition... conditions) {
    for (final Condition condition : conditions) {
      if (condition.isSatisfied()) {
        lastSatisfied = condition;
        return true;
      }
    }
    lastSatisfied = null;
    return false;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (final Condition condition : getConditions()) {
      if (sb.length() > 0) {
        sb.append(" OR ");
      }
      sb.append(condition);
    }
    return sb.toString();
  }

  @Override
  public String explainSatisfied() {
    if (lastSatisfied != null) {
      return lastSatisfied.explainSatisfied();
    }
    final StringBuilder sb = new StringBuilder();
    for (final Condition condition : getConditions()) {
      if (sb.length() > 0) {
        sb.append(" OR ");
      }
      sb.append(condition.explainSatisfied());
    }
    return sb.toString();
  }

  @Override
  public String explainUnsatisfied() {
    final StringBuilder sb = new StringBuilder();
    for (final Condition condition : getConditions()) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append(condition.explainUnsatisfied());
    }
    return sb.toString();
  }

}
