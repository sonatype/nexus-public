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
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.PlexusRoleListResourceResponse;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource for listing security roles.
 *
 * @author bdemers
 * @see RoleListPlexusResource
 */
@Singleton
@Typed(PlexusResource.class)
@Named("PlexusRoleListPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(PlexusRoleListPlexusResource.RESOURCE_URI)
@Deprecated
public class PlexusRoleListPlexusResource
    extends AbstractSecurityPlexusResource
{

  public static final String SOURCE_ID_KEY = "sourceId";

  public static final String RESOURCE_URI = "/plexus_roles/{" + SOURCE_ID_KEY + "}";

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/plexus_roles/*", "authcBasic,perms[security:roles]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Retrieves the list of security roles.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PlexusRoleListResourceResponse.class, pathParams = {@PathParam("sourceId")})
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String source = this.getSourceId(request);

    // get roles for the source
    Set<Role> roles;
    try {
      roles = this.getSecuritySystem().listRoles(source);
    }
    catch (NoSuchAuthorizationManagerException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Role Source '" + source
          + "' could not be found.");
    }

    PlexusRoleListResourceResponse resourceResponse = new PlexusRoleListResourceResponse();
    for (Role role : roles) {
      resourceResponse.addData(this.securityToRestModel(role));
    }

    return resourceResponse;
  }

  protected String getSourceId(Request request) {
    return getRequestAttribute(request, SOURCE_ID_KEY);
  }
}
