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
package org.sonatype.nexus.testsuite.p2.p2r02;

import java.io.File;

import org.sonatype.nexus.testsuite.p2.AbstractNexusP2GeneratorIT;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class P2R0201DeployLegacyJarIT
    extends AbstractNexusP2GeneratorIT
{

  public P2R0201DeployLegacyJarIT() {
    super("p2r02");
  }

  /**
   * When deploying a legacy jar (non OSGi bundle), p2Artifacts & p2Content are not created.
   */
  @Test
  public void test()
      throws Exception
  {
    createP2MetadataGeneratorCapability();

    deployArtifacts(getTestResourceAsFile("artifacts/jars"));

    final File p2Artifacts = storageP2ArtifactsFor("commons-logging", "commons-logging", "1.1.1");
    assertThat("p2Artifacts does not exist", p2Artifacts.exists(), is(false));

    final File p2Content = storageP2ContentFor("commons-logging", "commons-logging", "1.1.1");
    assertThat("p2Content does not exist", p2Content.exists(), is(false));
  }

}
