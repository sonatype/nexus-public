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
package org.sonatype.nexus.testsuite.repo.nexus537;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.test.utils.MavenDeployer;
import org.sonatype.nexus.test.utils.TargetMessageUtil;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeMethodPropertyDescriptor;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.apache.maven.index.artifact.Gav;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;
import static org.sonatype.nexus.test.utils.StatusMatchers.isNotFound;

/**
 * Creates a few repo targets and make sure the privileges work correctly.
 */
public class Nexus537RepoTargetsIT
    extends AbstractPrivilegeTest
{

  private String fooPrivCreateId;

  private String fooPrivReadId;

  private String fooPrivUpdateId;

  private String fooPrivDeleteId;

  private String barPrivCreateId;

  private String barPrivReadId;

  private String barPrivUpdateId;

  private String barPrivDeleteId;

  private String groupFooPrivCreateId;

  private String groupFooPrivReadId;

  private String groupFooPrivUpdateId;

  private String groupFooPrivDeleteId;

  private Gav repo1BarArtifact;

  private Gav repo1FooArtifact;

  private Gav repo2BarArtifact;

  private Gav repo2FooArtifact;

  private Gav repo1BarArtifactDelete;

  private Gav repo1FooArtifactDelete;

  private Gav repo2BarArtifactDelete;

  private Gav repo2FooArtifactDelete;

  private static final String REPO1_ID = "repo1";

  private static final String REPO2_ID = "repo2";

  private static final String GROUP_ID = "test-group";

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    repo1BarArtifact =
        new Gav(this.getTestId(), "repo1-bar-artifact", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo1-bar-artifact", false, null, false, null);
    repo1FooArtifact =
        new Gav(this.getTestId(), "repo1-foo-artifact", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo1-foo-artifact", false, null, false, null);
    repo2BarArtifact =
        new Gav(this.getTestId(), "repo2-bar-artifact", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo2-bar-artifact", false, null, false, null);
    repo2FooArtifact =
        new Gav(this.getTestId(), "repo2-foo-artifact", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo2-foo-artifact", false, null, false, null);

    repo1BarArtifactDelete =
        new Gav(this.getTestId(), "repo1-bar-artifact-delete", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo1-bar-artifact-delete", false, null, false, null);
    repo1FooArtifactDelete =
        new Gav(this.getTestId(), "repo1-foo-artifact-delete", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo1-foo-artifact-delete", false, null, false, null);
    repo2BarArtifactDelete =
        new Gav(this.getTestId(), "repo2-bar-artifact-delete", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo2-bar-artifact-delete", false, null, false, null);
    repo2FooArtifactDelete =
        new Gav(this.getTestId(), "repo2-foo-artifact-delete", "1.0.0", null, "jar", 0, new Date().getTime(),
            "repo2-foo-artifact-delete", false, null, false, null);
  }

  @Override
  public void runOnce()
      throws Exception
  {
    super.runOnce();
    TargetMessageUtil.removeAllTarget();
  }

  @Override
  @Before
  public void resetTestUserPrivs()
      throws Exception
  {
    this.overwriteUserRole(TEST_USER_NAME, "doReadTest-noAccess", "17");
    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");
    // "6", "14","19","44","54","55","57","58","64","70"
    this.printUserPrivs(TEST_USER_NAME);
  }

  @Override
  protected void overwriteUserRole(String userId, String newRoleName, String... permissions)
      throws IOException
  {
    super.overwriteUserRole(userId, newRoleName, permissions);
    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");
  }

  @Test
  public void doReadTest()
      throws Exception
  {

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // test-user should not be able to download anything.
    this.download(REPO1_ID, repo1BarArtifact, false);
    this.download(REPO1_ID, repo1FooArtifact, false);
    this.download(REPO2_ID, repo2BarArtifact, false);
    this.download(REPO2_ID, repo2FooArtifact, false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "fooPrivReadId", this.fooPrivReadId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.download(REPO1_ID, repo1BarArtifact, false);
    this.download(REPO1_ID, repo1FooArtifact, true);
    this.download(REPO2_ID, repo2BarArtifact, false);
    this.download(REPO2_ID, repo2FooArtifact, false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "barPrivReadId", this.barPrivReadId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.download(REPO1_ID, repo1BarArtifact, true);
    this.download(REPO1_ID, repo1FooArtifact, false);
    this.download(REPO2_ID, repo2BarArtifact, false);
    this.download(REPO2_ID, repo2FooArtifact, false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "groupPrivReadId", this.groupFooPrivReadId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // try the group
    this.groupDownload(repo1BarArtifact, false);
    this.groupDownload(repo1FooArtifact, true);
    this.groupDownload(repo2BarArtifact, false);
    this.groupDownload(repo2FooArtifact, true);

    this.download(REPO1_ID, repo1BarArtifact, false);
    this.download(REPO1_ID, repo1FooArtifact, true); // has direct access
    this.download(REPO2_ID, repo2BarArtifact, false);
    this.download(REPO2_ID, repo2FooArtifact, true); // has access to group

  }

  @Test
  public void doCreateRepoTargetTest()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // test-user should not be able to upload anything.
    this.deploy(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), false);
    this.deploy(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), false);
    this.deploy(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.deploy(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "fooPrivUpdateId", this.fooPrivUpdateId, this.fooPrivCreateId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.deploy(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), false);
    this.deploy(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), true);
    this.deploy(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.deploy(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "barPrivUpdateId", this.barPrivUpdateId, this.barPrivCreateId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.deploy(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), true);
    this.deploy(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), false);
    this.deploy(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.deploy(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "groupFooPrivUpdateId", this.groupFooPrivUpdateId,
        this.groupFooPrivCreateId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.deploy(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), false);
    this.deploy(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), true); // has direct
    // access
    this.deploy(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.deploy(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), true); // has access
    // from group

  }

  @Test
  public void artifactUplaodTest()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // test-user should not be able to upload anything.
    this.upload(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), false);
    this.upload(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), false);
    this.upload(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.upload(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "fooPrivUpdateId", this.fooPrivUpdateId, this.fooPrivCreateId, "65"); // 65
    // is
    // upload
    // priv

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.upload(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), false);
    this.upload(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), true);
    this.upload(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.upload(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "barPrivUpdateId", this.barPrivUpdateId, this.barPrivCreateId, "65");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.upload(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), true);
    this.upload(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), false);
    this.upload(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.upload(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "groupFooPrivUpdateId", this.groupFooPrivUpdateId, "65");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.upload(repo1BarArtifact, REPO1_ID, this.getTestFile("repo1-bar-artifact.jar"), false);
    this.upload(repo1FooArtifact, REPO1_ID, this.getTestFile("repo1-foo-artifact.jar"), true); // has direct
    // access
    this.upload(repo2BarArtifact, REPO2_ID, this.getTestFile("repo2-bar-artifact.jar"), false);
    this.upload(repo2FooArtifact, REPO2_ID, this.getTestFile("repo2-foo-artifact.jar"), true); // has access
    // from group
  }

  @Test
  public void doDeleteTest()
      throws Exception
  {
    // deploy the artifacts first, we need to use different once because i have no idea how to order the tests with
    // JUnit
    getDeployUtils().deployUsingGavWithRest(REPO1_ID, repo1BarArtifactDelete,
        this.getTestFile("repo1-bar-artifact.jar"));
    getDeployUtils().deployUsingGavWithRest(REPO1_ID, repo1FooArtifactDelete,
        this.getTestFile("repo1-foo-artifact.jar"));
    getDeployUtils().deployUsingGavWithRest(REPO2_ID, repo2BarArtifactDelete,
        this.getTestFile("repo2-bar-artifact.jar"));
    getDeployUtils().deployUsingGavWithRest(REPO2_ID, repo2FooArtifactDelete,
        this.getTestFile("repo2-foo-artifact.jar"));

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.delete(repo1BarArtifactDelete, REPO1_ID, false);
    this.delete(repo1FooArtifactDelete, REPO1_ID, false);
    this.delete(repo2BarArtifactDelete, REPO2_ID, false);
    this.delete(repo2FooArtifactDelete, REPO2_ID, false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "fooPrivDeleteId", this.fooPrivDeleteId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.delete(repo1BarArtifactDelete, REPO1_ID, false);
    this.delete(repo1FooArtifactDelete, REPO1_ID, true);
    this.delete(repo2BarArtifactDelete, REPO2_ID, false);
    this.delete(repo2FooArtifactDelete, REPO2_ID, false);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "barPrivDeleteId", this.barPrivDeleteId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.delete(repo1BarArtifactDelete, REPO1_ID, true);
    this.delete(repo1FooArtifactDelete, REPO1_ID, false);
    this.delete(repo2BarArtifactDelete, REPO2_ID, false);
    this.delete(repo2FooArtifactDelete, REPO2_ID, false);

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    getDeployUtils().deployUsingGavWithRest(REPO1_ID, repo1BarArtifactDelete,
        this.getTestFile("repo1-bar-artifact.jar"));
    getDeployUtils().deployUsingGavWithRest(REPO1_ID, repo1FooArtifactDelete,
        this.getTestFile("repo1-foo-artifact.jar"));
    getDeployUtils().deployUsingGavWithRest(REPO2_ID, repo2BarArtifactDelete,
        this.getTestFile("repo2-bar-artifact.jar"));
    getDeployUtils().deployUsingGavWithRest(REPO2_ID, repo2FooArtifactDelete,
        this.getTestFile("repo2-foo-artifact.jar"));

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // now give
    this.overwriteUserRole(TEST_USER_NAME, "groupFooPrivDeleteId", this.groupFooPrivDeleteId);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    this.delete(repo1BarArtifactDelete, REPO1_ID, false);
    this.delete(repo1FooArtifactDelete, REPO1_ID, true);
    this.delete(repo2BarArtifactDelete, REPO2_ID, false);
    this.delete(repo2FooArtifactDelete, REPO2_ID, true);

  }

  private File download(String repoId, Gav gav, boolean shouldDownload) {
    File result = null;
    try {
      result = this.downloadArtifactFromRepository(repoId, gav, "target/nexus537jars/");
      Assert.assertTrue("Artifact download should have thrown exception", shouldDownload);
    }
    catch (IOException e) {
      Assert.assertFalse("Artifact should have downloaded: \n" + e.getMessage(), shouldDownload);
    }

    return result;
  }

  private void deploy(Gav gav, String repoId, File fileToDeploy, boolean shouldUpload)
      throws InterruptedException, Exception
  {
    try {

      // DeployUtils.forkDeployWithWagon( this.getContainer(), "http", this.getRepositoryUrl( repoId ),
      // fileToDeploy, this.getRelitiveArtifactPath( gav ) );
      Verifier verifier =
          MavenDeployer.deployAndGetVerifier(gav, this.getRepositoryUrl(repoId), fileToDeploy,
              this.getOverridableFile("settings.xml"));

      Assert.assertTrue("Artifact upload should have thrown exception", shouldUpload);
    }
    catch (VerificationException e) {
      Assert.assertFalse("Artifact should have uploaded: \n" + e.getMessage(), shouldUpload);
    }

    // if we made it this far we should also test download, because upload implies download
    this.download(repoId, gav, shouldUpload);

  }

  private void upload(Gav gav, String repoId, File fileToDeploy, boolean shouldUpload)
      throws InterruptedException, IOException
  {
    int status = getDeployUtils().deployUsingGavWithRest(repoId, gav, fileToDeploy);

    Assert.assertTrue("Artifact upload returned: " + status
        + (shouldUpload ? " expected sucess" : " expected failure"), (201 == status && shouldUpload) || !shouldUpload);
    // if we made it this far we should also test download, because upload implies download
    this.download(repoId, gav, shouldUpload);
  }

  private void delete(Gav gav, String repoId, boolean shouldDelete)
      throws IOException
  {
    String url = REPOSITORY_RELATIVE_URL + repoId + "/" + this.getRelitiveArtifactPath(gav);

    if (!shouldDelete) {
      RequestFacade.doDelete(url, respondsWithStatusCode(403));
    }
    else {
      RequestFacade.doGet(url, respondsWithStatusCode(200));
      RequestFacade.doDelete(url, respondsWithStatusCode(204));
      RequestFacade.doGetForStatus(url, isNotFound());
    }
  }

  private File groupDownload(Gav gav, boolean shouldDownload) {
    File result = null;
    try {
      result = this.downloadArtifactFromGroup(GROUP_ID, gav, "target/nexus537jars/");
      Assert.assertTrue("Artifact download should have thrown exception", shouldDownload);
    }
    catch (IOException e) {
      Assert.assertFalse("Artifact should have downloaded: \n" + e.getMessage(), shouldDownload);
    }

    return result;
  }

  @Override
  @Before
  public void oncePerClassSetUp()
      throws Exception
  {
    super.oncePerClassSetUp();

    // create my repo targets
    RepositoryTargetResource fooTarget = new RepositoryTargetResource();
    fooTarget.setContentClass("maven2");
    fooTarget.setName("Foo");
    fooTarget.addPattern(".*nexu.537/repo.-foo.*");
    fooTarget = this.targetUtil.createTarget(fooTarget);

    RepositoryTargetResource barTarget = new RepositoryTargetResource();
    barTarget.setContentClass("maven2");
    barTarget.setName("Bar");
    barTarget.addPattern(".*nexu.537/repo.-bar.*");
    barTarget = this.targetUtil.createTarget(barTarget);

    // now create a couple privs
    PrivilegeResource fooPriv = new PrivilegeResource();
    fooPriv.addMethod("create");
    fooPriv.addMethod("read");
    fooPriv.addMethod("update");
    fooPriv.addMethod("delete");
    fooPriv.setName("FooPriv");
    fooPriv.setType(TargetPrivilegeDescriptor.TYPE);
    fooPriv.setRepositoryTargetId(fooTarget.getId());
    fooPriv.setRepositoryId("repo1");
    // get the Resource object
    List<PrivilegeStatusResource> fooPrivs = this.privUtil.createPrivileges(fooPriv);

    for (Iterator<PrivilegeStatusResource> iter = fooPrivs.iterator(); iter.hasNext(); ) {
      PrivilegeStatusResource privilegeBaseStatusResource = iter.next();

      if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("create,read")) {
        fooPrivCreateId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("read")) {
        fooPrivReadId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("update,read")) {
        fooPrivUpdateId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("delete,read")) {
        fooPrivDeleteId = privilegeBaseStatusResource.getId();
      }
      else {
        Assert.fail("Unknown Privilege found, id: "
            + privilegeBaseStatusResource.getId()
            + " method: "
            + getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
            ApplicationPrivilegeMethodPropertyDescriptor.ID));
      }
    }

    // now create a couple privs
    PrivilegeResource barPriv = new PrivilegeResource();
    barPriv.addMethod("create");
    barPriv.addMethod("read");
    barPriv.addMethod("update");
    barPriv.addMethod("delete");
    barPriv.setName("BarPriv");
    barPriv.setType(TargetPrivilegeDescriptor.TYPE);
    barPriv.setRepositoryTargetId(barTarget.getId());
    barPriv.setRepositoryId("repo1");

    // get the Resource object
    List<PrivilegeStatusResource> barPrivs = this.privUtil.createPrivileges(barPriv);

    for (Iterator<PrivilegeStatusResource> iter = barPrivs.iterator(); iter.hasNext(); ) {
      PrivilegeStatusResource privilegeBaseStatusResource = iter.next();

      if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("create,read")) {
        barPrivCreateId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("read")) {
        barPrivReadId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("update,read")) {
        barPrivUpdateId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("delete,read")) {
        barPrivDeleteId = privilegeBaseStatusResource.getId();
      }
      else {
        Assert.fail("Unknown Privilege found, id: "
            + privilegeBaseStatusResource.getId()
            + " method: "
            + getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
            ApplicationPrivilegeMethodPropertyDescriptor.ID));
      }
    }

    // now create a couple privs
    PrivilegeResource groupPriv = new PrivilegeResource();
    groupPriv.addMethod("create");
    groupPriv.addMethod("read");
    groupPriv.addMethod("update");
    groupPriv.addMethod("delete");
    groupPriv.setName("GroupPriv");
    groupPriv.setType(TargetPrivilegeDescriptor.TYPE);
    groupPriv.setRepositoryTargetId(fooTarget.getId());
    groupPriv.setRepositoryGroupId(GROUP_ID);
    // groupPriv.setRepositoryId( repositoryId )
    // groupPriv.setName( name )
    // groupPriv.setDescription( description )

    // get the Resource object
    List<PrivilegeStatusResource> groupPrivs = this.privUtil.createPrivileges(groupPriv);

    for (Iterator<PrivilegeStatusResource> iter = groupPrivs.iterator(); iter.hasNext(); ) {
      PrivilegeStatusResource privilegeBaseStatusResource = iter.next();

      if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("create,read")) {
        groupFooPrivCreateId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("read")) {
        groupFooPrivReadId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("update,read")) {
        groupFooPrivUpdateId = privilegeBaseStatusResource.getId();
      }
      else if (getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeMethodPropertyDescriptor.ID).equals("delete,read")) {
        groupFooPrivDeleteId = privilegeBaseStatusResource.getId();
      }
      else {
        Assert.fail("Unknown Privilege found, id: "
            + privilegeBaseStatusResource.getId()
            + " method: "
            + getSecurityConfigUtil().getPrivilegeProperty(privilegeBaseStatusResource,
            ApplicationPrivilegeMethodPropertyDescriptor.ID));
      }
    }

  }
}
