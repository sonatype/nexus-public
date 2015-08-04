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
package org.sonatype.nexus.testsuite.repo.nexus531;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.ITGroups.SECURITY;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test Repo CRUD privileges.
 */
public class Nexus531RepositoryCrudPermissionIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  @Category(SECURITY.class)
  public void testCreatePermission()
      throws IOException
  {
    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");

    RepositoryResource repo = new RepositoryResource();
    repo.setId("testCreatePermission");
    repo.setName("testCreatePermission");
    repo.setRepoType("hosted");
    repo.setProvider("maven1");
    // format is neglected by server from now on, provider is the new guy in the town
    repo.setFormat("maven1");
    repo.setRepoPolicy(RepositoryPolicy.SNAPSHOT.name());
    repo.setChecksumPolicy("IGNORE");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    Response response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give create
    this.giveUserPrivilege(TEST_USER_NAME, "5");

    // now.... it should work...
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    repo = (RepositoryResource) this.repoUtil.getRepository(repo.getId());

    // read should succeed (inherited)
    response = this.repoUtil.sendMessage(Method.GET, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.repoUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.repoUtil.sendMessage(Method.DELETE, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  @Category(SECURITY.class)
  public void testUpdatePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");

    RepositoryResource repo = new RepositoryResource();
    repo.setId("testUpdatePermission");
    repo.setName("testUpdatePermission");
    repo.setRepoType("hosted");
    repo.setProvider("maven1");
    // format is neglected by server from now on, provider is the new guy in the town
    repo.setFormat("maven1");
    repo.setRepoPolicy(RepositoryPolicy.SNAPSHOT.name());
    repo.setChecksumPolicy("IGNORE");

    Response response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    repo = (RepositoryResource) this.repoUtil.getRepository(repo.getId());

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update repo
    repo.setName("tesUpdatePermission2");
    response = this.repoUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give update
    this.giveUserPrivilege(TEST_USER_NAME, "7");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...
    response = this.repoUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // read should succeed (inherited)
    response = this.repoUtil.sendMessage(Method.GET, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.repoUtil.sendMessage(Method.DELETE, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  @Category(SECURITY.class)
  public void testReadPermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");

    RepositoryResource repo = new RepositoryResource();
    repo.setId("testReadPermission");
    repo.setName("testReadPermission");
    repo.setRepoType("hosted");
    repo.setProvider("maven1");
    // format is neglected by server from now on, provider is the new guy in the town
    repo.setFormat("maven1");
    repo.setRepoPolicy(RepositoryPolicy.SNAPSHOT.name());
    repo.setChecksumPolicy("IGNORE");

    Response response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    repo = (RepositoryResource) this.repoUtil.getRepository(repo.getId());

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update repo
    repo.setName("tesUpdatePermission2");
    response = this.repoUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give read
    this.giveUserPrivilege(TEST_USER_NAME, "6");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // read should fail
    response = this.repoUtil.sendMessage(Method.GET, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.repoUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // should work now...
    response = this.repoUtil.sendMessage(Method.DELETE, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }


  @Test
  @Category(SECURITY.class)
  public void testDeletePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");

    RepositoryResource repo = new RepositoryResource();
    repo.setId("testDeletePermission");
    repo.setName("testDeletePermission");
    repo.setRepoType("hosted");
    repo.setProvider("maven1");
    // format is neglected by server from now on, provider is the new guy in the town
    repo.setFormat("maven1");
    repo.setRepoPolicy(RepositoryPolicy.SNAPSHOT.name());
    repo.setChecksumPolicy("IGNORE");

    Response response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    repo = (RepositoryResource) this.repoUtil.getRepository(repo.getId());

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update repo
    repo.setName("tesUpdatePermission2");
    response = this.repoUtil.sendMessage(Method.DELETE, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give delete
    this.giveUserPrivilege(TEST_USER_NAME, "8");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // read should succeed (inherited)
    response = this.repoUtil.sendMessage(Method.GET, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.repoUtil.sendMessage(Method.POST, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.repoUtil.sendMessage(Method.PUT, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // should work now...
    response = this.repoUtil.sendMessage(Method.DELETE, repo);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 204);

  }

}
