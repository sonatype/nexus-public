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
package org.sonatype.security.rest.privileges;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.AssignedPrivilegeListResource;
import org.sonatype.security.rest.model.AssignedPrivilegeListResourceResponse;
import org.sonatype.security.rest.model.ParentNode;
import org.sonatype.security.usermanagement.RoleIdentifier;
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
 * REST resource to retrieve the list of Privileges assigned to a user. Will also include a tree that details how the
 * privilege is assigned to this user (through roles).
 */
@Singleton
@Typed(PlexusResource.class)
@Named("AssignedPrivilegesPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(AssignedPrivilegesPlexusResource.RESOURCE_URI)
public class AssignedPrivilegesPlexusResource
    extends AbstractSecurityPlexusResource
{
  public static final String USER_ID_KEY = "userId";

  public static final String RESOURCE_URI = "/assigned_privileges/{" + USER_ID_KEY + "}";

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/assigned_privileges/*", "authcBasic,perms[security:users]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Retrieves the list of privileges assigned to the user.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = AssignedPrivilegeListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String userId = getUserId(request);

    try {
      AssignedPrivilegeListResourceResponse responseResource = new AssignedPrivilegeListResourceResponse();

      User user = this.getSecuritySystem().getUser(userId);

      AuthorizationManager authzManager = getSecuritySystem().getAuthorizationManager("default");

      for (RoleIdentifier roleIdentifier : user.getRoles()) {
        try {
          handleRole(authzManager.getRole(roleIdentifier.getRoleId()), null, authzManager,
              responseResource);
        }
        catch (NoSuchRoleException e) {
          getLogger().debug("Invalid roleId: " + roleIdentifier.getRoleId() + " from source: "
              + roleIdentifier.getSource() + " not found.");
        }
      }

      return responseResource;
    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "User: " + userId + " could not be found.");
    }
    catch (NoSuchAuthorizationManagerException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Unable to load default authorization manager");
    }
  }

  protected void handleRole(Role role, List<Role> parentList, AuthorizationManager authzManager,
                            AssignedPrivilegeListResourceResponse response)
  {
    List<Role> newParentList = new ArrayList<Role>();
    if (parentList != null) {
      newParentList.addAll(parentList);
    }

    newParentList.add(0, role);

    for (String roleId : role.getRoles()) {
      try {
        handleRole(authzManager.getRole(roleId), newParentList, authzManager, response);
      }
      catch (NoSuchRoleException e) {
        getLogger().debug("handleRole() failed, roleId: " + roleId + " not found");
      }
    }

    for (String privilegeId : role.getPrivileges()) {
      try {
        handlePrivilege(authzManager.getPrivilege(privilegeId), newParentList, response);
      }
      catch (NoSuchPrivilegeException e) {
        getLogger().debug("handleRole() failed, privilegeId: " + privilegeId + " not found");
      }
    }
  }

  protected void handlePrivilege(Privilege privilege, List<Role> parentList,
                                 AssignedPrivilegeListResourceResponse response)
  {
    AssignedPrivilegeListResource foundResource = null;

    // First check to see if resource already exists
    for (AssignedPrivilegeListResource resource : response.getData()) {
      if (resource.getId().equals(privilege.getId())) {
        foundResource = resource;
        break;
      }
    }

    // if not, create it
    if (foundResource == null) {
      foundResource = new AssignedPrivilegeListResource();
      foundResource.setId(privilege.getId());
      foundResource.setName(privilege.getName());
      response.addData(foundResource);
    }

    ParentNode root = null;
    ParentNode parent = null;

    // iterate through each role and add to tree
    for (Role role : parentList) {
      ParentNode newParent = new ParentNode();
      newParent.setId(role.getRoleId());
      newParent.setName(role.getName());

      // if we dont yet have a root, we are on first item
      if (root == null) {
        root = newParent;
        parent = root;
      }
      else {
        parent.addParent(newParent);
        parent = newParent;
      }
    }

    foundResource.addParent(root);
  }

  protected String getUserId(Request request) {
    return getRequestAttribute(request, USER_ID_KEY);
  }
}
