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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.sonatype.security.rest.model.UserResourceResponse;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource for getting user information.
 *
 * @author tstevens
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserPlexusResource.RESOURCE_URI)
public class UserPlexusResource
    extends AbstractUserPlexusResource
{

  public static final String RESOURCE_URI = "/users/{" + USER_ID_KEY + "}";

  public UserPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new UserResourceRequest();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/users/*", "authcBasic,perms[security:users]");
  }

  protected String getUserId(Request request) {
    return getRequestAttribute(request, USER_ID_KEY);
  }

  /**
   * Retrieves a user's information.
   *
   * @param userId The Id of the user.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = UserResourceResponse.class, pathParams = {@PathParam("userId")})
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {

    UserResourceResponse result = new UserResourceResponse();

    try {
      result.setData(securityToRestModel(getSecuritySystem().getUser(getUserId(request)), request, false));

    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
    }
    return result;
  }

  /**
   * Updates a user's information.
   *
   * @param userId The Id of the user.
   */
  @Override
  @POST
  @ResourceMethodSignature(output = UserResourceResponse.class, pathParams = {@PathParam("userId")})
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    UserResourceRequest resourceRequest = (UserResourceRequest) payload;
    UserResourceResponse result = null;

    if (resourceRequest != null) {
      UserResource resource = resourceRequest.getData();

      // the password can not be set on update, The only way to set a password is using the users_setpw resource
      if (StringUtils.isNotEmpty(resource.getPassword())) {
        throw new PlexusResourceException(
            Status.CLIENT_ERROR_BAD_REQUEST,
            this.getErrorResponse("*",
                "Updating a users password using this URI is not allowed."));
      }

      try {
        User user = restToSecurityModel(getSecuritySystem().getUser(resource.getUserId()), resource);

        validateUserContainment(user);

        getSecuritySystem().updateUser(user);

        result = new UserResourceResponse();

        result.setData(resourceRequest.getData());

        result.getData().setResourceURI(createChildReference(request, resource.getUserId()).toString());

      }
      catch (InvalidConfigurationException e) {
        // build and throw exception
        handleInvalidConfigurationException(e);
      }
      catch (UserNotFoundException e) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
      }
      catch (NoSuchUserManagerException e) {
        ErrorResponse errorResponse = getErrorResponse("*", e.getMessage());
        throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to create user.",
            errorResponse);
      }
    }
    return result;
  }

  /**
   * Removes a user.
   *
   * @param userId The Id of the user.
   */
  @Override
  @DELETE
  @ResourceMethodSignature(pathParams = {@PathParam("userId")})
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    try {
      // not allowed to delete system Anonymous user
      if (isAnonymousUser(getUserId(request), request)) {
        String error =
            "The user with user ID ["
                + getUserId(request)
                +
                "] cannot be deleted, since it is marked user used for Anonymous access in Server Administration. To delete this user, disable anonymous access or, change the anonymous username and password to another valid values!";

        getLogger()
            .info("Anonymous user cannot be deleted! Unset the Allow Anonymous access first in Server Administration!");

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, error);
      }

      // not allowed to delete the current user himself
      if (isCurrentUser(request)) {
        String error =
            "The user with user ID [" + getUserId(request)
                + "] cannot be deleted, as that is the user currently logged into the application.";

        getLogger().info("The user with user ID ["
            + getUserId(request)
            + "] cannot be deleted, as that is the user currently logged into the application.");

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, error);
      }

      getSecuritySystem().deleteUser(getUserId(request));

      response.setStatus(Status.SUCCESS_NO_CONTENT);

    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
    }
  }

  protected boolean isCurrentUser(Request request) {
    Subject subject = SecurityUtils.getSubject();
    if (subject == null || subject.getPrincipal() == null) {
      return false; // not the current user because there is no current user
    }

    return subject.getPrincipal().equals(getUserId(request));
  }

}
