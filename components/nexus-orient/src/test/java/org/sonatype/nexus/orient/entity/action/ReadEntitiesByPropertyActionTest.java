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
package org.sonatype.nexus.orient.entity.action;

import java.util.List;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.orient.entity.EntityAdapter;

import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadEntitiesByPropertyActionTest
    extends TestSupport
{
  @Mock
  private ODatabaseDocumentTx db;

  private EntityAdapter<Entity> entityAdapter = new TestEntityAdapter();

  private ReadEntitiesByPropertyAction<Entity> underTest;

  @Before
  public void setup() {
    underTest = new ReadEntitiesByPropertyAction<>(entityAdapter, "test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptySetNotAccepted() {
    underTest.execute(db, emptySet());
  }

  @Test
  public void testExecute() {
    Set<String> params = ImmutableSet.of("1", "2");

    ArgumentCaptor<OSQLSynchQuery> argumentCaptor = ArgumentCaptor.forClass(OSQLSynchQuery.class);
    OCommandRequest oCommandRequest = mock(OCommandRequest.class);
    when(db.command(argumentCaptor.capture())).thenReturn(oCommandRequest);
    when(oCommandRequest.execute(params)).thenReturn(newArrayList(new ODocument(), new ODocument()));

    List<Entity> results = underTest.execute(db, params);

    assertThat(argumentCaptor.getValue().getText(), equalTo("SELECT FROM test WHERE test IN['1','2']"));
    assertThat(results, notNullValue());
    assertThat(results.size(), equalTo(2));
  }

  private class TestEntityAdapter
      extends EntityAdapter<Entity>
  {
    TestEntityAdapter() {
      super("test");
    }

    @Override
    protected void defineType(final OClass type) {
    }

    @Override
    protected Entity newEntity() {
      return new AbstractEntity() { };
    }

    @Override
    protected void readFields(final ODocument document, final Entity entity) {

    }

    @Override
    protected void writeFields(final ODocument document, final Entity entity) {

    }
  }
}
