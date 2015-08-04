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
package org.sonatype.nexus.testsuite.deploy.nxcm970;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Nxcm970SimultaneousUploadDownloadIT
    extends AbstractNexusIntegrationTest
{
  private Executor executor = Executors.newSingleThreadExecutor();

  @BeforeClass
  public static void cleanUp()
      throws Exception
  {
    AbstractNexusIntegrationTest.cleanWorkDir();
  }

  @Test
  public void testSimultaneousUploadDownload()
      throws Exception
  {
    // preparations
    String baseUrl = getRepositoryUrl("nexus-test-harness-repo");
    // add path
    String targetUrl = baseUrl + "nxcm970/artifact/1.0/artifact-1.0.pom";

    // create deployer that we will control how long to "deploy"
    ContinuousDeployer continuousDeployer = new ContinuousDeployer(targetUrl);

    // download the subjectArtifact -- should result in 404
    // downloadSubjectArtifact( false, baseUrl );

    // start deploying the subjectArtifact -- should work on it
    executor.execute(continuousDeployer);

    // download the subjectArtifact -- should result in 404
    downloadSubjectArtifact(false, baseUrl);

    // let it work a lil'
    Thread.sleep(1000);

    // download the subjectArtifact -- should result in 404
    downloadSubjectArtifact(false, baseUrl);

    // let it work a lil'
    Thread.sleep(1000);

    // download the subjectArtifact -- should result in 404
    downloadSubjectArtifact(false, baseUrl);

    // finish deploying the subjectArtifaft -- should finish successfully
    continuousDeployer.finishDeploying();

    // wait to finish the HTTP tx, check result
    while (!continuousDeployer.isFinished()) {
      Thread.sleep(200);
    }

    Assert.assertTrue("Deployment failed: " + continuousDeployer.getResult(),
        continuousDeployer.getResult() == 201);

    // download the subjectArtifact -- should result in 200, found
    downloadSubjectArtifact(true, baseUrl);
  }

  // ==

  protected void downloadSubjectArtifact(boolean shouldSucceed, String baseUrl) {
    try {
      downloadArtifact(baseUrl, "nxcm970", "artifact", "1.0", "pom", null, "./target/downloaded-jars");

      if (!shouldSucceed) {
        Assert.fail("Should not succeed the retrieval!");
      }
    }
    catch (IOException e) {
      if (shouldSucceed) {
        Assert.fail("Should succeed the retrieval!");
      }
    }
  }

}
