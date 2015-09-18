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
package org.sonatype.nexus.common.entity

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Test

import static org.junit.Assert.fail

/**
 * Tests for {@link EntityHelper}
 */
class EntityHelperTest
    extends TestSupport
{
  @Test
  void 'entity with-out metadata'() {
    def entity = new Entity() {};
    assert !EntityHelper.hasMetadata(entity)

    try {
      EntityHelper.metadata(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }

    try {
      EntityHelper.isDetached(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }

    try {
      EntityHelper.id(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }

    try {
      EntityHelper.version(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  void 'entity with metadata'() {
    def entity = new Entity() {};
    entity.setEntityMetadata(new DetachedEntityMetadata(new DetachedEntityId('a'), new DetachedEntityVersion('1')))

    assert EntityHelper.hasMetadata(entity)
    assert EntityHelper.metadata(entity) != null
    assert EntityHelper.isDetached(entity)
    assert EntityHelper.id(entity).value == 'a'
    assert EntityHelper.version(entity).value == '1'
  }
}
