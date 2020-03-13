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
package org.sonatype.nexus.repository.pypi.upgrade;

import java.io.File;
import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class PyPiUpgrade_1_2Test
    extends TestSupport
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock
  private ApplicationDirectories applicationDirectories;

  private PyPiUpgrade_1_2 underTest;

  @Before
  public void setup() throws IOException {
    when(applicationDirectories.getWorkDirectory("db")).thenReturn(tempFolder.newFolder());
    underTest = new PyPiUpgrade_1_2(applicationDirectories);
  }

  @Test
  public void testApply() throws Exception {
    underTest.apply();

    File markerFile = new File(applicationDirectories.getWorkDirectory("db"), PyPiUpgrade_1_2.MARKER_FILE);
    assertTrue(markerFile.exists());
  }
}
