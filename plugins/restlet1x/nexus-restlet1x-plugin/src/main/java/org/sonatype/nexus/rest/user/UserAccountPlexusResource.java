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
package org.sonatype.nexus.rest.user;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.rest.model.UserAccount;
import org.sonatype.nexus.rest.model.UserAccountRequestResponseWrapper;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.security.authorization.AuthorizationException;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Resource managing user account details.
 *
 * @since 2.1
 */
@Named
@Singleton
@Path("/user_account/{" + UserAccountPlexusResource.ACCOUNT_ID_KEY + "}")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class UserAccountPlexusResource
    extends AbstractUserAccountPlexusResource
{
  public static final String ACCOUNT_ID_KEY = "accountId";

  public UserAccountPlexusResource() {
    this.setReadable(true);

    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new UserAccountRequestResponseWrapper();
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/user_account/*", "authcBasic");
  }

  @Override
  public String getResourceUri() {
    return "/user_account/{" + ACCOUNT_ID_KEY + "}";
  }

  /**
   * Returns the account information for given accountId.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(UserAccountPlexusResource.ACCOUNT_ID_KEY)},
      output = UserAccountRequestResponseWrapper.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    UserAccountRequestResponseWrapper result = new UserAccountRequestResponseWrapper();

    try {
      User user = userAccountManager.readAccount(getUserId(request));

      result.setData(nexusToRestModel(user, request));
    }
    catch (UserNotFoundException e) {
      String msg = "User account '" + getUserId(request) + "' not found.";

      getLogger().debug(msg, e);

      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, msg);
    }
    catch (AuthorizationException e) {
      throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "Not allowed to read account '"
          + getUserId(request) + "'.");
    }

    return result;
  }

  /**
   * Updates account information for given accountId.
   */
  @Override
  @PUT
  @ResourceMethodSignature(pathParams = {@PathParam(UserAccountPlexusResource.ACCOUNT_ID_KEY)},
      input = UserAccountRequestResponseWrapper.class, output = UserAccountRequestResponseWrapper.class)
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    UserAccountRequestResponseWrapper result = new UserAccountRequestResponseWrapper();

    UserAccount dto = ((UserAccountRequestResponseWrapper) payload).getData();

    try {
      User user = getSecuritySystem().getUser(getUserId(request));

      user.setFirstName(dto.getFirstName());
      user.setLastName(dto.getLastName());

      user.setEmailAddress(dto.getEmail());

      userAccountManager.updateAccount(user);

      result.setData(nexusToRestModel(user, request));
    }
    catch (InvalidConfigurationException e) {
      handleInvalidConfigurationException(e);

      return null;
    }
    catch (UserNotFoundException e) {
      String msg = "User account '" + getUserId(request) + "' not found.";

      getLogger().debug(msg, e);

      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, msg);
    }
    catch (NoSuchUserManagerException e) {
      ErrorResponse errorResponse = getErrorResponse("*", e.getMessage());

      throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to update account '"
          + dto.getUserId() + "'.", errorResponse);
    }
    catch (AuthorizationException e) {
      throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "Not allowed to update account '" + dto.getUserId()
          + "'.");
    }

    return result;
  }

  protected String getUserId(Request request) {
    return request.getAttributes().get(ACCOUNT_ID_KEY).toString();
  }
}
