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

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeDescriptor;
import org.sonatype.security.rest.model.PrivilegeStatusResourceResponse;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource for managing security privileges.
 *
 * @author tstevens
 */
@Singleton
@Typed(PlexusResource.class)
@Named("PrivilegePlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(PrivilegePlexusResource.RESOURCE_URI)
public class PrivilegePlexusResource
    extends AbstractPrivilegePlexusResource
{

  public static final String RESOURCE_URI = "/privileges/{" + PRIVILEGE_ID_KEY + "}";

  protected static final String PRIVILEGE_SOURCE = "default";

  public PrivilegePlexusResource() {
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
    return new PathProtectionDescriptor("/privileges/*", "authcBasic,perms[security:privileges]");
  }

  protected String getPrivilegeId(Request request) {
    return getRequestAttribute(request, PRIVILEGE_ID_KEY);
  }

  /**
   * Retrieves the details of a security privilege.
   *
   * @param privilegeId The Id of the privilege.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PrivilegeStatusResourceResponse.class, pathParams = {@PathParam("privilegeId")})
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    PrivilegeStatusResourceResponse result = new PrivilegeStatusResourceResponse();

    Privilege priv = null;

    try {
      AuthorizationManager authzManager = getSecuritySystem().getAuthorizationManager(PRIVILEGE_SOURCE);
      priv = authzManager.getPrivilege(getPrivilegeId(request));
    }
    catch (NoSuchPrivilegeException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Privilege could not be found.");
    }
    catch (NoSuchAuthorizationManagerException e) {
      this.getLogger().warn("Could not found AuthorizationManager: " + PRIVILEGE_SOURCE, e);
      // we should not ever get here
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Authorization Manager for: "
          + PRIVILEGE_SOURCE + " could not be found.");
    }

    result.setData(securityToRestModel(priv, request, false));

    return result;
  }

  /**
   * Removes a security privilege.
   *
   * @param privilegeId The Id of the privilege to be removed.
   */
  @Override
  @DELETE
  @ResourceMethodSignature(pathParams = {@PathParam("privilegeId")})
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    Privilege priv;

    try {
      AuthorizationManager authzManager = getSecuritySystem().getAuthorizationManager(PRIVILEGE_SOURCE);

      priv = authzManager.getPrivilege(getPrivilegeId(request));

      if (priv.getType().equals(ApplicationPrivilegeDescriptor.TYPE)) {
        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
            "Cannot delete an application type privilege");
      }
      else {
        authzManager.deletePrivilege(getPrivilegeId(request));
      }
    }
    catch (NoSuchPrivilegeException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
    }
    catch (NoSuchAuthorizationManagerException e) {
      this.getLogger().warn("Could not found AuthorizationManager: " + PRIVILEGE_SOURCE, e);
      // we should not ever get here
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Authorization Manager for: "
          + PRIVILEGE_SOURCE + " could not be found.");
    }
  }

}
