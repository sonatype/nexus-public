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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.security.rest.model.UserToRoleResource;
import org.sonatype.security.rest.model.UserToRoleResourceRequest;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource to manage a users list of roles. Used when a user belongs to an external source.
 *
 * @author bdemers
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserToRolePlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserToRolePlexusResource.RESOURCE_URI)
public class UserToRolePlexusResource
    extends AbstractUserPlexusResource
{

  public static final String SOURCE_ID_KEY = "sourceId";

  public static final String RESOURCE_URI = "/user_to_roles/{" + SOURCE_ID_KEY + "}/{" + USER_ID_KEY + "}";

  public UserToRolePlexusResource() {
    this.setModifiable(true);
    this.setReadable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new UserToRoleResourceRequest();
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/user_to_roles/*/*", "authcBasic,perms[security:users]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  protected String getUserId(Request request) {
    return getRequestAttribute(request, USER_ID_KEY);
  }

  protected String getSourceId(Request request) {
    return getRequestAttribute(request, SOURCE_ID_KEY);
  }

  /**
   * Sets a users roles.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   * @param userId   The Id of the user.
   */
  @Override
  @PUT
  @ResourceMethodSignature(input = UserToRoleResourceRequest.class, pathParams = {
      @PathParam("sourceId"),
      @PathParam("userId")
  })
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    UserToRoleResourceRequest mappingRequest = (UserToRoleResourceRequest) payload;

    if (mappingRequest.getData() == null) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
          "User Role Mapping was not found in the Request.");
    }

    String userId = this.getUserId(request);
    String sourceId = this.getSourceId(request);

    // check if the user exists
    try {
      if (this.getSecuritySystem().getUser(userId, sourceId) == null) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found.");
      }
    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found.");
    }
    catch (NoSuchUserManagerException e) {
      this.getLogger().warn(e.getMessage(), e);
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found.");
    }

    // get the dto
    UserToRoleResource userToRole = mappingRequest.getData();

    Set<RoleIdentifier> roleIdentifiers = this.restToSecurityModel(userToRole);

    if (roleIdentifiers.size() == 0) {
      throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.",
          getErrorResponse("roles", "User requires one or more roles."));
    }

    try {
      // this will throw if we cannot find the user, in that case we will create one.
      getSecuritySystem().setUsersRoles(userToRole.getUserId(), userToRole.getSource(), roleIdentifiers);
    }
    catch (InvalidConfigurationException e) {
      this.handleInvalidConfigurationException(e);
    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found.");
    }

    response.setStatus(Status.SUCCESS_NO_CONTENT);
    return null;
  }

  /**
   * Gets a users roles.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   * @param userId   The Id of the user.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = UserToRoleResourceRequest.class, pathParams = {
      @PathParam("sourceId"),
      @PathParam("userId")
  })
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String userId = this.getUserId(request);

    String sourceId = this.getSourceId(request);

    try {
      Set<RoleIdentifier> roleIds = getSecuritySystem().getUsersRoles(userId, sourceId);

      UserToRoleResourceRequest resp = new UserToRoleResourceRequest();

      resp.setData(securityToRestModel(userId, sourceId, roleIds));

      return resp;
    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not find user '" + userId + "'.");
    }
    catch (NoSuchUserManagerException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Could not find user manager for source '"
          + sourceId + "'.");
    }
  }

  /**
   * Removes all roles from a user.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   * @param userId   The Id of the user.
   */
  @Override
  @DELETE
  @ResourceMethodSignature(pathParams = {@PathParam("sourceId"), @PathParam("userId")})
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    // get the userId
    String userId = this.getUserId(request);
    String source = this.getSourceId(request);

    try {
      getSecuritySystem().setUsersRoles(userId, source, null);
    }
    catch (InvalidConfigurationException e) {
      this.handleInvalidConfigurationException(e);
    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found.");
    }
  }

  private Set<RoleIdentifier> restToSecurityModel(UserToRoleResource restRoleMapping) {
    // FIXME: loss of roles source, currently we only support CRUDS on the XML realm but, that is temporary.

    Set<RoleIdentifier> roleIdentifiers = new HashSet<RoleIdentifier>();

    for (String roleId : (List<String>) restRoleMapping.getRoles()) {
      roleIdentifiers.add(new RoleIdentifier(DEFAULT_SOURCE, roleId));
    }

    return roleIdentifiers;
  }

  private UserToRoleResource securityToRestModel(String userId, String source, Set<RoleIdentifier> roleIds) {
    UserToRoleResource resource = new UserToRoleResource();

    resource.setUserId(userId);

    resource.setSource(source);

    List<String> roles = new ArrayList<String>();

    for (RoleIdentifier roleId : roleIds) {
      roles.add(roleId.getRoleId());
    }

    resource.setRoles(roles);

    return resource;
  }
}
