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
package org.sonatype.nexus.orient.entity;

import java.util.function.Supplier;

/**
 * Potential conflict states - values are ordered from 'least' to 'most' amount of conflict.
 *
 * @since 3.14
 */
public enum ConflictState
{
  /**
   * No differences found so far; the original change could end up as a no-op.
   */
  IGNORE,
  /**
   * Found differences but no conflicts; the original change could still be applied.
   */
  ALLOW,
  /**
   * Found conflicts but they were deconflicted by merging further changes on top.
   */
  MERGE,
  /**
   * Found a conflict that cannot be deconflicted; the request must be denied.
   */
  DENY {
    @Override
    public ConflictState andThen(final Supplier<ConflictState> nextStep) {
      return DENY; // short-circuit any further deconflicting
    }
  };

  /**
   * Invokes the next step and returns whichever state is higher.
   */
  public ConflictState andThen(final Supplier<ConflictState> nextStep) {
    return max(this, nextStep.get());
  }

  public static ConflictState max(final ConflictState lhs, final ConflictState rhs) {
    return lhs.compareTo(rhs) >= 0 ? lhs : rhs;
  }
}
