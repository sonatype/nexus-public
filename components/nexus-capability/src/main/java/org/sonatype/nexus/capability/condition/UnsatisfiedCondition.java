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
 * A condition that is never satisfied.
 *
 * @since capabilities 2.0
 */
public class UnsatisfiedCondition
    implements Condition
{

  private final String reason;

  public UnsatisfiedCondition(final String reason) {
    this.reason = reason;
  }

  @Override
  public boolean isSatisfied() {
    return false;
  }

  @Override
  public UnsatisfiedCondition bind() {
    // do nothing
    return this;
  }

  @Override
  public UnsatisfiedCondition release() {
    // do nothing
    return this;
  }

  @Override
  public String toString() {
    return reason;
  }

  @Override
  public String explainSatisfied() {
    return "Not " + reason;
  }

  @Override
  public String explainUnsatisfied() {
    return reason;
  }
}
