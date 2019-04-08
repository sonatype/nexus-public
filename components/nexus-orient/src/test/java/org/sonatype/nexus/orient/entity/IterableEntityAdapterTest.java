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
package org.sonatype.nexus.orient.entity;

import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.Entity;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class IterableEntityAdapterTest
    extends TestSupport
{
  @Spy
  IterableEntityAdapter<Entity> underTest = new TestIterableEntityAdapter();

  @Mock
  Entity goodEntity;

  @Mock
  ODocument goodRecord;

  @Mock
  ODocument badRecord;

  @Before
  public void setUp() {
    when(goodRecord.getRecord()).thenReturn(goodRecord);
    when(badRecord.getRecord()).thenReturn(badRecord);
    when(underTest.readEntity(goodRecord)).thenReturn(goodEntity);
    when(underTest.readEntity(badRecord)).thenThrow(new NoClassDefFoundError());
  }

  @Test
  public void nullEntitiesAreSkipped() {
    assertThat(underTest.transform(Arrays.asList(null, goodRecord, null)), contains(goodEntity));
  }

  @Test
  public void malformedEntitiesAreSkipped() {
    assertThat(underTest.transform(Arrays.asList(badRecord, goodRecord)), contains(goodEntity));
  }

  private class TestIterableEntityAdapter
      extends IterableEntityAdapter<Entity>
  {
    public TestIterableEntityAdapter() {
      super("test");
    }

    @Override
    protected void defineType(final OClass type) {
      // no-op
    }

    @Override
    protected Entity newEntity() {
      return new AbstractEntity()
      {
        // empty
      };
    }

    @Override
    protected void attachMetadata(final Entity entity, final ODocument document) {
      // no-op
    }

    @Override
    protected void readFields(final ODocument document, final Entity entity) throws Exception {
      // no-op
    }

    @Override
    protected void writeFields(final ODocument document, final Entity entity) throws Exception {
      // no-op
    }
  }
}
