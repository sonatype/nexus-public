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
package org.sonatype.nexus.testsuite.repo.nexus133;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test the privileges for CRUD operations.
 */
public class Nexus133TargetPermissionIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testCreatePermission()
      throws IOException
  {
    RepositoryTargetResource target = new RepositoryTargetResource();
    target.setContentClass("maven2");
    target.setName("testCreatePermission");
    target.addPattern(".*testCreatePermission.*");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    Response response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give create
    this.giveUserPrivilege("test-user", "45");

    // now.... it should work...
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    target = this.targetUtil.getResourceFromResponse(response);

    // read should succeed (inherited)
    response = this.targetUtil.sendMessage(Method.GET, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.targetUtil.sendMessage(Method.PUT, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.targetUtil.sendMessage(Method.DELETE, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testUpdatePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    RepositoryTargetResource target = new RepositoryTargetResource();
    target.setContentClass("maven2");
    target.setName("testUpdatePermission");
    target.addPattern(".*testUpdatePermission.*");

    Response response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    target = this.targetUtil.getResourceFromResponse(response);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update user
    target.setName("tesUpdatePermission2");
    response = this.targetUtil.sendMessage(Method.PUT, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give create
    this.giveUserPrivilege("test-user", "47");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...
    response = this.targetUtil.sendMessage(Method.PUT, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // read should succeed (inherited)
    response = this.targetUtil.sendMessage(Method.GET, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.targetUtil.sendMessage(Method.DELETE, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testReadPermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    RepositoryTargetResource target = new RepositoryTargetResource();
    target.setContentClass("maven2");
    target.setName("testReadPermission");
    target.addPattern(".*testReadPermission.*");

    Response response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    target = this.targetUtil.getResourceFromResponse(response);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update user
    target.setName("tesUpdatePermission2");
    response = this.targetUtil.sendMessage(Method.PUT, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give create
    this.giveUserPrivilege("test-user", "46");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // read should fail
    response = this.targetUtil.sendMessage(Method.GET, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.targetUtil.sendMessage(Method.PUT, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // should work now...
    response = this.targetUtil.sendMessage(Method.DELETE, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }


  @Test
  public void testDeletePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    RepositoryTargetResource target = new RepositoryTargetResource();
    target.setContentClass("maven2");
    target.setName("testDeletePermission");
    target.addPattern(".*testDeletePermission.*");

    Response response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    target = this.targetUtil.getResourceFromResponse(response);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update user
    target.setName("tesUpdatePermission2");
    response = this.targetUtil.sendMessage(Method.DELETE, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give create
    this.giveUserPrivilege("test-user", "48");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // read should succeed (inherited)
    response = this.targetUtil.sendMessage(Method.GET, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.targetUtil.sendMessage(Method.POST, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.targetUtil.sendMessage(Method.PUT, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // should work now...
    response = this.targetUtil.sendMessage(Method.DELETE, target);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 204);

  }

}
