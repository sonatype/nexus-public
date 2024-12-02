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
package org.sonatype.nexus.common.math;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link Math2}
 */
public class Math2Test
    extends TestSupport
{
  @Test
  public void testAddClamped() {
    // no overflow or underflow
    assertThat(Math2.addClamped(0L, 0L), is(0L));
    assertThat(Math2.addClamped(Long.MAX_VALUE, 0L), is(Long.MAX_VALUE));
    assertThat(Math2.addClamped(0L, Long.MAX_VALUE), is(Long.MAX_VALUE));
    assertThat(Math2.addClamped(Long.MAX_VALUE, -1L), is(Long.MAX_VALUE - 1L));
    assertThat(Math2.addClamped(-1L, Long.MAX_VALUE), is(Long.MAX_VALUE - 1L));
    assertThat(Math2.addClamped(Long.MIN_VALUE, 0L), is(Long.MIN_VALUE));
    assertThat(Math2.addClamped(0L, Long.MIN_VALUE), is(Long.MIN_VALUE));
    assertThat(Math2.addClamped(Long.MIN_VALUE, 1L), is(Long.MIN_VALUE + 1));
    assertThat(Math2.addClamped(1L, Long.MIN_VALUE), is(Long.MIN_VALUE + 1));
    assertThat(Math2.addClamped(Long.MIN_VALUE, Long.MAX_VALUE), is(-1L));
    assertThat(Math2.addClamped(Long.MAX_VALUE, Long.MIN_VALUE), is(-1L));

    // overflow
    assertThat(Math2.addClamped(Long.MAX_VALUE, 1L), is(Long.MAX_VALUE));
    assertThat(Math2.addClamped(Long.MAX_VALUE, Long.MAX_VALUE), is(Long.MAX_VALUE));
    assertThat(Math2.addClamped(Long.MAX_VALUE - 1L, Long.MAX_VALUE - 1L), is(Long.MAX_VALUE));

    // underflow
    assertThat(Math2.addClamped(Long.MIN_VALUE, -1L), is(Long.MIN_VALUE));
    assertThat(Math2.addClamped(Long.MIN_VALUE, Long.MIN_VALUE), is(Long.MIN_VALUE));
    assertThat(Math2.addClamped(Long.MIN_VALUE + 1L, Long.MIN_VALUE + 1L), is(Long.MIN_VALUE));
  }
}
