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

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.rest.model.UserChangePasswordRequest;
import org.sonatype.security.rest.model.UserChangePasswordResource;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * REST resource to allow an administrator to change a user's password.
 *
 * @author bdemers
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserSetPasswordPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserSetPasswordPlexusResource.RESOURCE_URI)
public class UserSetPasswordPlexusResource
    extends AbstractUserPlexusResource
{

  public static final String RESOURCE_URI = "/users_setpw";

  public UserSetPasswordPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new UserChangePasswordRequest();
  }

  @Override
  public String getResourceUri() {
    return "/users_setpw";
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[security:userssetpw]");
  }

  /**
   * Changes a user's password.
   */
  @Override
  @POST
  @ResourceMethodSignature(input = UserChangePasswordRequest.class)
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    UserChangePasswordRequest changePasswordRequest = (UserChangePasswordRequest) payload;

    if (changePasswordRequest != null) {
      UserChangePasswordResource resource = changePasswordRequest.getData();

      try {
        if (!isAnonymousUser(resource.getUserId(), request)) {
          getSecuritySystem().changePassword(resource.getUserId(), resource.getNewPassword());

          response.setStatus(Status.SUCCESS_NO_CONTENT);
        }
        else {
          response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Anonymous user cannot change password!");

          getLogger().debug("Anonymous user password change is blocked!");
        }
      }
      catch (UserNotFoundException e) {
        getLogger().debug("Invalid user ID!", e);

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid credentials supplied.");
      }
      catch (InvalidConfigurationException e) {
        // this should never happen
        getLogger().warn("Failed to set password!", e);

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Failed to set password!.");
      }

    }
    // don't return anything because the status is a 204
    return null;
  }
}
