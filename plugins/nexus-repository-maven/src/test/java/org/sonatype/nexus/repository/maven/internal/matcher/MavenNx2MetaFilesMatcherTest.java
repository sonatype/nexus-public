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
package org.sonatype.nexus.repository.maven.internal.matcher;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MavenNx2MetaFilesMatcherTest
    extends TestSupport
{
  @Mock
  MavenPathParser mavenPathParser;

  @Mock
  MavenPath mavenPath;

  @Mock
  Context context;

  @Mock
  Request request;

  MavenNx2MetaFilesMatcher underTest;

  @Before
  public void setup() {
    when(mavenPathParser.parsePath(any())).thenReturn(mavenPath);
    when(context.getRequest()).thenReturn(request);
    when(context.getAttributes()).thenReturn(new AttributesMap());

    underTest = new MavenNx2MetaFilesMatcher(mavenPathParser);
  }

  @Test
  public void testMatches() {
    when(request.getPath()).thenReturn("/.meta/prefixes.txt");
    assertThat(underTest.matches(context), is(true));
    when(request.getPath()).thenReturn("/.meta/somethingelse.txt");
    assertThat(underTest.matches(context), is(true));
  }

  @Test
  public void testNonMatches() {
    when(request.getPath()).thenReturn("/real/content.txt");
    assertThat(underTest.matches(context), is(false));
  }
}
