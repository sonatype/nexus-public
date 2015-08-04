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
package org.sonatype.nexus.testsuite.repo.nexus387;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Blocking, Exclusive, Inclusive Routes Tests
 */
public class Nexus387RoutesIT
    extends AbstractNexusIntegrationTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testExclusive()
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId() + ".exclusive", "exclusive", "1.0.0", null, "jar", 0, new Date().getTime(),
            "Simple Test Artifact", false, null, false, null);

    try {
      // should fail
      this.downloadArtifactFromGroup("exclusive-single", gav, "target/downloads/exclude");
      Assert.fail("Resource should not have been found.");
    }
    catch (IOException e) {
    }

    File artifact = this.downloadArtifactFromGroup("exclusive-group", gav, "target/downloads/exclude");
    Assert.assertNotNull(artifact);

    String line = this.getFirstLineOfFile(artifact);
    Assert.assertEquals("Jar contained: " + this.getFirstLineOfFile(artifact)
        + ", expected: exclusive2", line, "exclusive2");

    artifact = this.downloadArtifactFromGroup("other-group", gav, "target/downloads/exclude");
    Assert.assertNotNull(artifact);

    line = this.getFirstLineOfFile(artifact);
    Assert.assertEquals("Jar contained: " + line + ", expected: exclusive1", line, "exclusive1");

  }

  @Test
  public void testInclusive()
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId() + ".inclusive", "inclusive", "1.0.0", null, "jar", 0, new Date().getTime(),
            "Simple Test Artifact", false, null, false, null);

    File artifact = this.downloadArtifactFromGroup("inclusive-single", gav, "target/downloads/include");

    String line = this.getFirstLineOfFile(artifact);
    Assert.assertEquals("Jar contained: " + this.getFirstLineOfFile(artifact)
        + ", expected: inclusive1", line, "inclusive1");

    artifact = this.downloadArtifactFromGroup("inclusive-group", gav, "target/downloads/include");

    line = this.getFirstLineOfFile(artifact);
    Assert.assertEquals("Jar contained: " + this.getFirstLineOfFile(artifact)
        + ", expected: inclusive2", line, "inclusive2");

    artifact = this.downloadArtifactFromGroup("other-group", gav, "target/downloads/include");

    line = this.getFirstLineOfFile(artifact);
    Assert.assertEquals("Jar contained: " + this.getFirstLineOfFile(artifact)
        + ", expected: inclusive1", line, "inclusive1");

  }

  @Test
  public void testBlocking()
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId() + ".blocking", "blocking", "1.0.0", null, "jar", 0, new Date().getTime(),
            "Simple Test Artifact", false, null, false, null);

    try {

      this.downloadArtifactFromGroup("blocking-group", gav, "target/downloads/blocking");
      Assert.fail("This file should not have been found.");

    }
    catch (IOException e) {
    }
    File artifact = this.downloadArtifactFromGroup("other-group", gav, "target/downloads/blocking");

    String line = this.getFirstLineOfFile(artifact);
    Assert.assertEquals("Jar contained: " + this.getFirstLineOfFile(artifact)
        + ", expected: blocking1", line, "blocking1");

  }

  private String getFirstLineOfFile(File file)
      throws IOException
  {
    BufferedReader bReader = new BufferedReader(new FileReader(file));
    String line = bReader.readLine().trim(); // only need one line
    bReader.close();

    return line;

  }

}
