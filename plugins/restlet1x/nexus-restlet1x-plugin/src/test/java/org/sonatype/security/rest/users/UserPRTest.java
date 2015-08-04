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
package org.sonatype.security.rest.users;

import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;

import junit.framework.Assert;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class UserPRTest
    extends AbstractSecurityRestTest
{

  public void testAddUser()
      throws Exception
  {

    PlexusResource resource = this.lookup(PlexusResource.class, "UserListPlexusResource");

    UserResourceRequest resourceRequest = new UserResourceRequest();
    UserResource userResource = new UserResource();
    resourceRequest.setData(userResource);
    userResource.setEmail("test@test.com");
    userResource.setFirstName("firstAddUser");
    userResource.setStatus("active");
    userResource.setUserId("testAddUser");
    userResource.addRole("admin");

    // try
    // {

    resource.post(null, this.buildRequest(), null, resourceRequest);
    // }
    // catch ( PlexusResourceException e )
    // {
    // ErrorResponse errorResponse = (ErrorResponse) e.getResultObject();
    // ErrorMessage errorMessage = (ErrorMessage) errorResponse.getErrors().get( 0 );
    // Assert.fail( e.getMessage() + ": " + errorMessage.getMsg() );
    // }

    // now list
    resource.get(null, this.buildRequest(), null, null);

  }

  public void testInvalidEmailAddUser()
      throws Exception
  {

    PlexusResource resource = this.lookup(PlexusResource.class, "UserListPlexusResource");

    UserResourceRequest resourceRequest = new UserResourceRequest();
    UserResource userResource = new UserResource();
    resourceRequest.setData(userResource);
    userResource.setEmail("testInvalidEmailAddUser");
    userResource.setFirstName("firstTestInvalidEmailAddUser");
    userResource.setLastName("firstTestInvalidEmailAddUser");
    userResource.setStatus("active");
    userResource.setUserId("testInvalidEmailAddUser");
    userResource.addRole("admin");

    try {

      resource.post(null, this.buildRequest(), null, resourceRequest);
      Assert.fail("expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse errorResponse = (ErrorResponse) e.getResultObject();
      ErrorMessage errorMessage = (ErrorMessage) errorResponse.getErrors().get(0);
      Assert.assertTrue(errorMessage.getId().contains("email"));
    }

    // now list
    resource.get(null, this.buildRequest(), null, null);
  }

  public void testUserIdWithSpace()
      throws Exception
  {

    PlexusResource resource = this.lookup(PlexusResource.class, "UserListPlexusResource");

    UserResourceRequest resourceRequest = new UserResourceRequest();
    UserResource userResource = new UserResource();
    resourceRequest.setData(userResource);
    userResource.setEmail("testUserIdWithSpace@testUserIdWithSpace.com");
    userResource.setFirstName("testUserIdWithSpace");
    userResource.setLastName("Last Name testUserIdWithSpace");
    userResource.setStatus("active");
    userResource.setUserId("test User Id With Space");
    userResource.addRole("admin");

    try {

      resource.post(null, this.buildRequest(), null, resourceRequest);
      Assert.fail("expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse errorResponse = (ErrorResponse) e.getResultObject();
      ErrorMessage errorMessage = (ErrorMessage) errorResponse.getErrors().get(0);
      Assert.assertTrue(errorMessage.getId().contains("userId"));
    }

    // fix it
    userResource.setUserId("testUserIdWithSpace");
    resource.post(null, this.buildRequest(), null, resourceRequest);

    // NOTE: update not supported

  }

  public void testUpdateUserValidation()
      throws Exception
  {
    // test user creation with NO status

    // add a user
    PlexusResource resource = this.lookup(PlexusResource.class, "UserListPlexusResource");

    UserResourceRequest resourceRequest = new UserResourceRequest();
    UserResource userResource = new UserResource();
    resourceRequest.setData(userResource);
    userResource.setEmail("testUpdateUserValidation@test.com");
    userResource.setLastName("testUpdateUserValidation");
    userResource.setStatus("active");
    userResource.setUserId("testUpdateUserValidation");
    userResource.addRole("admin");

    resource.post(null, this.buildRequest(), null, resourceRequest);

    // remove the status
    userResource.setStatus("");

    resource = this.lookup(PlexusResource.class, "UserPlexusResource");
    try {
      resource.put(null, this.buildRequest(), null, resourceRequest);
      Assert.fail("expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      // expected
    }

  }

  public void testInvalidEmailUpdateUserValidation()
      throws Exception
  {
    // test user creation with NO status

    // add a user
    PlexusResource resource = this.lookup(PlexusResource.class, "UserListPlexusResource");

    UserResourceRequest resourceRequest = new UserResourceRequest();
    UserResource userResource = new UserResource();
    resourceRequest.setData(userResource);
    userResource.setEmail("testInvalidEmailUpdateUserValidation@test.com");
    userResource.setLastName("testInvalidEmailUpdateUserValidation");
    userResource.setStatus("active");
    userResource.setUserId("testInvalidEmailUpdateUserValidation");
    userResource.addRole("admin");

    resource.post(null, this.buildRequest(), null, resourceRequest);

    // remove the status
    userResource.setEmail("invalidEmailAddress");

    resource = this.lookup(PlexusResource.class, "UserPlexusResource");
    try {
      resource.put(null, this.buildRequest(), null, resourceRequest);
      Assert.fail("expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse errorResponse = (ErrorResponse) e.getResultObject();
      ErrorMessage errorMessage = (ErrorMessage) errorResponse.getErrors().get(0);
      Assert.assertTrue(errorMessage.getId().contains("email"));
    }

  }

  private Request buildRequest() {
    Request request = new Request();

    Reference ref = new Reference("http://localhost:12345/");

    request.setRootRef(ref);
    request.setResourceRef(new Reference(ref, "users"));

    return request;
  }

}
