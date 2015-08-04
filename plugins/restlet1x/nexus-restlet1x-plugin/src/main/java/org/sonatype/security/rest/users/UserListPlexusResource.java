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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.security.rest.model.UserListResourceResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.sonatype.security.rest.model.UserResourceResponse;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource for listing and creating users.
 *
 * @author tstevens
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserListPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserListPlexusResource.RESOURCE_URI)
public class UserListPlexusResource
    extends AbstractUserPlexusResource
{

  public static final String RESOURCE_URI = "/users";

  public UserListPlexusResource() {
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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[security:users]");
  }

  /**
   * Retrieves the list of users.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = UserListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    UserListResourceResponse result = new UserListResourceResponse();

    for (User user : getSecuritySystem().searchUsers(new UserSearchCriteria(null, null, DEFAULT_SOURCE))) {
      UserResource res = securityToRestModel(user, request, true);

      if (res != null) {
        result.addData(res);
      }
    }

    return result;
  }

  /**
   * Creates a user.
   */
  @Override
  @POST
  @ResourceMethodSignature(input = UserResourceRequest.class, output = UserResourceResponse.class)
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    UserResourceRequest requestResource = (UserResourceRequest) payload;
    UserResourceResponse result = null;

    if (requestResource != null) {
      UserResource resource = requestResource.getData();

      try {
        User user = restToSecurityModel(null, resource);

        validateUserContainment(user);

        String password = resource.getPassword();
        getSecuritySystem().addUser(user, password);

        result = new UserResourceResponse();

        // Update the status, as that may have changed
        resource.setStatus(user.getStatus().name());

        resource.setResourceURI(createChildReference(request, resource.getUserId()).toString());

        result.setData(resource);

      }
      catch (InvalidConfigurationException e) {
        // build and throw exception
        handleInvalidConfigurationException(e);
      }
      catch (NoSuchUserManagerException e) {
        ErrorResponse errorResponse = getErrorResponse("*", e.getMessage());
        throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to create user.",
            errorResponse);
      }
    }
    return result;
  }

}
