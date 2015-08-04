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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.rest.model.ExternalRoleMappingListResourceResponse;
import org.sonatype.security.rest.model.ExternalRoleMappingResource;
import org.sonatype.security.usermanagement.xml.SecurityXmlUserManager;

import org.apache.commons.lang.StringUtils;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource for listing external role mappings. An external role mapping, maps a role of an external source to one
 * of managed by the system, giving a user all the privileges contained in this system role.
 *
 * @author bdemers
 */
@Singleton
@Typed(PlexusResource.class)
@Named("ExternalRoleMappingListPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(ExternalRoleMappingListPlexusResource.RESOURCE_URI)
public class ExternalRoleMappingListPlexusResource
    extends AbstractRolePlexusResource
{

  public static final String SOURCE_ID_KEY = "sourceId";

  public static final String RESOURCE_URI = "/external_role_map/{" + SOURCE_ID_KEY + "}";

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/external_role_map/*", "authcBasic,perms[security:roles]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Retrieves the list of external role mappings.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = ExternalRoleMappingListResourceResponse.class,
      pathParams = {@PathParam("sourceId")})
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String source = this.getSourceId(request);

    try {
      // get roles for the source
      Set<Role> roles = this.getSecuritySystem().listRoles(source);

      if (roles == null) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Role Source '" + source
            + "' could not be found.");
      }

      Set<Role> defaultRoles = this.getSecuritySystem().listRoles(SecurityXmlUserManager.SOURCE);

      Map<Role, Set<Role>> roleMap = new HashMap<Role, Set<Role>>();

      for (Role defaultRole : defaultRoles) {
        for (Role role : roles) {
          // if the roleId matches (and the source doesn't)
          if (!StringUtils.equals(defaultRole.getSource(), role.getSource())
              && StringUtils.equals(defaultRole.getRoleId(), role.getRoleId())) {
            Set<Role> mappedRoles = roleMap.get(defaultRole);
            // if we don't have any currently mapped roles, add it to the map,
            // if we do then just add to the set

            if (mappedRoles == null) {
              mappedRoles = new HashSet<Role>();
              mappedRoles.add(role);
              roleMap.put(defaultRole, mappedRoles);
            }
            else {
              // just add this new role to the current set
              mappedRoles.add(role);
            }

            roleMap.put(defaultRole, mappedRoles);
          }
        }
      }

      // now put this in a resource
      ExternalRoleMappingListResourceResponse result = new ExternalRoleMappingListResourceResponse();

      for (Role defaultRole : roleMap.keySet()) {
        ExternalRoleMappingResource resource = new ExternalRoleMappingResource();
        result.addData(resource);
        resource.setDefaultRole(this.securityToRestModel(defaultRole));

        for (Role mappedRole : roleMap.get(defaultRole)) {
          resource.addMappedRole(this.securityToRestModel(mappedRole));
        }
      }

      return result;

    }
    catch (NoSuchAuthorizationManagerException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Role Source '" + source
          + "' could not be found.");
    }

  }

  protected String getSourceId(Request request) {
    return getRequestAttribute(request, SOURCE_ID_KEY);
  }
}
