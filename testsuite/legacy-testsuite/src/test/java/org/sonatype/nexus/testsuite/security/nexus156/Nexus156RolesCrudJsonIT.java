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
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.security.rest.model.RoleResource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * CRUD tests for JSON request/response.
 */
public class Nexus156RolesCrudJsonIT
    extends AbstractNexusIntegrationTest
{

  protected RoleMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    this.messageUtil = new RoleMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Test
  public void createRoleTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("Create Test Role");
    resource.setName("CreateRole");
    resource.setSessionTimeout(30);
    resource.addPrivilege("1");
    resource.addPrivilege("2");

    this.messageUtil.createRole(resource);
  }

  @Test
  public void createRoleWithIdTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("Create Test Role With ID");
    resource.setName("CreateRoleWithID");
    resource.setId("CreateRoleWithID");
    resource.setSessionTimeout(30);
    resource.addPrivilege("1");
    resource.addPrivilege("2");

    this.messageUtil.createRole(resource);
  }

  @Test
  public void listTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("Create Test Role");
    resource.setName("ListTestRole");
    resource.setSessionTimeout(30);
    resource.addPrivilege("1");

    // create a role
    this.messageUtil.createRole(resource);

    // now that we have at least one element stored (more from other tests, most likely)

    // NEED to work around a GET problem with the REST client
    List<RoleResource> roles = this.messageUtil.getList();
    getSecurityConfigUtil().verifyRolesComplete(roles);

  }

  public void readTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("Read Test Role");
    resource.setName("ReadRole");
    resource.setSessionTimeout(31);
    resource.addPrivilege("3");
    resource.addPrivilege("4");
    resource = this.messageUtil.createRole(resource);

    // get the Resource object
    RoleResource responseResource = this.messageUtil.getRole(resource.getId());

    Assert.assertEquals(responseResource.getId(), resource.getId());
    Assert.assertEquals(responseResource.getDescription(), resource.getDescription());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getPrivileges(), responseResource.getPrivileges());
    Assert.assertEquals(resource.getRoles(), responseResource.getRoles());
  }

  @Test
  public void updateTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("Update Test Role");
    resource.setName("UpdateRole");
    resource.setSessionTimeout(99999);
    resource.addPrivilege("5");
    resource.addPrivilege("4");

    RoleResource responseResource = this.messageUtil.createRole(resource);

    // update the Role
    // TODO: add tests that changes the Id
    resource.setId(responseResource.getId());
    resource.setName("UpdateRole Again");
    resource.setDescription("Update Test Role Again");
    resource.getPrivileges().clear(); // clear the privs
    resource.addPrivilege("6");
    resource.setSessionTimeout(10);

    Response response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not update Role: " + response.getStatus());
    }

    // get the Resource object
    responseResource = this.messageUtil.getResourceFromResponse(response);

    Assert.assertEquals(responseResource.getId(), resource.getId());
    Assert.assertEquals(responseResource.getDescription(), resource.getDescription());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getSessionTimeout(), responseResource.getSessionTimeout());
    Assert.assertEquals(resource.getPrivileges(), responseResource.getPrivileges());
    Assert.assertEquals(resource.getRoles(), responseResource.getRoles());

    getSecurityConfigUtil().verifyRole(resource);
  }

  @Test
  public void deleteTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("Delete Test Role");
    resource.setName("deleteRole");
    resource.setSessionTimeout(1);
    resource.addPrivilege("7");
    resource.addPrivilege("8");

    RoleResource responseResource = this.messageUtil.createRole(resource);

    // use the new ID
    Response response = this.messageUtil.sendMessage(Method.DELETE, responseResource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not delete Role: " + response.getStatus());
    }

    // TODO: check if deleted
    Assert.assertNull(getSecurityConfigUtil().getCRole(responseResource.getId()));
  }

}
