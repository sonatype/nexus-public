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
package org.sonatype.nexus.ui;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;

import org.sonatype.goodies.testsupport.TestSupport;

import org.eclipse.sisu.space.ClassSpace;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class UiUtilTest
    extends TestSupport
{
  @Mock
  private ClassSpace space;

  @Test
  public void getHashedFilename() throws Exception {
    Enumeration<URL> mockedResponse = enumeration(asList(
        new File("/nexus-frontend-bundle.js").toURI().toURL()
    ));
    when(space.findEntries("static", "nexus-frontend-bundle.js", true)).thenReturn(mockedResponse);

    String hashedFilename = UiUtil.getPathForFile("nexus-frontend-bundle.js", space);

    assertThat(hashedFilename, is("/nexus-frontend-bundle.js"));
  }
}
