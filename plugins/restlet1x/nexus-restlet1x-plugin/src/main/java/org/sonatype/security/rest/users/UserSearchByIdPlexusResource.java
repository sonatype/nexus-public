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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource that searches for users based on a users source and partial user Id (user Id starts with xxx).
 *
 * @author bdemers
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserSearchByIdPlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserSearchByIdPlexusResource.RESOURCE_URI)
public class UserSearchByIdPlexusResource
    extends AbstractUserSearchPlexusResource
{
  public static final String USER_ID_KEY = "userId";

  public static final String RESOURCE_URI = "/user_search/{" + USER_SOURCE_KEY + "}/{" + USER_ID_KEY + "}";

  public UserSearchByIdPlexusResource() {
    setModifiable(false);
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/user_search/*/*", "authcBasic,perms[security:users]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Returns a list of users in which the source and partial user id matches the parameters.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   * @param userId   The Id of the user.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PlexusUserListResourceResponse.class, pathParams = {
      @PathParam("userId"),
      @PathParam("sourceId")
  })
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {

    UserSearchCriteria criteria = new UserSearchCriteria();
    criteria.setUserId(this.getSearchArg(request));
    criteria.setSource(this.getUserSource(request));

    return this.search(criteria);
  }

  protected String getSearchArg(Request request) {
    return getRequestAttribute(request, USER_ID_KEY);
  }

}
