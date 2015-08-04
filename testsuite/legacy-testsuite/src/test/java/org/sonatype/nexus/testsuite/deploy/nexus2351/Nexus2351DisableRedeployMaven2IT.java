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

public class Nexus2351DisableRedeployMaven2IT
    extends AbstractNexusIntegrationTest
{

  private RepositoryMessageUtil repoUtil = null;

  private File artifact;

  private File artifactMD5;

  @Before
  public void setup()
      throws Exception
  {
    artifact = this.getTestFile("artifact.jar");
    artifactMD5 = this.getTestFile("artifact.jar.md5");
  }

  public Nexus2351DisableRedeployMaven2IT() {

  }

  @Before
  public void init()
      throws ComponentLookupException
  {
    this.repoUtil = new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void testM2ReleaseAllowRedeploy()
      throws Exception
  {

    String repoId = this.getTestId() + "-testM2ReleaseAllowRedeploy";

    this.createM2Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.RELEASE);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar");

    // now test checksums
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseAllowRedeploy-1.0.0.jar.md5");
  }

  @Test
  public void testM2ReleaseNoRedeploy()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM2ReleaseNoRedeploy";

    this.createM2Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.RELEASE);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar");

    // checksum should work
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar.md5");

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM2Repo/group/testM2ReleaseNoRedeploy/1.0.0/testM2ReleaseNoRedeploy-1.0.0.jar.md5");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }
  }

  @Test
  public void testM2ReleaseNoRedeployMultipleVersions()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM2ReleaseNoRedeployMultipleVersions";

    this.createM2Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.RELEASE);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.0/testM2ReleaseNoRedeployMultipleVersions-1.0.0.jar");

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.0/testM2ReleaseNoRedeployMultipleVersions-1.0.0.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.1/testM2ReleaseNoRedeployMultipleVersions-1.0.1.jar");

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM2Repo/group/testM2ReleaseNoRedeployMultipleVersions/1.0.1/testM2ReleaseNoRedeployMultipleVersions-1.0.1.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

  }

  @Test
  public void testM2ReleaseReadOnly()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM2ReleaseReadOnly";

    this.createM2Repo(repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.RELEASE);

    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM2Repo/group/testM2ReleaseReadOnly/1.0.0/testM2ReleaseReadOnly-1.0.0.jar");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }

    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM2Repo/group/testM2ReleaseAllowRedeploy/1.0.0/testM2ReleaseReadOnly-1.0.0.jar.md5");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }

  }

  @Test
  public void testM2SnapshotAllowRedeploy()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM2SnapshotAllowRedeploy";

    this.createM2Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE, RepositoryPolicy.SNAPSHOT);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-216.jar");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-217.jar");
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-218.jar");

    // now for the MD5
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-217.jar.md5");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-20090729.054915-218.jar.md5");

    // now for just the -SNAPSHOT

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-1.0.0-SNAPSHOT.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-1.0.0-SNAPSHOT.jar");

    // MD5
    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-1.0.0-SNAPSHOT.jar.md5");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2SnapshotAllowRedeploy/1.0.0-SNAPSHOT/testM2SnapshotAllowRedeploy-1.0.0-SNAPSHOT.jar.md5");

  }

  @Test
  public void testM2SnapshotNoRedeploy()
      throws Exception
  {
    String repoId = this.getTestId() + "-testM2SnapshotNoRedeploy";

    this.createM2Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.SNAPSHOT);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-20090729.054915-218.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-20090729.054915-219.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-20090729.054915-220.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-1.0.0-SNAPSHOT.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-1.0.0-SNAPSHOT.jar");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-1.0.0-SNAPSHOT.jar.md5");

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "testM2Repo/group/testM2SnapshotNoRedeploy/1.0.0-SNAPSHOT/testM2SnapshotNoRedeploy-1.0.0-SNAPSHOT.jar.md5");
  }

  @Test
  public void testM2SnapshotReadOnly()
      throws Exception
  {

    String repoId = this.getTestId() + "-testM2SnapshotReadOnly";

    this.createM2Repo(repoId, RepositoryWritePolicy.READ_ONLY, RepositoryPolicy.SNAPSHOT);

    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-20090729.054915-218.jar");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }
    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-20090729.054915-218.jar.md5");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }
    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-1.0.0-SNAPSHOT.jar.md5");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }
    try {

      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "testM2Repo/group/testM2SnapshotReadOnly/1.0.0-SNAPSHOT/testM2SnapshotReadOnly-1.0.0-SNAPSHOT.jar");
      Assert.fail("expected TransferFailedException");

    }
    catch (TransferFailedException e) {
      // expected
    }
  }

  /**
   * NEXUS-7808
   * Verify that redeploy is not allowed for artifacts that contains SNAPSHOT in version but are not actually snapshots
   * as defined by M2 version policy.
   */
  @Test
  public void snapshotLikeRedeployM2()
      throws Exception
  {
    String repoId = this.getTestId() + "-snapshotLikeRedeployM2";
    createM2Repo(repoId, RepositoryWritePolicy.ALLOW_WRITE_ONCE, RepositoryPolicy.RELEASE);

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
        "group/snapshotLikeRedeployM2/1.0.0-SNAPSHOT-FOO-999/snapshotLikeRedeployM2-1.0.0-SNAPSHOT-FOO-999.jar");

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifact,
          "group/snapshotLikeRedeployM2/1.0.0-SNAPSHOT-FOO-999/snapshotLikeRedeployM2-1.0.0-SNAPSHOT-FOO-999.jar");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }

    getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
        "group/snapshotLikeRedeployM2/1.0.0-SNAPSHOT-FOO-999/snapshotLikeRedeployM2-1.0.0-SNAPSHOT-FOO-999.jar.md5");

    try {
      getDeployUtils().deployWithWagon("http", this.getRepositoryUrl(repoId), artifactMD5,
          "group/snapshotLikeRedeployM2/1.0.0-SNAPSHOT-FOO-999/snapshotLikeRedeployM2-1.0.0-SNAPSHOT-FOO-999.jar.md5");
      Assert.fail("expected TransferFailedException");
    }
    catch (TransferFailedException e) {
      // expected
    }
  }

  private void createM2Repo(String repoId, RepositoryWritePolicy writePolicy, RepositoryPolicy releasePolicy)
      throws Exception
  {
    RepositoryResource repo = new RepositoryResource();

    repo.setId(repoId);
    repo.setBrowseable(true);
    repo.setExposed(true);
    repo.setRepoType("hosted");
    repo.setName(repoId);
    repo.setRepoPolicy(releasePolicy.name());
    repo.setWritePolicy(writePolicy.name());
    repo.setProvider("maven2");
    repo.setFormat("maven2");
    repo.setIndexable(false);

    this.repoUtil.createRepository(repo);
  }

}
