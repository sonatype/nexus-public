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

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.rest.model.UserResource;

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
public class Nexus142UserValidationIT
    extends AbstractNexusIntegrationTest
{

  protected UserMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    this.messageUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Test
  public void createUserWithNoRoles()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("createUserWithNoRoles");
    resource.setUserId("createUserWithNoRoles");
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    // no roles
    // resource.addRole( "role1" );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    String responseText = response.getEntity().getText();
    Assert.assertTrue("Error message: " + responseText, responseText.startsWith("{\"errors\":"));

  }

  @Test
  public void updateUsersPasswordTest()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("updateUsersPasswordTest");
    resource.setUserId("updateUsersPasswordTest");
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("role1");

    resource = this.messageUtil.createUser(resource);

    resource.setPassword("SHOULD-FAIL");
    Response response = this.messageUtil.sendMessage(Method.PUT, resource);

    String responseText = response.getEntity().getText();
    Assert.assertFalse("Expected failure: Satus: " + response.getStatus() + "\n Response Text:" + responseText,
        response.getStatus().isSuccess());
    Assert.assertTrue("Error message: " + responseText, responseText.startsWith("{\"errors\":"));

  }

  @Test
  public void createUserWithNoUserId()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("createUserWithNoUserId");
    // resource.setUserId( "createUserWithNoUserId" );
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("role1");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

  }

  @Test
  public void createUserWithNoEmail()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("createUserWithNoEmail");
    resource.setUserId("createUserWithNoEmail");
    resource.setStatus("active");
    // resource.setEmail( "nexus@user.com" );
    resource.addRole("role1");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  @Test
  public void createUserInvalidRole()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("createUserInvalidRole");
    resource.setUserId("createUserInvalidRole");
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("INVALID-ROLE");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  @Test
  public void createUserDuplicateUserId()
      throws IOException
  {
    UserResource resource = new UserResource();

    resource.setEmail("test@email.com");
    resource.setFirstName("name");
    resource.setStatus("active");
    resource.setUserId("dup-user");
    resource.addRole("role1");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("User should have been created: " + response.getStatus());
    }

    resource = new UserResource();

    resource.setEmail("test2@email.com");
    resource.setFirstName("name");
    resource.setStatus("active");
    resource.setUserId("dup-user");
    resource.addRole("role1");

    response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  public void createUserDuplicateEmail()
      throws IOException
  {
    UserResource resource = new UserResource();

    resource.setEmail("dup@email.com");
    resource.setFirstName("name");
    resource.setStatus("active");
    resource.setUserId("user1");
    resource.addRole("role1");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("User should have been created: " + response.getStatus());
    }

    resource = new UserResource();

    resource.setEmail("dup@email.com");
    resource.setFirstName("name");
    resource.setStatus("active");
    resource.setUserId("user2");
    resource.addRole("role1");

    response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));
  }

  @Test
  public void updateValidation()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("updateValidation");
    resource.setUserId("updateValidation");
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("role1");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create user: " + response.getStatus());
    }

    // get the Resource object
    UserResource responseResource = this.messageUtil.getResourceFromResponse(response);

    Assert.assertEquals(responseResource.getFirstName(), resource.getFirstName());
    Assert.assertEquals(responseResource.getUserId(), resource.getUserId());
    Assert.assertEquals(responseResource.getStatus(), resource.getStatus());
    Assert.assertEquals(responseResource.getEmail(), resource.getEmail());
    Assert.assertEquals(resource.getRoles(), responseResource.getRoles());

    getSecurityConfigUtil().verifyUser(resource);

    // update the user

    resource.setFirstName("updateValidation");
    resource.setUserId("updateValidation");
    resource.setStatus("active");
    resource.setEmail("");
    resource.addRole("role1");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

    /**
     * NO STATUS
     */
    resource.setFirstName("updateValidation");
    resource.setUserId("updateValidation");
    resource.setStatus("");
    resource.setEmail("nexus@user.com");
    resource.addRole("role1");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }

    String errorText = response.getEntity().getText();

    Assert.assertTrue("expected error, but was: " + errorText, errorText.startsWith("{\"errors\":"));

    // FIXME: should we keep supporting this?
    // /**
    // * NO ROLES
    // */
    // resource.setFirstName( "updateValidation" );
    // resource.setUserId( "updateValidation" );
    // resource.setStatus( "active" );
    // resource.setEmail( "nexus@user.com" );
    // resource.getRoles().clear();
    //
    // response = this.messageUtil.sendMessage( Method.PUT, resource );
    //
    //
    // if ( response.getStatus().isSuccess() )
    // {
    // Assert.fail( "User should not have been created: " + response.getStatus() );
    // }
    // Assert.assertTrue( response.getEntity().getText().startsWith( "{\"errors\":" ) );

    /**
     * INVALID ROLE
     */
    resource.setFirstName("updateValidation");
    resource.setUserId("updateValidation");
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("INVALID_ROLE");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

    /**
     * NO NAME
     */
    resource.setFirstName("");
    resource.setUserId("updateValidation");
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("role1");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

    /**
     * NO USER ID
     */
    resource.setFirstName("updateValidation");
    resource.setUserId(null);
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("role1");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }

    // This is actually not a validation error, but a 'not found' error, so result will NOT contain the validation
    // errors
    // Assert.assertTrue( response.getEntity().getText().startsWith( "{\"errors\":" ) );

    /**
     * DUPLICATE EMAIL
     */
    UserResource duplicateResource = new UserResource();

    duplicateResource.setEmail("dup@email.com");
    duplicateResource.setFirstName("dupname");
    duplicateResource.setStatus("active");
    duplicateResource.setUserId("dup-user2");
    duplicateResource.addRole("role1");

    response = this.messageUtil.sendMessage(Method.POST, duplicateResource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("User should have been created: " + response.getStatus());
    }

    resource.setUserId(responseResource.getUserId());
    resource.setEmail("dup@email.com");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("User should not have been created: " + response.getStatus());
    }
  }

}
