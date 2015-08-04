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
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.realms.tools.ConfigurationManager;
import org.sonatype.security.realms.tools.NoSuchRoleMappingException;
import org.sonatype.security.rest.model.UserToRoleResource;
import org.sonatype.security.rest.model.UserToRoleResourceRequest;

import junit.framework.Assert;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;

public class UserToRolePRTest
    extends AbstractSecurityRestTest
{

  private UserToRolePlexusResource getResource()
      throws Exception
  {
    return (UserToRolePlexusResource) this.lookup(PlexusResource.class, "UserToRolePlexusResource");
  }

  private ConfigurationManager getConfig()
      throws Exception
  {
    return this.lookup(ConfigurationManager.class);
  }

  public void testPutWithRoles()
      throws Exception
  {
    UserToRolePlexusResource resource = getResource();

    Request request = new Request();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "jcoder");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, REALM_KEY);

    UserToRoleResourceRequest requestRequest = new UserToRoleResourceRequest();
    UserToRoleResource jcoderBefore = new UserToRoleResource();
    requestRequest.setData(jcoderBefore);
    jcoderBefore.setUserId("jcoder");
    jcoderBefore.setSource(REALM_KEY);
    jcoderBefore.addRole("developer");

    Assert.assertNull(resource.put(null, request, response, requestRequest));
  }

  public void testPutWithOutRoles()
      throws Exception
  {
    UserToRolePlexusResource resource = getResource();

    Request request = new Request();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "jcoder");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, REALM_KEY);

    UserToRoleResourceRequest requestRequest = new UserToRoleResourceRequest();
    UserToRoleResource jcoderBefore = new UserToRoleResource();
    requestRequest.setData(jcoderBefore);
    jcoderBefore.setUserId("jcoder");
    jcoderBefore.setSource(REALM_KEY);

    try {
      resource.put(null, request, response, requestRequest);
      Assert.fail("Expected ResourceException");
    }
    catch (PlexusResourceException e) {
      // expected
      Assert.assertEquals(400, e.getStatus().getCode());
      Assert.assertTrue(this.getErrorString((ErrorResponse) e.getResultObject(), 0).toLowerCase().contains("role"));
    }
  }

  public void testPutUserNotInConfigYet()
      throws Exception
  {
    UserToRolePlexusResource resource = getResource();

    Request request = new Request();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "cdugas");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, REALM_KEY);

    UserToRoleResourceRequest requestRequest = new UserToRoleResourceRequest();
    UserToRoleResource cdugasBefore = new UserToRoleResource();
    requestRequest.setData(cdugasBefore);
    cdugasBefore.setUserId("cdugas");
    cdugasBefore.setSource(REALM_KEY);
    cdugasBefore.addRole("developer");

    Assert.assertNull(resource.put(null, request, response, requestRequest));
  }

  public void testExternalRoleNotValidTest()
      throws Exception
  {

    UserToRolePlexusResource resource = getResource();

    Request request = new Request();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "cdugas");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, REALM_KEY);

    UserToRoleResourceRequest requestRequest = new UserToRoleResourceRequest();
    UserToRoleResource cdugasBefore = new UserToRoleResource();
    requestRequest.setData(cdugasBefore);
    cdugasBefore.setUserId("cdugas");
    cdugasBefore.setSource(REALM_KEY);
    cdugasBefore.addRole("developerINVALID");
    cdugasBefore.addRole("repomaintainerINVALID");

    try {
      resource.put(null, request, response, requestRequest);
      Assert.fail("Expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      String error = this.getErrorString((ErrorResponse) e.getResultObject(), 0);
      Assert.assertTrue(error.contains("repomaintainer"));
    }
  }

  protected String getErrorString(ErrorResponse errorResponse, int index) {
    return ((ErrorMessage) errorResponse.getErrors().get(index)).getMsg();
  }

  public void testPutWithRolesAndDelete()
      throws Exception
  {
    UserToRolePlexusResource resource = getResource();

    Request request = new Request();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "jcoder");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, REALM_KEY);

    UserToRoleResourceRequest requestRequest = new UserToRoleResourceRequest();
    UserToRoleResource jcoderBefore = new UserToRoleResource();
    requestRequest.setData(jcoderBefore);
    jcoderBefore.setUserId("jcoder");
    jcoderBefore.setSource(REALM_KEY);
    jcoderBefore.addRole("developer");

    Assert.assertNull(resource.put(null, request, response, requestRequest));

    // check config
    CUserRoleMapping mapping = this.getConfig().readUserRoleMapping("jcoder", REALM_KEY);
    Assert.assertEquals(1, mapping.getRoles().size());
    Assert.assertTrue(mapping.getRoles().contains("developer"));

    // now delete
    resource.delete(null, request, response);

    // check config
    try {
      this.getConfig().readUserRoleMapping("jcoder", REALM_KEY);
      Assert.fail("Expected: NoSuchRoleMappingException");
    }
    catch (NoSuchRoleMappingException e) {
      // expected
    }

  }

  public void testPutUserNotInConfigYetAndDelete()
      throws Exception
  {
    UserToRolePlexusResource resource = getResource();

    Request request = new Request();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "cdugas");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, REALM_KEY);

    UserToRoleResourceRequest requestRequest = new UserToRoleResourceRequest();
    UserToRoleResource cdugasBefore = new UserToRoleResource();
    requestRequest.setData(cdugasBefore);
    cdugasBefore.setUserId("cdugas");
    cdugasBefore.setSource(REALM_KEY);
    cdugasBefore.addRole("developer");

    Assert.assertNull(resource.put(null, request, response, requestRequest));

    // check config
    CUserRoleMapping mapping = this.getConfig().readUserRoleMapping("cdugas", REALM_KEY);
    Assert.assertEquals(1, mapping.getRoles().size());
    Assert.assertTrue(mapping.getRoles().contains("developer"));

    // now delete
    resource.delete(null, request, response);

    // check config
    try {
      this.getConfig().readUserRoleMapping("cdugas", REALM_KEY);
      Assert.fail("Expected: NoSuchRoleMappingException");
    }
    catch (NoSuchRoleMappingException e) {
      // expected
    }

  }

  public void testDelete404()
      throws Exception
  {

    UserToRolePlexusResource resource = getResource();

    Request request = new Request();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "FOO-USER");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, REALM_KEY);

    UserToRoleResourceRequest requestRequest = new UserToRoleResourceRequest();
    UserToRoleResource cdugasBefore = new UserToRoleResource();
    requestRequest.setData(cdugasBefore);
    cdugasBefore.setUserId("FOO-USER");
    cdugasBefore.setSource(REALM_KEY);
    cdugasBefore.addRole("developer");

    try {
      resource.delete(null, request, response);
    }
    catch (ResourceException e) {
      Assert.assertEquals(404, e.getStatus().getCode());
    }

  }

}
