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
package org.sonatype.nexus.yum.internal;

import java.io.File;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.yum.internal.RepositoryUtils.getBaseDir;

public class RepositoryUtilsTest
    extends TestSupport
{

  @Test
  public void shouldConvertRepositoryLocalUrlToFile()
      throws Exception
  {
    assertConvertion("file:/hallo/world", "/hallo/world");
    assertConvertion("/hallo/world", "/hallo/world");
  }

  private void assertConvertion(String localUrl, String expected)
      throws Exception
  {
    final Repository repo = mock(Repository.class);
    when(repo.getLocalUrl()).thenReturn(localUrl);

    // make the expected path canonical, due to Windows
    String canonicalPath = new File(expected).getCanonicalPath();
    assertThat(getBaseDir(repo).getAbsolutePath(), is(canonicalPath));
  }
}
