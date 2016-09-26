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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.collect.ImmutableMap;
import org.apache.shiro.subject.Subject;
import org.elasticsearch.search.lookup.SourceLookup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * Tests for {@link ContentAuthPluginScript}.
 */
public class ContentAuthPluginScriptTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repository-name";

  private static final String PATH = "path";

  private static final String FORMAT = "format";

  @Mock
  Subject subject;

  @Mock
  VariableSource variableSource;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  SourceLookup sourceLookup;

  ContentAuthPluginScript underTest;

  @Before
  public void setup() {
    sourceLookup = new SourceLookup();
    when(variableResolverAdapterManager.get(FORMAT)).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromSourceLookup(eq(sourceLookup), anyMap())).thenReturn(variableSource);
    underTest = new ContentAuthPluginScript(subject, contentPermissionChecker,
        variableResolverAdapterManager) {
      @Override
      protected SourceLookup getSourceLookup() {
        return sourceLookup;
      }
    };
  }

  @Test
  public void testPermitted() {
    sourceLookup.setSource(new ImmutableMap.Builder<String, Object>()
        .put("format", FORMAT)
        .put("repository_name", REPOSITORY_NAME)
        .put("assets", Collections.singletonList(Collections.singletonMap("name", PATH)))
        .build());
    when(contentPermissionChecker.isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource)).thenReturn(true);
    assertThat(underTest.run(), is(true));
    verify(contentPermissionChecker, times(1)).isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource);
  }

  @Test
  public void testNotPermitted() {
    sourceLookup.setSource(new ImmutableMap.Builder<String, Object>()
        .put("format", FORMAT)
        .put("repository_name", REPOSITORY_NAME)
        .put("assets", Collections.singletonList(Collections.singletonMap("name", PATH)))
        .build());
    when(contentPermissionChecker.isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource)).thenReturn(false);
    assertThat(underTest.run(), is(false));
    verify(contentPermissionChecker, times(1)).isPermitted(REPOSITORY_NAME, FORMAT, BROWSE, variableSource);
  }

  @Test
  public void testWithoutAssets() {
    sourceLookup.setSource(new ImmutableMap.Builder<String, Object>()
        .put("format", FORMAT)
        .put("repository_name", REPOSITORY_NAME)
        .build());
    assertThat(underTest.run(), is(false));
    verifyZeroInteractions(contentPermissionChecker);
  }
}
