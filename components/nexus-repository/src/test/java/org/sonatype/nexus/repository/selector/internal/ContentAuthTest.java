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
package org.sonatype.nexus.repository.selector.internal;

import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * Tests for {@link ContentAuth}.
 */
public class ContentAuthTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repository-name";

  private static final String PATH = "path";

  private static final String FORMAT = "format";

  @Mock
  VariableSource variableSource;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  ODocument assetDocument;

  @Mock
  ODocument componentDocument;

  @Mock
  ODocument bucketDocument;

  @Mock
  OCommandRequest commandRequest;

  @Mock
  ODatabaseDocumentInternal database;

  ContentAuth underTest;

  @Before
  public void setup() {
    when(variableResolverAdapterManager.get(FORMAT)).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromDocument(assetDocument)).thenReturn(variableSource);

    when(bucketDocument.getRecord()).thenReturn(bucketDocument);
    when(bucketDocument.field("repository_name", String.class)).thenReturn(REPOSITORY_NAME);
    when(bucketDocument.getIdentity()).thenReturn(mock(ORID.class));

    when(assetDocument.getClassName()).thenReturn("asset");
    when(assetDocument.getRecord()).thenReturn(assetDocument);
    when(assetDocument.field("bucket", OIdentifiable.class)).thenReturn(bucketDocument);
    when(assetDocument.field("name", String.class)).thenReturn(PATH);
    when(assetDocument.field("format", String.class)).thenReturn(FORMAT);

    when(componentDocument.getClassName()).thenReturn("component");
    when(componentDocument.getRecord()).thenReturn(componentDocument);
    when(componentDocument.field("bucket", OIdentifiable.class)).thenReturn(bucketDocument);
    when(componentDocument.getDatabase()).thenReturn(database);
    when(componentDocument.getIdentity()).thenReturn(mock(ORID.class));

    when(commandRequest.execute(any(Map.class))).thenReturn(Collections.singletonList(assetDocument));
    when(database.command(any(OCommandRequest.class))).thenReturn(commandRequest);

    underTest = new ContentAuth(contentPermissionChecker, variableResolverAdapterManager);

    // This feels odd...
    ODatabaseRecordThreadLocal.INSTANCE = new ODatabaseRecordThreadLocal();
    ODatabaseRecordThreadLocal.INSTANCE.set(database);
  }

  @Test
  public void testAssetPermitted() {
    when(contentPermissionChecker.isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource)).thenReturn(true);
    assertThat(underTest.execute(underTest, null, null, new Object[] { assetDocument }, null), is(true));
    verify(contentPermissionChecker, times(1)).isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource);
  }

  @Test
  public void testAssetNotPermitted() {
    when(contentPermissionChecker.isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource)).thenReturn(false);
    assertThat(underTest.execute(underTest, null, null, new Object[] { assetDocument }, null), is(false));
    verify(contentPermissionChecker, times(1)).isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource);
  }

  @Test
  public void testComponentPermitted() {
    when(contentPermissionChecker.isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource)).thenReturn(true);
    assertThat(underTest.execute(underTest, null, null, new Object[] { componentDocument }, null), is(true));
    verify(contentPermissionChecker, times(1)).isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource);
  }

  @Test
  public void testComponentNotPermitted() {
    when(contentPermissionChecker.isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource)).thenReturn(false);
    assertThat(underTest.execute(underTest, null, null, new Object[] { componentDocument }, null), is(false));
    verify(contentPermissionChecker, times(1)).isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource);
  }
}
