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
package org.sonatype.nexus.repository.conda.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.conda.internal.AssetKind;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.conda.internal.AssetKind.*;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.CONDA_EXT;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.INDEX_HTML;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.TAR_BZ2_EXT;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.arch;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.build;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.buildArchAssetPath;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.buildAssetPath;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.buildCondaPackagePath;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.name;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.path;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.version;

/**
 * @since 3.19
 */
public class CondaPathUtilsTest
    extends TestSupport
{
  private static final String MAIN = "main";

  private static final String VERSION = "3.0.0";

  private static final String BUILD = "123";

  private static final String NUMPY = "numpy";

  private static final String ARCH = "osx";

  private static final String PATH = "/" + MAIN + "/" + ARCH + "/";

  @Mock
  TokenMatcher.State state;

  @Before
  public void setUp() {
    Map<String, String> tokens = setupTokens(VERSION, MAIN, BUILD, NUMPY, ARCH, ARCH_CONDA_PACKAGE);
    when(state.getTokens()).thenReturn(tokens);
  }

  @Test
  public void archTest() {
    String result = arch(state);

    assertThat(result, is(equalTo(ARCH)));
  }

  @Test
  public void nameTest() {
    String result = name(state);

    assertThat(result, is(equalTo(NUMPY)));
  }

  @Test
  public void pathTest() {
    String result = path(state);

    assertThat(result, is(equalTo("/" + MAIN)));
  }

  @Test
  public void versionTest() {
    String result = version(state);

    assertThat(result, is(equalTo(VERSION)));
  }

  @Test
  public void buildTest() {
    String result = build(state);

    assertThat(result, is(equalTo(BUILD)));
  }

  @Test
  public void buildAssetPathTest() {
    String result = buildAssetPath(state, INDEX_HTML);

    assertThat(result, is(equalTo("/" + MAIN + "/index.html")));
  }

  @Test
  public void buildArchAssetPathTest() {
    String result = buildArchAssetPath(state, INDEX_HTML);

    assertThat(result, is(equalTo(PATH + "index.html")));
  }

  @Test
  public void buildTarPackagePathTest() {
    Map<String, String> tokens = setupTokens(VERSION, MAIN, BUILD, NUMPY, ARCH, ARCH_TAR_PACKAGE);
    when(state.getTokens()).thenReturn(tokens);

    String result = buildCondaPackagePath(state);
    assertThat(result, is(equalTo(PATH + "numpy-3.0.0-123.tar.bz2")));
  }

  @Test
  public void buildCondaPackagePathTest() {
    String result = buildCondaPackagePath(state);

    assertThat(result, is(equalTo(PATH + "numpy-3.0.0-123.conda")));
  }

  private Map<String, String> setupTokens(final String version,
                                          final String path,
                                          final String build,
                                          final String name,
                                          final String arch,
                                          final AssetKind assetKind)
  {
    Map<String, String> tokens = new HashMap<>();
    tokens.put("version", version);
    tokens.put("path", "/" + path);
    tokens.put("build", build);
    tokens.put("name", name);
    tokens.put("arch", arch);
    tokens.put("format", ARCH_TAR_PACKAGE.equals(assetKind) ? TAR_BZ2_EXT : CONDA_EXT);

    return tokens;
  }
}
