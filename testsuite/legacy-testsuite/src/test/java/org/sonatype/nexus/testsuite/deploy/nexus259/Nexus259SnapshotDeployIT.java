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
package org.sonatype.nexus.testsuite.deploy.nexus259;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;

import org.apache.http.HttpStatus;
import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Deploys a snapshot artifact using a wagon and REST (both gav and pom) REST should fail
 */
public class Nexus259SnapshotDeployIT
    extends AbstractNexusIntegrationTest
{

  private static final String TEST_SNAPSHOT_REPO = "nexus-test-harness-snapshot-repo";

  public Nexus259SnapshotDeployIT() {
    super(TEST_SNAPSHOT_REPO);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void deployUsingRest()
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId(), "uploadWithGav", "1.0.0-SNAPSHOT", null, "xml", 0,
            new Date().getTime(), "Simple Test Artifact", false, null, false, null);

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    int status = getDeployUtils().deployUsingGavWithRest(uploadURL, TEST_SNAPSHOT_REPO, gav, fileToDeploy);

    if (status != HttpStatus.SC_BAD_REQUEST) {
      Assert.fail("Snapshot repositories do not allow manual file upload: " + status);
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
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId(), "uploadWithGav", "1.0.0-SNAPSHOT", null, "xml", 0,
            new Date().getTime(), "Simple Test Artifact", false, null, false, null);

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    File pomFile = this.getTestFile("pom.xml");

    // the Restlet Client does not support multipart forms: http://restlet.tigris.org/issues/show_bug.cgi?id=71

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    int status = getDeployUtils()
        .deployUsingPomWithRest(uploadURL, TEST_SNAPSHOT_REPO, fileToDeploy, pomFile, null, null);

    if (status != HttpStatus.SC_BAD_REQUEST) {
      Assert.fail("Snapshot repositories do not allow manual file upload: " + status);
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
