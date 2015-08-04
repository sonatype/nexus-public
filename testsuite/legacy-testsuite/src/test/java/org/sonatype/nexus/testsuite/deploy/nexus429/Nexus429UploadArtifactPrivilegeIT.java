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
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.ITGroups.SECURITY;
import org.sonatype.nexus.integrationtests.TestContainer;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

/**
 * Test the privilege for manual artifact upload.
 */
@Category(SECURITY.class)
public class Nexus429UploadArtifactPrivilegeIT
    extends AbstractPrivilegeTest
{
  private static final String TEST_RELEASE_REPO = "nexus-test-harness-release-repo";

  public Nexus429UploadArtifactPrivilegeIT() {
    super(TEST_RELEASE_REPO);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  public void deployPrivWithPom()
      throws Exception
  {
    // GAV
    Gav gav =
        new Gav(this.getTestId(), "uploadWithGav", "1.0.0", null, "xml", 0, new Date().getTime(), "",
            false, null, false, null);

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    File pomFile = this.getTestFile("pom.xml");

    // deploy
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    // with pom should fail
    int status =
        getDeployUtils().deployUsingPomWithRest(uploadURL, TEST_RELEASE_REPO, fileToDeploy, pomFile, null, null);
    Assert.assertEquals("Status should have been 403", status, 403);

    // give deployment role
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    this.giveUserPrivilege("test-user", "65");
    this.giveUserRole("test-user", "repo-all-full");

    // try again
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    status =
        getDeployUtils().deployUsingPomWithRest(uploadURL, TEST_RELEASE_REPO, fileToDeploy, pomFile, null, null);
    Assert.assertEquals("Status should have been 201", status, 201);
  }

  public void deployPrivWithGav()
      throws Exception
  {
    // GAV
    Gav gav =
        new Gav(this.getTestId(), "uploadWithGav", "1.0.0", null, "xml", 0, new Date().getTime(), "",
            false, null, false, null);

    // file to deploy
    File fileToDeploy = this.getTestFile(gav.getArtifactId() + "." + gav.getExtension());

    // deploy
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // url to upload to
    String uploadURL = this.getBaseNexusUrl() + "service/local/artifact/maven/content";

    // with gav should fail
    int status = getDeployUtils().deployUsingGavWithRest(uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy);
    Assert.assertEquals("Status should have been 403", status, 403);

    // give deployment role
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    this.giveUserPrivilege("test-user", "65");
    this.giveUserRole("test-user", "repo-all-full");

    // try again
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    status = getDeployUtils().deployUsingGavWithRest(uploadURL, TEST_RELEASE_REPO, gav, fileToDeploy);
    Assert.assertEquals("Status should have been 201", status, 201);

  }

}
