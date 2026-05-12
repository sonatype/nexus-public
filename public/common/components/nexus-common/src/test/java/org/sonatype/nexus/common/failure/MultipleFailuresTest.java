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
package org.sonatype.nexus.common.failure;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link MultipleFailures}.
 */
public class MultipleFailuresTest
{
  private MultipleFailures underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new MultipleFailures();
  }

  @Test
  public void propagate_with_no_failures() throws Exception {
    assertTrue(underTest.getFailures().isEmpty());
    underTest.maybePropagate();
  }

  @Test
  public void propagate_single() throws Exception {
    underTest.add(new Exception("TEST"));
    try {
      underTest.maybePropagate("OOPS");
      fail();
    }
    catch (Exception e) {
      Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed.length, is(1));
      assertThat(suppressed[0].getMessage(), is("TEST"));
    }
  }

  @Test
  public void propagate_multi() throws Exception {
    underTest.add(new Exception("FOO"));
    underTest.add(new Exception("BAR"));
    try {
      underTest.maybePropagate("OOPS");
      fail();
    }
    catch (Exception e) {
      Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed.length, is(2));
      assertThat(suppressed[0].getMessage(), is("FOO"));
      assertThat(suppressed[1].getMessage(), is("BAR"));
    }
  }
}
