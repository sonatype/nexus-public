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
package org.sonatype.nexus.testsuite.security.nexus156;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.security.rest.model.RoleResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test the privileges for CRUD operations.
 */
public class Nexus156RolesPermissionIT
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
    RoleResource role = new RoleResource();

    role.setDescription("testCreatePermission");
    role.setName("testCreatePermission");
    role.setSessionTimeout(30);
    role.addPrivilege("1");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    Response response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give create
    this.giveUserPrivilege("test-user", "34");


    // now.... it should work...
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);

    role = this.roleUtil.getResourceFromResponse(response);

    // read should succeed (inherited)
    response = this.roleUtil.sendMessage(Method.GET, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.roleUtil.sendMessage(Method.PUT, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.roleUtil.sendMessage(Method.DELETE, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testUpdatePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    RoleResource role = new RoleResource();
    role.setDescription("testUpdatePermission");
    role.setName("testUpdatePermission");
    role.setSessionTimeout(30);
    role.addPrivilege("1");

    Response response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    role = this.roleUtil.getResourceFromResponse(response);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update user
    role.setName("testUpdatePermission2");
    response = this.roleUtil.sendMessage(Method.PUT, role);
    //        log.debug( "PROBLEM: "+ this.userUtil.getUser( "test-user" ) );
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give update
    this.giveUserPrivilege("test-user", "36");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...

    // update user
    response = this.roleUtil.sendMessage(Method.PUT, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // read should succeed (inherited)
    response = this.roleUtil.sendMessage(Method.GET, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.roleUtil.sendMessage(Method.DELETE, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);


  }

  @Test
  public void testReadPermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    RoleResource role = new RoleResource();
    role.setDescription("testReadPermission");
    role.setName("testReadPermission");
    role.setSessionTimeout(30);
    role.addPrivilege("1");

    Response response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    role = this.roleUtil.getResourceFromResponse(response);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    response = this.roleUtil.sendMessage(Method.PUT, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give read
    this.giveUserPrivilege("test-user", "35");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...

    // update user
    response = this.roleUtil.sendMessage(Method.PUT, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // read should fail
    response = this.roleUtil.sendMessage(Method.GET, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.roleUtil.sendMessage(Method.DELETE, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);


  }

  @Test
  public void testDeletePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    RoleResource role = new RoleResource();
    role.setDescription("testUpdatePermission");
    role.setName("testUpdatePermission");
    role.setSessionTimeout(30);
    role.addPrivilege("1");

    Response response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);
    role = this.roleUtil.getResourceFromResponse(response);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");


    response = this.roleUtil.sendMessage(Method.DELETE, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give create
    this.giveUserPrivilege("test-user", "37");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...

    // update user
    response = this.roleUtil.sendMessage(Method.PUT, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // read should succeed (inherited)
    response = this.roleUtil.sendMessage(Method.GET, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.roleUtil.sendMessage(Method.POST, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.roleUtil.sendMessage(Method.DELETE, role);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 204);


  }


}
