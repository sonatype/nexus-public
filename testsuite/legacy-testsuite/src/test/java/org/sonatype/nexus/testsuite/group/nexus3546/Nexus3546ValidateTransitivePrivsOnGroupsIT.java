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
package org.sonatype.nexus.testsuite.group.nexus3546;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Status;

public class Nexus3546ValidateTransitivePrivsOnGroupsIT
    extends AbstractPrivilegeTest
{
  private static final String GROUP_1_ID = "nexus3546group1";

  private static final String GROUP_2_ID = "nexus3546group2";

  @Test
  public void validateGroupsInGroups()
      throws Exception
  {
    Gav gav = GavUtil.newGav("nexus3546", "artifact", "1.0.0");

    prepare(gav);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // should be able to download from repo
    File downloaded = downloadArtifactFromRepository(REPO_TEST_HARNESS_RELEASE_REPO, gav,
        "./target/downloaded-jars/from-repo");
    Assert.assertTrue(downloaded.exists());

    // and parent group of repo
    downloaded = downloadArtifactFromGroup(GROUP_1_ID, gav, "./target/downloaded-jars/from-group-1");
    Assert.assertTrue(downloaded.exists());

    // and parent group of group
    downloaded = downloadArtifactFromGroup(GROUP_2_ID, gav, "./target/downloaded-jars/from-group-2");

    Assert.assertTrue(downloaded.exists());
  }

  private void prepare(Gav gav)
      throws Exception
  {
    //use admin for this part
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    File artifact = getTestFile("artifact.jar");

    int code = getDeployUtils().deployUsingGavWithRest(REPO_TEST_HARNESS_RELEASE_REPO, gav, artifact);
    Assert.assertTrue("Unable to deploy artifact " + code, Status.isSuccess(code));

    //create group 1
    RepositoryGroupResource group = new RepositoryGroupResource();
    group.setId(GROUP_1_ID);
    group.setFormat("maven2");
    group.setProvider("maven2");
    group.setName(GROUP_1_ID);
    group.setExposed(true);
    RepositoryGroupMemberRepository repo = new RepositoryGroupMemberRepository();
    repo.setId(REPO_TEST_HARNESS_RELEASE_REPO);
    group.addRepository(repo);
    groupUtil.createGroup(group);

    TaskScheduleUtil.waitForAllTasksToStop();

    //create group 2
    group = new RepositoryGroupResource();
    group.setId(GROUP_2_ID);
    group.setFormat("maven2");
    group.setProvider("maven2");
    group.setName(GROUP_2_ID);
    group.setExposed(true);
    repo = new RepositoryGroupMemberRepository();
    repo.setId(GROUP_1_ID);
    group.addRepository(repo);
    groupUtil.createGroup(group);

    TaskScheduleUtil.waitForAllTasksToStop();

    resetTestUserPrivs();
    addPriv(TEST_USER_NAME, "nexus3546priv", TargetPrivilegeDescriptor.TYPE, "1", null,
        GROUP_2_ID, "read");

    TaskScheduleUtil.waitForAllTasksToStop();
  }
}
