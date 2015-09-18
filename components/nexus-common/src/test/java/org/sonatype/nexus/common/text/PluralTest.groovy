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
package org.sonatype.nexus.common.text

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Test

/**
 * Tests for {@link Plural}
 */
class PluralTest
  extends TestSupport
{
  @Test
  void 'simple plural'() {
    assert Plural.of(-1, 'dog') == '-1 dogs'
    assert Plural.of(0, 'dog') == '0 dogs'
    assert Plural.of(1, 'dog') == '1 dog'
    assert Plural.of(2, 'dog') == '2 dogs'
  }

  @Test
  void 'complex plural'() {
    assert Plural.of(-1, 'candy', 'candies') == '-1 candies'
    assert Plural.of(0, 'candy', 'candies') == '0 candies'
    assert Plural.of(1, 'candy', 'candies') == '1 candy'
    assert Plural.of(2, 'candy', 'candies') == '2 candies'
  }
}
