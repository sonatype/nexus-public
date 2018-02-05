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
 * A simple linear number sequence (linear equation).
 */
public class LinearNumberSequence
    implements NumberSequence
{
  private final long start;

  private final long step;

  private final long multiplier;

  private final long shift;

  private long current;

  public LinearNumberSequence(final long start, final long step, final long multiplier, final long shift) {
    this.start = start;
    this.step = step;
    this.multiplier = multiplier;
    this.shift = shift;
  }

  @Override
  public long next() {
    current = current + step;
    return peek();
  }

  @Override
  public long prev() {
    current = current - step;
    return peek();
  }

  @Override
  public long peek() {
    return (current * multiplier) + shift;
  }

  @Override
  public void reset() {
    this.current = start;
  }
}
