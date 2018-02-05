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
package org.sonatype.nexus.common.sequence;

/**
 * Number sequence that wraps another sequence and imposes a lower limit to it.
 */
public class LowerLimitNumberSequence
    extends NumberSequenceWrapper
{
  private long lowerLimit;

  public LowerLimitNumberSequence(final NumberSequence numberSequence, final long lowerLimit) {
    super(numberSequence);
    this.lowerLimit = lowerLimit;
  }

  public long getLowerLimit() {
    return lowerLimit;
  }

  public void setLowerLimit(long lowerLimit) {
    this.lowerLimit = lowerLimit;
  }

  @Override
  public long prev() {
    // we allow only "one step" under the limit
    // when we reach that, we do not want state change on wrapped sequence
    // (next will be "reset" anyway)
    final long wrapped = super.peek();

    if (wrapped < lowerLimit) {
      // we are already "one step" below
      return lowerLimit;
    }
    else {
      // perform the state change
      final long wrappedPrev = super.prev();

      if (wrappedPrev < lowerLimit) {
        // this one step made it
        return lowerLimit;
      }
      else {
        // we are still good
        return wrappedPrev;
      }
    }
  }

  @Override
  public long peek() {
    final long wrapped = super.peek();

    if (wrapped < lowerLimit) {
      return lowerLimit;
    }
    else {
      return wrapped;
    }
  }
}
