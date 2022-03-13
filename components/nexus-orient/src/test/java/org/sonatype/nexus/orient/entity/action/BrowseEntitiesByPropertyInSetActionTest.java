package org.sonatype.nexus.orient.entity.action;

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

import java.util.HashSet;

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrowseEntitiesByPropertyInSetActionTest {
  private static final String PROPERTY = "property";
  private static final String ENTITY_NAME = "testEntityName";

  @Mock
  private IterableEntityAdapter<Entity> adapter;
  @Mock
  private ODatabaseDocumentTx db;
  @Mock
  private Iterable<ODocument> documents;
  @Mock
  private Iterable<Entity> results;
  @Mock
  private OCommandRequest command;

  private BrowseEntitiesByPropertyInSetAction<Entity> browseEntitiesByPropertyInSetAction;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    when(adapter.getTypeName()).thenReturn(ENTITY_NAME);

    when(db.command(any(OSQLQuery.class))).thenReturn(command);
    when(command.execute(any(Object[].class))).thenReturn(documents);
    when(adapter.transform(documents)).thenReturn(results);

    browseEntitiesByPropertyInSetAction = new BrowseEntitiesByPropertyInSetAction<>(adapter, PROPERTY);
  }

  @Test(expected = NullPointerException.class)
  public void enforceNonNullAdapter() {
    new BrowseEntitiesByPropertyInSetAction<>(null, PROPERTY);
  }

  @Test(expected = NullPointerException.class)
  public void enforceNonNullProperty() {
    new BrowseEntitiesByPropertyInSetAction<>(adapter, null);
  }

  @Test(expected = NullPointerException.class)
  public void enforceNonNullDB() {
    browseEntitiesByPropertyInSetAction.execute(null, newHashSet("Test"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void enforceNonNullValues() {
    browseEntitiesByPropertyInSetAction.execute(db, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void enforceNonEmptyValues() {
    browseEntitiesByPropertyInSetAction.execute(db, new HashSet<String>());
  }

  @Test
  public void buildsCorrectQueryForSingle() {
    ArgumentCaptor<OSQLQuery> queryCaptor = ArgumentCaptor.forClass(OSQLQuery.class);
    browseEntitiesByPropertyInSetAction.execute(db, newHashSet("Test"));
    verify(db, times(1)).command(queryCaptor.capture());
    assertEquals("SELECT * FROM " + ENTITY_NAME + " WHERE " + PROPERTY + " IN [ ? ]", queryCaptor.getValue().getText());
  }

  @Test
  public void buildsCorrectQueryForMultiple() {
    ArgumentCaptor<OSQLQuery> queryCaptor = ArgumentCaptor.forClass(OSQLQuery.class);
    browseEntitiesByPropertyInSetAction.execute(db, newHashSet("Test1", "Test2", "Test3"));
    verify(db, times(1)).command(queryCaptor.capture());
    assertEquals("SELECT * FROM " + ENTITY_NAME + " WHERE " + PROPERTY + " IN [ ?,?,? ]", queryCaptor.getValue().getText());
  }
}
