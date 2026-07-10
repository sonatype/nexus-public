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
package org.sonatype.nexus.blobstore.api;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link OperationMetrics}.
 */
public class OperationMetricsTest
{
  private OperationMetrics underTest;

  @Before
  public void setUp() {
    underTest = new OperationMetrics();
  }

  @Test
  public void testDefaultsAreZero() {
    assertThat(underTest.getBlobSize(), is(0L));
    assertThat(underTest.getSuccessfulRequests(), is(0L));
    assertThat(underTest.getTimeOnRequests(), is(0L));
    assertThat(underTest.getErrorRequests(), is(0L));
  }

  @Test
  public void testSettersAndGetters() {
    underTest.setBlobSize(123L);
    underTest.setSuccessfulRequests(45L);
    underTest.setTimeOnRequests(67L);
    underTest.setErrorRequests(89L);

    assertThat(underTest.getBlobSize(), is(123L));
    assertThat(underTest.getSuccessfulRequests(), is(45L));
    assertThat(underTest.getTimeOnRequests(), is(67L));
    assertThat(underTest.getErrorRequests(), is(89L));
  }

  @Test
  public void testAddBlobSize() {
    underTest.addBlobSize(100L);
    assertThat(underTest.getBlobSize(), is(100L));

    underTest.addBlobSize(50L);
    assertThat(underTest.getBlobSize(), is(150L));
  }

  @Test
  public void testAddTimeOnRequests() {
    underTest.addTimeOnRequests(200L);
    assertThat(underTest.getTimeOnRequests(), is(200L));

    underTest.addTimeOnRequests(25L);
    assertThat(underTest.getTimeOnRequests(), is(225L));
  }

  @Test
  public void testAddSuccessfulRequest() {
    underTest.addSuccessfulRequest();
    assertThat(underTest.getSuccessfulRequests(), is(1L));

    underTest.addSuccessfulRequest();
    assertThat(underTest.getSuccessfulRequests(), is(2L));
  }

  @Test
  public void testAddErrorRequest() {
    underTest.addErrorRequest();
    assertThat(underTest.getErrorRequests(), is(1L));

    underTest.addErrorRequest();
    assertThat(underTest.getErrorRequests(), is(2L));
  }

  @Test
  public void testClear() {
    underTest.setBlobSize(123L);
    underTest.setSuccessfulRequests(45L);
    underTest.setTimeOnRequests(67L);
    underTest.setErrorRequests(89L);

    underTest.clear();

    assertThat(underTest.getBlobSize(), is(0L));
    assertThat(underTest.getSuccessfulRequests(), is(0L));
    assertThat(underTest.getTimeOnRequests(), is(0L));
    assertThat(underTest.getErrorRequests(), is(0L));
  }

  @Test
  public void testAddAggregatesMetrics() {
    underTest.setBlobSize(100L);
    underTest.setSuccessfulRequests(10L);
    underTest.setTimeOnRequests(1000L);
    underTest.setErrorRequests(2L);

    OperationMetrics other = new OperationMetrics();
    other.setBlobSize(50L);
    other.setSuccessfulRequests(5L);
    other.setTimeOnRequests(500L);
    other.setErrorRequests(3L);

    OperationMetrics result = underTest.add(other);

    assertThat(result.getBlobSize(), is(150L));
    assertThat(result.getSuccessfulRequests(), is(15L));
    assertThat(result.getTimeOnRequests(), is(1500L));
    assertThat(result.getErrorRequests(), is(5L));
  }

  @Test
  public void testAddReturnsNewInstanceAndDoesNotMutateOperands() {
    underTest.setBlobSize(100L);
    underTest.setSuccessfulRequests(10L);
    underTest.setTimeOnRequests(1000L);
    underTest.setErrorRequests(2L);

    OperationMetrics other = new OperationMetrics();
    other.setBlobSize(50L);
    other.setSuccessfulRequests(5L);
    other.setTimeOnRequests(500L);
    other.setErrorRequests(3L);

    OperationMetrics result = underTest.add(other);

    assertThat(result, is(not(sameInstance(underTest))));
    assertThat(result, is(not(sameInstance(other))));

    // the new instance carries the aggregated values
    assertThat(result.getBlobSize(), is(150L));
    assertThat(result.getSuccessfulRequests(), is(15L));
    assertThat(result.getTimeOnRequests(), is(1500L));
    assertThat(result.getErrorRequests(), is(5L));

    // every field of both operands is left unchanged
    assertThat(underTest.getBlobSize(), is(100L));
    assertThat(underTest.getSuccessfulRequests(), is(10L));
    assertThat(underTest.getTimeOnRequests(), is(1000L));
    assertThat(underTest.getErrorRequests(), is(2L));

    assertThat(other.getBlobSize(), is(50L));
    assertThat(other.getSuccessfulRequests(), is(5L));
    assertThat(other.getTimeOnRequests(), is(500L));
    assertThat(other.getErrorRequests(), is(3L));
  }

