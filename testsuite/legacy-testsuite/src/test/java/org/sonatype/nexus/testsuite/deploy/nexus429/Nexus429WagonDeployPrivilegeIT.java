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
package org.sonatype.nexus.testsuite.deploy.nexus429;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.MavenDeployer;

import org.apache.commons.io.FileUtils;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test the privilege wagon style deployments.
 */
public class Nexus429WagonDeployPrivilegeIT
    extends AbstractPrivilegeTest
{
  private static final String TEST_RELEASE_REPO = "nexus-test-harness-release-repo";

  private static final String TEST_SNAPSHOT_REPO = "nexus-test-harness-snapshot-repo";

  public Nexus429WagonDeployPrivilegeIT() {
    super(TEST_RELEASE_REPO);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void doReleaseArtifactTest()
      throws Exception
  {
    Gav gav =
        new Gav(this.getTestId(), "artifact", "1.0.0", null, "xml", 0, new Date().getTime(), "", false,
            null, false, null);
    this.setTestRepositoryId(TEST_RELEASE_REPO);
    this.deployPrivWithWagon(gav, this.getNexusTestRepoUrl());
  }

  @Test
  public void doSnapshotArtifactTest()
      throws Exception
  {
    Gav gav =
        new Gav(this.getTestId(), "artifact", "1.0.0-SNAPSHOT", null, "xml", 0, new Date().getTime(), "",
            false, null, false, null);
    this.setTestRepositoryId(TEST_SNAPSHOT_REPO);
    this.deployPrivWithWagon(gav, this.getNexusTestRepoUrl());
  }

  @Override
  public void resetTestUserPrivs()
      throws Exception
  {
    super.resetTestUserPrivs();

    // give use view access to everything, (this acts like > 1.4)
    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");
  }

  private void deployPrivWithWagon(Gav gav, String repoUrl)
      throws Exception
  {
    this.resetTestUserPrivs();

    Verifier verifier = null;

    this.deleteFromRepository(this.getTestId());

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    // we need to delete the files...
    this.deleteFromRepository(this.getTestId() + "/");

    // deploy
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // with pom should fail

    try {
      verifier =
          MavenDeployer.deployAndGetVerifier(gav, repoUrl, fileToDeploy,
              this.getOverridableFile("settings.xml"));
      failTest(verifier);
    }
    catch (VerificationException e) {
      // expected 403
    }

    // give deployment role
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // try again
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // if this fails it will throw an error
    try {
      verifier =
          MavenDeployer.deployAndGetVerifier(gav, repoUrl, fileToDeploy,
              this.getOverridableFile("settings.xml"));
      failTest(verifier);
    }
    catch (VerificationException e) {
      // expected 403
    }

    // try again
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    TestContainer.getInstance().getTestContext().useAdminForRequests();
    this.giveUserPrivilege("test-user", "T5");

    // if this fails it will throw an error
    verifier =
        MavenDeployer.deployAndGetVerifier(gav, repoUrl, fileToDeploy, this.getOverridableFile("settings.xml"));
    verifier.verifyErrorFreeLog();

    // do it again as an update, this should fail

    // if this fails it will throw an error
    try {
      verifier =
          MavenDeployer.deployAndGetVerifier(gav, repoUrl, fileToDeploy,
              this.getOverridableFile("settings.xml"));
      failTest(verifier);
    }
    catch (VerificationException e) {
      // expected 403
    }

    // now the user should be able to redeploy
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    this.giveUserPrivilege("test-user", "T3");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);
    // if this fails it will throw an error
    verifier =
        MavenDeployer.deployAndGetVerifier(gav, repoUrl, fileToDeploy, this.getOverridableFile("settings.xml"));
    verifier.verifyErrorFreeLog();

    // check the services url too, ( just the GET for now )
    // just check the parent dir, incase this is a SNAPSHOT repo
    Response response =
        RequestFacade.sendMessage(new URL(
            this.getNexusTestRepoServiceUrl() + gav.getGroupId().replace('.', '/') + "/" + gav.getArtifactId() + "/" +
                gav.getVersion() + "/"),
            Method.GET, null);
    Assert.assertEquals("Artifact should have been downloaded", response.getStatus().getCode(), 200);


    // make sure delete does not work
    response =
        RequestFacade.sendMessage("content/repositories/" + this.getTestRepositoryId() + "/" + this.getTestId(),
            Method.DELETE);
    Assert.assertEquals("Artifact should have been deleted", response.getStatus().getCode(), 403);

  }

  // FIXME: refactor AbstractMavenNexusIT and sub class this from there.

  /**
   * Workaround to get some decent logging when tests fail
   */
  protected void failTest(Verifier verifier)
      throws IOException
  {
    File logFile = new File(verifier.getBasedir(), "log.txt");
    String log = FileUtils.readFileToString(logFile);
    Assert.fail(log);
  }

}
