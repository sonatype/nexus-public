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
import org.sonatype.security.rest.model.UserForgotPasswordRequest;
import org.sonatype.security.rest.model.UserForgotPasswordResource;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * REST resource to reset a users password. Default implementations will generate and email a user password to the
 * user.
 *
 * @author tstevens
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserForgotPasswordPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserForgotPasswordPlexusResource.RESOURCE_URI)
public class UserForgotPasswordPlexusResource
    extends AbstractUserPlexusResource
{
  public static final String RESOURCE_URI = "/users_forgotpw";

  public UserForgotPasswordPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new UserForgotPasswordRequest();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[security:usersforgotpw]");
  }

  /**
   * Reset a user's password.
   */
  @Override
  @POST
  @ResourceMethodSignature(input = UserForgotPasswordRequest.class)
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    UserForgotPasswordRequest forgotPasswordRequest = (UserForgotPasswordRequest) payload;

    if (forgotPasswordRequest != null) {
      UserForgotPasswordResource resource = forgotPasswordRequest.getData();

      try {
        if (!isAnonymousUser(resource.getUserId(), request)) {
          getSecuritySystem().forgotPassword(resource.getUserId(), resource.getEmail());

          response.setStatus(Status.SUCCESS_ACCEPTED);
        }
        else {
          response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Anonymous user cannot forget password");

          getLogger().debug("Anonymous user forgot password is blocked");
        }
      }
      catch (UserNotFoundException e) {
        getLogger().debug("Invalid Username", e);

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid Username");
      }
      catch (InvalidConfigurationException e) {
        // this should never happen
        getLogger().warn("Failed to set password!", e);

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Failed to set password!.");
      }
    }
    // return null because the status is 202
    return null;
  }

}
