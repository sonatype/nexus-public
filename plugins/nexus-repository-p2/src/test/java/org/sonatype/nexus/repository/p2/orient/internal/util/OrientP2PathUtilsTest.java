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
package org.sonatype.nexus.repository.p2.orient.internal.util;

import java.util.Collections;
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
import static org.sonatype.nexus.repository.p2.orient.internal.util.OrientP2PathUtils.name;

public class OrientP2PathUtilsTest
    extends TestSupport
{
  @Mock
  Context context;

  @Mock
  TokenMatcher.State state;

  @Mock
  AttributesMap attributesMap;

  private final String fakeComponentName = "eclipsepackage1";

  private final String fakeFileName = fakeComponentName + "_2_3.jar";


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
    String componentName = OrientP2PathUtils.componentName(state);
    assertThat(componentName, is(equalTo(fakeComponentName)));
  }

  @Test
  public void matcherState() throws Exception {
    when(context.getAttributes())
        .thenReturn(attributesMap);
    when(attributesMap.require(TokenMatcher.State.class))
        .thenReturn(state);
    TokenMatcher.State testState = OrientP2PathUtils.matcherState(context);
    assertThat(testState, instanceOf(TokenMatcher.State.class));
  }
}
