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
package org.sonatype.nexus.common.throwables

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.fail

/**
 * Tests for {@link MultipleFailures}.
 */
class MultipleFailuresTest
    extends TestSupport
{
  private MultipleFailures underTest

  @Before
  void setUp() {
    underTest = new MultipleFailures()
  }

  @Test
  void 'propagate with no failures'() {
    assert underTest.failures.isEmpty()
    underTest.maybePropagate()
  }

  @Test
  void 'propagate single'() {
    underTest.add(new Exception('TEST'))
    try {
      underTest.maybePropagate('OOPS')
      fail()
    }
    catch (Exception e) {
      log e.toString(), e
      println '----8<----'
      e.printStackTrace()
      println '---->8----'

      def suppressed = e.getSuppressed()
      assert suppressed.length == 1
      assert suppressed[0].message == 'TEST'
    }
  }

  @Test
  void 'propagate multi'() {
    underTest.add(new Exception('FOO'))
    underTest.add(new Exception('BAR'))
    try {
      underTest.maybePropagate('OOPS')
      fail()
    }
    catch (Exception e) {
      log e.toString(), e
      println '----8<----'
      e.printStackTrace()
      println '---->8----'

      def suppressed = e.getSuppressed()
      assert suppressed.length == 2
      assert suppressed[0].message == 'FOO'
      assert suppressed[1].message == 'BAR'
    }
  }
}
