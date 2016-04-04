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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests [@link RandomExponentialSequence}
 */
public class RandomExponentialSequenceTest
{
  @Test
  public void noRandomness() {
    RandomExponentialSequence seq = RandomExponentialSequence.builder()
        .start(1)
        .factor(2)
        .maxDeviation(0.0f)
        .build();

    Assert.assertThat(seq.next(), Matchers.is(1L));
    Assert.assertThat(seq.next(), Matchers.is(2L));
    Assert.assertThat(seq.next(), Matchers.is(4L));
    Assert.assertThat(seq.next(), Matchers.is(8L));
    Assert.assertThat(seq.next(), Matchers.is(16L));
  }

  @Test
  public void fullRandomness() {
    for (int i = 0; i < 1000; i++) {
      RandomExponentialSequence seq = RandomExponentialSequence.builder()
          .start(10)
          .factor(2)
          .maxDeviation(2.0f)
          .build();

      Assert.assertThat(seq.next(), Matchers.is(10L));
      long n = seq.next();
      Assert.assertThat(n, Matchers.greaterThanOrEqualTo(10L));
      Assert.assertThat(n, Matchers.lessThanOrEqualTo(40L));
    }
  }
}
