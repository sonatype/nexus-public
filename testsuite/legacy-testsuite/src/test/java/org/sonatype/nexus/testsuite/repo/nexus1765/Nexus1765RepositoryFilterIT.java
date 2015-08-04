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
package org.sonatype.nexus.testsuite.repo.nexus1765;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;

public class Nexus1765RepositoryFilterIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void getRepositoriesListNoAccessTest()
      throws Exception
  {
    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    List<RepositoryListResource> repoList = repoUtil.getList();
    Assert.assertEquals(0, repoList.size());
  }

  @Test
  public void getRepositoriesListWithAccessTest()
      throws Exception
  {
    // give the user view access to
    String repoId = this.getTestRepositoryId();
    String viewPriv = "repository-" + repoId;
    this.addPrivilege(TEST_USER_NAME, viewPriv);

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    List<RepositoryListResource> repoList = repoUtil.getList();
    Assert.assertEquals(1, repoList.size());
    Assert.assertEquals(repoList.get(0).getId(), repoId);
  }

  @Test
  public void getRepositoryNoAccessTest()
      throws Exception
  {
    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    RequestFacade.doGet("service/local/repositories/" + getTestRepositoryId(), respondsWithStatusCode(403));
  }

  @Test
  public void updateRepositoryNoAccessTest()
      throws Exception
  {

    RepositoryBaseResource repo = repoUtil.getRepository(getTestRepositoryId());
    repo.setName("new name");

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Response response = this.repoUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Status: " + response.getStatus(), response.getStatus().getCode(), 403);
  }

  @Test
  public void createRepositoryNoAccessTest()
      throws Exception
  {

    String repoId = "test-repo";

    RepositoryBaseResource repo = this.repoUtil.getRepository(this.getTestRepositoryId());
    repo.setId(repoId);

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Response response = this.repoUtil.sendMessage(Method.POST, repo, repoId);
    Assert.assertEquals("Status: " + response.getStatus(), response.getStatus().getCode(), 403);
  }

  @Test
  public void deleteRepositoryNoAccessTest()
      throws Exception
  {

    String repoId = this.getTestRepositoryId();

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Response response = RequestFacade.sendMessage("service/local/repositories/" + repoId, Method.DELETE);
    Assert.assertEquals("Status: " + response.getStatus(), response.getStatus().getCode(), 403);
  }

  @Test
  public void getGroupListNoAccessTest()
      throws Exception
  {
    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    List<RepositoryGroupListResource> groupList = groupUtil.getList();
    Assert.assertEquals(0, groupList.size());
  }

  @Test
  public void getGroupListWithAccessTest()
      throws Exception
  {
    // give the user view access to
    String repoId = "public";
    String viewPriv = "repository-" + repoId;
    this.addPrivilege(TEST_USER_NAME, viewPriv);

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    List<RepositoryGroupListResource> groupList = groupUtil.getList();
    Assert.assertEquals(1, groupList.size());
    Assert.assertEquals(groupList.get(0).getId(), repoId);
  }

  @Test
  public void getGroupNoAccessTest()
      throws Exception
  {
    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    String repoId = "public";
    RequestFacade.doGet(GroupMessageUtil.SERVICE_PART + "/" + repoId, respondsWithStatusCode(403));
  }

  @Test
  public void updateGroupNoAccessTest()
      throws Exception
  {

    String repoId = "public";

    RepositoryGroupResource repo = this.groupUtil.getGroup(repoId);
    repo.setName("new name");

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Response response = this.groupUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Status: " + response.getStatus(), response.getStatus().getCode(), 403);
  }

  @Test
  public void createGroupNoAccessTest()
      throws Exception
  {

    String repoId = "test-group";

    RepositoryGroupResource repo = this.groupUtil.getGroup("public");
    repo.setId(repoId);

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Response response = this.groupUtil.sendMessage(Method.POST, repo, repoId);
    Assert.assertEquals("Status: " + response.getStatus(), response.getStatus().getCode(), 403);
  }

  @Test
  public void deleteGroupNoAccessTest()
      throws Exception
  {

    String repoId = "public";

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Response response = RequestFacade.sendMessage(GroupMessageUtil.SERVICE_PART + "/" + repoId, Method.DELETE);
    Assert.assertEquals("Status: " + response.getStatus(), response.getStatus().getCode(), 403);
  }

}
