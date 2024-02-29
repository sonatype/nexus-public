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
package org.sonatype.nexus.bootstrap.osgi;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.goodies.testsupport.hamcrest.FileMatchers.exists;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.NEXUS_EDITION;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.NEXUS_FEATURES;

public class ProStarterNexusEditionNonParameterizedTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ProStarterNexusEdition underTest;

  @Before
  public void setup() {
    underTest = new ProStarterNexusEdition();
  }

  @Test
  public void testApply() throws IOException {
    Properties properties = new Properties();
    File fakeWorkDir = temporaryFolder.newFolder();
    underTest.apply(properties, fakeWorkDir.toPath());
    assertThat(properties.get(NEXUS_EDITION), is("nexus-pro-starter-edition"));
    assertThat(properties.get(NEXUS_FEATURES), is("nexus-pro-starter-feature"));
    assertThat(new File(fakeWorkDir, "edition_pro_starter"), exists());
  }
}