  @Test
  public void testAddBlobSizeAccumulatesOntoExistingAndAcceptsNegativeDelta() {
    underTest.setBlobSize(100L);

    underTest.addBlobSize(25L);
    assertThat(underTest.getBlobSize(), is(125L));

    underTest.addBlobSize(-50L);
    assertThat(underTest.getBlobSize(), is(75L));

    // adding to blobSize does not touch the unrelated counters
    assertThat(underTest.getSuccessfulRequests(), is(0L));
    assertThat(underTest.getTimeOnRequests(), is(0L));
    assertThat(underTest.getErrorRequests(), is(0L));
  }

  @Test
  public void testAddTimeOnRequestsAccumulatesOntoExistingAndAcceptsNegativeDelta() {
    underTest.setTimeOnRequests(1000L);

    underTest.addTimeOnRequests(250L);
    assertThat(underTest.getTimeOnRequests(), is(1250L));

    underTest.addTimeOnRequests(-500L);
    assertThat(underTest.getTimeOnRequests(), is(750L));

    // adding to timeOnRequests does not touch the unrelated counters
    assertThat(underTest.getBlobSize(), is(0L));
    assertThat(underTest.getSuccessfulRequests(), is(0L));
    assertThat(underTest.getErrorRequests(), is(0L));
  }

  @Test
  public void testAddClampsAtLongMaxValueOnOverflow() {
    underTest.setBlobSize(Long.MAX_VALUE);
    underTest.setSuccessfulRequests(Long.MAX_VALUE);
    underTest.setTimeOnRequests(Long.MAX_VALUE);
    underTest.setErrorRequests(Long.MAX_VALUE);

    OperationMetrics other = new OperationMetrics();
    other.setBlobSize(1L);
    other.setSuccessfulRequests(100L);
    other.setTimeOnRequests(Long.MAX_VALUE);
    other.setErrorRequests(1L);

    OperationMetrics result = underTest.add(other);

    assertThat(result.getBlobSize(), is(Long.MAX_VALUE));
    assertThat(result.getSuccessfulRequests(), is(Long.MAX_VALUE));
    assertThat(result.getTimeOnRequests(), is(Long.MAX_VALUE));
    assertThat(result.getErrorRequests(), is(Long.MAX_VALUE));
  }

  @Test
  public void testAddClampsAtLongMinValueOnNegativeOverflow() {
    underTest.setBlobSize(Long.MIN_VALUE);
    underTest.setSuccessfulRequests(Long.MIN_VALUE);
    underTest.setTimeOnRequests(Long.MIN_VALUE);
    underTest.setErrorRequests(Long.MIN_VALUE);

    OperationMetrics other = new OperationMetrics();
    other.setBlobSize(-1L);
    other.setSuccessfulRequests(-100L);
    other.setTimeOnRequests(Long.MIN_VALUE);
    other.setErrorRequests(-1L);

    OperationMetrics result = underTest.add(other);

    assertThat(result.getBlobSize(), is(Long.MIN_VALUE));
    assertThat(result.getSuccessfulRequests(), is(Long.MIN_VALUE));
    assertThat(result.getTimeOnRequests(), is(Long.MIN_VALUE));
    assertThat(result.getErrorRequests(), is(Long.MIN_VALUE));
  }

  @Test
  public void testAddWithSelfDoublesValuesAndDoesNotMutateOperand() {
    underTest.setBlobSize(7L);
    underTest.setSuccessfulRequests(8L);
    underTest.setTimeOnRequests(9L);
    underTest.setErrorRequests(10L);

    OperationMetrics result = underTest.add(underTest);

    assertThat(result, is(not(sameInstance(underTest))));

    assertThat(result.getBlobSize(), is(14L));
    assertThat(result.getSuccessfulRequests(), is(16L));
    assertThat(result.getTimeOnRequests(), is(18L));
    assertThat(result.getErrorRequests(), is(20L));

    // aggregating an instance with itself leaves the operand untouched
    assertThat(underTest.getBlobSize(), is(7L));
    assertThat(underTest.getSuccessfulRequests(), is(8L));
    assertThat(underTest.getTimeOnRequests(), is(9L));
    assertThat(underTest.getErrorRequests(), is(10L));
  }

  @Test
  public void testToString() {
    underTest.setBlobSize(1L);
    underTest.setSuccessfulRequests(2L);
    underTest.setTimeOnRequests(3L);
    underTest.setErrorRequests(4L);

    String expected = "OperationMetrics{blobSize=1, successfulRequests=2, timeOnRequests=3, errorRequests=4}";

    assertThat(underTest.toString(), is(expected));
  }
}
