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
package org.sonatype.nexus.rest.authentication;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.security.rest.authentication.AbstractLoginPlexusResource;
import org.sonatype.security.rest.model.AuthenticationLoginResourceResponse;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * The login resource handler. It creates a user token.
 *
 * @author bdemers
 */
@Named
@Singleton
@Path(AbstractLoginPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class NexusLoginPlexusResource
    extends AbstractLoginPlexusResource
{

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    // this is the ONLY resource using authcNxBasic, as the UI can't receive 401 errors from teh server
    // as the browser login pops up, which is no good in this case
    return new PathProtectionDescriptor(getResourceUri(), "authcNxBasic,perms[nexus:authentication]");
  }

  /**
   * Login to the application, will return a set of permissions available to the specified user.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = AuthenticationLoginResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return super.get(context, request, response, variant);
  }
}
