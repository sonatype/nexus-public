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
package org.sonatype.nexus.testsuite.p2.nexus5057;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.testsuite.p2.AbstractNexusP2GeneratorIT;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.exists;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.readable;

public class NEXUS5057EnsureEclipseBundleShapeIsSupportedIT
    extends
    AbstractNexusP2GeneratorIT
{

  public NEXUS5057EnsureEclipseBundleShapeIsSupportedIT() {
    super("nexus5057");
  }

  /**
   * See <a href="https://issues.sonatype.org/browse/NEXUS-5057">NEXUS-5057</a>.
   * <p/>
   * When a bundle is deployed that specifies
   * <code>Eclipse-BundleShape: dir</code> in the MANIFEST.MF then the
   * <code>*-p2Content.xml</code> file should include the correct instruction for whether the content has been
   * zipped.
   * <p>
   * <ul>
   * <li>repo location during test run = target/nexus/sonatype-work/storage/nexus5057/</li>
   * <li>p2Artifact file = test/nexus5057/1.0.0/nexus5057-1.0.0-p2Artifacts.xml</li>
   * <li>p2Content file = test/nexus5057/1.0.0/nexus5057-1.0.0-p2Content.xml</li>
   * </ul>
   * </p>
   */
  @Test
  public void test()
      throws Exception
  {
    createP2MetadataGeneratorCapability();
    createP2RepositoryAggregatorCapability();

    deployArtifacts(getTestResourceAsFile("artifacts/jars"));

    // ensure link created
    final File file = downloadFile(
        new URL(getNexusTestRepoUrl() + "/.meta/p2/plugins/test.nexus5057_1.0.0.jar"),
        new File("target/downloads/" + this.getClass().getSimpleName() + "/test.nexus5057_1.0.0.jar")
            .getCanonicalPath()
    );

    assertThat(file, is(readable()));

    // ensure p2Content created
    final File p2Content = downloadP2ContentFor("test", "nexus5057", "1.0.0");
    assertThat("p2Content has been downloaded", p2Content, is(notNullValue()));
    assertThat("p2Content exists", p2Content.exists(), is(true));

    // ensure repositories are valid
    final File installDir = new File("target/eclipse/nexus5057");

    installUsingP2(getNexusTestRepoUrl() + "/.meta/p2", "test.nexus5057", installDir.getCanonicalPath());

    // ensure that bundle is unzipped
    final File bundle = new File(installDir, "plugins/test.nexus5057_1.0.0.jar");
    assertThat(bundle, not(exists()));

    final File bundleDir = new File(installDir, "plugins/test.nexus5057_1.0.0/nexus5057/placeholder.txt");
    assertThat("bundle must be unzipped when installed", bundleDir, is(readable()));
  }

}
