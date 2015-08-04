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

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus2351DisableRedeployMaven1IT
    extends AbstractNexusIntegrationTest
{

  private RepositoryMessageUtil repoUtil = null;

  private File artifact;

  private File artifactMD5;

  public Nexus2351DisableRedeployMaven1IT() {

  }

  @Before
  public void init()
      throws ComponentLookupException
  {
    this.repoUtil = new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Before
  public void create() {
    artifact = this.getTestFile("artifact.jar");
    artifactMD5 = this.getTestFile("artifact.jar.md5");
  }

  @Test
  public void testM1ReleaseAllowRedeploy()
      throws Exception
  {

    String repoId = this.getTestId() + "-testM1ReleaseAllowRedeploy";

    this.createM1Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.RELEASE);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-1.0.0.jar");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-1.0.0.jar");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-1.0.0.jar");

    // now test checksums
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-1.0.0.jar.md5");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-1.0.0.jar.md5");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-1.0.0.jar.md5");
  }

  @Test
  public void testM1ReleaseNoRedeploy()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM1ReleaseNoRedeploy";

    this.createM1Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.RELEASE);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseNoRedeploy-1.0.0.jar");

    // checksum should work
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseNoRedeploy-1.0.0.jar.md5");

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM1Repo.group/jars/testM1ReleaseNoRedeploy-1.0.0.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM1Repo.group/jars/testM1ReleaseNoRedeploy-1.0.0.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM1Repo.group/jars/testM1ReleaseNoRedeploy-1.0.0.jar.md5");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }
  }

  @Test
  public void testM1ReleaseReadOnly()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM1ReleaseReadOnly";

    this.createM1Repo(repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.RELEASE);

    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM1Repo.group/jars/testM1ReleaseReadOnly-1.0.0.jar");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }

    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM1Repo.group/jars/testM1ReleaseReadOnly-1.0.0.jar.md5");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

  }

  @Test
  public void testM1SnapshotAllowRedeploy()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM1SnapshotAllowRedeploy";

    this.createM1Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.SNAPSHOT);

    // ONLY SUPPORT -SNAPSHOT
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifact,
    // "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-20090101.jar" );
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifact,
    // "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-20090102.jar" );
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifact,
    // "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-20090103.jar" );
    //
    // // now for the MD5
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifactMD5,
    // "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-20090103.jar.md5" );
    //
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifactMD5,
    // "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-20090103.jar.md5" );

    // now for just the -SNAPSHOT

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-SNAPSHOT.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-SNAPSHOT.jar");

    // MD5
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-SNAPSHOT.jar.md5");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseAllowRedeploy-SNAPSHOT.jar.md5");

  }

  @Test
  public void testM1SnapshotNoRedeploy()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM1SnapshotNoRedeploy";

    this.createM1Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.SNAPSHOT);

    // ONLY SUPPORT -SNAPSHOT for M1
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifact,
    // "testM1Repo.group/jars/testM1ReleaseNoRedeploy-20090101.jar" );
    //
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifact,
    // "testM1Repo.group/jars/testM1ReleaseNoRedeploy-20090102.jar" );
    //
    // DeployUtils.deployWithWagon(
    // this,
    // "http",
    // this.getRepositoryUrl( repoId ),
    // artifact,
    // "testM1Repo.group/jars/testM1ReleaseNoRedeploy-20090102.jar" );

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseNoRedeploy-SNAPSHOT.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM1Repo.group/jars/testM1ReleaseNoRedeploy-SNAPSHOT.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseNoRedeploy-SNAPSHOT.jar.md5");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM1Repo.group/jars/testM1ReleaseNoRedeploy-SNAPSHOT.jar.md5");
  }

  @Test
  public void testM1SnapshotReadOnly()
      throws Exception
  {

    String repoId = this.getTestId() + "-testM1SnapshotReadOnly";

    this.createM1Repo(repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.SNAPSHOT);

    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM1Repo.group/jars/testM1ReleaseReadOnly-20090102.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }
    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM1Repo.group/jars/testM1ReleaseReadOnly-20090102.jar.md5");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }
    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM1Repo.group/jars/testM1ReleaseReadOnly-SNAPSHOT.jar.md5");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }
    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM1Repo.group/jars/testM1ReleaseReadOnly-SNAPSHOT.jar");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }
  }

  private void createM1Repo(String repoId, RepositoryWritePolicy writePolicy, RepositoryPolicy releasePolicy)
      throws IOException
  {
    RepositoryResource repo = new RepositoryResource();

    repo.setId(repoId);
    repo.setBrowseable(true);
    repo.setExposed(true);
    repo.setRepoType("hosted");
    repo.setName(repoId);
    repo.setRepoPolicy(releasePolicy.name());
    repo.setWritePolicy(writePolicy.name());
    repo.setProvider("maven1");
    repo.setFormat("maven1");

    this.repoUtil.createRepository(repo);
  }

}
