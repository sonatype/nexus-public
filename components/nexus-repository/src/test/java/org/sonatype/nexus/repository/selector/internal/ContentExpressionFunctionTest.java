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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.AssetVariableResolver;
import org.sonatype.nexus.selector.OrientSelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.VariableSource;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ContentExpressionFunction}.
 */
public class ContentExpressionFunctionTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repository-name";

  private static final String PATH = "path";

  private static final String FORMAT = "format";

  @Mock
  VariableSource variableSource;

  @Mock
  AssetVariableResolver assetVariableResolver;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  ODocument assetDocument;

  @Mock
  ODocument bucketDocument;

  @Mock
  OCommandRequest commandRequest;

  @Mock
  ODatabaseDocumentInternal database;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private OrientContentAuthHelper contentAuthHelper;

  ContentExpressionFunction underTest;

  @Before
  public void setup() {
    when(variableResolverAdapterManager.get(FORMAT)).thenReturn(assetVariableResolver);
    when(assetVariableResolver.fromDocument(assetDocument)).thenReturn(variableSource);

    when(bucketDocument.getRecord()).thenReturn(bucketDocument);
    when(bucketDocument.field("repository_name", String.class)).thenReturn(REPOSITORY_NAME);
    when(bucketDocument.getIdentity()).thenReturn(mock(ORID.class));

    when(assetDocument.getClassName()).thenReturn("asset");
    when(assetDocument.getRecord()).thenReturn(assetDocument);
    when(assetDocument.field("bucket", OIdentifiable.class)).thenReturn(bucketDocument);
    when(assetDocument.field("name", String.class)).thenReturn(PATH);
    when(assetDocument.field("format", String.class)).thenReturn(FORMAT);

    when(commandRequest.execute(any(Map.class))).thenReturn(Collections.singletonList(assetDocument));
    when(database.command(any(OCommandRequest.class))).thenReturn(commandRequest);

    when(selectorManager.newSelectorConfiguration()).thenAnswer(invocation -> new OrientSelectorConfiguration());

    underTest = new ContentExpressionFunction(variableResolverAdapterManager, selectorManager, contentAuthHelper);
  }

  @Test
  public void testMatchingAsset_SingleRepository() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(any(), any(), eq(REPOSITORY_NAME))).thenReturn(true);
    assertThat(underTest
        .execute(underTest, null, null, new Object[]{assetDocument, "jexlexpression", REPOSITORY_NAME, ""},
            null), is(true));
  }

  @Test
  public void testMatchingAsset_AllRepositories() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(any(), any(), eq(REPOSITORY_NAME))).thenReturn(true);
    Map<String, List<String>> repoToContainedGroupMap = new HashMap<>();
    repoToContainedGroupMap.put(REPOSITORY_NAME, Arrays.asList(REPOSITORY_NAME));
    assertThat(underTest.execute(underTest, null, null, new Object[]{
        assetDocument, "jexlexpression", "*", repoToContainedGroupMap
    }, null), is(true));
  }

  @Test
  public void testMatchingAsset_AllRepositories_inGroup() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(any(), any(), eq(REPOSITORY_NAME), eq("groupRepo"))).thenReturn(true);
    Map<String, List<String>> repoToContainedGroupMap = new HashMap<>();
    repoToContainedGroupMap.put(REPOSITORY_NAME, Arrays.asList(REPOSITORY_NAME, "groupRepo"));
    assertThat(underTest.execute(underTest, null, null, new Object[]{
        assetDocument, "jexlexpression", "*", repoToContainedGroupMap
    }, null), is(true));
  }

  @Test
  public void testNonMatchingAsset_SingleRepository() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);
    assertThat(underTest
        .execute(underTest, null, null, new Object[]{assetDocument, "jexlexpression", REPOSITORY_NAME, ""},
            null), is(false));
  }

  @Test
  public void testNonMatchingAsset_AllRepositories() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);
    Map<String, List<String>> repoToContainedGroupMap = new HashMap<>();
    repoToContainedGroupMap.put(REPOSITORY_NAME, Collections.emptyList());
    assertThat(underTest.execute(underTest, null, null, new Object[]{
        assetDocument, "jexlexpression", "*", repoToContainedGroupMap
    }, null), is(false));
  }

  @Test
  public void testNonMatchingAsset_AllRepositories_inGroup() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);
    Map<String, List<String>> repoToContainedGroupMap = new HashMap<>();
    repoToContainedGroupMap.put(REPOSITORY_NAME, Arrays.asList("groupRepo"));
    assertThat(underTest.execute(underTest, null, null, new Object[]{
        assetDocument, "jexlexpression", "*", repoToContainedGroupMap
    }, null), is(false));
  }
}
