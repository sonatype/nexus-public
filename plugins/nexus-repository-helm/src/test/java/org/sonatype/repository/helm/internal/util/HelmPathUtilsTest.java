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
package org.sonatype.repository.helm.internal.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.repository.helm.internal.metadata.IndexYamlAbsoluteUrlRewriter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HelmPathUtilsTest
    extends TestSupport
{
  private static final String FILENAME = "mongodb-0.4.9.tgz";

  private HelmPathUtils underTest;

  @Mock
  private TokenMatcher.State state;

  @Before
  public void setUp() throws Exception {
    underTest = new HelmPathUtils(new IndexYamlAbsoluteUrlRewriter());
  }

  @Test
  public void filename() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("filename", FILENAME);
    when(state.getTokens()).thenReturn(map);
    String result = underTest.filename(state);
    assertThat(result, is(equalTo(FILENAME)));
  }

  @Test
  public void testContentFileUrl() throws IOException {
    Content content = mock(Content.class);
    when(content.openInputStream()).thenReturn(getClass().getResourceAsStream("indexresult.yaml"));
    String url = underTest.contentFileUrl(FILENAME, content).get();
    assertThat(url, is(equalTo("mongodb-0.5.2.tgz")));
  }

  @Test
  public void testContentFileUrlWithDashes() throws IOException {
    Content content = mock(Content.class);
    when(content.openInputStream()).thenReturn(getClass().getResourceAsStream("indexWithDashes.yaml"));
    String url = underTest.contentFileUrl("cert-manager-v1.1.0-alpha.1.tgz", content).get();
    assertThat(url, is(equalTo("charts/cert-manager-v1.1.0-alpha.1.tgz")));
  }
}
