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
package org.sonatype.nexus.testsuite.deploy.nexus258;

import java.io.File;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import org.apache.http.HttpStatus;
import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * Deploys a release artifact using a wagon and REST (both gav and pom)
 */
public class Nexus258ReleaseDeployIT
    extends AbstractNexusIntegrationTest
{

  private static final String TEST_RELEASE_REPO = "nexus-test-harness-release-repo";

  public Nexus258ReleaseDeployIT() {
    super(TEST_RELEASE_REPO);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void deploywithGavUsingRest()
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId(), "uploadWithGav", "1.0.0", null, "xml", 0,
            new Date().getTime(), "Simple Test Artifact", false, null, false, null);

    // file to deploy
    File fileToDeploy =
        this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    int status = getDeployUtils().deployUsingGavWithRest(uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy);

    if (status != HttpStatus.SC_CREATED) {
      Assert.fail("File did not upload successfully, status code: " + status);
    }

    // download it
    File artifact = downloadArtifact(gav, "./target/downloaded-jars");

    // make sure its here
    assertTrue(artifact.exists());

    // make sure it is what we expect.
    assertTrue(FileTestingUtils.compareFileSHA1s(fileToDeploy, artifact));
  }


  @Test
  public void deployWithPomUsingRest()
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId(), "uploadWithPom", "1.0.0", null, "xml", 0,
            new Date().getTime(), "Simple Test Artifact", false, null, false, null);

    // file to deploy
    File fileToDeploy =
        this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    File pomFile =
        this.getTestFile("pom.xml");

    // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    int status = getDeployUtils()
        .deployUsingPomWithRest(uploadURL, TEST_RELEASE_REPO, fileToDeploy, pomFile, null, null);

    if (status != HttpStatus.SC_CREATED) {
      Assert.fail("File did not upload successfully, status code: " + status);
    }

    // download it
    File artifact = downloadArtifact(gav, "./target/downloaded-jars");

    // make sure its here
    assertTrue(artifact.exists());

    // make sure it is what we expect.
    assertTrue(FileTestingUtils.compareFileSHA1s(fileToDeploy, artifact));

  }


}
