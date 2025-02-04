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

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for various {@link NumberSequence} implementations.
 */
public class NumberSequenceTest
    extends TestSupport
{
  @Test
  public void testConstantSequence() {
    long startValue = 10;

    ConstantNumberSequence cs = new ConstantNumberSequence(startValue);

    for (int i = 0; i < 20; i++) {
      Assert.assertEquals(startValue, cs.next());
    }

    cs.reset();

    for (int i = 0; i < 20; i++) {
      Assert.assertEquals(startValue, cs.next());
    }
  }

  @Test
  public void testLinearSequence() {
    long startValue = 0;

    // step=1, multiplier=1, shift=0
    // f(x) = 1*x+0 = x; x starts at 1
    LinearNumberSequence ls = new LinearNumberSequence(startValue, 1, 1, 0);

    for (int i = 1; i < 20; i++) {
      Assert.assertEquals(i, ls.next());
    }

    ls.reset();

    // forth and back
    for (int i = 1; i < 20; i++) {
      Assert.assertEquals(i, ls.next());
    }
    for (int i = 18; i >= 1; i--) {
      Assert.assertEquals(i, ls.prev());
    }
  }

  @Test
  public void testLinearSequenceBitMore() {
    long startValue = 0;

    // step=10, multiplier=2, shift=10
    // f(x) = 1*x+0 = x; x starts at 1
    LinearNumberSequence ls = new LinearNumberSequence(startValue, 10, 2, 10);

    long f = 0;

    for (int i = 1; i < 20; i++) {
      f = 2 * (i * 10) + 10;
      Assert.assertEquals(f, ls.next());
    }

    ls.reset();

    // forth and back
    for (int i = 1; i < 20; i++) {
      f = 2 * (i * 10) + 10;
      Assert.assertEquals(f, ls.next());
    }
    for (int i = 18; i >= 1; i--) {
      f = 2 * (i * 10) + 10;
      Assert.assertEquals(f, ls.prev());
    }
  }

  @Test
  public void testFibonacciSequence() {
    int[] fibonacciNumbers = new int[]{1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233};

    FibonacciNumberSequence fs = new FibonacciNumberSequence();

    for (int f : fibonacciNumbers) {
      Assert.assertEquals(f, fs.next());
    }

    fs.reset();

    for (int f : fibonacciNumbers) {
      Assert.assertEquals(f, fs.next());
    }
  }

  @Test
  public void testFoxiedFibonacciSequence() {
    int[] fibonacciNumbers = new int[]{10, 10, 20, 30, 50, 80, 130, 210, 340, 550, 890, 1440, 2330};

    FibonacciNumberSequence fs = new FibonacciNumberSequence(10);

    for (int f : fibonacciNumbers) {
      Assert.assertEquals(f, fs.next());
    }

    fs.reset();

    for (int f : fibonacciNumbers) {
      Assert.assertEquals(f, fs.next());
    }
  }

  private static void ArrayUtils_reverse(int[] array) {
    if (array == null) {
      return;
    }
    int i = 0;
    int j = array.length - 1;
    int tmp;
    while (j > i) {
      tmp = array[j];
      array[j] = array[i];
      array[i] = tmp;
      j--;
      i++;
    }
  }

  @Test
  public void testFibonacciSequenceBackAndForth() {
    int[] fibonacciNumbers = new int[]{10, 10, 20, 30, 50, 80, 130, 210, 340, 550, 890, 1440, 2330};

    FibonacciNumberSequence fs = new FibonacciNumberSequence(10);

    for (int f : fibonacciNumbers) {
      Assert.assertEquals(f, fs.next());
    }

    fs.reset();

    for (int f : fibonacciNumbers) {
      Assert.assertEquals(f, fs.next());
    }

    ArrayUtils_reverse(fibonacciNumbers);

    for (int f : fibonacciNumbers) {
      Assert.assertEquals(f, fs.prev());
    }
  }

  @Test
  public void testLinearSequenceWithLimiter() {
    long startValue = 0;

    // step=1, multiplier=1, shift=0
    // f(x) = 1*x+0 = x; x starts at 1
    LinearNumberSequence ls = new LinearNumberSequence(startValue, 1, 1, 0);
    LowerLimitNumberSequence seq = new LowerLimitNumberSequence(ls, 1);

    // go prev 5 times, it should actually result in ONE state change
    for (int i = 1; i < 5; i++) {
      Assert.assertEquals(1, seq.prev());
    }

    Assert.assertEquals(1, seq.peek());

    Assert.assertEquals(1, seq.next());
    Assert.assertEquals(2, seq.next());

    seq.reset();

    Assert.assertEquals(1, seq.next());
    Assert.assertEquals(2, seq.next());
  }

}
