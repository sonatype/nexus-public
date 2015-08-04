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
 * Extra CRUD validation tests.
 */
public class Nexus156RolesValidationIT
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
  public void roleWithNoPrivsTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("roleWithNoPrivsTest");
    resource.setName("roleWithNoPrivsTest");
    resource.setSessionTimeout(30);
    // resource.addPrivilege( "priv1" );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Role should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  @Test
  public void roleWithNoName()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("roleWithNoName");
    // resource.setName( "roleWithNoName" );
    resource.setSessionTimeout(30);
    resource.addPrivilege("1");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Role should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  @Test
  public void roleWithSpaceInId()
      throws IOException
  {
    RoleResource resource = new RoleResource();

    resource.setId("role With Space In Id");
    resource.setDescription("roleWithSpaceInId");
    resource.setName("roleWithSpaceInId");
    resource.setSessionTimeout(30);
    resource.addPrivilege("1");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Response: " + response.getEntity().getText() + "Role should have been created: "
          + response.getStatus());
    }

    // make sure the get works too
    Assert.assertEquals(this.messageUtil.getRole(resource.getId()).getId(), "role With Space In Id");
  }

  @Test
  public void duplicateIdTest()
      throws IOException
  {

    RoleResource resource = new RoleResource();

    resource.setDescription("duplicateIdTest");
    resource.setName("duplicateIdTest");
    resource.setId("duplicateIdTest");
    resource.setSessionTimeout(30);
    resource.addPrivilege("1");

    // create
    resource = this.messageUtil.createRole(resource);
    Assert.assertEquals(resource.getId(), "duplicateIdTest");

    // update
    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Role should not have been updated: " + response.getStatus() + "New Id: "
          + this.messageUtil.getResourceFromResponse(response).getId());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  @Test
  public void createRecursiveContainment()
      throws IOException
  {
    RoleResource resourceA = new RoleResource();
    resourceA.setName("recursive1");
    resourceA.setSessionTimeout(60);
    resourceA.addPrivilege("1");

    Response response = this.messageUtil.sendMessage(Method.POST, resourceA);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Role should have been created: " + response.getStatus());
    }

    // get the Resource object
    RoleResource responseResourceA = this.messageUtil.getResourceFromResponse(response);

    RoleResource resourceB = new RoleResource();
    resourceB = new RoleResource();
    resourceB.setName("recursive2");
    resourceB.setSessionTimeout(60);
    resourceB.addRole(responseResourceA.getId());

    response = this.messageUtil.sendMessage(Method.POST, resourceB);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Role should have been created: " + response.getStatus());
    }

    // get the Resource object
    RoleResource responseResourceB = this.messageUtil.getResourceFromResponse(response);

    RoleResource resourceC = new RoleResource();
    resourceC = new RoleResource();
    resourceC.setName("recursive3");
    resourceC.setSessionTimeout(60);
    resourceC.addRole(responseResourceB.getId());

    response = this.messageUtil.sendMessage(Method.POST, resourceC);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Role should have been created: " + response.getStatus());
    }

    // get the Resource object
    RoleResource responseResourceC = this.messageUtil.getResourceFromResponse(response);

    resourceA.setId(responseResourceA.getId());
    resourceA.getRoles().clear();
    resourceA.addRole(responseResourceC.getId());

    response = this.messageUtil.sendMessage(Method.PUT, resourceA);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Role should not have been updated: " + response.getStatus());
    }

    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  @Test
  public void updateValidationTests()
      throws IOException
  {
    RoleResource resource = new RoleResource();

    resource.setDescription("updateValidationTests");
    resource.setName("updateValidationTests");
    resource.setSessionTimeout(99999);
    resource.addPrivilege("5");
    resource.addPrivilege("4");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create role: " + response.getStatus());
    }

    // get the Resource object
    RoleResource responseResource = this.messageUtil.getResourceFromResponse(response);

    // make sure the id != null
    Assert.assertNotNull(responseResource.getId());

    resource.setId(responseResource.getId());

    Assert.assertEquals(responseResource.getDescription(), resource.getDescription());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getSessionTimeout(), responseResource.getSessionTimeout());
    Assert.assertEquals(resource.getPrivileges(), responseResource.getPrivileges());
    Assert.assertEquals(resource.getRoles(), responseResource.getRoles());

    getSecurityConfigUtil().verifyRole(resource);

        /*
         * NO Name
         */
    resource.setDescription("updateValidationTests");
    resource.setName(null);
    resource.setSessionTimeout(99999);
    resource.addPrivilege("5");
    resource.addPrivilege("4");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Role should not have been updated: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

        /*
         * NO Privs
         */
    resource.setDescription("updateValidationTests");
    resource.setName("updateValidationTests");
    resource.setSessionTimeout(99999);
    resource.getPrivileges().clear();

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Role should not have been updated: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

        /*
         * INVALID Privs
         */
    resource.setDescription("updateValidationTests");
    resource.setName("updateValidationTests");
    resource.setSessionTimeout(99999);
    resource.getPrivileges().clear();
    resource.getPrivileges().add("junk");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Role should have been updated: " + response.getStatus());
    }

        /*
         * Update Id
         */
    resource.setDescription("updateValidationTests");
    resource.setName("updateValidationTests");
    resource.setId("NEW-ID-WILL-FAIL");
    resource.setSessionTimeout(99999);
    resource.addPrivilege("5");
    resource.addPrivilege("4");

    response = this.messageUtil.sendMessage(Method.PUT, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().isSuccess()) {
      Assert.fail("Role should not have been updated: " + response.getStatus());
    }
    // expect a 404
    Assert.assertEquals(404, response.getStatus().getCode());

  }

}
