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
package org.sonatype.nexus.testsuite.security.nexus233;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test the privileges for CRUD operations.
 */
public class Nexus233PrivilegePermissionIT
    extends AbstractPrivilegeTest
{

  @Test
  public void testCreatePermission()
      throws IOException
  {
    PrivilegeResource privilege = new PrivilegeResource();
    privilege.addMethod("read");
    privilege.setName("createReadMethodTest");
    privilege.setType(TargetPrivilegeDescriptor.TYPE);
    privilege.setRepositoryTargetId("testTarget");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    Response response = this.privUtil.sendMessage(Method.POST, privilege);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give create
    this.giveUserPrivilege("test-user", "30");

    // now.... it should work...
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    response = this.privUtil.sendMessage(Method.POST, privilege);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    PrivilegeStatusResource responsePrivilege = this.privUtil.getResourceListFromResponse(response).get(0);

    // read should succeed (inherited by create)
    response = this.privUtil.sendMessage(Method.GET, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.privUtil.sendMessage(Method.PUT, privilege, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.privUtil.sendMessage(Method.DELETE, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testReadPermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    PrivilegeResource privilege = new PrivilegeResource();
    privilege.addMethod("read");
    privilege.setName("createReadMethodTest");
    privilege.setType(TargetPrivilegeDescriptor.TYPE);
    privilege.setRepositoryTargetId("testTarget");

    Response response = this.privUtil.sendMessage(Method.POST, privilege);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    PrivilegeStatusResource responsePrivilege = this.privUtil.getResourceListFromResponse(response).get(0);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");


    response = this.privUtil.sendMessage(Method.GET, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give create
    this.giveUserPrivilege("test-user", "31");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...
    response = this.privUtil.sendMessage(Method.PUT, privilege, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // read should fail
    response = this.privUtil.sendMessage(Method.GET, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.privUtil.sendMessage(Method.POST, privilege);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.privUtil.sendMessage(Method.DELETE, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testDeletePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    PrivilegeResource privilege = new PrivilegeResource();
    privilege.addMethod("read");
    privilege.setName("createReadMethodTest");
    privilege.setType(TargetPrivilegeDescriptor.TYPE);
    privilege.setRepositoryTargetId("testTarget");

    Response response = this.privUtil.sendMessage(Method.POST, privilege);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    PrivilegeStatusResource responsePrivilege = this.privUtil.getResourceListFromResponse(response).get(0);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    response = this.privUtil.sendMessage(Method.DELETE, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give delete
    this.giveUserPrivilege("test-user", "33");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...
    response = this.privUtil.sendMessage(Method.PUT, privilege, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // read should succeed (inherited by delete)
    response = this.privUtil.sendMessage(Method.GET, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.privUtil.sendMessage(Method.POST, privilege);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.privUtil.sendMessage(Method.DELETE, null, responsePrivilege.getId());
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 204);

  }
}
