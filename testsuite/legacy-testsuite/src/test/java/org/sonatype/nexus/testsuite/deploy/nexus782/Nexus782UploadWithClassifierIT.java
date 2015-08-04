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
package org.sonatype.nexus.testsuite.deploy.nexus782;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import org.apache.maven.index.artifact.Gav;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Status;

import static org.junit.Assert.assertTrue;

public class Nexus782UploadWithClassifierIT
    extends AbstractNexusIntegrationTest
{

  private Gav artifactGav = new Gav(this.getTestId(), "same-pom", "1.2.5", null, "jar", 0, new Date().getTime(),
      "same-pom", false, null, false, null);

  private Gav artifactClassifierGav = new Gav(this.getTestId(), "same-pom", "1.2.5", "CLASSIFIER", "jar", 0,
      new Date().getTime(), "same-pom", false, null, false, null);

  public Nexus782UploadWithClassifierIT()
      throws Exception
  {
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void withSamePomNoExtention()
      throws IOException
  {

    File artifactFile = this.getTestFile("same-pom/same-pom.jar");
    File artifactClassifierFile = this.getTestFile("same-pom/same-pom-classifier.jar");
    File pomFile = this.getTestFile("same-pom/pom.xml");

    // upload jar artifact
    int result =
        getDeployUtils().deployUsingPomWithRest(this.getTestRepositoryId(), artifactFile, pomFile, null, null);
    assertTrue("Got error from server: " + result, Status.isSuccess(result));

    // make sure everything is cool so far
    this.checkUpload(this.artifactGav, artifactFile);

    // upload jar artifact with classifier
    result =
        getDeployUtils().deployUsingPomWithRest(this.getTestRepositoryId(), artifactClassifierFile, pomFile,
            this.artifactClassifierGav.getClassifier(), null);
    assertTrue("Got error from server: " + result, Status.isSuccess(result));

    // now check files again
    this.checkUpload(this.artifactGav, artifactFile);
    this.checkUpload(this.artifactClassifierGav, artifactClassifierFile);

  }

  @Test
  public void withSamePomExtention()
      throws Exception
  {

    File artifactFile = this.getTestFile("same-pom/same-pom.jar");
    File artifactClassifierFile = this.getTestFile("same-pom/same-pom-classifier.jar");
    File pomFile = this.getTestFile("same-pom/pom.xml");

    // upload jar artifact
    int result =
        getDeployUtils().deployUsingPomWithRest(this.getTestRepositoryId(), artifactFile, pomFile, null,
            this.artifactGav.getExtension());
    assertTrue("Got error from server: " + result, Status.isSuccess(result));
    getEventInspectorsUtil().waitForCalmPeriod();

    // make sure everything is cool so far
    this.checkUpload(this.artifactGav, artifactFile);

    // upload jar artifact with classifier
    result =
        getDeployUtils().deployUsingPomWithRest(this.getTestRepositoryId(), artifactClassifierFile, pomFile,
            this.artifactClassifierGav.getClassifier(),
            this.artifactClassifierGav.getExtension());
    assertTrue("Got error from server: " + result, Status.isSuccess(result));

    // now check files again
    this.checkUpload(this.artifactGav, artifactFile);
    this.checkUpload(this.artifactClassifierGav, artifactClassifierFile);

  }

  private void checkUpload(Gav gav, File originalFile)
      throws IOException
  {
    // download it
    File artifact = downloadArtifact(gav, "./target/downloaded-jars");

    // make sure its here
    assertTrue(artifact.exists());

    // make sure it is what we expect.
    assertTrue(FileTestingUtils.compareFileSHA1s(originalFile, artifact));
  }

}
