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
package org.sonatype.nexus.testsuite.deploy.nexus167;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.wagon.TransferFailedException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Deploy a release artifact to a snapshot repo. (should fail)
 */
public class Nexus167ReleaseToSnapshotIT
    extends AbstractNexusIntegrationTest
{

  private static final String TEST_SNAPSHOT_REPO = "nexus-test-harness-snapshot-repo";

  public Nexus167ReleaseToSnapshotIT() {
    super(TEST_SNAPSHOT_REPO);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void deployReleaseToSnapshot()
      throws Exception
  {
    Gav gav = new Gav(
        this.getTestId(),
        "simpleArtifact",
        "1.0.0",
        null,
        "xml",
        0,
        new Date().getTime(),
        "Simple Test Artifact",
        false,
        null,
        false,
        null);

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    try {
      // deploy it
      // this should fail
      getDeployUtils().deployWithWagon("http", this.getNexusTestRepoUrl(), fileToDeploy, this
          .getRelitiveArtifactPath(gav));
      Assert.fail("Should not be able to deploy a releases artifact into a snapshot repo");
    }
    catch (TransferFailedException e) {
      // cstamas: HTTP 400 is returned from now on
      // this is expected
    }

  }

  @Test
  public void deployUsingRest()
      throws Exception
  {

    Gav gav = new Gav(
        this.getTestId(),
        "uploadWithGav",
        "1.0.0",
        null,
        "xml",
        0,
        new Date().getTime(),
        "Simple Test Artifact",
        false,
        null,
        false,
        null);

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    // the method we are calling
    HttpPost filePost = new HttpPost(uploadURL);
    filePost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);

    int status = getDeployUtils().deployUsingGavWithRest(uploadURL, TEST_SNAPSHOT_REPO, gav, fileToDeploy);

    if (status != HttpStatus.SC_BAD_REQUEST) {
      Assert.fail("Upload attempt should have returned a 400, it returned:  " + status);
    }

    boolean fileWasUploaded = true;
    try {
      // download it
      downloadArtifact(gav, "./target/downloaded-jars");
    }
    catch (FileNotFoundException e) {
      fileWasUploaded = false;
    }

    Assert.assertFalse("The file was uploaded and it should not have been.", fileWasUploaded);
  }

  @Test
  public void deploywithPomUsingRest()
      throws HttpException,
             Exception
  {

    Gav gav = new Gav(
        this.getTestId(),
        "uploadWithPom",
        "1.0.0",
        null,
        "xml",
        0,
        new Date().getTime(),
        "Simple Test Artifact",
        false,
        null,
        false,
        null);

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    File pomFile = this.getTestFile("pom.xml");

    // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    int status = getDeployUtils().deployUsingPomWithRest(
        uploadURL,
        TEST_SNAPSHOT_REPO,
        fileToDeploy,
        pomFile,
        null,
        null);

    if (status != HttpStatus.SC_BAD_REQUEST) {
      Assert.fail("Upload attempt should have returned a 400, it returned:  " + status);
    }

    boolean fileWasUploaded = true;
    try {
      // download it
      downloadArtifact(gav, "./target/downloaded-jars");
    }
    catch (FileNotFoundException e) {
      fileWasUploaded = false;
    }

    Assert.assertFalse("The file was uploaded and it should not have been.", fileWasUploaded);
  }

}
