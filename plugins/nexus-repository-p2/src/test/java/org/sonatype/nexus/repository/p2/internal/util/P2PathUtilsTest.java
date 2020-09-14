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
package org.sonatype.nexus.repository.p2.internal.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.p2.internal.util.P2PathUtils.maybePath;
import static org.sonatype.nexus.repository.p2.internal.util.P2PathUtils.name;
import static org.sonatype.nexus.repository.p2.internal.util.P2PathUtils.path;

public class P2PathUtilsTest
    extends TestSupport
{
  @Mock
  Context context;

  @Mock
  TokenMatcher.State state;

  @Mock
  AttributesMap attributesMap;

  private final String fakePath = "fakepath";

  private final String fakeComponentName = "eclipsepackage1";

  private final String fakeFileName = fakeComponentName + "_2_3.jar";

  private final String fakeExtension = "pack.gz";

  @Test
  public void pathWithState() throws Exception {
    final Map<String, String> someMap = Collections.singletonMap("path", fakePath);
    when(state.getTokens())
        .thenReturn(someMap);
    String path = path(state);
    assertThat(path, is(equalTo(fakePath)));
  }

  @Test
  public void pathWithPathAndFileName() throws Exception {
    String path = path(fakePath, fakeFileName);
    String expectedResult = fakePath + "/" + fakeFileName;
    assertThat(path, is(equalTo(expectedResult)));
  }

  @Test
  public void pathWithNoPathAndFileName() throws Exception {
    String path = path("", fakeFileName);
    String expectedResult = fakeFileName;
    assertThat(path, is(equalTo(expectedResult)));
  }

  @Test
  public void pathTestMaybePath() throws Exception {
    final Map<String, String> someMap = new HashMap<>();
    someMap.put("name", fakeFileName);
    someMap.put("extension", fakeExtension);
    when(state.getTokens())
        .thenReturn(someMap);
    String path = maybePath(state);

    assertThat(path, is(equalTo(fakeFileName + "." + fakeExtension)));

    someMap.put("path", fakePath);
    path = maybePath(state);

    assertThat(path, is(equalTo(fakePath + "/" + fakeFileName + "." + fakeExtension)));
  }

  @Test
  public void filename() throws Exception {
    final Map<String, String> someMap = Collections.singletonMap("name", fakeFileName);
    when(state.getTokens())
        .thenReturn(someMap);
    String filename = name(state);
    assertThat(filename, is(equalTo(fakeFileName)));
  }

  @Test
  public void componentName() throws Exception {
    final Map<String, String> someMap = Collections.singletonMap("name", fakeFileName);
    when(state.getTokens())
        .thenReturn(someMap);
    String componentName = P2PathUtils.componentName(state);
    assertThat(componentName, is(equalTo(fakeComponentName)));
  }

  @Test
  public void pathWithPathAndFilenameAndExtension() throws Exception {
    String path = path(fakePath, fakeFileName, fakeExtension);
    String expectedResult = fakePath + "/" + fakeFileName + "." + fakeExtension;
    assertThat(path, is(equalTo(expectedResult)));
  }

  @Test
  public void matcherState() throws Exception {
    when(context.getAttributes())
        .thenReturn(attributesMap);
    when(attributesMap.require(TokenMatcher.State.class))
        .thenReturn(state);
    TokenMatcher.State testState = P2PathUtils.matcherState(context);
    assertThat(testState, instanceOf(TokenMatcher.State.class));
  }
}
