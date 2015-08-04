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
package org.sonatype.security.rest.roles;

import java.util.List;

import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.RoleResource;

import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public abstract class AbstractRolePlexusResource
    extends AbstractSecurityPlexusResource
{

  protected static final String ROLE_SOURCE = "default";

  public RoleResource securityToRestModel(Role role, Request request, boolean appendResourceId) {
    // and will convert to the rest object
    RoleResource resource = new RoleResource();

    resource.setDescription(role.getDescription());
    resource.setId(role.getRoleId());
    resource.setName(role.getName());

    String resourceId = "";
    if (appendResourceId) {
      resourceId = resource.getId();
    }
    resource.setResourceURI(this.createChildReference(request, resourceId).toString());

    resource.setUserManaged(!role.isReadOnly());

    for (String roleId : role.getRoles()) {
      resource.addRole(roleId);
    }

    for (String privId : role.getPrivileges()) {
      resource.addPrivilege(privId);
    }

    return resource;
  }

  public Role restToSecurityModel(Role role, RoleResource resource) {
    if (role == null) {
      role = new Role();
    }

    role.setRoleId(resource.getId());

    role.setDescription(resource.getDescription());
    role.setName(resource.getName());

    role.getRoles().clear();
    for (String roleId : (List<String>) resource.getRoles()) {
      role.addRole(roleId);
    }

    role.getPrivileges().clear();
    for (String privId : (List<String>) resource.getPrivileges()) {
      role.addPrivilege(privId);
    }

    return role;
  }

  public void validateRoleContainment(Role role)
      throws ResourceException
  {
    if (role.getRoles().size() == 0 && role.getPrivileges().size() == 0) {
      throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.",
          getErrorResponse("privileges",
              "One or more roles/privilegs are required."));
    }
  }

}
