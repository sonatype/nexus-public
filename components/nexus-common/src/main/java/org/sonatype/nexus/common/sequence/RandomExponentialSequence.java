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

import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Produces a partially random, exponentially increasing sequence of numeric values.
 *
 * @since 3.0
 */
public class RandomExponentialSequence
    implements NumberSequence
{
  private final long start;

  private final double factor;

  private final double maxDeviation;

  private final Random random = new Random();

  private Long current;

  public static class Builder
  {
    private Builder() {
    }

    private long start = 1;

    private double factor = 2.0f;

    private double maxDeviation = 0.0f;

    public Builder start(int start) {
      checkArgument(start >= 0);
      this.start = start;
      return this;
    }

    public Builder factor(double factor) {
      checkArgument(factor >= 1f);
      this.factor = factor;
      return this;
    }

    /**
     * The maximum magnitude of the random value added or subtracted from the factor before multiplying.
     */
    public Builder maxDeviation(double maxDeviation) {
      checkArgument(maxDeviation >= 0.0);
      this.maxDeviation = maxDeviation;
      return this;
    }

    public RandomExponentialSequence build() {
      return new RandomExponentialSequence(start, factor, maxDeviation);
    }
  }

  public static Builder builder() {
    return new RandomExponentialSequence.Builder();
  }

  private RandomExponentialSequence(final long start, final double factor, final double maxDeviation) {
    this.start = start;
    this.factor = factor;
    this.maxDeviation = maxDeviation;
    reset();
  }

  public long next() {
    // A value from -deviation to +deviation
    double deviation = random.nextDouble() * maxDeviation * (random.nextBoolean() ? 1.0 : -1.0);

    long currentValue = current;
    // Update the value to use next
    current = (long) (Math.max(1, deviation + factor) * current);
    return currentValue;
  }

  @Override
  public long prev() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long peek() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reset() {
    current = start;
  }
}
