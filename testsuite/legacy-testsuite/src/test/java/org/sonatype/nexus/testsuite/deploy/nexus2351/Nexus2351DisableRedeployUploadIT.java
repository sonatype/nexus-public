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
package org.sonatype.nexus.testsuite.deploy.nexus2351;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractMavenNexusIT;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus2351DisableRedeployUploadIT
    extends AbstractMavenNexusIT
{

  private RepositoryMessageUtil repoUtil = null;

  public Nexus2351DisableRedeployUploadIT()
      throws ComponentLookupException
  {

  }

  @Before
  public void init()
      throws ComponentLookupException
  {
    this.repoUtil = new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void disableReleaseAllowRedeployWithMavenTest()
      throws Exception
  {
    final String repoId = "nexus2351disableReleaseAllowRedeployWithMavenTest";
    setWritePolicy(repoId, RepositoryWritePolicy.ALLOW_WRITE);

    Gav gav1 =
        new Gav(this.getTestId(), "release-deploy", "1.0.0", null, "jar", 0, new Date().getTime(),
            "release-deploy", false, null, false, null);

    Gav gav2 =
        new Gav(this.getTestId(), "release-deploy", "1.0.1", null, "jar", 0, new Date().getTime(),
            "release-deploy", false, null, false, null);
    File mavenProject1 = getTestFile("maven-project-1");
    File mavenProject2 = getTestFile("maven-project-2");
    this.deployWithMavenExpectSuccess(mavenProject1, repoId);
    Metadata metadata = this.downloadMetadataFromRepository(gav1, repoId);
    Date firstDeployDate = this.getLastDeployTimeStamp(metadata);
    // we need to sleep 1 second, because we are dealing with a one second accuracy
    Thread.sleep(1000);
    this.deployWithMavenExpectSuccess(mavenProject1, repoId);
    metadata = this.downloadMetadataFromRepository(gav1, repoId);
    Date secondDeployDate = this.getLastDeployTimeStamp(metadata);
    Assert.assertTrue(
        "deploy date was not updated, or is incorrect, first: " + firstDeployDate + " second: " + secondDeployDate,
        firstDeployDate.before(secondDeployDate));
    // we need to sleep 1 second, because we are dealing with a one second accuracy
    Thread.sleep(1000);
    this.deployWithMavenExpectSuccess(mavenProject1, repoId);
    metadata = this.downloadMetadataFromRepository(gav1, repoId);
    Date thirdDeployDate = this.getLastDeployTimeStamp(metadata);
    Assert.assertTrue(
        "deploy date was not updated, or is incorrect, second: " + firstDeployDate + " third: " + secondDeployDate,
        secondDeployDate.before(thirdDeployDate));
    this.deployWithMavenExpectSuccess(mavenProject2, repoId);
    metadata = this.downloadMetadataFromRepository(gav2, repoId);
    // now check the metadata for both versions
    Assert.assertTrue(metadata.getVersioning().getVersions().contains("1.0.0"));
    Assert.assertTrue(metadata.getVersioning().getVersions().contains("1.0.1"));

    Assert.assertEquals(2, metadata.getVersioning().getVersions().size());

  }

  // @Test FIXME: BROKEN NEXUS-2395
  public void disableReleaseAllowRedeployWithUploadTest()
      throws Exception
  {
    final String repoId = "nexus2351disableReleaseAllowRedeployWithUploadTest";
    setWritePolicy(repoId, RepositoryWritePolicy.ALLOW_WRITE);

    Gav gav =
        new Gav(this.getTestId(), "release-deploy", "1.0.0", null, "jar", 0, new Date().getTime(),
            "release-deploy", false, null, false, null);

    File fileToDeploy = getTestFile("artifact.jar");

    Assert.assertEquals(201, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));
    Metadata metadata = this.downloadMetadataFromRepository(gav, repoId);
    Date firstDeployDate = this.getLastDeployTimeStamp(metadata);
    // we need to sleep 1 second, because we are dealing with a one second accuracy
    Thread.sleep(1000);

    Assert.assertEquals(201, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));
    metadata = this.downloadMetadataFromRepository(gav, repoId);
    Date secondDeployDate = this.getLastDeployTimeStamp(metadata);
    Assert.assertTrue(
        "deploy date was not updated, or is incorrect, first: " + firstDeployDate + " second: " + secondDeployDate,
        firstDeployDate.before(secondDeployDate));
    // we need to sleep 1 second, because we are dealing with a one second accuracy
    Thread.sleep(1000);

    Assert.assertEquals(201, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));
    metadata = this.downloadMetadataFromRepository(gav, repoId);
    Date thirdDeployDate = this.getLastDeployTimeStamp(metadata);
    Assert.assertTrue(
        "deploy date was not updated, or is incorrect, second: " + firstDeployDate + " third: " + secondDeployDate,
        secondDeployDate.before(thirdDeployDate));
  }

  @Test
  public void disableReleaseReadOnlyWithUploadTest()
      throws Exception
  {
    String repoId = "nexus2351disableReleaseReadOnlyWithUploadTest";
    setWritePolicy(repoId, RepositoryWritePolicy.READ_ONLY);

    Gav gav =
        new Gav(this.getTestId(), "disableReleaseReadOnlyWithUploadTest", "1.0.0", null, "jar", 0,
            new Date().getTime(), "disableReleaseReadOnlyWithUploadTest", false, null, false, null);

    File fileToDeploy = getTestFile("artifact.jar");

    Assert.assertEquals(400, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));
    Assert.assertEquals(400, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));

  }

  @Test
  public void disableReleaseReadOnlyWithMavenTest()
      throws Exception
  {

    String repoId = "nexus2351disableReleaseReadOnlyWithMavenTest";
    setWritePolicy(repoId, RepositoryWritePolicy.READ_ONLY);

    File mavenProject = getTestFile("maven-project-1");

    this.deployWithMavenExpectFailure(mavenProject, repoId);
    this.deployWithMavenExpectFailure(mavenProject, repoId);

  }

  @Test
  public void disableReleaseNoRedeployWithUploadTest()
      throws Exception
  {

    String repoId = "nexus2351disableReleaseNoRedeployWithUploadTest";
    setWritePolicy(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE);

    Gav gav =
        new Gav(this.getTestId(), "disableReleaseNoRedeployTest", "1.0.0", null, "jar", 0, new Date().getTime(),
            "disableReleaseNoRedeployTest", false, null, false, null);

    File fileToDeploy = getTestFile("artifact.jar");

    Assert.assertEquals(201, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));
    Assert.assertEquals(400, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));
    Assert.assertEquals(400, getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy));

  }

  @Test
  public void disableReleaseNoRedeployWithMavenTest()
      throws Exception
  {

    String repoId = "nexus2351disableReleaseNoRedeployWithMavenTest";
    setWritePolicy(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE);

    Gav gav1 =
        new Gav(this.getTestId(), "release-deploy", "1.0.0", null, "jar", 0, new Date().getTime(),
            "disableReleaseNoRedeployTest", false, null, false, null);

    Gav gav2 =
        new Gav(this.getTestId(), "release-deploy", "1.0.1", null, "jar", 0, new Date().getTime(),
            "disableReleaseNoRedeployTest", false, null, false, null);

    File mavenProject1 = getTestFile("maven-project-1");
    File mavenProject2 = getTestFile("maven-project-2");

    // (deploy whould work, once)
    this.deployWithMavenExpectSuccess(mavenProject1, repoId);
    Metadata metadata = this.downloadMetadataFromRepository(gav1, repoId);
    Date firstDeployDate = this.getLastDeployTimeStamp(metadata);
    // we need to sleep 1 second, because we are dealing with a one second accuracy
    Thread.sleep(1000);

    // deploy again (should fail)
    this.deployWithMavenExpectFailure(mavenProject1, repoId);
    metadata = this.downloadMetadataFromRepository(gav1, repoId);
    Assert.assertEquals(this.getLastDeployTimeStamp(metadata), firstDeployDate);

    // deploy a new version
    this.deployWithMavenExpectSuccess(mavenProject2, repoId);
    metadata = this.downloadMetadataFromRepository(gav2, repoId);

    // now check the metadata for both versions
    Assert.assertTrue(metadata.getVersioning().getVersions().contains("1.0.0"));
    Assert.assertTrue(metadata.getVersioning().getVersions().contains("1.0.1"));

    Assert.assertEquals(2, metadata.getVersioning().getVersions().size());

  }

  private Date getLastDeployTimeStamp(Metadata metadata)
      throws ParseException
  {
    String lastUpdateString = metadata.getVersioning().getLastUpdated();

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    return dateFormat.parse(lastUpdateString);
  }

  private void deployWithMavenExpectSuccess(File mavenProject, String targetRepoId)
      throws VerificationException, IOException
  {
    // deploy using maven
    Verifier verifier = this.createVerifier(mavenProject);
    try {
      verifier.getCliOptions().add("-DaltDeploymentRepository=repo::default::" + createUrl(targetRepoId));
      verifier.executeGoal("deploy");
      verifier.verifyErrorFreeLog();
    }

    catch (VerificationException e) {
      File logs = new File(nexusLogDir);
      File bkp = new File("./target/logs/nexus2351-bkp");
      bkp.mkdirs();
      FileUtils.copyDirectory(logs, bkp);
      failTest(verifier);
    }
  }

  private void deployWithMavenExpectFailure(File mavenProject, String targetRepoId)
      throws VerificationException, IOException
  {
    // deploy using maven
    Verifier verifier = this.createVerifier(mavenProject);
    try {
      verifier.getCliOptions().add("-DaltDeploymentRepository=repo::default::" + createUrl(targetRepoId));
      verifier.executeGoal("deploy");

      verifier.verifyErrorFreeLog();

      Assert.fail("Should return 401 error");
    }
    catch (VerificationException e) {
      // expect error
    }

  }

  private String createUrl(String targetRepoId) {
    return "http:////localhost:" + TestProperties.getString("nexus.application.port")
        + "/nexus/content/repositories/" + targetRepoId;
  }

  private RepositoryResource setWritePolicy(String repoId, RepositoryWritePolicy policy)
      throws Exception
  {
    RepositoryResource repo = (RepositoryResource) this.repoUtil.getRepository(repoId);
    repo.setWritePolicy(policy.name());
    repo = (RepositoryResource) this.repoUtil.updateRepo(repo);

    TaskScheduleUtil.waitForAllTasksToStop();

    return repo;
  }

}
