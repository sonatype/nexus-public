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
package org.sonatype.nexus.repository.httpbridge.internal.describe

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Test

/**
 * Tests for {@link DescribeType}.
 */
class DescribeTypeTest
    extends TestSupport
{
  @Test
  void 'parse html'() {
    assert DescribeType.parse('html') == DescribeType.HTML
  }

  @Test
  void 'parse json'() {
    assert DescribeType.parse('json') == DescribeType.JSON
  }

  @Test
  void 'parse blank as html'() {
    assert DescribeType.parse('') == DescribeType.HTML
  }

  @Test
  void 'parse true as html'() {
    assert DescribeType.parse('true') == DescribeType.HTML
  }
}
