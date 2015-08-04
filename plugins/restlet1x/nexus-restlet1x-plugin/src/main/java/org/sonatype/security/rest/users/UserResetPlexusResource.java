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
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
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
 * <BR/>
 * This resource is similar to {@link UserForgotPasswordPlexusResource} except that a system administrator can reset
 * other users passwords.
 *
 * @author tstevens
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserResetPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserResetPlexusResource.RESOURCE_URI)
public class UserResetPlexusResource
    extends AbstractUserPlexusResource
{

  public static final String RESOURCE_URI = "/users_reset/{" + USER_ID_KEY + "}";

  public UserResetPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/users_reset/*", "authcBasic,perms[security:usersreset]");
  }

  /**
   * Reset a user's password.
   *
   * @param userId The id of the user.
   */
  @Override
  @DELETE
  @ResourceMethodSignature(pathParams = {@PathParam("userId")})
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    final String userId = getRequestAttribute(request, USER_ID_KEY);

    try {
      if (!isAnonymousUser(userId, request)) {
        getSecuritySystem().resetPassword(userId);

        response.setStatus(Status.SUCCESS_NO_CONTENT);
      }
      else {
        getLogger().debug("Anonymous user password reset is blocked!");

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Anonymous user cannot reset password!");
      }
    }
    catch (UserNotFoundException e) {
      getLogger().debug("Invalid userid: " + userId, e);

      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "User ID not found!", e);
    }
    catch (InvalidConfigurationException e) {
      // this should never happen
      getLogger().warn("Failed to set password!", e);

      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Failed to set password!.", e);
    }
  }

}
