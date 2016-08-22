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
package org.sonatype.nexus.orient.entity

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.entity.Entity

import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument
import org.junit.Test

import static org.junit.Assert.fail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Tests for {@link AttachedEntityHelper}
 */
class AttachedEntityHelperTest
    extends TestSupport
{
  @Test
  void 'entity with-out metadata'() {
    def entity = new Entity() {};

    try {
      AttachedEntityHelper.metadata(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }

    try {
      AttachedEntityHelper.isAttached(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }

    try {
      AttachedEntityHelper.document(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }

    try {
      AttachedEntityHelper.id(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }

    try {
      AttachedEntityHelper.version(entity)
      fail()
    }
    catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  void 'entity with metadata'() {
    def entity = new Entity() {};

    ODocument doc = mock(ODocument.class)

    ORID rid = new ORecordId(1, 1)
    when(doc.getIdentity()).thenReturn(rid)

    int rv = 1
    when(doc.getVersion()).thenReturn(rv)

    EntityAdapter entityAdapter = mock(EntityAdapter.class)
    entity.setEntityMetadata(new AttachedEntityMetadata(entityAdapter, doc));

    assert AttachedEntityHelper.metadata(entity) != null
    assert AttachedEntityHelper.isAttached(entity)
    assert AttachedEntityHelper.document(entity) == doc
    assert AttachedEntityHelper.id(entity) == rid
    assert AttachedEntityHelper.version(entity) == rv
  }
}
