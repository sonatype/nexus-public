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
package org.sonatype.nexus.common.entity;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EntityHelper}
 */
public class EntityHelperTest
    extends TestSupport
{
  @Test
  public void testEntityWithoutMetadata() {
    AbstractEntity entity = new AbstractEntity()
    {
    };
    assertFalse(EntityHelper.hasMetadata(entity));

    assertThrows(IllegalStateException.class, () -> EntityHelper.metadata(entity));

    assertThrows(IllegalStateException.class, () -> EntityHelper.isDetached(entity));

    assertThrows(IllegalStateException.class, () -> EntityHelper.id(entity));

    assertThrows(IllegalStateException.class, () -> EntityHelper.version(entity));
  }

  @Test
  public void testEntityWithMetadata() {
    AbstractEntity entity = new AbstractEntity()
    {
    };
    entity.setEntityMetadata(new DetachedEntityMetadata(new DetachedEntityId("a"), new DetachedEntityVersion("1")));

    assertTrue(EntityHelper.hasMetadata(entity));
    assertThat(EntityHelper.metadata(entity), notNullValue());
    assertTrue(EntityHelper.isDetached(entity));
    assertThat(EntityHelper.id(entity).getValue(), is("a"));
    assertThat(EntityHelper.version(entity).getValue(), is("1"));
  }
}
