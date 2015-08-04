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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * REST resource to email the user his/her user Id.
 *
 * @author tstevens
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserForgotIdPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserForgotIdPlexusResource.RESOURCE_URI)
public class UserForgotIdPlexusResource
    extends AbstractUserPlexusResource
{

  public static final String RESOURCE_URI = "/users_forgotid/{" + USER_EMAIL_KEY + "}";

  public UserForgotIdPlexusResource() {
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
    return new PathProtectionDescriptor("/users_forgotid/*", "authcBasic,perms[security:usersforgotid]");
  }

  /**
   * Email user his/her user Id.
   *
   * @param email The email address of the user.
   */
  @Override
  @POST
  @ResourceMethodSignature(pathParams = {@PathParam("email")})
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    final String email = getRequestAttribute(request, USER_EMAIL_KEY);

    try {
      getSecuritySystem().forgotUsername(email);

      response.setStatus(Status.SUCCESS_ACCEPTED);
    }
    catch (UserNotFoundException e) {
      getLogger().debug("Invalid email received: " + email, e);

      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Email address not found!");
    }
    // don't return anything because we are setting the status to 202
    return null;
  }

}
