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
package org.sonatype.nexus.repository.golang.internal.util;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class GolangPathUtilsTest
    extends TestSupport
{
  @Mock
  private TokenMatcher.State state;

  private GolangPathUtils underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new GolangPathUtils();

    when(state.getTokens()).thenReturn(ImmutableMap.of(
        "module", "github.com/sonatype/example",
        "version", "v1.0.2",
        "extension", "zip"
    ));
  }

  @Test
  public void module() {
    String module = underTest.module(state);

    assertThat(module, is(equalTo("github.com/sonatype/example")));
  }

  @Test
  public void version() {
    String version = underTest.version(state);

    assertThat(version, is(equalTo("v1.0.2")));
  }

  @Test
  public void extension() {
    String extension = underTest.extension(state);

    assertThat(extension, is(equalTo("zip")));
  }

  @Test
  public void assetPath() {
    String assetPath = underTest.assetPath(state);

    assertThat(assetPath, is(equalTo("github.com/sonatype/example/@v/v1.0.2.zip")));
  }

  @Test
  public void list() {
    String listPath = underTest.listPath(state);

    assertThat(listPath, is(equalTo("github.com/sonatype/example/@v/list")));
  }
}
