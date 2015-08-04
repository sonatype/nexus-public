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
package org.sonatype.nexus.testsuite.deploy.nexus260;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Deploys an artifact multiple times. (this is allowed)
 */
public class Nexus260MultipleDeployIT
    extends AbstractNexusIntegrationTest
{

  public Nexus260MultipleDeployIT() {
    super("nexus-test-harness-repo");
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void singleDeployTest()
      throws Exception
  {
    // file to deploy
    File fileToDeploy = this.getTestFile("singleDeployTest.xml");

    // deploy it
    getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy,
        "org/sonatype/nexus-integration-tests/multiple-deploy-test/singleDeployTest/1/singleDeployTest-1.xml");

    // download it
    File artifact =
        downloadArtifact("org.sonatype.nexus-integration-tests.multiple-deploy-test", "singleDeployTest", "1",
            "xml", null, "./target/downloaded-jars");

    // make sure its here
    assertTrue(artifact.exists());

    // make sure it is what we expect.
    assertTrue(FileTestingUtils.compareFileSHA1s(fileToDeploy, artifact));
  }

  @Test
  public void deploySameFileMultipleTimesTest()
      throws Exception
  {
    // file to deploy
    File fileToDeploy = this.getTestFile("deploySameFileMultipleTimesTest.xml");

    String deployPath =
        "org/sonatype/nexus-integration-tests/multiple-deploy-test/deploySameFileMultipleTimesTest/1/deploySameFileMultipleTimesTest-1.xml";

    // deploy it
    getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy, deployPath);

    // deploy it
    getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy, deployPath);
    // deploy it
    getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy, deployPath);

    // download it
    File artifact =
        downloadArtifact("org.sonatype.nexus-integration-tests.multiple-deploy-test",
            "deploySameFileMultipleTimesTest", "1", "xml", null, "./target/downloaded-jars");

    // make sure its here
    assertTrue(artifact.exists());

    // make sure it is what we expect.
    assertTrue(FileTestingUtils.compareFileSHA1s(fileToDeploy, artifact));

  }

  @Test
  public void deployChangedFileMultipleTimesTest()
      throws Exception
  {
    // files to deploy
    File fileToDeploy1 = this.getTestFile("deployChangedFileMultipleTimesTest1.xml");
    File fileToDeploy2 = this.getTestFile("deployChangedFileMultipleTimesTest2.xml");
    File fileToDeploy3 = this.getTestFile("deployChangedFileMultipleTimesTest3.xml");

    String deployPath =
        "org/sonatype/nexus-integration-tests/multiple-deploy-test/deployChangedFileMultipleTimesTest/1/deployChangedFileMultipleTimesTest-1.xml";

    // deploy it
    getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy1, deployPath);

    // deploy it
    getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy2, deployPath);
    // deploy it
    getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy3, deployPath);

    // download it
    File artifact =
        downloadArtifact("org.sonatype.nexus-integration-tests.multiple-deploy-test",
            "deployChangedFileMultipleTimesTest", "1", "xml", null, "./target/downloaded-jars");

    // make sure its here
    assertTrue(artifact.exists());

    // make sure it is what we expect.
    assertTrue(FileTestingUtils.compareFileSHA1s(fileToDeploy3, artifact));

    // this should pass if the above passed
    assertFalse(FileTestingUtils.compareFileSHA1s(fileToDeploy2, artifact));

  }
}
