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
package org.sonatype.nexus.testsuite.security.nexus142;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.security.rest.model.UserResource;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test the privileges for CRUD operations.
 */
public class Nexus142UserPermissionIT
    extends AbstractPrivilegeTest
{

  @Test
  public void testCreatePermission()
      throws IOException
  {
    // create a user with anon access

    UserResource user = new UserResource();
    user.setEmail("tesCreatePermission@foo.org");
    user.setFirstName("tesCreatePermission");
    user.setUserId("tesCreatePermission");
    user.setStatus("active");
    user.addRole("anonymous");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    Response response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give create
    this.giveUserPrivilege("test-user", "38");

    // print out the users privs
    // this.printUserPrivs( "test-user" );

    // now.... it should work...
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);

    // read should succeed (inherited)
    response = this.userUtil.sendMessage(Method.GET, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.userUtil.sendMessage(Method.PUT, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.userUtil.sendMessage(Method.DELETE, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testUpdatePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    UserResource user = new UserResource();
    user.setEmail("tesUpdatePermission@foo.org");
    user.setFirstName("tesUpdatePermission");
    user.setUserId("tesUpdatePermission");
    user.setStatus("active");
    user.addRole("anonymous");

    Response response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update user
    response = this.userUtil.sendMessage(Method.PUT, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give update
    this.giveUserPrivilege("test-user", "40");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...

    // update user
    user.setUserId("tesUpdatePermission");
    response = this.userUtil.sendMessage(Method.PUT, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // read should succeed (inherited)
    response = this.userUtil.sendMessage(Method.GET, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.userUtil.sendMessage(Method.DELETE, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testReadPermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    UserResource user = new UserResource();
    user.setEmail("testReadPermission@foo.org");
    user.setFirstName("testReadPermission");
    user.setUserId("testReadPermission");
    user.setStatus("active");
    user.addRole("anonymous");

    Response response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update user
    response = this.userUtil.sendMessage(Method.PUT, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give read
    this.giveUserPrivilege("test-user", "39");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...

    // update user
    response = this.userUtil.sendMessage(Method.GET, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // read should fail
    response = this.userUtil.sendMessage(Method.PUT, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // update should fail
    response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.userUtil.sendMessage(Method.DELETE, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

  }

  @Test
  public void testDeletePermission()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    UserResource user = new UserResource();
    user.setEmail("testDeletePermission@foo.org");
    user.setFirstName("testDeletePermission");
    user.setUserId("testDeletePermission");
    user.setStatus("active");
    user.addRole("anonymous");

    Response response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 201);

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // update user
    response = this.userUtil.sendMessage(Method.DELETE, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // use admin
    TestContainer.getInstance().getTestContext().setUsername("admin");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // now give delete
    this.giveUserPrivilege("test-user", "41");

    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    // should work now...

    // update user
    response = this.userUtil.sendMessage(Method.PUT, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // read should succeed (inherited)
    response = this.userUtil.sendMessage(Method.GET, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 200);

    // update should fail
    response = this.userUtil.sendMessage(Method.POST, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 403);

    // delete should fail
    response = this.userUtil.sendMessage(Method.DELETE, user);
    Assert.assertEquals("Response status: ", response.getStatus().getCode(), 204);

  }

}
