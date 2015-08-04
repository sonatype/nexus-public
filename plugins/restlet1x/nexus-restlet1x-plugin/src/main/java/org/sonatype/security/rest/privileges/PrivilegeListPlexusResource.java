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

import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.rest.model.PrivilegeListResourceResponse;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Handles the GET request for the Security privileges.
 *
 * @author tstevens
 */
@Singleton
@Typed(PlexusResource.class)
@Named("PrivilegeListPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(PrivilegeListPlexusResource.RESOURCE_URI)
public class PrivilegeListPlexusResource
    extends AbstractPrivilegePlexusResource
{

  public static final String RESOURCE_URI = "/privileges";

  public PrivilegeListPlexusResource() {
    setRequireStrictChecking(false);
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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[security:privileges]");
  }

  /**
   * Retrieves the list of security privileges.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PrivilegeListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    PrivilegeListResourceResponse result = new PrivilegeListResourceResponse();

    Set<Privilege> privs = getSecuritySystem().listPrivileges();

    for (Privilege priv : privs) {
      PrivilegeStatusResource res = securityToRestModel(priv, request, true);

      if (res != null) {
        result.addData(res);
      }
    }

    return result;
  }
}
