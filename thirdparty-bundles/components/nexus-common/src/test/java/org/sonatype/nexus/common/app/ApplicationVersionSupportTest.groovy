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
package org.sonatype.nexus.common.app

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link ApplicationVersionSupport}.
 */
class ApplicationVersionSupportTest
    extends TestSupport
{
  ApplicationVersionSupport underTest

  Properties props

  @Before
  void setUp() {
    props = new Properties()
    underTest = new ApplicationVersionSupport() {
      @Override
      def Properties getProperties() {
        return props
      }

      @Override
      String getEdition() {
        return 'TEST'
      }
    }
  }

  @Test
  void 'edition returns non-UNKNOWN'() {
    assert underTest.edition == 'TEST'
  }

  @Test
  void 'version returns value'() {
    props[ApplicationVersionSupport.VERSION] = '123'
    assert underTest.version == '123'
  }

  @Test
  void 'missing version returns UNKNOWN'() {
    assert underTest.version == ApplicationVersionSupport.UNKNOWN
  }
}
